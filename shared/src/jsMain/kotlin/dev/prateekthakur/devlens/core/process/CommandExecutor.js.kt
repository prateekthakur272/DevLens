package dev.prateekthakur.devlens.core.process

/** Web has no process-execution access; DevLens targets desktop (PRD §5). */
actual class CommandExecutor actual constructor() {
    actual suspend fun run(
        executable: String,
        args: List<String>,
        workingDir: String?,
        timeoutMillis: Long,
    ): CommandResult = CommandResult(
        command = listOf(executable) + args,
        exitCode = -1,
        stdout = "",
        stderr = "Command execution is not supported on this platform",
        durationMillis = 0,
    )

    actual suspend fun which(executable: String): String? = null

    actual suspend fun startStreaming(
        executable: String,
        args: List<String>,
        onLine: (String) -> Unit,
    ): StreamHandle = StreamHandle {}
}
