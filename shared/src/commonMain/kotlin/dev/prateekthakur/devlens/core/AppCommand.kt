package dev.prateekthakur.devlens.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.prateekthakur.devlens.domain.report.ReportFormat

/** The report formats offered by "Export report" (PDF has no [ReportFormat]; it is rendered separately). */
enum class ExportKind(val label: String, val extension: String, val format: ReportFormat?) {
    PDF("PDF document (.pdf)", "pdf", null),
    MARKDOWN("Markdown (.md)", "md", ReportFormat.MARKDOWN),
    JSON("JSON (.json)", "json", ReportFormat.JSON),
    TEXT("Plain text (.txt)", "txt", ReportFormat.PLAIN_TEXT),
}

/** Actions the host menu bar can ask the analysis screen to perform. */
sealed interface AppCommand {
    data object OpenFile : AppCommand
    data object Reanalyze : AppCommand
    data object Decompile : AppCommand
    data object OpenEditor : AppCommand
    data class Export(val kind: ExportKind) : AppCommand
    data class SelectTab(val index: Int) : AppCommand
}

/** What the menu bar needs to know to enable/disable its items. Written by the analysis screen. */
class AppSession {
    var hasFile by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var decompiling by mutableStateOf(false)
    var decompiledDir by mutableStateOf<String?>(null)
}
