package dev.prateekthakur.devlens.ui.apk

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SuggestionChip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import dev.prateekthakur.devlens.ui.common.RingGauge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import dev.prateekthakur.devlens.core.io.PlatformFiles
import dev.prateekthakur.devlens.domain.report.ReportFormat
import dev.prateekthakur.devlens.domain.report.PdfReportWriter
import dev.prateekthakur.devlens.domain.report.ReportGenerator
import dev.prateekthakur.devlens.ui.common.LocalSnackbarHostState
import dev.prateekthakur.devlens.ui.settings.LocalSettingsLauncher
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.prateekthakur.devlens.core.io.FilePicker
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.Flow
import dev.prateekthakur.devlens.core.AppCommand
import dev.prateekthakur.devlens.core.AppSession
import dev.prateekthakur.devlens.core.ExportKind
import dev.prateekthakur.devlens.domain.apk.ApkAnalyzer
import dev.prateekthakur.devlens.domain.apk.ApkIdentityReader
import org.jetbrains.compose.resources.decodeToImageBitmap
import dev.prateekthakur.devlens.domain.apk.ApkFileEntry
import dev.prateekthakur.devlens.domain.apk.ApkMetadata
import dev.prateekthakur.devlens.domain.decompile.ApkDecompiler
import dev.prateekthakur.devlens.domain.decompile.DecompiledProject
import dev.prateekthakur.devlens.domain.dex.DexClassAnalyzer
import dev.prateekthakur.devlens.domain.dex.DexClassInfo
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.manifest.ManifestInfo
import dev.prateekthakur.devlens.domain.permission.PermissionAnalyzer
import dev.prateekthakur.devlens.domain.permission.PermissionInfo
import dev.prateekthakur.devlens.domain.permission.PermissionRisk
import dev.prateekthakur.devlens.domain.secrets.ApkSecretsAnalyzer
import dev.prateekthakur.devlens.domain.secrets.SecretFinding
import dev.prateekthakur.devlens.domain.security.SecurityAnalyzer
import dev.prateekthakur.devlens.domain.security.SecurityFinding
import dev.prateekthakur.devlens.domain.security.SecurityInput
import dev.prateekthakur.devlens.domain.security.Severity
import dev.prateekthakur.devlens.domain.signing.SigningAnalyzer
import dev.prateekthakur.devlens.domain.signing.SigningInfo
import dev.prateekthakur.devlens.domain.size.SizeAnalyzer
import dev.prateekthakur.devlens.domain.size.SizeReport
import dev.prateekthakur.devlens.ui.common.AnalyzingAnimation
import dev.prateekthakur.devlens.ui.common.EdgeTabBar
import dev.prateekthakur.devlens.ui.common.DonutChart
import dev.prateekthakur.devlens.ui.common.EmptyState
import dev.prateekthakur.devlens.ui.common.ErrorBanner
import dev.prateekthakur.devlens.ui.common.ExpandableCard
import dev.prateekthakur.devlens.ui.common.LabeledValue
import dev.prateekthakur.devlens.ui.common.LoadingBlock
import dev.prateekthakur.devlens.ui.common.SectionCard
import dev.prateekthakur.devlens.ui.common.Tag
import dev.prateekthakur.devlens.ui.theme.Spacing
import dev.prateekthakur.devlens.ui.theme.riskTone
import dev.prateekthakur.devlens.ui.theme.severityTone
import kotlinx.coroutines.launch

sealed interface DecompileState {
    data object Idle : DecompileState
    data class Running(val progress: Float) : DecompileState
    data class Done(val project: DecompiledProject) : DecompileState
    data class Failed(val message: String) : DecompileState
}

/** Titles of the analysis tabs, in order (also used by the menu bar Go to Tab). */
val ApkTabTitles = listOf("Overview", "Manifest", "Permissions", "Security", "Size", "Structure", "Classes", "Secrets", "Signing")

class ApkAnalysisState {
    var metadata by mutableStateOf<ApkMetadata?>(null)
    var manifest by mutableStateOf<ManifestInfo?>(null)
    var permissions by mutableStateOf<List<PermissionInfo>>(emptyList())
    var security by mutableStateOf<List<SecurityFinding>>(emptyList())
    var size by mutableStateOf<SizeReport?>(null)
    var files by mutableStateOf<List<ApkFileEntry>>(emptyList())
    var dexClasses by mutableStateOf<List<DexClassInfo>>(emptyList())
    var secrets by mutableStateOf<List<SecretFinding>>(emptyList())
    var signing by mutableStateOf<SigningInfo?>(null)
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    var appLabel by mutableStateOf<String?>(null)
    var iconPng by mutableStateOf<ByteArray?>(null)
    var icon by mutableStateOf<ImageBitmap?>(null)
    var decompile by mutableStateOf<DecompileState>(DecompileState.Idle)
    var editorOpen by mutableStateOf(false)

    fun reset() {
        decompile = DecompileState.Idle; editorOpen = false
        appLabel = null; iconPng = null; icon = null
        metadata = null; manifest = null; permissions = emptyList(); security = emptyList()
        size = null; files = emptyList(); dexClasses = emptyList(); secrets = emptyList()
        signing = null; error = null
    }
}

/** PRD §12-18 + reverse-engineering extensions — APK/AAB Inspector, Manifest, Permissions,
 * Security, Size, a DEX class browser, a strings/secrets scanner and a signing-certificate
 * inspector, all in one workspace. */
@Composable
fun ApkScreen(
    apkAnalyzer: ApkAnalyzer,
    manifestAnalyzer: ManifestAnalyzer,
    permissionAnalyzer: PermissionAnalyzer,
    securityAnalyzer: SecurityAnalyzer,
    sizeAnalyzer: SizeAnalyzer,
    dexClassAnalyzer: DexClassAnalyzer,
    secretsAnalyzer: ApkSecretsAnalyzer,
    signingAnalyzer: SigningAnalyzer,
    reportGenerator: ReportGenerator,
    pdfReportWriter: PdfReportWriter,
    apkDecompiler: ApkDecompiler,
    identityReader: ApkIdentityReader,
    fileDrops: Flow<String>,
    commands: Flow<AppCommand>,
    session: AppSession,
) {
    val snackbar = LocalSnackbarHostState.current
    val editorLauncher = LocalEditorLauncher.current
    val files = remember { PlatformFiles() }
    var apkPath by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    val state = remember { ApkAnalysisState() }
    val scope = rememberCoroutineScope()

    fun analyze(path: String) {
        apkPath = path
        selectedTab = 0
        state.reset()
        state.loading = true
        scope.launch {
            manifestAnalyzer.analyze(path)
                .onSuccess { state.manifest = it }
                .onFailure { state.error = it.message }
            apkAnalyzer.analyze(path).onSuccess { state.metadata = it }
            permissionAnalyzer.analyze(path).onSuccess { state.permissions = it }
            sizeAnalyzer.analyze(path).onSuccess { state.size = it }
            apkAnalyzer.listFiles(path).onSuccess { state.files = it }
            dexClassAnalyzer.analyze(path).onSuccess { state.dexClasses = it }
            secretsAnalyzer.analyze(path).onSuccess { state.secrets = it }
            signingAnalyzer.analyze(path).onSuccess { state.signing = it }
            // Rules run last, over everything gathered above, so nothing is analyzed twice.
            state.security = securityAnalyzer.evaluate(
                SecurityInput(
                    manifest = state.manifest,
                    permissions = state.permissions,
                    signing = state.signing,
                    metadata = state.metadata,
                    files = state.files,
                    classes = state.dexClasses,
                    secrets = state.secrets,
                    isBundle = path.endsWith(".aab", ignoreCase = true),
                ),
            )
            identityReader.read(path).onSuccess { id ->
                state.appLabel = id.label
                state.iconPng = id.iconPng
                state.icon = id.iconPng?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
            }
            state.loading = false
        }
    }

    /** The one entry point for every way of opening a file (dialog, drag and drop): only `.apk` and `.aab` are accepted. */
    suspend fun openApk(candidate: String) {
        if (candidate.endsWith(".apk", ignoreCase = true) || candidate.endsWith(".aab", ignoreCase = true)) analyze(candidate)
        else snackbar.showSnackbar("Only .apk and .aab files can be opened")
    }

    fun pickAndAnalyze() {
        // Some platforms (macOS) ignore the dialog's file filter, so the choice is validated again here.
        scope.launch { FilePicker.pickFile("Open APK or AAB", "apk,aab")?.let { openApk(it) } }
    }

    fun exportReport(path: String, kind: ExportKind) {
        scope.launch {
            val target = FilePicker.pickSaveFile("Save Report", "${reportBaseName(state, path)}.${kind.extension}") ?: return@launch
            val result = if (kind.format == null) {
                val finalPath = if (target.endsWith(".pdf", ignoreCase = true)) target else "$target.pdf"
                pdfReportWriter.write(buildApkReportData(state, path), finalPath).map { finalPath }
            } else {
                files.writeText(target, reportGenerator.render(buildApkReport(state, path), kind.format)).map { target }
            }
            result
                .onSuccess { snackbar.showSnackbar("Report saved to $it") }
                .onFailure { snackbar.showSnackbar("Failed to save report: ${it.message}") }
        }
    }

    fun openEditor(project: DecompiledProject, path: String) {
        val launcher = editorLauncher
        if (launcher != null) launcher(project, path.substringAfterLast('/').substringAfterLast('\\')) else state.editorOpen = true
    }

    fun startDecompile(path: String) {
        if (state.decompile is DecompileState.Running) return
        state.decompile = DecompileState.Running(0f)
        scope.launch {
            apkDecompiler.decompile(path) { state.decompile = DecompileState.Running(it) }
                .onSuccess {
                    state.decompile = DecompileState.Done(it)
                    openEditor(it, path)
                }
                .onFailure { state.decompile = DecompileState.Failed(it.message ?: "Decompilation failed.") }
        }
    }

    SideEffect {
        session.hasFile = apkPath != null
        session.loading = state.loading
        session.decompiling = state.decompile is DecompileState.Running
        session.decompiledDir = (state.decompile as? DecompileState.Done)?.project?.outputDir
    }

    LaunchedEffect(Unit) {
        commands.collect { command ->
            val current = apkPath
            when (command) {
                AppCommand.OpenFile -> if (!state.loading) pickAndAnalyze()
                AppCommand.Reanalyze -> if (current != null && !state.loading) analyze(current)
                AppCommand.Decompile -> if (current != null && !state.loading) {
                    (state.decompile as? DecompileState.Done)?.let { openEditor(it.project, current) } ?: startDecompile(current)
                }
                AppCommand.OpenEditor -> if (current != null) (state.decompile as? DecompileState.Done)?.let { openEditor(it.project, current) }
                is AppCommand.Export -> if (current != null && !state.loading && state.metadata != null) exportReport(current, command.kind)
                is AppCommand.SelectTab -> if (current != null && !state.loading && command.index in ApkTabTitles.indices) selectedTab = command.index
            }
        }
    }

    LaunchedEffect(Unit) {
        fileDrops.collect { dropped ->
            if (!state.loading) openApk(dropped)
        }
    }

    val path = apkPath
    if (path == null) {
        WelcomeView(onOpen = ::pickAndAnalyze)
        return
    }

    val decompiled = (state.decompile as? DecompileState.Done)?.project
    if (state.editorOpen && decompiled != null) {
        CodeEditorScreen(decompiled, path.substringAfterLast('/').substringAfterLast('\\'), onClose = { state.editorOpen = false })
        return
    }

    Row(modifier = Modifier.fillMaxSize()) {
        ApkSidePane(
            path = path,
            loading = state.loading,
            metadata = state.metadata,
            topSeverity = state.security.minByOrNull { it.severity.ordinal }?.severity,
            findingCount = state.security.size,
            appLabel = state.appLabel,
            icon = state.icon,
            onViewFindings = { selectedTab = 3 },
            decompile = state.decompile,
            onDecompile = { startDecompile(path) },
            onOpenEditor = { (state.decompile as? DecompileState.Done)?.let { openEditor(it.project, path) } },
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
        if (!state.loading) EdgeTabBar(
            ApkTabTitles,
            selectedTab,
            onSelect = { selectedTab = it },
            badges = listOf(
                null,
                null,
                state.permissions.takeIf { it.isNotEmpty() }?.let { it.size.toString() to Color(0xFF757575) },
                state.security.takeIf { it.isNotEmpty() }?.let { it.size.toString() to (severityChartColors[it.first().severity] ?: Color(0xFF757575)) },
                null,
                state.files.takeIf { it.isNotEmpty() }?.let { it.size.toString() to Color(0xFF757575) },
                state.dexClasses.takeIf { it.isNotEmpty() }?.let { it.size.toString() to Color(0xFF757575) },
                state.secrets.takeIf { it.isNotEmpty() }?.let { it.size.toString() to Color(0xFFD32F2F) },
                null,
            ),
        )
        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.xl)) {
                state.error?.let { ErrorBanner(it) }

            if (state.loading) {
                AnalyzingAnimation(Modifier.weight(1f))
                return@Column
            }

            Column(modifier = Modifier.fillMaxSize().padding(top = if (selectedTab in listOf(5, 6)) Spacing.lg else 0.dp)) {
                when (selectedTab) {
                    0 -> OverviewTab(state)
                    1 -> ManifestTab(state.manifest)
                    2 -> PermissionsTab(state.permissions)
                    3 -> SecurityTab(state.security)
                    4 -> SizeTab(state.size)
                    5 -> StructureTab(state.files)
                    6 -> ClassesTab(state.dexClasses)
                    7 -> SecretsTab(state.secrets)
                    8 -> SigningTab(state.signing)
                }
            }
        }
    ApkFooter(fileName = path.substringAfterLast('/').substringAfterLast('\\'), loading = state.loading, onOpenAnother = ::pickAndAnalyze, canExport = !state.loading && state.metadata != null, onExport = { exportReport(path, it) })
        }
    }
}


/** Status bar of the analysis page: what is loaded on the left, actions on the right. */
@Composable
private fun ApkFooter(
    fileName: String,
    loading: Boolean,
    onOpenAnother: () -> Unit,
    canExport: Boolean,
    onExport: (ExportKind) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val openSettings = LocalSettingsLauncher.current
    val cs = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().background(cs.surfaceContainerLow)) {
        HorizontalDivider(color = cs.outlineVariant)
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Mono(
                if (loading) "[..] analyzing $fileName" else "[ok] analyzed $fileName",
                color = cs.onSurfaceVariant,
                size = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (openSettings != null) {
                OutlinedButton(
                    onClick = openSettings,
                    shape = ReShape,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.padding(end = 6.dp).height(26.dp),
                ) { Text("Settings", style = MaterialTheme.typography.labelSmall) }
            }
            Box(modifier = Modifier.padding(end = 6.dp)) {
                OutlinedButton(
                    onClick = { menuOpen = true },
                    enabled = canExport,
                    shape = ReShape,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp),
                ) { Text("Export report ▾", style = MaterialTheme.typography.labelSmall) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    ExportKind.entries.forEach { kind ->
                        DropdownMenuItem(text = { Text(kind.label, style = MaterialTheme.typography.bodySmall) }, onClick = { menuOpen = false; onExport(kind) })
                    }
                }
            }
            Button(
                onClick = onOpenAnother,
                enabled = !loading,
                shape = ReShape,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(26.dp),
            ) { Text("Open another", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/** First screen: a full-window "no file loaded" view. */
@Composable
private fun WelcomeView(onOpen: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize().background(cs.surfaceContainerLow)) {
        Text(
            "DEVLENS - APK ANALYSIS",
            color = cs.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.fillMaxWidth().background(cs.surfaceContainerHigh).padding(horizontal = 14.dp, vertical = 6.dp),
        )
        HorizontalDivider(color = cs.outlineVariant)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(Spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Mono("no file loaded", bold = true, size = 16.sp)
            Mono(
                "open an .apk or .aab to inspect its manifest, permissions, signing certificate, strings and secrets, and to decompile it.\nyou can also drag and drop one anywhere in this window.",
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            )
            Button(
                onClick = onOpen,
                shape = ReShape,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp),
            ) { Text("File ▸ Open APK / AAB…", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

internal val severityChartColors = mapOf(
    Severity.CRITICAL to Color(0xFFB71C1C),
    Severity.HIGH to Color(0xFFE53935),
    Severity.MEDIUM to Color(0xFFEF6C00),
    Severity.LOW to Color(0xFFFBC02D),
    Severity.INFO to Color(0xFF3D5AFE),
)

/** 0-100, higher is safer. Weighted by severity; each possible secret costs a flat amount. */
internal fun securityScore(findings: List<SecurityFinding>, secretCount: Int): Int {
    val penalty = findings.sumOf {
        when (it.severity) {
            Severity.CRITICAL -> 30
            Severity.HIGH -> 15
            Severity.MEDIUM -> 7
            Severity.LOW -> 2
            Severity.INFO -> 0
        } as Int
    } + secretCount.coerceAtMost(6) * 5
    return (100 - penalty).coerceIn(0, 100)
}

internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${(kb * 10).toInt() / 10.0} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${(mb * 10).toInt() / 10.0} MB"
    return "${(mb / 1024.0 * 10).toInt() / 10.0} GB"
}
