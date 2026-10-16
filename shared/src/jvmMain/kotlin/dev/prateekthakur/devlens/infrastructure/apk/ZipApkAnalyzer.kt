package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.domain.apk.ApkAnalyzer
import dev.prateekthakur.devlens.domain.apk.ApkFileCategory
import dev.prateekthakur.devlens.domain.apk.ApkFileEntry
import dev.prateekthakur.devlens.domain.apk.ApkMetadata
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** PRD §12-13 — combines manifest facts (via aapt2) with zip-derived structure facts. */
class ZipApkAnalyzer(
    private val manifestAnalyzer: ManifestAnalyzer,
) : ApkAnalyzer {

    override suspend fun analyze(apkPath: String): Result<ApkMetadata> {
        val manifest = manifestAnalyzer.analyze(apkPath).getOrElse { return Result.failure(it) }
        val entries = listFiles(apkPath).getOrElse { return Result.failure(it) }

        val abis = entries
            .filter { it.category == ApkFileCategory.NATIVE_LIB }
            .mapNotNull { Regex("""(?:^|/)lib/([^/]+)/""").find(it.path)?.groupValues?.get(1) }
            .distinct()

        return Result.success(
            ApkMetadata(
                packageName = manifest.packageName,
                versionName = manifest.versionName ?: "",
                versionCode = manifest.versionCode ?: 0,
                minSdk = manifest.minSdk ?: 0,
                targetSdk = manifest.targetSdk ?: 0,
                sizeBytes = withContext(Dispatchers.IO) { File(apkPath).length() },
                abis = abis,
                dexFileCount = entries.count { it.category == ApkFileCategory.DEX },
                nativeLibraryCount = entries.count { it.category == ApkFileCategory.NATIVE_LIB },
            ),
        )
    }

    override suspend fun listFiles(apkPath: String): Result<List<ApkFileEntry>> = withContext(Dispatchers.IO) {
        runCatching { ZipInspector.listEntries(apkPath) }
    }
}
