package dev.prateekthakur.devlens.domain.flutter

/** PRD §27-28 — Flutter project + dependency analysis. */
data class FlutterProjectInfo(
    val flutterVersion: String?,
    val dartVersion: String?,
    val dependencies: List<FlutterDependency>,
)

data class FlutterDependency(
    val name: String,
    val versionConstraint: String,
    val resolvedVersion: String?,
    val isDirect: Boolean,
    val isOutdated: Boolean = false,
)

interface FlutterProjectAnalyzer {
    suspend fun analyze(projectDir: String): Result<FlutterProjectInfo>
}

/** PRD §27-28 — reads pubspec.yaml/pubspec.lock directly; never modifies project files. */
class PubspecFlutterProjectAnalyzer(
    private val files: dev.prateekthakur.devlens.core.io.PlatformFiles = dev.prateekthakur.devlens.core.io.PlatformFiles(),
    private val executor: dev.prateekthakur.devlens.core.process.CommandExecutor = dev.prateekthakur.devlens.core.process.CommandExecutor(),
) : FlutterProjectAnalyzer {

    override suspend fun analyze(projectDir: String): Result<FlutterProjectInfo> {
        val pubspecPath = files.join(projectDir, "pubspec.yaml")
        val pubspecText = files.readText(pubspecPath)
            ?: return Result.failure(IllegalArgumentException("No pubspec.yaml found at $projectDir"))

        val lockedVersions = files.readText(files.join(projectDir, "pubspec.lock"))?.let { parseLockVersions(it) } ?: emptyMap()
        val direct = parseDependencySection(pubspecText, "dependencies:")
        val dev = parseDependencySection(pubspecText, "dev_dependencies:")

        val dependencies = (direct.map { it to true } + dev.map { it to false }).map { (dep, isDirect) ->
            val (name, constraint) = dep
            FlutterDependency(
                name = name,
                versionConstraint = constraint,
                resolvedVersion = lockedVersions[name],
                isDirect = isDirect,
            )
        }

        val flutterVersion = executor.run("flutter", listOf("--version")).stdout
            .lineSequence().firstOrNull { it.startsWith("Flutter ") }
            ?.removePrefix("Flutter ")?.substringBefore(' ')
        val dartVersion = executor.run("dart", listOf("--version")).let { it.stdout.ifBlank { it.stderr } }
            .substringAfter("version ", "").substringBefore(" ").ifBlank { null }

        return Result.success(
            FlutterProjectInfo(
                flutterVersion = flutterVersion,
                dartVersion = dartVersion,
                dependencies = dependencies,
            ),
        )
    }

    /** Extracts direct `name: constraint` children of a top-level YAML section (no external YAML parser). */
    private fun parseDependencySection(yaml: String, sectionHeader: String): List<Pair<String, String>> {
        val lines = yaml.lines()
        val startIndex = lines.indexOfFirst { it.trimEnd() == sectionHeader.removeSuffix(":") + ":" }
        if (startIndex < 0) return emptyList()

        val result = mutableListOf<Pair<String, String>>()
        for (i in (startIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val indent = line.takeWhile { it == ' ' }.length
            if (indent == 0) break // next top-level section
            if (indent != 2) continue // skip nested keys (e.g. sdk:/git:/path: sub-maps)
            val trimmed = line.trim()
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex < 0) continue
            val name = trimmed.substring(0, colonIndex).trim()
            val constraint = trimmed.substring(colonIndex + 1).trim().ifBlank { "(complex — see pubspec.yaml)" }
            result += name to constraint
        }
        return result
    }

    private fun parseLockVersions(lockYaml: String): Map<String, String> {
        val lines = lockYaml.lines()
        val versions = mutableMapOf<String, String>()
        var currentPackage: String? = null
        for (line in lines) {
            if (line.isBlank()) continue
            val indent = line.takeWhile { it == ' ' }.length
            val trimmed = line.trim()
            if (indent == 2 && trimmed.endsWith(":")) {
                currentPackage = trimmed.removeSuffix(":")
            } else if (indent == 4 && trimmed.startsWith("version:") && currentPackage != null) {
                versions[currentPackage] = trimmed.removePrefix("version:").trim().trim('"')
            }
        }
        return versions
    }
}
