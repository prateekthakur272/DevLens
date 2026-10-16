package dev.prateekthakur.devlens.ui.apk

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.prateekthakur.devlens.domain.apk.ApkMetadata
import dev.prateekthakur.devlens.domain.security.Severity
import dev.prateekthakur.devlens.ui.common.LocalSnackbarHostState
import kotlinx.coroutines.launch

/**
 * Left pane: the loaded file and its basic properties, like a "properties" sidebar in an RE tool.
 * Click any value to copy it.
 */
@Composable
fun ApkSidePane(
    path: String,
    loading: Boolean,
    metadata: ApkMetadata?,
    topSeverity: Severity?,
    findingCount: Int,
    appLabel: String?,
    icon: ImageBitmap?,
    onViewFindings: () -> Unit,
    decompile: DecompileState,
    onDecompile: () -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val fileName = path.substringAfterLast('/').substringAfterLast('\\')

    fun copy(label: String, value: String) {
        clipboard.setText(AnnotatedString(value))
        scope.launch { snackbar.showSnackbar("$label copied", duration = SnackbarDuration.Short) }
    }

    Surface(modifier = modifier.width(280.dp).fillMaxHeight(), shape = RectangleShape, color = cs.surfaceContainerLow) {
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    SectionStrip("File")
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(icon, fileName)
                            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                                Mono(appLabel ?: fileName, bold = true, size = 13.sp, modifier = Modifier.clickable { copy("Name", appLabel ?: fileName) })
                                if (appLabel != null) Mono(fileName, color = cs.onSurfaceVariant, size = 10.5.sp, modifier = Modifier.clickable { copy("File name", fileName) })
                            }
                        }
                        Mono(path, color = cs.onSurfaceVariant, size = 10.5.sp, modifier = Modifier.padding(top = 8.dp).clickable { copy("Path", path) })
                        StatusLine(loading, findingCount, topSeverity, onViewFindings)
                    }
                    SectionStrip("Properties")
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Prop("package", metadata?.packageName) { copy("Package", it) }
                        Prop("version", metadata?.let { "${it.versionName} (${it.versionCode})" }) { copy("Version", it) }
                        Prop("min sdk", metadata?.minSdk?.toString()) { copy("Min SDK", it) }
                        Prop("target sdk", metadata?.targetSdk?.toString()) { copy("Target SDK", it) }
                        Prop("size", metadata?.let { formatBytes(it.sizeBytes) }) { copy("Size", it) }
                        Prop("abis", metadata?.let { it.abis.joinToString().ifBlank { "none" } }) { copy("ABIs", it) }
                        Prop("dex files", metadata?.dexFileCount?.toString()) { copy("DEX files", it) }
                        Prop("native libs", metadata?.nativeLibraryCount?.toString()) { copy("Native libs", it) }
                    }
                }
                DecompileSection(decompile, enabled = !loading, onDecompile = onDecompile, onOpenEditor = onOpenEditor)
            }
            VerticalDivider(color = cs.outlineVariant)
        }
    }
}

/** Square launcher icon with a hairline border; a plain "APK" tile when the icon couldn't be read. */
@Composable
private fun AppIcon(icon: ImageBitmap?, fileName: String) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(48.dp).border(1.dp, cs.outlineVariant, ReShape).background(cs.surfaceContainer, ReShape).padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Image(icon, contentDescription = fileName, modifier = Modifier.fillMaxSize())
        else Mono("APK", color = cs.onSurfaceVariant, bold = true, size = 11.sp)
    }
}

@Composable
private fun SectionStrip(title: String) {
    val cs = MaterialTheme.colorScheme
    Column {
        Text(
            title.uppercase(),
            color = cs.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.fillMaxWidth().background(cs.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 5.dp),
        )
        HorizontalDivider(color = cs.outlineVariant)
    }
}

@Composable
private fun StatusLine(loading: Boolean, findingCount: Int, topSeverity: Severity?, onClick: () -> Unit) {
    val color = when {
        loading -> MaterialTheme.colorScheme.onSurfaceVariant
        topSeverity == null -> Color(0xFF2E7D32)
        else -> severityChartColors.getValue(topSeverity)
    }
    val text = when {
        loading -> "analyzing…"
        findingCount == 0 -> "no findings"
        else -> "$findingCount findings, worst ${topSeverity?.name}"
    }
    Mono(
        "» $text",
        color = color,
        bold = true,
        size = 11.5.sp,
        modifier = Modifier.padding(top = 8.dp).clickable(enabled = !loading && findingCount > 0, onClick = onClick),
    )
}

@Composable
private fun Prop(label: String, value: String?, onCopy: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = value != null) { value?.let(onCopy) }
            .padding(horizontal = 12.dp, vertical = 3.dp),
    ) {
        Mono(label, modifier = Modifier.width(78.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, size = 11.sp)
        Mono(value ?: "-", modifier = Modifier.weight(1f), size = 11.5.sp)
    }
}

/** Pinned to the bottom of the pane: starts decompilation, shows its progress, then opens the code editor. */
@Composable
private fun DecompileSection(decompile: DecompileState, enabled: Boolean, onDecompile: () -> Unit, onOpenEditor: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    HorizontalDivider(color = cs.outlineVariant)
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        when (decompile) {
            is DecompileState.Running -> {
                LinearProgressIndicator(
                    progress = { decompile.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Mono("decompiling… large apps can take a minute", color = cs.onSurfaceVariant, size = 10.5.sp, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
            }
            is DecompileState.Failed -> Mono(decompile.message, color = cs.error, size = 10.5.sp, modifier = Modifier.padding(bottom = 8.dp), maxLines = 3, overflow = TextOverflow.Ellipsis)
            is DecompileState.Done -> Mono("sources ready", color = cs.onSurfaceVariant, size = 10.5.sp, modifier = Modifier.padding(bottom = 8.dp))
            else -> Mono("reverse engineer to java + resources", color = cs.onSurfaceVariant, size = 10.5.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        Button(
            onClick = if (decompile is DecompileState.Done) onOpenEditor else onDecompile,
            enabled = enabled && decompile !is DecompileState.Running,
            shape = ReShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.fillMaxWidth().height(30.dp),
        ) {
            Text(
                when (decompile) {
                    is DecompileState.Idle -> "Decompile"
                    is DecompileState.Running -> "Decompiling… ${(decompile.progress * 100).toInt()}%"
                    is DecompileState.Done -> "Open in code editor"
                    is DecompileState.Failed -> "Retry decompile"
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
