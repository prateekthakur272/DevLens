package dev.prateekthakur.devlens.ui.apk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.prateekthakur.devlens.domain.apk.ApkFileEntry
import dev.prateekthakur.devlens.domain.apk.ApkMetadata
import dev.prateekthakur.devlens.domain.dex.DexClassInfo
import dev.prateekthakur.devlens.domain.manifest.ComponentInfo
import dev.prateekthakur.devlens.domain.manifest.ManifestInfo
import dev.prateekthakur.devlens.domain.permission.PermissionInfo
import dev.prateekthakur.devlens.domain.permission.PermissionRisk
import dev.prateekthakur.devlens.domain.secrets.SecretFinding
import dev.prateekthakur.devlens.domain.security.SecurityFinding
import dev.prateekthakur.devlens.domain.security.Severity
import dev.prateekthakur.devlens.domain.signing.SigningInfo
import dev.prateekthakur.devlens.domain.size.SizeReport

private val gap = 10.dp

private fun riskColor(r: PermissionRisk) = when (r) {
    PermissionRisk.DANGEROUS -> Color(0xFFD32F2F)
    PermissionRisk.SPECIAL -> Color(0xFFE65100)
    PermissionRisk.SIGNATURE -> Color(0xFF7B1FA2)
    PermissionRisk.NORMAL -> Color(0xFF2E7D32)
    PermissionRisk.UNKNOWN -> Color(0xFF757575)
}

// ---- Overview (summary + APK information + findings) --------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OverviewTab(state: ApkAnalysisState) {
    val cs = MaterialTheme.colorScheme
    var filter by remember(state.security) { mutableStateOf<Severity?>(null) }
    var picked by remember(state.security) { mutableStateOf<SecurityFinding?>(null) }
    val score = securityScore(state.security, state.secrets.size)
    val dangerous = state.permissions.filter { it.risk == PermissionRisk.DANGEROUS }
    val visible = state.security.filter { filter == null || it.severity == filter }
    val selected = picked?.takeIf { it in visible } ?: visible.firstOrNull()
    val m = state.manifest
    val exported = m?.let { it.activities.count { c -> c.exported } + it.services.count { c -> c.exported } + it.receivers.count { c -> c.exported } + it.providers.count { p -> p.exported } } ?: 0

    LazyColumn(contentPadding = PaddingValues(top = gap, bottom = gap), verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            RePanel("Summary") {
                val worst = state.security.minByOrNull { it.severity.ordinal }?.severity
                val scoreColor = when { score >= 85 -> Color(0xFF2E7D32); score >= 60 -> Color(0xFFE65100); score >= 35 -> Color(0xFFD32F2F); else -> Color(0xFFB71C1C) }
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    StatCell("risk score", "$score", scoreColor, modifier = Modifier.weight(1.4f, fill = true)) { SegMeter(score / 100f, scoreColor) }
                    VDivider()
                    StatCell("findings", state.security.size.toString(), worst?.let { severityChartColors.getValue(it) } ?: Color(0xFF2E7D32)) {
                        Mono(worst?.let { "worst: ${it.name.lowercase()}" } ?: "clean", color = cs.onSurfaceVariant, size = 10.5.sp)
                    }
                    VDivider()
                    StatCell("secrets", state.secrets.size.toString(), if (state.secrets.isEmpty()) cs.onSurface else cs.error) {
                        Mono("in strings", color = cs.onSurfaceVariant, size = 10.5.sp)
                    }
                    VDivider()
                    StatCell("dangerous perms", dangerous.size.toString(), if (dangerous.isEmpty()) cs.onSurface else riskColor(PermissionRisk.DANGEROUS)) {
                        Mono("of ${state.permissions.size}", color = cs.onSurfaceVariant, size = 10.5.sp)
                    }
                    VDivider()
                    StatCell("exported", exported.toString(), if (exported == 0) cs.onSurface else Color(0xFFE65100)) {
                        Mono("components", color = cs.onSurfaceVariant, size = 10.5.sp)
                    }
                }
                HorizontalDivider()
                // flat stacked severity distribution
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    val total = state.security.size
                    if (total == 0) {
                        Mono("no findings", color = cs.onSurfaceVariant)
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Severity.entries.forEach { sv ->
                                val n = state.security.count { it.severity == sv }
                                if (n > 0) Box(modifier = Modifier.weight(n.toFloat()).height(10.dp).background(severityChartColors.getValue(sv)))
                            }
                        }
                        FlowRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Severity.entries.forEach { sv ->
                                val n = state.security.count { it.severity == sv }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.padding(end = 6.dp).size(8.dp).background(severityChartColors.getValue(sv)))
                                    Mono("${severityShort(sv)} $n", size = 11.sp, color = if (n == 0) cs.onSurfaceVariant else cs.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { ApkInfoPanel(state.metadata) }
        item {
            RePanel("Findings", trailing = "${visible.size} / ${state.security.size}") {
                FlowRow(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Severity.entries.forEach { s ->
                        val n = state.security.count { it.severity == s }
                        ReToggle("${severityShort(s)} $n", filter == s, severityChartColors.getValue(s)) { filter = if (filter == s) null else s }
                    }
                }
                HorizontalDivider()
                FindingsTable(visible, selected, onSelect = { picked = it })
            }
        }
        item { FindingDetail(selected) }
        item {
            RePanel("Attack surface", trailing = "exported / total") {
                val rows = if (m == null) emptyList() else listOf(
                    Triple("activities", m.activities.count { it.exported }, m.activities.size),
                    Triple("services", m.services.count { it.exported }, m.services.size),
                    Triple("receivers", m.receivers.count { it.exported }, m.receivers.size),
                    Triple("providers", m.providers.count { it.exported }, m.providers.size),
                    Triple("dangerous permissions", dangerous.size, state.permissions.size),
                )
                ReStaticTable(listOf(ReCol("Component", 2f), ReCol("Exposed", width = 90.dp), ReCol("", 3f)), rows) { (name, count, total), i ->
                    when (i) {
                        0 -> Mono(name)
                        1 -> Mono("$count / $total", bold = count > 0)
                        else -> Box(modifier = Modifier.padding(top = 5.dp).fillMaxWidth().height(6.dp).background(cs.surfaceContainerHigh)) {
                            val frac = if (total == 0) 0f else count.toFloat() / total
                            Box(modifier = Modifier.fillMaxWidth(frac).height(6.dp).background(if (frac > 0.5f) cs.error else cs.secondary))
                        }
                    }
                }
            }
        }
        if (state.secrets.isNotEmpty()) item {
            RePanel("Possible secrets", trailing = "${state.secrets.size}") { SecretsTable(state.secrets.take(10)) }
        }
        if (dangerous.isNotEmpty()) item {
            RePanel("Dangerous permissions", trailing = "${dangerous.size}") {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    dangerous.forEach { Mono(it.name, color = riskColor(PermissionRisk.DANGEROUS)) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.VDivider() {
    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun HorizontalDivider() = androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun FindingsTable(findings: List<SecurityFinding>, selected: SecurityFinding?, onSelect: (SecurityFinding) -> Unit) {
    ReStaticTable(
        cols = findingCols,
        rows = findings,
        selected = selected,
        onSelect = onSelect,
        emptyText = "— no findings —",
        accent = { severityChartColors[it.severity] },
    ) { f, i -> findingCell(f, i) }
}

private val findingCols = listOf(ReCol("Sev", width = 64.dp), ReCol("Rule", width = 84.dp), ReCol("Category", width = 96.dp), ReCol("Finding", 3f), ReCol("Evidence", 2f))

@Composable
private fun findingCell(f: SecurityFinding, i: Int) {
    when (i) {
        0 -> SevLabel(f.severity)
        1 -> Mono(f.ruleId, color = MaterialTheme.colorScheme.onSurfaceVariant)
        2 -> Mono(f.category, color = MaterialTheme.colorScheme.onSurfaceVariant)
        3 -> Mono(f.finding)
        else -> Mono(f.evidence, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FindingDetail(f: SecurityFinding?, modifier: Modifier = Modifier) {
    RePanel("Finding detail", modifier = modifier, trailing = f?.ruleId) {
        if (f == null) {
            Mono("select a finding", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(10.dp))
        } else {
            SelectionContainer {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    ReKv("severity", f.severity.name)
                    ReKv("category", f.category)
                    f.reference?.let { ReKv("standards", it) }
                    ReKv("finding", f.finding)
                    ReKv("evidence", f.evidence)
                    ReKv("impact", f.impact)
                    ReKv("recommendation", f.recommendation)
                }
            }
        }
    }
}

// ---- Security (full-page master/detail) ------------------------------------------

@Composable
internal fun SecurityTab(findings: List<SecurityFinding>) {
    var picked by remember(findings) { mutableStateOf<SecurityFinding?>(null) }
    val selected = picked ?: findings.firstOrNull()
    Column(modifier = Modifier.fillMaxSize().padding(vertical = gap), verticalArrangement = Arrangement.spacedBy(gap)) {
        ReTable(findingCols, findings, modifier = Modifier.weight(1f), selected = selected, onSelect = { picked = it }, emptyText = "— no findings —", accent = { severityChartColors[it.severity] }) { f, i -> findingCell(f, i) }
        Box(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) { FindingDetail(selected) }
    }
}

// ---- Overview / Manifest ----------------------------------------------------------

@Composable
private fun ApkInfoPanel(metadata: ApkMetadata?) {
    if (metadata == null) return
    RePanel("APK information") {
        SelectionContainer {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                ReKv("package", metadata.packageName)
                ReKv("version name", metadata.versionName)
                ReKv("version code", metadata.versionCode.toString())
                ReKv("min sdk", metadata.minSdk.toString())
                ReKv("target sdk", metadata.targetSdk.toString())
                ReKv("size", "${formatBytes(metadata.sizeBytes)}  (${metadata.sizeBytes} bytes)")
                ReKv("abis", metadata.abis.joinToString().ifBlank { "none (no native code)" })
                ReKv("dex files", metadata.dexFileCount.toString())
                ReKv("native libs", metadata.nativeLibraryCount.toString())
            }
        }
    }
}

private val componentCols = listOf(ReCol("Name", 4f), ReCol("Exported", width = 80.dp), ReCol("Permission", 2f), ReCol("Filters", width = 64.dp))

@Composable
internal fun ManifestTab(manifest: ManifestInfo?) {
    if (manifest == null) return
    LazyColumn(contentPadding = PaddingValues(vertical = gap), verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            RePanel("Application flags") {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    ReKv("debuggable", manifest.debuggable.toString(), 190.dp)
                    ReKv("allowBackup", manifest.allowBackup.toString(), 190.dp)
                    ReKv("usesCleartextTraffic", manifest.usesCleartextTraffic.toString(), 190.dp)
                    ReKv("networkSecurityConfig", manifest.usesNetworkSecurityConfig.toString(), 190.dp)
                }
            }
        }
        item { ComponentTable("Activities", manifest.activities) }
        item { ComponentTable("Services", manifest.services) }
        item { ComponentTable("Receivers", manifest.receivers) }
        item {
            RePanel("Providers", trailing = "${manifest.providers.size}") {
                ReStaticTable(
                    listOf(ReCol("Name", 3f), ReCol("Authority", 3f), ReCol("Exported", width = 80.dp), ReCol("Read / write permission", 2f)),
                    manifest.providers,
                ) { p, i ->
                    when (i) {
                        0 -> Mono(p.name)
                        1 -> Mono(p.authority, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        2 -> Mono(p.exported.toString(), bold = p.exported)
                        else -> Mono("${p.readPermission ?: "-"} / ${p.writePermission ?: "-"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentTable(title: String, components: List<ComponentInfo>) {
    RePanel(title, trailing = "${components.count { it.exported }} exported / ${components.size}") {
        ReStaticTable(componentCols, components, accent = { c -> if (c.exported) Color(0xFFE65100) else null }) { c, i ->
            when (i) {
                0 -> Mono(c.name)
                1 -> Mono(if (c.exported) "yes" else "no", bold = c.exported, color = if (c.exported) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                2 -> Mono(c.permission ?: "-", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Mono(c.intentFilterCount.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---- Permissions / Size / Structure ------------------------------------------------

@Composable
internal fun PermissionsTab(permissions: List<PermissionInfo>) {
    Box(modifier = Modifier.fillMaxSize().padding(vertical = gap)) {
        ReTable(
            listOf(ReCol("Risk", width = 100.dp), ReCol("Permission", 3f), ReCol("Description", 4f)),
            permissions.sortedBy { if (it.risk == PermissionRisk.DANGEROUS) -1 else it.risk.ordinal },
            emptyText = "— no permissions declared —",
            accent = { p -> if (p.risk == PermissionRisk.DANGEROUS) riskColor(p.risk) else null },
        ) { p, i ->
            when (i) {
                0 -> Mono(p.risk.name, color = riskColor(p.risk), bold = true, size = 11.sp)
                1 -> Mono(p.name)
                else -> Mono(p.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun SizeTab(size: SizeReport?) {
    if (size == null) return
    val cs = MaterialTheme.colorScheme
    val total = size.totalBytes.coerceAtLeast(1)
    LazyColumn(contentPadding = PaddingValues(vertical = gap), verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            RePanel("Size by category", trailing = formatBytes(size.totalBytes)) {
                ReStaticTable(
                    listOf(ReCol("Category", 2f), ReCol("Size", width = 100.dp), ReCol("%", width = 60.dp), ReCol("", 3f)),
                    size.byCategoryBytes.entries.sortedByDescending { it.value },
                ) { e, i ->
                    when (i) {
                        0 -> Mono(e.key)
                        1 -> Mono(formatBytes(e.value))
                        2 -> Mono("${e.value * 100 / total}")
                        else -> Box(modifier = Modifier.padding(top = 5.dp).fillMaxWidth().height(6.dp).background(cs.surfaceContainerHigh)) {
                            Box(modifier = Modifier.fillMaxWidth((e.value.toFloat() / total).coerceIn(0f, 1f)).height(6.dp).background(cs.secondary))
                        }
                    }
                }
            }
        }
        item {
            RePanel("Largest files") {
                ReStaticTable(listOf(ReCol("Size", width = 100.dp), ReCol("Path", 1f)), size.largestFiles) { (path, bytes), i ->
                    if (i == 0) Mono(formatBytes(bytes)) else Mono(path, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun StructureTab(files: List<ApkFileEntry>) {
    val sorted = remember(files) { files.sortedByDescending { it.sizeBytes } }
    Box(modifier = Modifier.fillMaxSize().padding(vertical = gap)) {
        ReTable(listOf(ReCol("Size", width = 100.dp), ReCol("Category", width = 110.dp), ReCol("Path", 1f)), sorted, emptyText = "— no files —") { f, i ->
            when (i) {
                0 -> Mono(formatBytes(f.sizeBytes))
                1 -> Mono(f.category.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Mono(f.path)
            }
        }
    }
}

// ---- Classes -----------------------------------------------------------------------

@Composable
internal fun ClassesTab(classes: List<DexClassInfo>) {
    val cs = MaterialTheme.colorScheme
    if (classes.isEmpty()) {
        Mono("no DEX classes decoded — the APK may have no classes.dex, or it could not be parsed.", color = cs.onSurfaceVariant, modifier = Modifier.padding(vertical = gap))
        return
    }
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<DexClassInfo?>(null) }
    val rows = remember(classes, query) {
        (if (query.isBlank()) classes else classes.filter { it.descriptor.contains(query, ignoreCase = true) })
            .sortedByDescending { it.methodCount }
            .take(3000)
    }
    val selected = picked?.takeIf { it in rows }

    Column(modifier = Modifier.fillMaxSize().padding(vertical = gap), verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = cs.onSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(cs.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth().border(1.dp, cs.outlineVariant, ReShape).background(cs.surfaceContainerLow, ReShape).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        if (query.isEmpty()) Mono("filter classes…  (okhttp, firebase, MainActivity)", color = cs.onSurfaceVariant)
                        inner()
                    }
                },
            )
            Mono(
                "${rows.size} / ${classes.size} classes",
                color = cs.onSurfaceVariant,
                size = 11.5.sp,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        ReTable(
            listOf(ReCol("Class", 5f), ReCol("Methods", width = 80.dp), ReCol("Fields", width = 70.dp), ReCol("Flags", width = 120.dp)),
            rows,
            modifier = Modifier.weight(1f),
            selected = selected,
            onSelect = { picked = it },
        ) { k, i ->
            when (i) {
                0 -> Mono(k.descriptor)
                1 -> Mono(k.methodCount.toString())
                2 -> Mono(k.fieldCount.toString())
                else -> Mono(listOfNotNull(k.visibility, if (k.isInterface) "interface" else null, if (k.isAbstract) "abstract" else null).joinToString(" "), color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (selected != null) {
            RePanel("Class detail", trailing = selected.dexFile) {
                SelectionContainer {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ReKv("descriptor", selected.descriptor)
                        ReKv("superclass", selected.superclass ?: "-")
                        ReKv("interfaces", selected.interfaces.joinToString().ifBlank { "none" })
                    }
                }
            }
        }
    }
}

// ---- Secrets / Signing -----------------------------------------------------------------

@Composable
private fun SecretsTable(secrets: List<SecretFinding>) {
    ReStaticTable(listOf(ReCol("Kind", 2f), ReCol("Value (masked)", 3f), ReCol("Source", 3f)), secrets) { s, i ->
        when (i) {
            0 -> Mono(s.kind.label, color = MaterialTheme.colorScheme.error, bold = true, size = 11.5.sp)
            1 -> Mono(s.maskedValue)
            else -> Mono(s.sourceFile, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SecretsTab(secrets: List<SecretFinding>) {
    Box(modifier = Modifier.fillMaxSize().padding(vertical = gap)) {
        ReTable(listOf(ReCol("Kind", 2f), ReCol("Value (masked)", 3f), ReCol("Source", 3f)), secrets, emptyText = "— no likely secrets found in DEX / native / resource / asset strings —") { s, i ->
            when (i) {
                0 -> Mono(s.kind.label, color = MaterialTheme.colorScheme.error, bold = true, size = 11.5.sp)
                1 -> Mono(s.maskedValue)
                else -> Mono(s.sourceFile, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun SigningTab(signing: SigningInfo?) {
    if (signing == null) {
        Mono("signing information unavailable — apksigner may not be installed.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = gap))
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = gap), verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            RePanel("Signature schemes", trailing = if (signing.isDebugSigned) "DEBUG CERTIFICATE" else null) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    ReKv("verified", signing.verified.toString())
                    signing.schemesVerified.forEach { (scheme, ok) -> ReKv(scheme, if (ok) "verified" else "not used") }
                }
            }
        }
        signing.signers.forEachIndexed { index, s ->
            item {
                RePanel("Signer #${index + 1}") {
                    SelectionContainer {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            ReKv("subject", s.subjectDn)
                            ReKv("sha-256", s.sha256)
                            ReKv("sha-1", s.sha1)
                            s.keyAlgorithm?.let { ReKv("key algorithm", it) }
                            s.keySizeBits?.let { ReKv("key size", "$it bits") }
                        }
                    }
                }
            }
        }
    }
}
