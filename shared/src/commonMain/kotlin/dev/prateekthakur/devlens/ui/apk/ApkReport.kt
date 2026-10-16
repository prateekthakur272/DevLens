package dev.prateekthakur.devlens.ui.apk

import dev.prateekthakur.devlens.domain.report.Report
import dev.prateekthakur.devlens.domain.report.ReportSection

/** Builds an exportable [Report] from an already finished analysis — nothing is re-analyzed. */
internal fun buildApkReport(state: ApkAnalysisState, path: String): Report {
    val metadata = state.metadata
    val manifest = state.manifest
    val permissions = state.permissions
    val security = state.security
    val size = state.size
    val classes = state.dexClasses
    val secrets = state.secrets
    val signing = state.signing

    val sections = buildList {
        if (metadata != null) {
            add(
                ReportSection(
                    "Overview",
                    "Package: ${metadata.packageName}\nVersion: ${metadata.versionName} (${metadata.versionCode})\n" +
                        "Min/Target SDK: ${metadata.minSdk}/${metadata.targetSdk}\nSize: ${formatBytes(metadata.sizeBytes)}\nABIs: ${metadata.abis.joinToString()}",
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
            add(ReportSection("Permissions (${permissions.size})", permissions.joinToString("\n") { "- [${it.risk}] ${it.name}" }))
        }
        if (security.isNotEmpty()) {
            add(
                ReportSection(
                    "Security Findings (${security.size})",
                    security.joinToString("\n\n") {
                        "${it.ruleId} [${it.severity}] (${it.category}${it.reference?.let { r -> " · $r" } ?: ""}) ${it.finding}\n  Evidence: ${it.evidence}\n  Impact: ${it.impact}\n  Recommendation: ${it.recommendation}"
                    },
                ),
            )
        } else {
            add(ReportSection("Security Findings", "No findings."))
        }
        if (size != null) {
            add(
                ReportSection(
                    "Size",
                    "Total: ${formatBytes(size.totalBytes)}\n" + size.byCategoryBytes.entries.joinToString("\n") { "- ${it.key}: ${formatBytes(it.value)}" },
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
    return Report(title = "DevLens APK Report — ${metadata?.packageName ?: path}", sections = sections)
}

/** Structured snapshot of a finished analysis for the PDF writer. */
internal fun buildApkReportData(state: ApkAnalysisState, path: String) = dev.prateekthakur.devlens.domain.report.ApkReportData(
    fileName = path.substringAfterLast('/').substringAfterLast('\\'),
    appLabel = state.appLabel,
    icon = state.iconPng,
    path = path,
    score = securityScore(state.security, state.secrets.size),
    metadata = state.metadata,
    manifest = state.manifest,
    permissions = state.permissions,
    security = state.security,
    size = state.size,
    secrets = state.secrets,
    signing = state.signing,
    classCount = state.dexClasses.size,
    methodCount = state.dexClasses.sumOf { it.methodCount },
)

/** `report_<app>_<version>_<yyyy-MM-dd>` — app is the launcher label (else the package's last segment, else the file name). */
internal fun reportBaseName(state: ApkAnalysisState, path: String): String {
    fun clean(v: String) = v.map { if (it.isLetterOrDigit() || it == '.' || it == '-') it else '_' }.joinToString("").replace(Regex("_+"), "_").trim('_', '.')
    val app = state.appLabel?.takeIf { clean(it).isNotBlank() }
        ?: state.metadata?.packageName?.substringAfterLast('.')?.takeIf { it.isNotBlank() }
        ?: path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
    val version = state.metadata?.versionName?.takeIf { it.isNotBlank() } ?: "unknown"
    return "report_${clean(app)}_${clean(version)}_${dev.prateekthakur.devlens.core.io.todayStamp()}"
}
