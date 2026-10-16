package dev.prateekthakur.devlens.domain.logcat

/** PRD §21 — a single Logcat line. */
enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, FATAL }

data class LogEntry(
    val timestamp: String,
    val pid: Int,
    val tid: Int,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val packageName: String? = null,
)

/** Streams Logcat as it arrives; must never buffer the whole session in memory (PRD §42). */
interface LogcatSource {
    suspend fun start(serial: String, onEntry: (LogEntry) -> Unit)
    suspend fun stop()
}

/** PRD §21 — streams `adb logcat -v threadtime`, parsing each line incrementally. */
class AdbLogcatSource(
    private val executor: dev.prateekthakur.devlens.core.process.CommandExecutor = dev.prateekthakur.devlens.core.process.CommandExecutor(),
    private val sdkLocator: dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator =
        dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator(executor = executor),
) : LogcatSource {

    private var handle: dev.prateekthakur.devlens.core.process.StreamHandle? = null

    private val lineRegex = Regex(
        """^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEFS])\s+([^:]*):\s?(.*)$""",
    )

    override suspend fun start(serial: String, onEntry: (LogEntry) -> Unit) {
        val adb = sdkLocator.findAdb() ?: run {
            onEntry(errorEntry("ADB not found. Configure the SDK path in Settings."))
            return
        }
        handle = executor.startStreaming(adb, listOf("-s", serial, "logcat", "-v", "threadtime")) { line ->
            parseLine(line)?.let(onEntry)
        }
    }

    override suspend fun stop() {
        handle?.cancel()
        handle = null
    }

    private fun parseLine(line: String): LogEntry? {
        val match = lineRegex.find(line) ?: return null
        val level = when (match.groupValues[4]) {
            "V" -> LogLevel.VERBOSE
            "D" -> LogLevel.DEBUG
            "I" -> LogLevel.INFO
            "W" -> LogLevel.WARN
            "E" -> LogLevel.ERROR
            "F" -> LogLevel.FATAL
            else -> LogLevel.INFO
        }
        return LogEntry(
            timestamp = match.groupValues[1],
            pid = match.groupValues[2].toIntOrNull() ?: 0,
            tid = match.groupValues[3].toIntOrNull() ?: 0,
            level = level,
            tag = match.groupValues[5].trim(),
            message = match.groupValues[6],
        )
    }

    private fun errorEntry(message: String) = LogEntry(
        timestamp = "",
        pid = 0,
        tid = 0,
        level = LogLevel.ERROR,
        tag = "DevLens",
        message = message,
    )
}
