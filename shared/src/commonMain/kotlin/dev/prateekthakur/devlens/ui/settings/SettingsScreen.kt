package dev.prateekthakur.devlens.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.prateekthakur.devlens.core.AppInfo
import dev.prateekthakur.devlens.core.io.FilePicker
import dev.prateekthakur.devlens.core.io.PlatformEnv
import dev.prateekthakur.devlens.core.settings.AppSettings
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator
import dev.prateekthakur.devlens.ui.apk.Mono
import dev.prateekthakur.devlens.ui.apk.ReKv
import dev.prateekthakur.devlens.ui.apk.RePanel
import dev.prateekthakur.devlens.ui.apk.ReShape
import dev.prateekthakur.devlens.ui.apk.ReToggle
import kotlinx.coroutines.launch

private val ok = Color(0xFF2E7D32)

/** Settings window content: appearance, Android SDK, code editor, privacy. Changes apply and persist immediately. */
@Composable
fun SettingsScreen(settings: AppSettings, sdkLocator: AndroidSdkLocator) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableStateOf(0) }
    var sdkRoot by remember { mutableStateOf<String?>(null) }
    var aapt2 by remember { mutableStateOf<String?>(null) }
    var apksigner by remember { mutableStateOf<String?>(null) }
    var adb by remember { mutableStateOf<String?>(null) }
    var pathField by remember { mutableStateOf(settings.sdkPathOverride.orEmpty()) }

    LaunchedEffect(refresh) {
        sdkLocator.invalidate()
        sdkRoot = sdkLocator.sdkRoot()
        aapt2 = sdkLocator.findAapt2()
        apksigner = sdkLocator.findApksigner()
        adb = sdkLocator.findAdb()
    }

    fun applyPath(value: String?) {
        settings.updateSdkPathOverride(value)
        pathField = settings.sdkPathOverride.orEmpty()
        refresh++
    }

    Column(modifier = Modifier.fillMaxSize().background(cs.background)) {
    Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RePanel("Appearance") {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mono("theme", color = cs.onSurfaceVariant, size = 11.5.sp, modifier = Modifier.width(120.dp))
                ReToggle("Light", !settings.darkTheme, cs.primary) { settings.updateDarkTheme(false) }
                ReToggle("Dark", settings.darkTheme, cs.primary) { settings.updateDarkTheme(true) }
            }
        }

        RePanel("Android SDK", trailing = if (settings.sdkPathOverride != null) "custom path" else "auto-detect") {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                ToolRow("sdk root", sdkRoot)
                ToolRow("aapt2", aapt2)
                ToolRow("apksigner", apksigner)
                ToolRow("adb", adb)
            }
            Divider()
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Mono("SDK path override", color = cs.onSurfaceVariant, size = 11.sp)
                BasicTextField(
                    value = pathField,
                    onValueChange = { pathField = it },
                    singleLine = true,
                    textStyle = TextStyle(color = cs.onSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(cs.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.fillMaxWidth().border(1.dp, cs.outlineVariant, ReShape).background(cs.surfaceContainerLow, ReShape).padding(horizontal = 10.dp, vertical = 7.dp)) {
                            if (pathField.isEmpty()) Mono("leave empty to use ANDROID_HOME or the default location", color = cs.onSurfaceVariant)
                            inner()
                        }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton("Browse…", primary = false) { scope.launch { FilePicker.pickDirectory("Choose Android SDK folder")?.let { pathField = it } } }
                    SmallButton("Apply", primary = true) { applyPath(pathField) }
                    SmallButton("Reset", primary = false) { applyPath(null) }
                    SmallButton("Re-detect", primary = false) { refresh++ }
                }
                Mono(
                    "DevLens only reads from the SDK; it never modifies it. A custom path takes priority over tools on PATH.",
                    color = cs.onSurfaceVariant,
                    size = 10.5.sp,
                )
            }
        }

        RePanel("Code editor", trailing = "${settings.editorFontSize}px") {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mono("font size", color = cs.onSurfaceVariant, size = 11.5.sp, modifier = Modifier.width(120.dp))
                SmallButton("−", primary = false) { settings.updateEditorFontSize(settings.editorFontSize - 1) }
                Mono("${settings.editorFontSize}", bold = true, modifier = Modifier.width(28.dp))
                SmallButton("+", primary = false) { settings.updateEditorFontSize(settings.editorFontSize + 1) }
                SmallButton("Default", primary = false) { settings.updateEditorFontSize(AppSettings.DEFAULT_EDITOR_FONT_SIZE) }
            }
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(10.dp)) {
                Text(
                    "public static void main(String[] args) { System.out.println(\"hello\"); }",
                    color = Color(0xFFD4D4D4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.editorFontSize.sp,
                )
            }
        }

        RePanel("Privacy") {
            Mono(
                "DevLens is local-first: APKs, decompiled sources and reports are processed on this machine and are never uploaded. " +
                    "Settings are stored in ~/.devlens/settings.properties.",
                color = cs.onSurfaceVariant,
                size = 11.sp,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
    AboutFooter()
    }
}

/** Pinned bottom bar: app name + version, developer, and runtime info. */
@Composable
private fun AboutFooter() {
    val cs = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxWidth().background(cs.surfaceContainerLow)) {
        Divider()
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.padding(end = 8.dp).size(8.dp).background(cs.primary))
            Column(modifier = Modifier.weight(1f)) {
                Mono("${AppInfo.NAME} v${AppInfo.VERSION}", bold = true, size = 11.5.sp)
                Mono(AppInfo.TAGLINE, color = cs.onSurfaceVariant, size = 10.5.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Mono("developed by ${AppInfo.DEVELOPER}", size = 10.5.sp)
                Text(
                    AppInfo.GITHUB_URL.removePrefix("https://"),
                    color = cs.tertiary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { uriHandler.openUri(AppInfo.GITHUB_URL) },
                )
                Mono("${AppInfo.BUILT_WITH} · ${PlatformEnv.osName()}", color = cs.onSurfaceVariant, size = 10.sp)
            }
        }
    }
}

@Composable
private fun Divider() = androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun ToolRow(label: String, path: String?) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Mono(label, modifier = Modifier.width(100.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, size = 11.5.sp)
        Mono(if (path != null) "✓" else "✗", color = if (path != null) ok else MaterialTheme.colorScheme.error, bold = true, modifier = Modifier.width(20.dp))
        Mono(path ?: "not found", modifier = Modifier.weight(1f), color = if (path != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, size = 11.5.sp)
    }
}

@Composable
private fun SmallButton(label: String, primary: Boolean, onClick: () -> Unit) {
    val padding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    if (primary) {
        Button(onClick = onClick, shape = ReShape, contentPadding = padding, modifier = Modifier.height(28.dp)) { Text(label, style = MaterialTheme.typography.labelMedium) }
    } else {
        OutlinedButton(onClick = onClick, shape = ReShape, contentPadding = padding, modifier = Modifier.height(28.dp)) { Text(label, style = MaterialTheme.typography.labelMedium) }
    }
}
