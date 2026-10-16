package dev.prateekthakur.devlens.ui.reports

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.prateekthakur.devlens.core.io.FilePicker
import dev.prateekthakur.devlens.core.io.PlatformFiles
import dev.prateekthakur.devlens.domain.apk.ApkAnalyzer
import dev.prateekthakur.devlens.domain.dex.DexClassAnalyzer
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.permission.PermissionAnalyzer
import dev.prateekthakur.devlens.domain.report.Report
import dev.prateekthakur.devlens.domain.report.ReportFormat
import dev.prateekthakur.devlens.domain.report.ReportGenerator
import dev.prateekthakur.devlens.domain.report.ReportSection
import dev.prateekthakur.devlens.domain.secrets.ApkSecretsAnalyzer
import dev.prateekthakur.devlens.domain.security.SecurityAnalyzer
import dev.prateekthakur.devlens.domain.signing.SigningAnalyzer
import dev.prateekthakur.devlens.domain.size.SizeAnalyzer
import dev.prateekthakur.devlens.ui.common.LocalSnackbarHostState
import dev.prateekthakur.devlens.ui.common.ScreenHeader
import dev.prateekthakur.devlens.ui.common.SectionCard
import dev.prateekthakur.devlens.ui.theme.Spacing
import kotlinx.coroutines.launch

/** PRD §31 — generate a full APK report (metadata, manifest, permissions, security, size) and export it. */
@Composable
fun ReportsScreen(
    apkAnalyzer: ApkAnalyzer,
    manifestAnalyzer: ManifestAnalyzer,
    permissionAnalyzer: PermissionAnalyzer,
    securityAnalyzer: SecurityAnalyzer,
    sizeAnalyzer: SizeAnalyzer,
    dexClassAnalyzer: DexClassAnalyzer,
    secretsAnalyzer: ApkSecretsAnalyzer,
    signingAnalyzer: SigningAnalyzer,
    reportGenerator: ReportGenerator,
) {
    var apkPath by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(ReportFormat.MARKDOWN) }
    var formatMenuExpanded by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    val files = remember { PlatformFiles() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current

    fun generate() {
        if (apkPath.isBlank()) return
        generating = true
        scope.launch {
            val metadata = apkAnalyzer.analyze(apkPath).getOrNull()
            val manifest = manifestAnalyzer.analyze(apkPath).getOrNull()
            val permissions = permissionAnalyzer.analyze(apkPath).getOrDefault(emptyList())
            val security = securityAnalyzer.analyze(apkPath).getOrDefault(emptyList())
            val size = sizeAnalyzer.analyze(apkPath).getOrNull()
            val classes = dexClassAnalyzer.analyze(apkPath).getOrDefault(emptyList())
            val secrets = secretsAnalyzer.analyze(apkPath).getOrDefault(emptyList())
            val signing = signingAnalyzer.analyze(apkPath).getOrNull()

            val sections = buildList {
                if (metadata != null) {
                    add(
                        ReportSection(
                            "Overview",
                            "Package: ${metadata.packageName}\nVersion: ${metadata.versionName} (${metadata.versionCode})\n" +
                                "Min/Target SDK: ${metadata.minSdk}/${metadata.targetSdk}\nSize: ${metadata.sizeBytes} bytes\nABIs: ${metadata.abis.joinToString()}",
                        ),
                    )
                }
                if (manifest != null) {
                    add(
                        ReportSection(
                            "Manifest",
                            "Debuggable: ${manifest.debuggable}\nAllow backup: ${manifest.allowBackup}\n" +
                                "Cleartext traffic: ${manifest.usesCleartextTraffic}\nActivities: ${manifest.activities.size}, " +
                                "Services: ${manifest.services.size}, Receivers: ${manifest.receivers.size}, Providers: ${manifest.providers.size}",
                        ),
                    )
                }
                if (permissions.isNotEmpty()) {
                    add(
                        ReportSection(
                            "Permissions (${permissions.size})",
                            permissions.joinToString("\n") { "- [${it.risk}] ${it.name}" },
                        ),
                    )
                }
                if (security.isNotEmpty()) {
                    add(
                        ReportSection(
                            "Security Findings (${security.size})",
                            security.joinToString("\n\n") { "${it.ruleId} [${it.severity}] ${it.finding}\n  Recommendation: ${it.recommendation}" },
                        ),
                    )
                } else {
                    add(ReportSection("Security Findings", "No findings."))
                }
                if (size != null) {
                    add(
                        ReportSection(
                            "Size",
                            "Total: ${size.totalBytes} bytes\n" + size.byCategoryBytes.entries.joinToString("\n") { "- ${it.key}: ${it.value} bytes" },
                        ),
                    )
                }
                if (signing != null) {
                    add(
                        ReportSection(
                            "Signing",
                            "Verified: ${signing.verified}\nDebug-signed: ${signing.isDebugSigned}\n" +
                                "Schemes: " + signing.schemesVerified.entries.joinToString(", ") { "${it.key}=${it.value}" } + "\n" +
                                signing.signers.joinToString("\n") { "- ${it.subjectDn} (SHA-256: ${it.sha256})" },
                        ),
                    )
                }
                if (classes.isNotEmpty()) {
                    add(
                        ReportSection(
                            "DEX Classes",
                            "Total classes: ${classes.size} across ${classes.map { it.dexFile }.distinct().size} DEX file(s)\n" +
                                "Total methods: ${classes.sumOf { it.methodCount }}",
                        ),
                    )
                }
                if (secrets.isNotEmpty()) {
                    add(
                        ReportSection(
                            "Secrets Detected (${secrets.size})",
                            secrets.joinToString("\n") { "- [${it.kind.label}] ${it.maskedValue} (${it.sourceFile})" },
                        ),
                    )
                }
            }

            val report = Report(title = "DevLens APK Report — ${metadata?.packageName ?: apkPath}", sections = sections)
            preview = reportGenerator.render(report, format)
            generating = false
            snackbarHostState.showSnackbar("Report generated")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("Reports", "Combine metadata, manifest, permissions, security and size into one export")

        SectionCard(modifier = Modifier.padding(top = Spacing.lg)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(apkPath, { apkPath = it }, label = { Text("APK path") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { scope.launch { FilePicker.pickFile("Open APK", "apk")?.let { apkPath = it } } }, modifier = Modifier.padding(start = Spacing.sm)) {
                    Text("Browse")
                }
            }
            Row(modifier = Modifier.padding(top = Spacing.md), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box {
                    OutlinedButton(onClick = { formatMenuExpanded = true }) { Text(format.name) }
                    DropdownMenu(expanded = formatMenuExpanded, onDismissRequest = { formatMenuExpanded = false }) {
                        ReportFormat.entries.forEach { f ->
                            DropdownMenuItem(text = { Text(f.name) }, onClick = { format = f; formatMenuExpanded = false })
                        }
                    }
                }
                Button(onClick = { generate() }, modifier = Modifier.padding(start = Spacing.sm), enabled = !generating) {
                    if (generating) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Generate")
                    }
                }
                Button(
                    onClick = {
                        scope.launch {
                            val extension = when (format) {
                                ReportFormat.MARKDOWN -> "md"
                                ReportFormat.JSON -> "json"
                                ReportFormat.PLAIN_TEXT -> "txt"
                            }
                            val path = FilePicker.pickSaveFile("Save Report", "devlens-report.$extension") ?: return@launch
                            files.writeText(path, preview)
                                .onSuccess { snackbarHostState.showSnackbar("Saved to $path") }
                                .onFailure { snackbarHostState.showSnackbar("Failed to save: ${it.message}") }
                        }
                    },
                    modifier = Modifier.padding(start = Spacing.sm),
                    enabled = preview.isNotBlank(),
                ) { Text("Export") }
            }
        }

        SectionCard(title = "Preview", modifier = Modifier.padding(top = Spacing.lg)) {
            OutlinedTextField(
                value = preview,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().height(400.dp),
            )
        }
    }
}
