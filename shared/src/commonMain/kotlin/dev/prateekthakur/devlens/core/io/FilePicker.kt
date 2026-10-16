package dev.prateekthakur.devlens.core.io

/** Native "open file" / "choose directory" dialogs (PRD §12, §27 — Open file / Open project). */
expect object FilePicker {
    suspend fun pickFile(title: String, extensionFilter: String? = null): String?
    suspend fun pickDirectory(title: String): String?
    suspend fun pickSaveFile(title: String, defaultName: String): String?
}
