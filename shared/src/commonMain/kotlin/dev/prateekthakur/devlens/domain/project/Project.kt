package dev.prateekthakur.devlens.domain.project

/** PRD §33 — a Project groups the source dir, APKs, devices, logs, crashes and sessions it owns. */
data class Project(
    val id: String,
    val name: String,
    val sourceDirectory: String?,
    val kind: ProjectKind,
)

enum class ProjectKind { ANDROID, FLUTTER, UNKNOWN }
