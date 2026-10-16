package dev.prateekthakur.devlens.domain.crash

/** PRD §23 — extracted crash information. Never present speculation as confirmed fact. */
enum class CauseConfidence { ROOT_CAUSE, LIKELY_CAUSE, POSSIBLE_CAUSE }

data class CrashCause(
    val description: String,
    val confidence: CauseConfidence,
    val evidence: String,
)

data class CrashReport(
    val processName: String,
    val packageName: String,
    val threadName: String,
    val exceptionType: String,
    val exceptionMessage: String,
    val stackTrace: List<String>,
    val causes: List<CrashCause>,
)

interface CrashAnalyzer {
    fun analyze(rawLog: String): Result<CrashReport>
}

/** PRD §23 — parses a Java/Kotlin stack trace (from Logcat or a pasted crash report). */
class RegexCrashAnalyzer : CrashAnalyzer {

    private val processRegex = Regex("""Process:\s*([\w.:]+),\s*PID:\s*(\d+)""")
    private val fatalThreadRegex = Regex("""FATAL EXCEPTION:\s*(\S+)""")
    private val threadExceptionRegex = Regex("""Exception in thread "([^"]+)"""")
    private val exceptionLineRegex = Regex("""^((?:[\w]+\.)*[\w$]+(?:Exception|Error|Throwable))(?::\s*(.*))?$""")
    private val causedByRegex = Regex("""^Caused by:\s*((?:[\w]+\.)*[\w$]+(?:Exception|Error|Throwable))(?::\s*(.*))?$""")
    private val frameRegex = Regex("""^\s*at\s+(.+)$""")

    override fun analyze(rawLog: String): Result<CrashReport> {
        val lines = rawLog.lines()
        if (lines.none { exceptionLineRegex.matches(it.trim()) || causedByRegex.matches(it.trim()) }) {
            return Result.failure(IllegalArgumentException("No recognizable exception found in the provided text."))
        }

        val process = processRegex.find(rawLog)
        val threadName = fatalThreadRegex.find(rawLog)?.groupValues?.get(1)
            ?: threadExceptionRegex.find(rawLog)?.groupValues?.get(1)
            ?: "unknown"

        data class Block(val type: String, val message: String, val frames: MutableList<String> = mutableListOf())

        val blocks = mutableListOf<Block>()
        var current: Block? = null
        for (raw in lines) {
            val line = raw.trim()
            val causedBy = causedByRegex.find(line)
            if (causedBy != null) {
                current = Block(causedBy.groupValues[1], causedBy.groupValues.getOrElse(2) { "" })
                blocks += current
                continue
            }
            if (current == null) {
                val primary = exceptionLineRegex.find(line)
                if (primary != null) {
                    current = Block(primary.groupValues[1], primary.groupValues.getOrElse(2) { "" })
                    blocks += current
                    continue
                }
            }
            val frame = frameRegex.find(line)
            if (frame != null) current?.frames?.add(frame.groupValues[1])
        }

        if (blocks.isEmpty()) return Result.failure(IllegalArgumentException("No recognizable exception found in the provided text."))

        val primary = blocks.first()
        val deepest = blocks.last()

        val causes = mutableListOf<CrashCause>()
        if (blocks.size > 1) {
            causes += CrashCause(
                description = "${deepest.type}${deepest.message.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}",
                confidence = CauseConfidence.ROOT_CAUSE,
                evidence = deepest.frames.firstOrNull()?.let { "at $it" } ?: "Innermost \"Caused by\" exception.",
            )
            causes += CrashCause(
                description = "${primary.type}${primary.message.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}",
                confidence = CauseConfidence.LIKELY_CAUSE,
                evidence = "Top-level exception wrapping the root cause.",
            )
        } else {
            causes += CrashCause(
                description = "${primary.type}${primary.message.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}",
                confidence = CauseConfidence.ROOT_CAUSE,
                evidence = primary.frames.firstOrNull()?.let { "at $it" } ?: "No stack frames captured.",
            )
        }

        return Result.success(
            CrashReport(
                processName = process?.groupValues?.get(1) ?: "unknown",
                packageName = process?.groupValues?.get(1) ?: "unknown",
                threadName = threadName,
                exceptionType = primary.type,
                exceptionMessage = primary.message,
                stackTrace = primary.frames,
                causes = causes,
            ),
        )
    }
}
