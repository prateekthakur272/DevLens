package dev.prateekthakur.devlens.core.model

enum class ToolStatus { READY, WARNING, NOT_FOUND }

/**
 * Status of one required developer tool (ADB, SDK, Java, Flutter, Dart, Gradle, Git).
 * PRD §11 — DevLens must never silently modify the developer environment; it only reports.
 */
data class ToolCheck(
    val name: String,
    val status: ToolStatus,
    val version: String? = null,
    val path: String? = null,
    val minSupportedVersion: String? = null,
    val recommendedVersion: String? = null,
    val message: String? = null,
)
