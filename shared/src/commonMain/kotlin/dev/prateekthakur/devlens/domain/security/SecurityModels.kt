package dev.prateekthakur.devlens.domain.security

import dev.prateekthakur.devlens.domain.apk.ApkAnalyzer
import dev.prateekthakur.devlens.domain.apk.ApkFileEntry
import dev.prateekthakur.devlens.domain.apk.ApkMetadata
import dev.prateekthakur.devlens.domain.dex.DexClassAnalyzer
import dev.prateekthakur.devlens.domain.dex.DexClassInfo
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.manifest.ManifestInfo
import dev.prateekthakur.devlens.domain.permission.PermissionAnalyzer
import dev.prateekthakur.devlens.domain.permission.PermissionInfo
import dev.prateekthakur.devlens.domain.secrets.ApkSecretsAnalyzer
import dev.prateekthakur.devlens.domain.secrets.SecretFinding
import dev.prateekthakur.devlens.domain.signing.SigningAnalyzer
import dev.prateekthakur.devlens.domain.signing.SigningInfo

/** PRD §16 — security findings are produced by deterministic rules, never by AI. */
enum class Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }

data class SecurityFinding(
    val ruleId: String,
    val severity: Severity,
    val finding: String,
    val evidence: String,
    val impact: String,
    val recommendation: String,
    /** Area of the app the rule is about: Manifest, Components, Network, Permissions, Signing, Secrets, Files, Code. */
    val category: String = "General",
    /** Standards the rule maps to, e.g. `MASVS-NETWORK-1 · CWE-319`. */
    val reference: String? = null,
)

/** Everything the rules look at. Anything that could not be read is null/empty and its rules are skipped. */
data class SecurityInput(
    val manifest: ManifestInfo? = null,
    val permissions: List<PermissionInfo> = emptyList(),
    val signing: SigningInfo? = null,
    val metadata: ApkMetadata? = null,
    val files: List<ApkFileEntry> = emptyList(),
    val classes: List<DexClassInfo> = emptyList(),
    val secrets: List<SecretFinding> = emptyList(),
    /** True for an `.aab`: bundles carry a JAR upload-key signature, so APK signature-scheme rules do not apply. */
    val isBundle: Boolean = false,
)

interface SecurityAnalyzer {
    suspend fun analyze(apkPath: String): Result<List<SecurityFinding>>

    /** Runs the rules over already-gathered analysis results — no I/O, so it is cheap and deterministic. */
    fun evaluate(input: SecurityInput): List<SecurityFinding>
}

/**
 * PRD §16 — deterministic, rule-based security findings (see [SecurityRules]). AI is never consulted here;
 * it may only explain a finding this engine already produced (PRD §47).
 */
class SecurityRuleEngine(
    private val manifestAnalyzer: ManifestAnalyzer,
    private val permissionAnalyzer: PermissionAnalyzer,
    private val signingAnalyzer: SigningAnalyzer? = null,
    private val apkAnalyzer: ApkAnalyzer? = null,
    private val dexClassAnalyzer: DexClassAnalyzer? = null,
    private val secretsAnalyzer: ApkSecretsAnalyzer? = null,
) : SecurityAnalyzer {

    override suspend fun analyze(apkPath: String): Result<List<SecurityFinding>> {
        val manifest = manifestAnalyzer.analyze(apkPath).getOrElse { return Result.failure(it) }
        val input = SecurityInput(
            manifest = manifest,
            permissions = permissionAnalyzer.analyze(apkPath).getOrElse { emptyList() },
            signing = signingAnalyzer?.analyze(apkPath)?.getOrNull(),
            metadata = apkAnalyzer?.analyze(apkPath)?.getOrNull(),
            files = apkAnalyzer?.listFiles(apkPath)?.getOrNull().orEmpty(),
            classes = dexClassAnalyzer?.analyze(apkPath)?.getOrNull().orEmpty(),
            secrets = secretsAnalyzer?.analyze(apkPath)?.getOrNull().orEmpty(),
        )
        return Result.success(evaluate(input))
    }

    override fun evaluate(input: SecurityInput): List<SecurityFinding> = SecurityRules.evaluate(input)
}
