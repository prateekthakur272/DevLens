package dev.prateekthakur.devlens.infrastructure.decompile

import dev.prateekthakur.devlens.domain.decompile.ApkDecompiler
import dev.prateekthakur.devlens.domain.decompile.DecompiledProject

actual fun createApkDecompiler(): ApkDecompiler = object : ApkDecompiler {
    override suspend fun decompile(apkPath: String, onProgress: (Float) -> Unit): Result<DecompiledProject> =
        Result.failure(IllegalStateException("Decompiling is only available in the desktop app."))
}
