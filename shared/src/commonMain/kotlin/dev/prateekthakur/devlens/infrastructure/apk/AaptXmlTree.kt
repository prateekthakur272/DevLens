package dev.prateekthakur.devlens.infrastructure.apk

/** A node in the tree produced by `aapt2 dump xmltree`. */
class AaptXmlNode(val name: String) {
    val attributes = mutableMapOf<String, String>()
    val children = mutableListOf<AaptXmlNode>()

    fun attr(name: String): String? = attributes[name]

    fun childrenNamed(name: String): List<AaptXmlNode> = children.filter { it.name == name }
}

/**
 * Parses the indented text produced by `aapt2 dump xmltree <apk> --file AndroidManifest.xml`.
 * Attribute lines always immediately follow their owning element line, before any child
 * elements or the next sibling, so attributes attach to the current top-of-stack element
 * regardless of exact column indentation.
 */
object AaptXmlTreeParser {

    private val elementRegex = Regex("""^(\s*)E:\s+(\S+)""")
    private val resourceIdRegex = Regex("""\(0x[0-9a-fA-F]+\)""")

    fun parse(xmlTreeOutput: String): AaptXmlNode? {
        val root = AaptXmlNode("__root__")
        val stack = ArrayDeque<Pair<Int, AaptXmlNode>>()
        stack.addLast(-1 to root)

        for (rawLine in xmlTreeOutput.lineSequence()) {
            val elementMatch = elementRegex.find(rawLine)
            if (elementMatch != null) {
                val indent = elementMatch.groupValues[1].length
                val name = elementMatch.groupValues[2]
                while (stack.size > 1 && stack.last().first >= indent) stack.removeLast()
                val node = AaptXmlNode(name)
                stack.last().second.children += node
                stack.addLast(indent to node)
                continue
            }

            val trimmed = rawLine.trimStart()
            if (trimmed.startsWith("A:")) {
                parseAttribute(trimmed.removePrefix("A:").trim())?.let { (key, value) ->
                    stack.last().second.attributes[key] = value
                }
            }
        }

        return root.children.firstOrNull { it.name == "manifest" }
    }

    private fun parseAttribute(text: String): Pair<String, String>? {
        val withoutResId = resourceIdRegex.replace(text, "")
        val eqIndex = withoutResId.indexOf('=')
        if (eqIndex < 0) return null

        val rawName = withoutResId.substring(0, eqIndex).trim()
        val name = rawName.substringAfterLast(':')

        var rawValue = withoutResId.substring(eqIndex + 1).trim()
        val value = if (rawValue.startsWith('"')) {
            val end = rawValue.indexOf('"', 1)
            if (end > 0) rawValue.substring(1, end) else rawValue.trim('"')
        } else {
            rawValue.substringBefore(' ').trim()
        }
        return name to value
    }
}
