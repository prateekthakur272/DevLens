package dev.prateekthakur.devlens.ui.apk

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/** Editor palette (dark, VS Code-like) — independent of the app theme on purpose. */
internal object EditorColors {
    val background = Color(0xFF1E1E1E)
    val panel = Color(0xFF252526)
    val panelHigh = Color(0xFF2D2D30)
    val border = Color(0xFF3C3C3C)
    val text = Color(0xFFD4D4D4)
    val muted = Color(0xFF858585)
    val keyword = Color(0xFF569CD6)
    val string = Color(0xFFCE9178)
    val comment = Color(0xFF6A9955)
    val number = Color(0xFFB5CEA8)
    val type = Color(0xFF4EC9B0)
    val function = Color(0xFFDCDCAA)
    val annotation = Color(0xFFC586C0)
    val tag = Color(0xFF569CD6)
    val attribute = Color(0xFF9CDCFE)
    val selection = Color(0xFF37373D)
    val accent = Color(0xFF3DDC84)
}

internal enum class CodeLanguage(val label: String) { JAVA("Java"), XML("XML"), JSON("JSON"), PLAIN("Plain text");

    companion object {
        fun of(path: String): CodeLanguage = when (path.substringAfterLast('.', "").lowercase()) {
            "java", "kt" -> JAVA
            "xml", "html" -> XML
            "json" -> JSON
            else -> PLAIN
        }
    }
}

private val javaKeywords = setOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do",
    "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
    "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
    "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
    "true", "false", "null", "var", "fun", "val", "object", "when", "is", "in",
)

/** Highlights a whole file into one [AnnotatedString] per line. Block-comment state is carried across lines. */
internal fun highlightLines(text: String, language: CodeLanguage): List<AnnotatedString> {
    val lines = text.replace("\r\n", "\n").replace('\t', ' ').split('\n')
    return when (language) {
        CodeLanguage.JAVA -> {
            var inBlock = false
            lines.map { line ->
                val (styled, stillInBlock) = highlightJavaLine(line, inBlock)
                inBlock = stillInBlock
                styled
            }
        }
        CodeLanguage.XML -> lines.map { highlightXmlLine(it) }
        CodeLanguage.JSON -> lines.map { highlightJsonLine(it) }
        CodeLanguage.PLAIN -> lines.map { buildAnnotatedString { append(it) } }
    }
}

private fun highlightJavaLine(line: String, startsInBlock: Boolean): Pair<AnnotatedString, Boolean> {
    val spans = mutableListOf<Triple<Int, Int, Color>>()
    var inBlock = startsInBlock
    var i = 0
    val n = line.length
    while (i < n) {
        if (inBlock) {
            val end = line.indexOf("*/", i)
            if (end < 0) { spans += Triple(i, n, EditorColors.comment); i = n } else {
                spans += Triple(i, end + 2, EditorColors.comment); i = end + 2; inBlock = false
            }
            continue
        }
        val c = line[i]
        when {
            c == '/' && i + 1 < n && line[i + 1] == '/' -> { spans += Triple(i, n, EditorColors.comment); i = n }
            c == '/' && i + 1 < n && line[i + 1] == '*' -> { inBlock = true; val start = i; i += 2
                val end = line.indexOf("*/", i)
                if (end < 0) { spans += Triple(start, n, EditorColors.comment); i = n } else {
                    spans += Triple(start, end + 2, EditorColors.comment); i = end + 2; inBlock = false
                }
            }
            c == '"' || c == '\'' -> {
                val start = i; i++
                while (i < n && line[i] != c) { if (line[i] == '\\') i++; i++ }
                i = minOf(i + 1, n)
                spans += Triple(start, i, EditorColors.string)
            }
            c == '@' && i + 1 < n && line[i + 1].isLetter() -> {
                val start = i; i++
                while (i < n && (line[i].isLetterOrDigit() || line[i] == '_' || line[i] == '.')) i++
                spans += Triple(start, i, EditorColors.annotation)
            }
            c.isDigit() -> {
                val start = i
                while (i < n && (line[i].isLetterOrDigit() || line[i] == '.' || line[i] == '_')) i++
                spans += Triple(start, i, EditorColors.number)
            }
            c.isLetter() || c == '_' || c == '$' -> {
                val start = i
                while (i < n && (line[i].isLetterOrDigit() || line[i] == '_' || line[i] == '$')) i++
                val word = line.substring(start, i)
                when {
                    word in javaKeywords -> spans += Triple(start, i, EditorColors.keyword)
                    word[0].isUpperCase() -> spans += Triple(start, i, EditorColors.type)
                    i < n && line[i] == '(' -> spans += Triple(start, i, EditorColors.function)
                }
            }
            else -> i++
        }
    }
    return paint(line, spans) to inBlock
}

private val xmlComment = Regex("<!--.*?-->")
private val xmlTag = Regex("</?[\\w:.-]+|/?>")
private val xmlAttr = Regex("[\\w:.-]+(?==)")
private val xmlString = Regex("\"[^\"]*\"")

private fun highlightXmlLine(line: String): AnnotatedString {
    val spans = mutableListOf<Triple<Int, Int, Color>>()
    xmlTag.findAll(line).forEach { spans += Triple(it.range.first, it.range.last + 1, EditorColors.tag) }
    xmlAttr.findAll(line).forEach { spans += Triple(it.range.first, it.range.last + 1, EditorColors.attribute) }
    xmlString.findAll(line).forEach { spans += Triple(it.range.first, it.range.last + 1, EditorColors.string) }
    xmlComment.findAll(line).forEach { spans += Triple(it.range.first, it.range.last + 1, EditorColors.comment) }
    return paint(line, spans)
}

private val jsonString = Regex("\"(?:[^\"\\\\]|\\\\.)*\"")
private val jsonNumber = Regex("-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b")
private val jsonWord = Regex("\\b(?:true|false|null)\\b")

private fun highlightJsonLine(line: String): AnnotatedString {
    val spans = mutableListOf<Triple<Int, Int, Color>>()
    jsonNumber.findAll(line).forEach { spans += Triple(it.range.first, it.range.last + 1, EditorColors.number) }
    jsonWord.findAll(line).forEach { spans += Triple(it.range.first, it.range.last + 1, EditorColors.keyword) }
    jsonString.findAll(line).forEach {
        val isKey = line.substring(it.range.last + 1).trimStart().startsWith(":")
        spans += Triple(it.range.first, it.range.last + 1, if (isKey) EditorColors.attribute else EditorColors.string)
    }
    return paint(line, spans)
}

/** Later spans win over earlier ones on overlap (strings/comments are added last for that reason). */
private fun paint(line: String, spans: List<Triple<Int, Int, Color>>): AnnotatedString = buildAnnotatedString {
    append(line)
    spans.forEach { (s, e, c) -> if (s < e && e <= line.length) addStyle(SpanStyle(color = c), s, e) }
}
