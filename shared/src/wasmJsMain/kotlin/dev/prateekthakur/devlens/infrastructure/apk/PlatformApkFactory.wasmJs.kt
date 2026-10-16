package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.domain.apk.ApkAnalyzer
import dev.prateekthakur.devlens.domain.apk.ApkFileEntry
import dev.prateekthakur.devlens.domain.apk.ApkMetadata
import dev.prateekthakur.devlens.domain.dex.DexClassAnalyzer
import dev.prateekthakur.devlens.domain.dex.DexClassInfo
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.secrets.ApkSecretsAnalyzer
import dev.prateekthakur.devlens.domain.secrets.SecretFinding
import dev.prateekthakur.devlens.domain.size.SizeAnalyzer
import dev.prateekthakur.devlens.domain.size.SizeReport

private val unsupported = IllegalStateException("APK file analysis is not supported on this platform. Use the desktop app.")

actual fun createZipBasedApkAnalyzer(manifestAnalyzer: ManifestAnalyzer): ApkAnalyzer = object : ApkAnalyzer {
    override suspend fun analyze(apkPath: String): Result<ApkMetadata> = Result.failure(unsupported)
    override suspend fun listFiles(apkPath: String): Result<List<ApkFileEntry>> = Result.failure(unsupported)
}

actual fun createZipBasedSizeAnalyzer(): SizeAnalyzer = object : SizeAnalyzer {
    override suspend fun analyze(apkPath: String): Result<SizeReport> = Result.failure(unsupported)
}

actual fun createDexClassAnalyzer(): DexClassAnalyzer = object : DexClassAnalyzer {
    override suspend fun analyze(apkPath: String): Result<List<DexClassInfo>> = Result.failure(unsupported)
}

actual fun createApkSecretsAnalyzer(): ApkSecretsAnalyzer = object : ApkSecretsAnalyzer {
    override suspend fun analyze(apkPath: String): Result<List<SecretFinding>> = Result.failure(unsupported)
}
