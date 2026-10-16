package dev.prateekthakur.devlens.domain.decompile

/** One readable file produced by decompilation; [relativePath] uses '/' and is relative to [DecompiledProject.outputDir]. */
data class DecompiledFile(val relativePath: String, val sizeBytes: Long)

data class DecompiledProject(val outputDir: String, val files: List<DecompiledFile>) {
    val javaFileCount: Int get() = files.count { it.relativePath.endsWith(".java") }
}

/** Reverse-engineers an APK back into readable Java sources and decoded resources. */
interface ApkDecompiler {
    /** [onProgress] receives 0f..1f and may be called from background threads. */
    suspend fun decompile(apkPath: String, onProgress: (Float) -> Unit): Result<DecompiledProject>
}
