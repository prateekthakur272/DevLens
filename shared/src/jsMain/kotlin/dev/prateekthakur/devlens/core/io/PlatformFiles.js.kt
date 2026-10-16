package dev.prateekthakur.devlens.core.io

actual class PlatformFiles actual constructor() {
    actual suspend fun exists(path: String): Boolean = false
    actual suspend fun readText(path: String): String? = null
    actual suspend fun writeText(path: String, content: String): Result<Unit> =
        Result.failure(IllegalStateException("Not supported on this platform"))
    actual suspend fun listEntries(directoryPath: String): List<FileEntry> = emptyList()
    actual fun join(vararg parts: String): String = parts.joinToString("/")
}
