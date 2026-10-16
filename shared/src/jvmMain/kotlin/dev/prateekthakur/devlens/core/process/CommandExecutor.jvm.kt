package dev.prateekthakur.devlens.core.process

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

actual class CommandExecutor actual constructor() {

    actual suspend fun run(
        executable: String,
        args: List<String>,
        workingDir: String?,
        timeoutMillis: Long,
    ): CommandResult = withContext(Dispatchers.IO) {
        val fullCommand = listOf(executable) + args
        val started = System.currentTimeMillis()
        val builder = ProcessBuilder(fullCommand).apply {
            workingDir?.let { directory(File(it)) }
            redirectErrorStream(false)
        }

        val process = try {
            builder.start()
        } catch (e: Exception) {
            return@withContext CommandResult(
                command = fullCommand,
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Failed to start process",
                durationMillis = System.currentTimeMillis() - started,
            )
        }

        val stdout = process.inputStream.bufferedReader()
        val stderr = process.errorStream.bufferedReader()

        val completed = withTimeoutOrNull(timeoutMillis) {
            val out = stdout.readText()
            val err = stderr.readText()
            val code = process.waitFor()
            Triple(out, err, code)
        }

        if (completed == null) {
            process.destroyForcibly()
            return@withContext CommandResult(
                command = fullCommand,
                exitCode = -1,
                stdout = "",
                stderr = "Timed out after ${timeoutMillis}ms",
                durationMillis = System.currentTimeMillis() - started,
            )
        }

        val (out, err, code) = completed
        CommandResult(
            command = fullCommand,
            exitCode = code,
            stdout = out,
            stderr = err,
            durationMillis = System.currentTimeMillis() - started,
        )
    }

    actual suspend fun startStreaming(
        executable: String,
        args: List<String>,
        onLine: (String) -> Unit,
    ): StreamHandle = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(listOf(executable) + args)
            .redirectErrorStream(true)
            .start()

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            process.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) onLine(line)
            }
        }

        StreamHandle {
            withContext(Dispatchers.IO) { process.destroy() }
        }
    }

    actual suspend fun which(executable: String): String? = withContext(Dispatchers.IO) {
        val pathDirs = System.getenv("PATH")?.split(File.pathSeparator).orEmpty()
        val candidateNames = if (System.getProperty("os.name").lowercase().contains("win")) {
            listOf("$executable.exe", "$executable.bat", "$executable.cmd", executable)
        } else {
            listOf(executable)
        }
        for (dir in pathDirs) {
            for (name in candidateNames) {
                val file = File(dir, name)
                if (file.isFile && file.canExecute()) return@withContext file.absolutePath
            }
        }
        null
    }
}
