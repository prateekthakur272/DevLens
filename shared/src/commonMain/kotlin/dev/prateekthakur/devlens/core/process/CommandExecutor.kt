package dev.prateekthakur.devlens.core.process

/**
 * Result of running an external tool (adb, flutter, gradle, git, ...).
 * All external tool access must go through [CommandExecutor] (PRD §44) —
 * never build shell strings from untrusted input (PRD §40).
 */
data class CommandResult(
    val command: List<String>,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMillis: Long,
) {
    val isSuccess: Boolean get() = exitCode == 0
}

/** Handle to a long-running streamed process (e.g. `adb logcat`). */
class StreamHandle(private val cancelFn: suspend () -> Unit) {
    suspend fun cancel() = cancelFn()
}

/**
 * Executes external developer tools as argument vectors (never shell strings).
 * Desktop (JVM) uses ProcessBuilder; other targets have no process access and
 * report failure rather than silently no-op-ing.
 */
expect class CommandExecutor() {
    suspend fun run(
        executable: String,
        args: List<String> = emptyList(),
        workingDir: String? = null,
        timeoutMillis: Long = 10_000,
    ): CommandResult

    /** Resolves the absolute path of an executable on PATH, or null if not found. */
    suspend fun which(executable: String): String?

    /** Starts a long-running process and streams stdout line-by-line until [StreamHandle.cancel] is called. */
    suspend fun startStreaming(
        executable: String,
        args: List<String> = emptyList(),
        onLine: (String) -> Unit,
    ): StreamHandle
}
