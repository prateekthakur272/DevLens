package dev.prateekthakur.devlens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.awt.datatransfer.DataFlavor
import java.net.URI
import java.io.File
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.prateekthakur.devlens.core.AppCommand
import dev.prateekthakur.devlens.core.AppInfo
import dev.prateekthakur.devlens.core.ExportKind
import dev.prateekthakur.devlens.core.settings.AppSettings
import dev.prateekthakur.devlens.core.settings.LocalAppSettings
import dev.prateekthakur.devlens.di.AppServices
import dev.prateekthakur.devlens.domain.decompile.DecompiledProject
import dev.prateekthakur.devlens.ui.apk.ApkTabTitles
import dev.prateekthakur.devlens.ui.apk.CodeEditorScreen
import dev.prateekthakur.devlens.ui.apk.LocalEditorLauncher
import dev.prateekthakur.devlens.ui.settings.LocalSettingsLauncher
import dev.prateekthakur.devlens.ui.settings.SettingsScreen
import dev.prateekthakur.devlens.ui.theme.DevLensTheme

private fun openInFileManager(file: File) {
    runCatching { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file) }
}

private fun browse(url: String) {
    runCatching { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url)) }
}

private class EditorWindow(val project: DecompiledProject, val apkName: String)

/** Lets the user drop files onto the window; shows an overlay while a file is being dragged over it. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun ApkDropZone(onDrop: (String) -> Unit, content: @Composable () -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { dragging = true }
            override fun onExited(event: DragAndDropEvent) { dragging = false }
            override fun onEnded(event: DragAndDropEvent) { dragging = false }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                dragging = false
                val files = runCatching {
                    @Suppress("UNCHECKED_CAST")
                    event.awtTransferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                }.getOrNull().orEmpty()
                val first = files.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) || it.name.endsWith(".aab", ignoreCase = true) }
                    ?: files.firstOrNull() ?: return false
                onDrop(first.absolutePath)
                return true
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().dragAndDropTarget(
            shouldStartDragAndDrop = { it.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) },
            target = target,
        ),
    ) {
        content()
        if (dragging) {
            val green = Color(0xFF3DDC84)
            Box(
                modifier = Modifier.fillMaxSize().background(green.copy(alpha = 0.14f)).border(3.dp, green).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    "DROP .APK OR .AAB TO ANALYZE",
                    style = TextStyle(color = Color(0xFF1A1B20), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    modifier = Modifier.background(Color.White).border(1.dp, green).padding(horizontal = 24.dp, vertical = 14.dp),
                )
            }
        }
    }
}

/** The app icon, loaded once from the classpath. */
private val appIcon: BitmapPainter by lazy { BitmapPainter(useResource("app_icon.png", ::loadImageBitmap)) }

/** On macOS the Dock icon is separate from the window icon (and has the standard margin); set it so it also shows when run from Gradle/the IDE. */
private fun setDockIcon() {
    runCatching {
        if (java.awt.Taskbar.isTaskbarSupported()) {
            val image = javax.imageio.ImageIO.read(object {}.javaClass.getResourceAsStream("/dock_icon.png"))
            java.awt.Taskbar.getTaskbar().iconImage = image
        }
    }
}

fun main() = application {
    remember { setDockIcon() }
    val services = remember { AppServices() }
    val editors = remember { mutableStateListOf<EditorWindow>() }
    var settingsOpen by remember { mutableStateOf(false) }
    val isMac = System.getProperty("os.name").orEmpty().contains("Mac", ignoreCase = true)

    Window(
        onCloseRequest = ::exitApplication,
        title = "DevLens",
        icon = appIcon,
        state = rememberWindowState(size = DpSize(1280.dp, 820.dp), position = WindowPosition.Aligned(Alignment.Center)),
    ) {
        MenuBar {
            fun shortcut(key: Key, shift: Boolean = false) = KeyShortcut(key, ctrl = !isMac, meta = isMac, shift = shift)
            val session = services.session
            val canUseFile = session.hasFile && !session.loading

            Menu("File") {
                Item("Open APK or AAB…", shortcut = shortcut(Key.O), enabled = !session.loading) { services.commands.tryEmit(AppCommand.OpenFile) }
                Separator()
                Menu("Export Report", enabled = canUseFile) {
                    ExportKind.entries.forEach { kind -> Item(kind.label) { services.commands.tryEmit(AppCommand.Export(kind)) } }
                }
                Item("Reveal Decompiled Sources", enabled = session.decompiledDir != null) {
                    session.decompiledDir?.let { openInFileManager(File(it)) }
                }
                Separator()
                Item("Settings…", shortcut = shortcut(Key.Comma)) { settingsOpen = true }
                if (!isMac) {
                    Separator()
                    Item("Quit", onClick = ::exitApplication)
                }
            }
            Menu("Analysis") {
                Item("Re-analyze", shortcut = shortcut(Key.R), enabled = canUseFile) { services.commands.tryEmit(AppCommand.Reanalyze) }
                Item(
                    if (session.decompiledDir != null) "Open Code Editor" else "Decompile",
                    shortcut = shortcut(Key.D),
                    enabled = canUseFile && !session.decompiling,
                ) { services.commands.tryEmit(AppCommand.Decompile) }
                Item("Show Findings", shortcut = shortcut(Key.F), enabled = canUseFile) { services.commands.tryEmit(AppCommand.SelectTab(ApkTabTitles.indexOf("Security"))) }
            }
            Menu("View") {
                Menu("Theme") {
                    RadioButtonItem("Light", selected = !services.settings.darkTheme) { services.settings.updateDarkTheme(false) }
                    RadioButtonItem("Dark", selected = services.settings.darkTheme) { services.settings.updateDarkTheme(true) }
                }
                Menu("Code Editor Font Size") {
                    Item("Increase", shortcut = shortcut(Key.Equals)) { services.settings.updateEditorFontSize(services.settings.editorFontSize + 1) }
                    Item("Decrease", shortcut = shortcut(Key.Minus)) { services.settings.updateEditorFontSize(services.settings.editorFontSize - 1) }
                    Item("Reset", shortcut = shortcut(Key.Zero)) { services.settings.updateEditorFontSize(AppSettings.DEFAULT_EDITOR_FONT_SIZE) }
                }
                Separator()
                Menu("Go to Tab", enabled = canUseFile) {
                    val digits = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)
                    ApkTabTitles.forEachIndexed { index, title ->
                        Item(title, shortcut = digits.getOrNull(index)?.let { shortcut(it) }) { services.commands.tryEmit(AppCommand.SelectTab(index)) }
                    }
                }
            }
            Menu("Help") {
                Item("About DevLens") { settingsOpen = true }
                Separator()
                Item("Developer on GitHub") { browse(AppInfo.GITHUB_URL) }
                Item("Open Settings Folder") {
                    val dir = File(System.getProperty("user.home"), ".devlens").also { it.mkdirs() }
                    openInFileManager(dir)
                }
            }
        }
        CompositionLocalProvider(
            LocalSettingsLauncher provides { settingsOpen = true },
            LocalEditorLauncher provides { project, name ->
                // One editor window per decompiled output; re-opening just keeps the existing one.
                if (editors.none { it.project.outputDir == project.outputDir }) editors += EditorWindow(project, name)
            },
        ) {
            ApkDropZone(onDrop = { services.fileDrops.tryEmit(it) }) { App(services) }
        }
    }

    editors.toList().forEach { editor ->
        key(editor.project.outputDir) {
            Window(
                onCloseRequest = { editors.remove(editor) },
                title = "${editor.apkName} - decompiled - DevLens",
                icon = appIcon,
                state = rememberWindowState(size = DpSize(1280.dp, 820.dp), position = WindowPosition.Aligned(Alignment.Center)),
            ) {
                CompositionLocalProvider(LocalAppSettings provides services.settings) {
                    DevLensTheme(darkTheme = services.settings.darkTheme) {
                        CodeEditorScreen(editor.project, editor.apkName)
                    }
                }
            }
        }
    }

    if (settingsOpen) {
        Window(
            onCloseRequest = { settingsOpen = false },
            title = "Settings - DevLens",
            icon = appIcon,
            state = rememberWindowState(size = DpSize(720.dp, 700.dp), position = WindowPosition.Aligned(Alignment.Center)),
        ) {
            CompositionLocalProvider(LocalAppSettings provides services.settings) {
                DevLensTheme(darkTheme = services.settings.darkTheme) {
                    SettingsScreen(services.settings, services.sdkLocator)
                }
            }
        }
    }
}
