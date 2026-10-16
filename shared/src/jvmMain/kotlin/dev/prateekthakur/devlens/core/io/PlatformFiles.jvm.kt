package dev.prateekthakur.devlens.core.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class PlatformFiles actual constructor() {
    actual suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) { File(path).exists() }

    actual suspend fun readText(path: String): String? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.isFile) runCatching { file.readText() }.getOrNull() else null
    }

    actual suspend fun writeText(path: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { File(path).also { it.parentFile?.mkdirs() }.writeText(content) }
    }

    actual suspend fun listEntries(directoryPath: String): List<FileEntry> = withContext(Dispatchers.IO) {
        File(directoryPath).listFiles()?.map { FileEntry(it.name, it.isDirectory) } ?: emptyList()
    }

    actual fun join(vararg parts: String): String = parts.joinToString(File.separator)
}
