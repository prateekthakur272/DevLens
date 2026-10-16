package dev.prateekthakur.devlens.core.io

data class FileEntry(val name: String, val isDirectory: Boolean)

/** Minimal cross-platform filesystem access used by project analyzers and SDK detection. */
expect class PlatformFiles() {
    suspend fun exists(path: String): Boolean
    suspend fun readText(path: String): String?
    suspend fun writeText(path: String, content: String): Result<Unit>
    suspend fun listEntries(directoryPath: String): List<FileEntry>
    fun join(vararg parts: String): String
}
