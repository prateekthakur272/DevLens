package dev.prateekthakur.devlens.core.io

actual object FilePicker {
    actual suspend fun pickFile(title: String, extensionFilter: String?): String? = null
    actual suspend fun pickDirectory(title: String): String? = null
    actual suspend fun pickSaveFile(title: String, defaultName: String): String? = null
}
