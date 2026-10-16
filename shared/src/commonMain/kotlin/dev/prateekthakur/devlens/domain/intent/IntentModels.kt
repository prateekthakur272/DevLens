package dev.prateekthakur.devlens.domain.intent

/** PRD §25 — user-constructed intent under test. */
data class IntentSpec(
    val action: String?,
    val dataUri: String?,
    val mimeType: String?,
    val targetPackage: String?,
    val component: String?,
    val categories: List<String> = emptyList(),
    val flags: List<String> = emptyList(),
    val extras: Map<String, String> = emptyMap(),
)

data class IntentResolution(
    val packageName: String,
    val activityName: String,
)

interface IntentTester {
    suspend fun resolve(spec: IntentSpec, serial: String): Result<List<IntentResolution>>
    suspend fun launch(spec: IntentSpec, serial: String): Result<String>
}

/**
 * PRD §25 — builds `am start` / `cmd package resolve-activity` invocations as argument
 * vectors (never a shell string, per PRD §40) and runs them through ADB.
 */
class AdbIntentTester(
    private val executor: dev.prateekthakur.devlens.core.process.CommandExecutor = dev.prateekthakur.devlens.core.process.CommandExecutor(),
    private val sdkLocator: dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator =
        dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator(executor = executor),
) : IntentTester {

    private fun buildArgs(spec: IntentSpec): List<String> {
        val args = mutableListOf<String>()
        spec.action?.let { args += listOf("-a", it) }
        if (spec.dataUri != null && spec.mimeType != null) {
            args += listOf("-d", spec.dataUri, "-t", spec.mimeType)
        } else {
            spec.dataUri?.let { args += listOf("-d", it) }
            spec.mimeType?.let { args += listOf("-t", it) }
        }
        spec.categories.forEach { args += listOf("-c", it) }
        spec.component?.let { args += listOf("-n", it) }
        spec.targetPackage?.takeIf { spec.component == null }?.let { args += listOf("-p", it) }
        spec.flags.forEach { args += listOf("-f", it) }
        spec.extras.forEach { (key, value) -> args += listOf("--es", key, value) }
        return args
    }

    override suspend fun resolve(spec: IntentSpec, serial: String): Result<List<IntentResolution>> {
        val adb = sdkLocator.findAdb() ?: return Result.failure(IllegalStateException("ADB not found."))
        val args = listOf("-s", serial, "shell", "cmd", "package", "resolve-activity", "--brief") + buildArgs(spec)
        val result = executor.run(adb, args)
        if (!result.isSuccess) return Result.failure(IllegalStateException(result.stderr.ifBlank { "Intent resolution failed" }))

        val componentLine = result.stdout.lineSequence().lastOrNull { it.contains('/') && !it.contains(' ') }
        if (componentLine == null || result.stdout.contains("No activity found")) {
            return Result.success(emptyList())
        }
        val (pkg, activity) = componentLine.trim().split('/', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        return Result.success(listOf(IntentResolution(packageName = pkg, activityName = activity)))
    }

    override suspend fun launch(spec: IntentSpec, serial: String): Result<String> {
        val adb = sdkLocator.findAdb() ?: return Result.failure(IllegalStateException("ADB not found."))
        val args = listOf("-s", serial, "shell", "am", "start") + buildArgs(spec)
        val result = executor.run(adb, args)
        return if (result.isSuccess) {
            Result.success(result.stdout)
        } else {
            Result.failure(IllegalStateException(result.stderr.ifBlank { result.stdout }.ifBlank { "am start failed" }))
        }
    }
}
