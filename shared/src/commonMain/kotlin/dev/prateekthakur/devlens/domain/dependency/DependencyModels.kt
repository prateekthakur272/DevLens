package dev.prateekthakur.devlens.domain.dependency

/** PRD §28 — dependency graph node, shared across Flutter/Gradle dependency analysis. */
data class DependencyNode(
    val name: String,
    val version: String,
    val children: List<DependencyNode> = emptyList(),
)

interface DependencyAnalyzer {
    suspend fun analyze(projectDir: String): Result<List<DependencyNode>>
}

/** PRD §28 — real dependency graph via `flutter pub deps --style=tree`. */
class FlutterPubDependencyAnalyzer(
    private val executor: dev.prateekthakur.devlens.core.process.CommandExecutor = dev.prateekthakur.devlens.core.process.CommandExecutor(),
) : DependencyAnalyzer {

    private val markers = listOf("│   ", "    ", "├── ", "└── ")
    private val branchMarkers = setOf("├── ", "└── ")

    override suspend fun analyze(projectDir: String): Result<List<DependencyNode>> {
        val result = executor.run("flutter", listOf("pub", "deps", "--style=tree"), workingDir = projectDir, timeoutMillis = 30_000)
        if (!result.isSuccess) {
            return Result.failure(IllegalStateException(result.stderr.ifBlank { "flutter pub deps failed" }))
        }

        class Builder(val name: String, val version: String) {
            val children = mutableListOf<Builder>()
            fun toNode(): DependencyNode = DependencyNode(name, version, children.map { it.toNode() })
        }

        val roots = mutableListOf<Builder>()
        // stack entries: (depth level this children list accepts nodes at, that mutable children list)
        val stack = ArrayDeque<Pair<Int, MutableList<Builder>>>()
        stack.addLast(1 to roots)

        for (rawLine in result.stdout.lineSequence()) {
            if (rawLine.isBlank()) continue
            val depth = depthOf(rawLine)
            if (depth == 0) continue // root package / SDK header lines

            val content = rawLine.substring((depth * 4).coerceAtMost(rawLine.length)).trim()
            if (content.isBlank()) continue
            val parts = content.split(' ', limit = 2)
            val node = Builder(name = parts[0], version = parts.getOrElse(1) { "" }.trim())

            while (stack.size > 1 && stack.last().first > depth) stack.removeLast()
            stack.last().second += node
            stack.addLast(depth + 1 to node.children)
        }

        return Result.success(roots.map { it.toNode() })
    }

    private fun depthOf(line: String): Int {
        var index = 0
        var depth = 0
        while (index + 4 <= line.length) {
            val group = line.substring(index, index + 4)
            if (group in markers) {
                depth++
                index += 4
                if (group in branchMarkers) break
            } else {
                break
            }
        }
        return depth
    }
}
