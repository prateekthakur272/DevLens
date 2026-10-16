package dev.prateekthakur.devlens.domain.report

/** PRD §31 — export formats for a generated report. */
enum class ReportFormat { MARKDOWN, JSON, PLAIN_TEXT }

data class ReportSection(val title: String, val body: String)

data class Report(
    val title: String,
    val sections: List<ReportSection>,
)

interface ReportGenerator {
    fun render(report: Report, format: ReportFormat): String
}

/** PRD §31 — Markdown / JSON / plain-text export of any analysis session's findings. */
class DefaultReportGenerator : ReportGenerator {

    override fun render(report: Report, format: ReportFormat): String = when (format) {
        ReportFormat.MARKDOWN -> renderMarkdown(report)
        ReportFormat.PLAIN_TEXT -> renderPlainText(report)
        ReportFormat.JSON -> renderJson(report)
    }

    private fun renderMarkdown(report: Report): String = buildString {
        appendLine("# ${report.title}")
        appendLine()
        for (section in report.sections) {
            appendLine("## ${section.title}")
            appendLine()
            appendLine(section.body)
            appendLine()
        }
    }

    private fun renderPlainText(report: Report): String = buildString {
        appendLine(report.title)
        appendLine("=".repeat(report.title.length))
        appendLine()
        for (section in report.sections) {
            appendLine(section.title)
            appendLine("-".repeat(section.title.length))
            appendLine(section.body)
            appendLine()
        }
    }

    private fun renderJson(report: Report): String = buildString {
        append("{\"title\":").append(jsonString(report.title))
        append(",\"sections\":[")
        report.sections.forEachIndexed { index, section ->
            if (index > 0) append(",")
            append("{\"title\":").append(jsonString(section.title))
            append(",\"body\":").append(jsonString(section.body))
            append("}")
        }
        append("]}")
    }

    private fun jsonString(value: String): String = buildString {
        append('"')
        for (c in value) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c.code < 0x20) {
                    append("\\u")
                    append(c.code.toString(16).padStart(4, '0'))
                } else {
                    append(c)
                }
            }
        }
        append('"')
    }
}
