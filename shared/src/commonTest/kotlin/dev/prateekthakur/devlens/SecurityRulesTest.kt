package dev.prateekthakur.devlens

import dev.prateekthakur.devlens.domain.apk.ApkFileCategory
import dev.prateekthakur.devlens.domain.apk.ApkFileEntry
import dev.prateekthakur.devlens.domain.apk.ApkMetadata
import dev.prateekthakur.devlens.domain.dex.DexClassInfo
import dev.prateekthakur.devlens.domain.manifest.ComponentInfo
import dev.prateekthakur.devlens.domain.manifest.CustomPermission
import dev.prateekthakur.devlens.domain.manifest.IntentFilterInfo
import dev.prateekthakur.devlens.domain.manifest.ManifestInfo
import dev.prateekthakur.devlens.domain.manifest.ProviderInfo
import dev.prateekthakur.devlens.domain.permission.PermissionInfo
import dev.prateekthakur.devlens.domain.permission.PermissionRisk
import dev.prateekthakur.devlens.domain.secrets.SecretFinding
import dev.prateekthakur.devlens.domain.secrets.SecretKind
import dev.prateekthakur.devlens.domain.security.SecurityInput
import dev.prateekthakur.devlens.domain.security.SecurityRules
import dev.prateekthakur.devlens.domain.security.Severity
import dev.prateekthakur.devlens.domain.signing.SignerCertificate
import dev.prateekthakur.devlens.domain.signing.SigningInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityRulesTest {

    private fun manifest(
        debuggable: Boolean = false, allowBackup: Boolean = false, cleartext: Boolean = false, nsc: Boolean = true,
        minSdk: Int = 26, targetSdk: Int = 35, perms: List<String> = emptyList(),
        activities: List<ComponentInfo> = emptyList(), services: List<ComponentInfo> = emptyList(),
        receivers: List<ComponentInfo> = emptyList(), providers: List<ProviderInfo> = emptyList(),
        testOnly: Boolean = false, backupRules: Boolean = false, custom: List<CustomPermission> = emptyList(),
    ) = ManifestInfo(
        "com.example.app", "1.0", 1, minSdk, targetSdk, debuggable, allowBackup, cleartext, nsc, perms,
        activities, services, receivers, providers, testOnly, false, backupRules, custom,
    )

    private fun ids(input: SecurityInput) = SecurityRules.evaluate(input).map { it.ruleId }

    private val launcher = ComponentInfo(
        "com.example.Main", true, null, 1,
        intentFilters = listOf(IntentFilterInfo(listOf("android.intent.action.MAIN"), listOf("android.intent.category.LAUNCHER"), emptyList(), emptyList(), false)),
    )

    @Test
    fun cleanAppProducesNoFindings() {
        val result = SecurityRules.evaluate(SecurityInput(manifest = manifest(activities = listOf(launcher))))
        assertTrue(result.isEmpty(), "unexpected: ${result.map { it.ruleId }}")
    }

    @Test
    fun launcherActivityIsNotReportedAsExposed() {
        assertFalse("SEC-005" in ids(SecurityInput(manifest = manifest(activities = listOf(launcher)))))
    }

    @Test
    fun debuggableAndTestOnlyAreFlagged() {
        val ids = ids(SecurityInput(manifest = manifest(debuggable = true, testOnly = true)))
        assertTrue("SEC-001" in ids)
        assertTrue("SEC-011" in ids)
        assertEquals(Severity.CRITICAL, SecurityRules.evaluate(SecurityInput(manifest = manifest(debuggable = true))).first().severity)
    }

    @Test
    fun backupRuleOnlyFiresWhenUnscoped() {
        assertTrue("SEC-002" in ids(SecurityInput(manifest = manifest(allowBackup = true))))
        assertFalse("SEC-002" in ids(SecurityInput(manifest = manifest(allowBackup = true, backupRules = true))))
        assertFalse("SEC-002" in ids(SecurityInput(manifest = manifest(allowBackup = false))))
    }

    @Test
    fun sdkLevelRules() {
        val old = SecurityRules.evaluate(SecurityInput(manifest = manifest(minSdk = 19, targetSdk = 28)))
        assertEquals(Severity.HIGH, old.first { it.ruleId == "SEC-012" }.severity)
        assertEquals(Severity.MEDIUM, old.first { it.ruleId == "SEC-013" }.severity)
        assertTrue("SEC-012" !in ids(SecurityInput(manifest = manifest(targetSdk = 35))))
    }

    @Test
    fun exportedComponentRules() {
        val exposed = ComponentInfo("com.example.Exposed", true, null, 0)
        val protected = ComponentInfo("com.example.Protected", true, "com.example.PERM", 0)
        val ids = ids(
            SecurityInput(
                manifest = manifest(
                    activities = listOf(exposed, protected), services = listOf(exposed), receivers = listOf(exposed),
                    providers = listOf(ProviderInfo("com.example.P", "auth", true, null, null, grantUriPermissions = true)),
                ),
            ),
        )
        assertTrue(listOf("SEC-005", "SEC-015", "SEC-016", "SEC-006", "SEC-017").all { it in ids }, "got $ids")
    }

    @Test
    fun taskHijackingNeedsExportedNonLauncherWithRiskyLaunchMode() {
        val hijackable = ComponentInfo("com.example.A", true, null, 0, launchMode = 2)
        assertTrue("SEC-018" in ids(SecurityInput(manifest = manifest(activities = listOf(hijackable)))))
        assertFalse("SEC-018" in ids(SecurityInput(manifest = manifest(activities = listOf(hijackable.copy(exported = false))))))
    }

    @Test
    fun weakCustomPermissionAndDeepLinks() {
        val link = ComponentInfo(
            "com.example.Link", true, "perm", 1,
            intentFilters = listOf(IntentFilterInfo(listOf("android.intent.action.VIEW"), listOf("android.intent.category.BROWSABLE"), listOf("myapp", "https"), listOf("example.com"), false)),
        )
        val ids = ids(SecurityInput(manifest = manifest(activities = listOf(link), custom = listOf(CustomPermission("com.example.P", 0)))))
        assertTrue("SEC-019" in ids)
        assertTrue("SEC-020" in ids)
        assertTrue("SEC-021" in ids)
    }

    @Test
    fun networkRules() {
        val ids = ids(SecurityInput(manifest = manifest(cleartext = true, nsc = false, perms = listOf("android.permission.INTERNET")), classes = listOf(cls("Lcom/x/A;", "com.x", "A"))))
        assertTrue("SEC-003" in ids)
        assertTrue("SEC-004" in ids)
        assertTrue("SEC-022" in ids)
    }

    @Test
    fun sensitivePermissionGroups() {
        val ids = ids(SecurityInput(manifest = manifest(perms = listOf("android.permission.READ_SMS", "android.permission.SYSTEM_ALERT_WINDOW", "android.permission.CAMERA"))))
        assertTrue("SEC-030" in ids)
        assertTrue("SEC-034" in ids)
        assertFalse("SEC-031" in ids)
    }

    @Test
    fun signingRules() {
        val debug = SigningInfo(true, mapOf("v1" to true), listOf(SignerCertificate("CN=Android Debug, O=Android", "a", "b", "RSA", 1024)))
        val ids = ids(SecurityInput(signing = debug))
        assertTrue("SEC-009" in ids && "SEC-010" in ids && "SEC-040" in ids, "got $ids")
        val good = SigningInfo(true, mapOf("v1" to true, "v2" to true), listOf(SignerCertificate("CN=Release", "a", "b", "RSA", 4096)))
        assertTrue(SecurityRules.evaluate(SecurityInput(signing = good)).isEmpty())
    }

    @Test
    fun secretsAreGroupedPerKindWithSeverity() {
        val secrets = listOf(
            SecretFinding(SecretKind.PRIVATE_KEY, "-----BEGIN***", "assets/k.pem"),
            SecretFinding(SecretKind.GOOGLE_API_KEY, "AIzaSy***1", "classes.dex"),
            SecretFinding(SecretKind.GOOGLE_API_KEY, "AIzaSy***2", "classes.dex"),
        )
        val result = SecurityRules.evaluate(SecurityInput(secrets = secrets)).filter { it.ruleId == "SEC-050" }
        assertEquals(2, result.size)
        assertEquals(Severity.CRITICAL, result.first().severity)
        assertTrue(result.any { it.finding.contains("(2)") })
    }

    @Test
    fun fileAndCodeRules() {
        val files = listOf("assets/release.keystore", "assets/payload.apk", "assets/flutter_assets/kernel_blob.bin", "META-INF/CERT.RSA")
            .map { ApkFileEntry(it, 10, ApkFileCategory.ASSETS) }
        val classes = List(400) { cls("Lcom/app/Class$it;", "com.app", "Class$it") } + cls("Lcom/facebook/stetho/Stetho;", "com.facebook.stetho", "Stetho")
        val meta = ApkMetadata("com.example.app", "1.0", 1, 24, 35, 1000, listOf("armeabi-v7a"), 1, 1)
        val result = ids(SecurityInput(metadata = meta, files = files, classes = classes))
        assertTrue(listOf("SEC-060", "SEC-061", "SEC-062", "SEC-063", "SEC-064", "SEC-065").all { it in result }, "got $result")
        // META-INF signature blocks must not be reported as sensitive files.
        val sensitive = SecurityRules.evaluate(SecurityInput(files = files)).first { it.ruleId == "SEC-060" }
        assertFalse(sensitive.evidence.contains("CERT.RSA"))
    }

    @Test
    fun findingsAreSortedBySeverityThenRule() {
        val result = SecurityRules.evaluate(SecurityInput(manifest = manifest(debuggable = true, allowBackup = true, cleartext = true)))
        assertEquals(result.sortedWith(compareBy({ it.severity.ordinal }, { it.ruleId })), result)
    }

    @Test
    fun everyFindingHasCategoryAndStandardsReference() {
        val all = SecurityRules.evaluate(
            SecurityInput(
                manifest = manifest(debuggable = true, allowBackup = true, cleartext = true, nsc = false, minSdk = 19, targetSdk = 28, perms = listOf("android.permission.INTERNET", "android.permission.READ_SMS")),
            ),
        )
        assertTrue(all.isNotEmpty())
        assertTrue(all.all { it.category != "General" && !it.reference.isNullOrBlank() }, "missing metadata: ${all.filter { it.reference.isNullOrBlank() }.map { it.ruleId }}")
    }

    private fun cls(descriptor: String, pkg: String, name: String) =
        DexClassInfo(descriptor, pkg, name, null, emptyList(), false, false, false, "public", 0, 1, "classes.dex")
}
