package dev.prateekthakur.devlens.infrastructure.apk.dex

import dev.prateekthakur.devlens.domain.dex.DexClassAnalyzer
import dev.prateekthakur.devlens.domain.dex.DexClassInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/** PRD §16/§27 (SDK fingerprinting) — reads every classesN.dex in the APK, no decompiler required. */
class ApkDexClassAnalyzer : DexClassAnalyzer {

    private val dexEntryRegex = Regex("""(?:.*/)?classes\d*\.dex""")
    private val maxDexBytes = 96L * 1024 * 1024 // guard against a hostile/oversized entry (PRD §40)

    override suspend fun analyze(apkPath: String): Result<List<DexClassInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val classes = mutableListOf<DexClassInfo>()
            ZipFile(File(apkPath)).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory || !dexEntryRegex.matches(entry.name)) continue
                    if (entry.size in 0..maxDexBytes) {
                        val bytes = zip.getInputStream(entry).use { it.readBytes() }
                        classes += DexFileReader.readClasses(bytes, entry.name.substringAfterLast('/'))
                    }
                }
            }
            classes
        }
    }
}
