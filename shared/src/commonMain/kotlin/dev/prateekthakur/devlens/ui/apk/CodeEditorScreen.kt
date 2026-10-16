package dev.prateekthakur.devlens.ui.apk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.prateekthakur.devlens.core.io.PlatformFiles
import dev.prateekthakur.devlens.core.settings.AppSettings
import dev.prateekthakur.devlens.core.settings.LocalAppSettings
import dev.prateekthakur.devlens.domain.decompile.DecompiledFile
import dev.prateekthakur.devlens.domain.decompile.DecompiledProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val mono = FontFamily.Monospace
private const val MAX_CHARS = 1_500_000

// ---- file tree model -------------------------------------------------------

private class DirNode(val name: String, val path: String) {
    val dirs = mutableMapOf<String, DirNode>()
    val files = mutableListOf<DecompiledFile>()
}

private sealed interface TreeRow {
    val depth: Int
    data class Dir(val label: String, val path: String, override val depth: Int, val expanded: Boolean) : TreeRow
    data class File(val file: DecompiledFile, override val depth: Int, val subtitle: String? = null) : TreeRow
}

private fun buildTree(files: List<DecompiledFile>): DirNode {
    val root = DirNode("", "")
    for (f in files) {
        val parts = f.relativePath.split('/')
        var node = root
        for (i in 0 until parts.size - 1) {
            val childPath = parts.subList(0, i + 1).joinToString("/")
            node = node.dirs.getOrPut(parts[i]) { DirNode(parts[i], childPath) }
        }
        node.files += f
    }
    return root
}

/** Flattens the tree into rows; single-child package chains are merged (com/foo/bar) like an IDE does. */
private fun flatten(dir: DirNode, depth: Int, expanded: Set<String>, out: MutableList<TreeRow>) {
    for (child in dir.dirs.values.sortedBy { it.name }) {
        var d = child
        var label = child.name
        while (d.files.isEmpty() && d.dirs.size == 1) {
            d = d.dirs.values.first()
            label += "/" + d.name
        }
        val isOpen = d.path in expanded
        out += TreeRow.Dir(label, d.path, depth, isOpen)
        if (isOpen) flatten(d, depth + 1, expanded, out)
    }
    for (f in dir.files.sortedBy { it.relativePath.substringAfterLast('/') }) out += TreeRow.File(f, depth)
}

// ---- screen ----------------------------------------------------------------

/** Full-window, read-only code browser for decompiled output: file tree, open-file tabs, highlighted source. */
@Composable
fun CodeEditorScreen(project: DecompiledProject, apkName: String, onClose: (() -> Unit)? = null) {
    val root = remember(project) { buildTree(project.files) }
    var filter by remember(project) { mutableStateOf("") }
    val expanded = remember(project) { mutableStateListOf("sources") }
    val openTabs = remember(project) { mutableStateListOf<String>() }
    var active by remember(project) { mutableStateOf<String?>(null) }

    fun open(path: String) {
        if (path !in openTabs) openTabs += path
        active = path
    }

    fun close(path: String) {
        val index = openTabs.indexOf(path)
        openTabs.remove(path)
        if (active == path) active = openTabs.getOrNull(index.coerceAtMost(openTabs.size - 1))
    }

    // Open the decoded manifest straight away — it's the best starting point.
    LaunchedEffect(project) {
        project.files.firstOrNull { it.relativePath.endsWith("AndroidManifest.xml") }?.let { open(it.relativePath) }
    }

    Column(modifier = Modifier.fillMaxSize().background(EditorColors.background)) {
        EditorToolbar(apkName, project, onClose)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            FileTreePane(
                project = project,
                root = root,
                filter = filter,
                onFilter = { filter = it },
                expanded = expanded,
                activePath = active,
                onOpen = ::open,
            )
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(EditorColors.border))
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (openTabs.isNotEmpty()) FileTabs(openTabs, active, onSelect = { active = it }, onClose = ::close)
                val current = active
                if (current == null) {
                    EmptyEditor()
                } else {
                    CodeView(project, current)
                }
            }
        }
    }
}

@Composable
private fun EditorToolbar(apkName: String, project: DecompiledProject, onClose: (() -> Unit)?) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(EditorColors.panelHigh).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClose != null) {
                TextButton(onClick = onClose, colors = ButtonDefaults.textButtonColors(contentColor = EditorColors.accent)) {
                    Text("← Back to analysis", fontSize = 13.sp)
                }
            }
            Text(
                "$apkName  ·  decompiled",
                color = EditorColors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = if (onClose != null) 8.dp else 6.dp),
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                "${project.javaFileCount} Java files · ${project.files.size - project.javaFileCount} resources",
                color = EditorColors.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(EditorColors.border))
    }
}

@Composable
private fun FileTreePane(
    project: DecompiledProject,
    root: DirNode,
    filter: String,
    onFilter: (String) -> Unit,
    expanded: SnapshotStateList<String>,
    activePath: String?,
    onOpen: (String) -> Unit,
) {
    val rows = remember(root, filter, expanded.toList()) {
        val out = mutableListOf<TreeRow>()
        if (filter.isBlank()) {
            flatten(root, 0, expanded.toSet(), out)
        } else {
            project.files
                .filter { it.relativePath.contains(filter, ignoreCase = true) }
                .take(500)
                .forEach { out += TreeRow.File(it, 0, subtitle = it.relativePath.substringBeforeLast('/', "")) }
        }
        out
    }

    Column(modifier = Modifier.width(300.dp).fillMaxHeight().background(EditorColors.panel)) {
        Text("EXPLORER", color = EditorColors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 8.dp))
        BasicTextField(
            value = filter,
            onValueChange = onFilter,
            singleLine = true,
            textStyle = TextStyle(color = EditorColors.text, fontSize = 12.sp, fontFamily = mono),
            cursorBrush = SolidColor(EditorColors.accent),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxWidth().background(EditorColors.background).padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    if (filter.isEmpty()) Text("Search files…", color = EditorColors.muted, fontSize = 12.sp)
                    inner()
                }
            },
        )
        if (filter.isNotBlank() && rows.isEmpty()) {
            Text("No files match.", color = EditorColors.muted, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
        }
        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(rows, key = { (it as? TreeRow.File)?.file?.relativePath ?: "dir:" + (it as TreeRow.Dir).path }) { row ->
                when (row) {
                    is TreeRow.Dir -> TreeRowView(
                        indent = row.depth,
                        marker = if (row.expanded) "▾" else "▸",
                        markerColor = EditorColors.muted,
                        title = row.label,
                        titleColor = EditorColors.text,
                        selected = false,
                        onClick = { if (row.expanded) expanded.remove(row.path) else expanded.add(row.path) },
                    )
                    is TreeRow.File -> {
                        val name = row.file.relativePath.substringAfterLast('/')
                        TreeRowView(
                            indent = row.depth,
                            marker = "●",
                            markerColor = fileColor(name),
                            title = name,
                            titleColor = EditorColors.text,
                            subtitle = row.subtitle,
                            selected = row.file.relativePath == activePath,
                            onClick = { onOpen(row.file.relativePath) },
                        )
                    }
                }
            }
        }
    }
}

private fun fileColor(name: String): Color = when (name.substringAfterLast('.', "")) {
    "java", "kt" -> Color(0xFFE8A04C)
    "xml" -> Color(0xFF6FBF73)
    "json" -> Color(0xFFD7C14B)
    else -> EditorColors.muted
}

@Composable
private fun TreeRowView(
    indent: Int,
    marker: String,
    markerColor: Color,
    title: String,
    titleColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) EditorColors.selection else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = (10 + indent * 12).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(marker, color = markerColor, fontSize = 10.sp, modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = EditorColors.muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
            }
        }
    }
}

@Composable
private fun FileTabs(tabs: List<String>, active: String?, onSelect: (String) -> Unit, onClose: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(EditorColors.panel).horizontalScroll(rememberScrollState())) {
        tabs.forEach { path ->
            val isActive = path == active
            Row(
                modifier = Modifier
                    .background(if (isActive) EditorColors.background else EditorColors.panelHigh)
                    .clickable { onSelect(path) }
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    path.substringAfterLast('/'),
                    color = if (isActive) EditorColors.text else EditorColors.muted,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                )
                Text(
                    "×",
                    color = EditorColors.muted,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { onClose(path) }.padding(horizontal = 8.dp),
                )
            }
            Box(modifier = Modifier.width(1.dp).height(34.dp).background(EditorColors.border))
        }
    }
}

@Composable
private fun EmptyEditor() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Select a file from the explorer to view its source.", color = EditorColors.muted, fontSize = 13.sp)
    }
}

private class LoadedFile(val lines: List<AnnotatedString>, val language: CodeLanguage, val truncated: Boolean, val sizeLabel: String)

@Composable
private fun CodeView(project: DecompiledProject, relativePath: String) {
    val files = remember { PlatformFiles() }
    val fontSize = LocalAppSettings.current?.editorFontSize ?: AppSettings.DEFAULT_EDITOR_FONT_SIZE
    val loaded by produceState<LoadedFile?>(initialValue = null, relativePath) {
        value = null
        value = withContext(Dispatchers.Default) {
            val raw = files.readText(files.join(project.outputDir, *relativePath.split('/').toTypedArray()))
                ?: return@withContext LoadedFile(listOf(AnnotatedString("// Could not read this file.")), CodeLanguage.PLAIN, false, "")
            val truncated = raw.length > MAX_CHARS
            val text = if (truncated) raw.take(MAX_CHARS) + "\n// … truncated by DevLens (file is ${raw.length / 1024} KB)" else raw
            val language = CodeLanguage.of(relativePath)
            LoadedFile(highlightLines(text, language), language, truncated, "${raw.length / 1024} KB")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            relativePath.replace("/", "  ›  "),
            color = EditorColors.muted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        )
        val file = loaded
        if (file == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EditorColors.accent, modifier = Modifier.width(28.dp).height(28.dp), strokeWidth = 2.dp)
            }
        } else {
            val listState = rememberLazyListState()
            LaunchedEffect(relativePath) { listState.scrollToItem(0) }
            val gutterWidth = (file.lines.size.toString().length * fontSize * 0.7f + 22).dp
            Box(modifier = Modifier.weight(1f).fillMaxWidth().horizontalScroll(rememberScrollState())) {
                SelectionContainer {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxHeight()) {
                        itemsIndexed(file.lines) { index, line ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DisableSelection {
                                    Text(
                                        (index + 1).toString(),
                                        color = EditorColors.muted,
                                        fontFamily = mono,
                                        fontSize = (fontSize - 0.5f).sp,
                                        lineHeight = (fontSize * 1.55f).sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(gutterWidth).padding(end = 14.dp),
                                    )
                                }
                                Text(
                                    line,
                                    color = EditorColors.text,
                                    fontFamily = mono,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * 1.55f).sp,
                                    softWrap = false,
                                    maxLines = 1,
                                    modifier = Modifier.padding(end = 40.dp),
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(EditorColors.accent).padding(horizontal = 14.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${file.language.label}  ·  UTF-8", color = Color(0xFF00210E), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${file.lines.size} lines  ·  ${file.sizeLabel}" + if (file.truncated) "  ·  truncated" else "",
                    color = Color(0xFF00210E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
