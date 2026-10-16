package dev.prateekthakur.devlens.domain.security

import dev.prateekthakur.devlens.domain.manifest.ComponentInfo
import dev.prateekthakur.devlens.domain.permission.PermissionRisk
import dev.prateekthakur.devlens.domain.secrets.SecretKind

/**
 * The rule catalogue. Each rule is a small pure function over [SecurityInput]; every finding carries a stable
 * rule id, a category and the OWASP MASVS control / CWE it maps to, so results are explainable and auditable.
 *
 * Rule ids:
 *  Manifest & platform  SEC-001 debuggable · 002 backup · 011 testOnly · 012 targetSdk · 013 minSdk · 014 legacy storage
 *  Components           005 activity · 006 provider · 007 intent filters · 015 service · 016 receiver · 017 provider URI grants
 *                       018 task hijacking · 019 weak custom permission · 020 deep links
 *  Network              003 cleartext · 004 no network security config · 022 no pinning indicators
 *  Permissions          008 dangerous summary · 030-039 sensitive permission groups
 *  Signing              009 debug certificate · 010 no v2+ · 040 weak key
 *  Secrets              050 hardcoded secrets (one finding per kind)
 *  Files & code         060 sensitive files · 061 embedded code · 062 Flutter debug build · 063 dev tooling
 *                       064 not obfuscated · 065 no 64-bit libraries
 */
object SecurityRules {

    /** Google Play's minimum target API for updates (raised every August); below this is a policy and hardening gap. */
    private const val PLAY_TARGET_SDK = 35

    fun evaluate(input: SecurityInput): List<SecurityFinding> {
        val out = mutableListOf<SecurityFinding>()
        manifestRules(input, out)
        componentRules(input, out)
        networkRules(input, out)
        permissionRules(input, out)
        signingRules(input, out)
        secretRules(input, out)
        fileAndCodeRules(input, out)
        return out.sortedWith(compareBy({ it.severity.ordinal }, { it.ruleId }))
    }

    private fun f(
        id: String, sev: Severity, category: String, finding: String, evidence: String,
        impact: String, recommendation: String, reference: String,
    ) = SecurityFinding(id, sev, finding, evidence, impact, recommendation, category, reference)

    private fun list(items: List<String>, max: Int = 6): String =
        items.take(max).joinToString(", ") + if (items.size > max) " (+${items.size - max} more)" else ""

    // ---- Manifest & platform -------------------------------------------------------

    private fun manifestRules(input: SecurityInput, out: MutableList<SecurityFinding>) {
        val m = input.manifest ?: return

        if (m.debuggable) out += f(
            "SEC-001", Severity.CRITICAL, "Manifest", "Application is debuggable.",
            "AndroidManifest.xml: application android:debuggable=\"true\"",
            "A debuggable build lets any local user or app attach a debugger, read process memory and bypass the app sandbox on non-rooted devices.",
            "Never ship debuggable=true. Remove the attribute or set it to false for release build types.",
            "MASVS-RESILIENCE-4 · CWE-489",
        )

        if (m.testOnly) out += f(
            "SEC-011", Severity.HIGH, "Manifest", "Application is marked test-only.",
            "AndroidManifest.xml: application android:testOnly=\"true\"",
            "Test-only builds are not meant for distribution, are usually unobfuscated debug builds, and cannot be installed from Google Play.",
            "Build the release variant; remove android:testOnly (it is added automatically by some IDE run configurations).",
            "MASVS-RESILIENCE-4 · CWE-489",
        )

        if (m.allowBackup && !m.hasBackupRules) out += f(
            "SEC-002", Severity.MEDIUM, "Manifest", "Application allows unrestricted backup.",
            "android:allowBackup is true or unspecified (defaults to true) and no fullBackupContent / dataExtractionRules scope it.",
            "Private app data (databases, shared preferences, tokens) can be extracted with adb backup or cloud backup.",
            "Set android:allowBackup=\"false\", or exclude sensitive files with android:dataExtractionRules (API 31+) and android:fullBackupContent.",
            "MASVS-STORAGE-2 · CWE-530",
        )

        m.targetSdk?.let { target ->
            if (target < PLAY_TARGET_SDK) out += f(
                "SEC-012", if (target < 29) Severity.HIGH else Severity.MEDIUM, "Manifest", "Targets an outdated Android API level ($target).",
                "targetSdkVersion=$target (current Play requirement: $PLAY_TARGET_SDK or higher)",
                "Apps targeting older APIs opt out of newer platform protections (scoped storage, background limits, safer defaults for exported components and cleartext traffic) and are rejected by Google Play.",
                "Raise targetSdkVersion to $PLAY_TARGET_SDK or later and test the behavior changes.",
                "MASVS-PLATFORM-1 · CWE-1104",
            )
        }

        m.minSdk?.let { min ->
            if (min < 24) out += f(
                "SEC-013", if (min < 21) Severity.MEDIUM else Severity.LOW, "Manifest", "Supports very old Android versions (minSdk $min).",
                "minSdkVersion=$min",
                "Devices below API 24 no longer receive security patches and lack protections such as network security config defaults, file-based encryption and modern TLS.",
                "Raise minSdkVersion to at least 24 (preferably 26+) unless the user base requires otherwise.",
                "MASVS-PLATFORM-1 · CWE-1104",
            )
        }

        if (m.requestLegacyExternalStorage) out += f(
            "SEC-014", if ((m.targetSdk ?: 0) <= 29) Severity.MEDIUM else Severity.LOW, "Manifest", "Opts out of scoped storage.",
            "AndroidManifest.xml: application android:requestLegacyExternalStorage=\"true\"",
            "Broad access to shared storage lets other apps read files this app writes, and exposes the app's files to any app with storage permission.",
            "Use scoped storage (MediaStore, Storage Access Framework) and keep sensitive files in app-private storage.",
            "MASVS-STORAGE-2 · CWE-922",
        )
    }

    // ---- Components ------------------------------------------------------------------

    private fun componentRules(input: SecurityInput, out: MutableList<SecurityFinding>) {
        val m = input.manifest ?: return

        fun exposed(components: List<ComponentInfo>) =
            components.filter { it.exported && it.enabled && it.permission == null && !it.isLauncher }

        exposed(m.activities).let { list ->
            if (list.isNotEmpty()) out += f(
                "SEC-005", Severity.MEDIUM, "Components", "Exported activities without a required permission.",
                list(list.map { it.name }),
                "Any app on the device can start these activities with crafted extras — a common route to authentication bypass and intent redirection.",
                "Set android:exported=\"false\" unless external access is required; otherwise require a signature permission and validate every extra and URI.",
                "MASVS-PLATFORM-1 · CWE-926",
            )
        }
        exposed(m.services).let { list ->
            if (list.isNotEmpty()) out += f(
                "SEC-015", Severity.HIGH, "Components", "Exported services without a required permission.",
                list(list.map { it.name }),
                "Any app can start or bind to these services and invoke their functionality, often with the app's own privileges.",
                "Set android:exported=\"false\", or protect the service with a signature-level permission and verify the caller.",
                "MASVS-PLATFORM-1 · CWE-926",
            )
        }
        exposed(m.receivers).let { list ->
            if (list.isNotEmpty()) out += f(
                "SEC-016", Severity.MEDIUM, "Components", "Exported broadcast receivers without a required permission.",
                list(list.map { it.name }),
                "Any app can send broadcasts to these receivers, spoofing system or internal events.",
                "Set android:exported=\"false\", register receivers at runtime with RECEIVER_NOT_EXPORTED, or require a permission.",
                "MASVS-PLATFORM-1 · CWE-925",
            )
        }

        val openProviders = m.providers.filter { it.exported && it.readPermission == null && it.writePermission == null }
        if (openProviders.isNotEmpty()) out += f(
            "SEC-006", Severity.HIGH, "Components", "Exported content providers without read/write permission.",
            list(openProviders.map { "${it.name} (${it.authority})" }),
            "Any app can query or modify the provider's data, and unsanitized selection arguments allow SQL injection and path traversal.",
            "Set android:exported=\"false\" or require readPermission/writePermission; parameterize queries and validate paths.",
            "MASVS-PLATFORM-1 · CWE-200",
        )

        val grantingExported = m.providers.filter { it.exported && it.grantUriPermissions }
        if (grantingExported.isNotEmpty()) out += f(
            "SEC-017", Severity.HIGH, "Components", "Exported provider grants URI permissions.",
            list(grantingExported.map { it.name }),
            "An exported provider with grantUriPermissions lets other apps obtain access to arbitrary content URIs it serves — a known route to private file theft (FileProvider must never be exported).",
            "Set android:exported=\"false\" and grant URI access per-intent with FLAG_GRANT_READ_URI_PERMISSION; restrict paths in grant-uri-permission.",
            "MASVS-PLATFORM-1 · CWE-284",
        )

        val hijackable = m.activities.filter {
            it.exported && it.enabled && !it.isLauncher &&
                (it.launchMode >= 2 || it.allowTaskReparenting || (it.taskAffinity != null && it.taskAffinity != m.packageName && it.taskAffinity.isNotEmpty()))
        }
        if (hijackable.isNotEmpty()) out += f(
            "SEC-018", Severity.MEDIUM, "Components", "Activities susceptible to task hijacking.",
            list(hijackable.map { a -> "${a.name} (launchMode=${a.launchMode}${a.taskAffinity?.let { ", taskAffinity=$it" } ?: ""}${if (a.allowTaskReparenting) ", allowTaskReparenting" else ""})" }),
            "Exported activities with singleTask/singleInstance, a custom taskAffinity or task reparenting can be placed into another app's task stack (StrandHogg-style phishing and UI redress).",
            "Use the default launchMode and an empty taskAffinity (taskAffinity=\"\") for non-launcher activities; set exported=false where possible.",
            "MASVS-PLATFORM-3 · CWE-1021",
        )

        val weakPermissions = m.customPermissions.filter { it.protectionLevel <= 1 }
        if (weakPermissions.isNotEmpty()) out += f(
            "SEC-019", Severity.MEDIUM, "Components", "Custom permissions with a weak protection level.",
            list(weakPermissions.map { "${it.name} (${if (it.protectionLevel == 0) "normal" else "dangerous"})" }),
            "Normal permissions are granted to any app that asks; they offer no real access control for the components they guard, and another app can pre-register the same name.",
            "Use android:protectionLevel=\"signature\" for permissions that protect your own components.",
            "MASVS-PLATFORM-1 · CWE-284",
        )

        val filters = (m.activities + m.services + m.receivers).filter { it.exported && it.enabled }
            .flatMap { c -> c.intentFilters.map { c to it } }
            .filter { (_, f) -> "android.intent.category.BROWSABLE" in f.categories }
        val customScheme = filters.filter { (_, f) -> f.schemes.any { it !in setOf("http", "https") } }
        if (customScheme.isNotEmpty()) out += f(
            "SEC-020", Severity.LOW, "Components", "Deep links use custom URI schemes.",
            list(customScheme.flatMap { (c, f) -> f.schemes.filter { it !in setOf("http", "https") }.map { "$it:// → ${c.name.substringAfterLast('.')}" } }.distinct()),
            "Any app can register the same custom scheme and intercept links meant for this app (scheme hijacking), including OAuth redirects and tokens.",
            "Prefer verified HTTPS App Links (android:autoVerify=\"true\") and never pass secrets in deep-link parameters; validate all link data.",
            "MASVS-PLATFORM-1 · CWE-939",
        )
        val unverifiedWeb = filters.filter { (_, f) -> f.schemes.any { it == "http" || it == "https" } && !f.autoVerify }
        if (unverifiedWeb.isNotEmpty()) out += f(
            "SEC-021", Severity.LOW, "Components", "Web links are not verified (no autoVerify).",
            list(unverifiedWeb.flatMap { (c, f) -> f.hosts.ifEmpty { listOf("(any host)") }.map { "$it → ${c.name.substringAfterLast('.')}" } }.distinct()),
            "Without Digital Asset Links verification another app can claim the same web links, and users get a disambiguation dialog.",
            "Add android:autoVerify=\"true\" to the intent filter and host a valid assetlinks.json.",
            "MASVS-PLATFORM-1 · CWE-939",
        )

        val withFilters = m.activities.filter { it.exported && it.enabled && !it.isLauncher && it.intentFilters.isNotEmpty() }
        if (withFilters.isNotEmpty()) out += f(
            "SEC-007", Severity.INFO, "Components", "Exported activities declare intent filters (external entry points).",
            list(withFilters.map { it.name }),
            "These activities can be launched by any app or deep link. All extras and data URIs must be treated as untrusted input.",
            "Validate and sanitize every Intent extra and URI that reaches these activities.",
            "MASVS-PLATFORM-2 · CWE-20",
        )
    }

    // ---- Network ---------------------------------------------------------------------------

    private fun networkRules(input: SecurityInput, out: MutableList<SecurityFinding>) {
        val m = input.manifest ?: return
        val usesInternet = "android.permission.INTERNET" in m.usesPermissions

        if (m.usesCleartextTraffic) out += f(
            "SEC-003", Severity.HIGH, "Network", "Application permits cleartext (unencrypted) network traffic.",
            "usesCleartextTraffic=true (explicit, or implied by targetSdkVersion < 28).",
            "Traffic sent without TLS can be read or modified by anyone on the network path.",
            "Set android:usesCleartextTraffic=\"false\" and allow cleartext only for specific domains in a Network Security Config.",
            "MASVS-NETWORK-1 · CWE-319",
        )

        if (usesInternet && !m.usesNetworkSecurityConfig) out += f(
            "SEC-004", Severity.LOW, "Network", "No Network Security Configuration declared.",
            "The application has INTERNET access but no android:networkSecurityConfig.",
            "The app relies on platform defaults for trust anchors, cleartext policy and certificate pinning.",
            "Add a Network Security Config that sets cleartext policy, restricts trust anchors and pins critical domains.",
            "MASVS-NETWORK-2 · CWE-295",
        )

        val hasPinning = input.classes.any {
            it.descriptor.contains("okhttp3/CertificatePinner") || it.descriptor.contains("TrustKit") ||
                it.descriptor.contains("datatheorem/android/trustkit")
        }
        if (usesInternet && !m.usesNetworkSecurityConfig && input.classes.isNotEmpty() && !hasPinning) out += f(
            "SEC-022", Severity.INFO, "Network", "No certificate pinning indicators found.",
            "No Network Security Config and no OkHttp CertificatePinner / TrustKit classes were found.",
            "Without pinning, a user-installed or compromised CA can intercept TLS traffic to the app's backend.",
            "Pin the certificates or public keys of your own backend (Network Security Config <pin-set> or OkHttp CertificatePinner).",
            "MASVS-NETWORK-2 · CWE-295",
        )
    }

    // ---- Permissions ---------------------------------------------------------------------------

    private class SensitiveGroup(val id: String, val severity: Severity, val title: String, val impact: String, val ref: String, val perms: Set<String>)

    private val sensitive = listOf(
        SensitiveGroup(
            "SEC-030", Severity.HIGH, "Reads or sends SMS / MMS.",
            "SMS can carry one-time passwords and personal messages; the access is restricted by Google Play and is a classic spyware and fraud capability.",
            "MASVS-PRIVACY-1 · CWE-359",
            setOf("READ_SMS", "RECEIVE_SMS", "SEND_SMS", "RECEIVE_MMS", "RECEIVE_WAP_PUSH", "WRITE_SMS"),
        ),
        SensitiveGroup(
            "SEC-031", Severity.MEDIUM, "Accesses call logs or controls calls.",
            "Call history and call control expose private communication metadata and enable toll fraud.",
            "MASVS-PRIVACY-1 · CWE-359",
            setOf("READ_CALL_LOG", "WRITE_CALL_LOG", "PROCESS_OUTGOING_CALLS", "CALL_PHONE", "ANSWER_PHONE_CALLS"),
        ),
        SensitiveGroup(
            "SEC-032", Severity.MEDIUM, "Tracks location in the background.",
            "Continuous background location builds a detailed movement profile and needs a strong justification and disclosure.",
            "MASVS-PRIVACY-1 · CWE-359",
            setOf("ACCESS_BACKGROUND_LOCATION"),
        ),
        SensitiveGroup(
            "SEC-033", Severity.HIGH, "Requests all-files storage access.",
            "MANAGE_EXTERNAL_STORAGE gives access to nearly all shared storage and is restricted by Google Play.",
            "MASVS-STORAGE-2 · CWE-922",
            setOf("MANAGE_EXTERNAL_STORAGE"),
        ),
        SensitiveGroup(
            "SEC-034", Severity.MEDIUM, "Can draw over other apps.",
            "Overlays enable tapjacking and phishing screens that imitate other apps (e.g. banking).",
            "MASVS-PLATFORM-3 · CWE-1021",
            setOf("SYSTEM_ALERT_WINDOW"),
        ),
        SensitiveGroup(
            "SEC-035", Severity.HIGH, "Can install or remove packages.",
            "Installing or deleting other apps is a dropper and sideloading capability.",
            "MASVS-PLATFORM-1 · CWE-494",
            setOf("REQUEST_INSTALL_PACKAGES", "INSTALL_PACKAGES", "DELETE_PACKAGES", "REQUEST_DELETE_PACKAGES"),
        ),
        SensitiveGroup(
            "SEC-036", Severity.HIGH, "Binds privileged system services (accessibility / device admin).",
            "Accessibility and device-admin access can read the screen, inject input and block uninstall; they are heavily abused by banking trojans.",
            "MASVS-PLATFORM-1 · CWE-269",
            setOf("BIND_ACCESSIBILITY_SERVICE", "BIND_DEVICE_ADMIN", "BIND_NOTIFICATION_LISTENER_SERVICE"),
        ),
        SensitiveGroup(
            "SEC-037", Severity.LOW, "Reads device identifiers.",
            "Phone state and numbers allow persistent device tracking.",
            "MASVS-PRIVACY-2 · CWE-359",
            setOf("READ_PHONE_STATE", "READ_PHONE_NUMBERS", "READ_PRIVILEGED_PHONE_STATE"),
        ),
        SensitiveGroup(
            "SEC-038", Severity.LOW, "Can enumerate all installed apps.",
            "The installed-app list is used to fingerprint users and target other apps.",
            "MASVS-PRIVACY-2 · CWE-359",
            setOf("QUERY_ALL_PACKAGES", "GET_INSTALLED_APPS"),
        ),
        SensitiveGroup(
            "SEC-039", Severity.HIGH, "Can change system settings.",
            "WRITE_SECURE_SETTINGS and WRITE_SETTINGS allow altering device security configuration.",
            "MASVS-PLATFORM-1 · CWE-269",
            setOf("WRITE_SECURE_SETTINGS", "WRITE_SETTINGS", "CHANGE_CONFIGURATION"),
        ),
    )

    private fun permissionRules(input: SecurityInput, out: MutableList<SecurityFinding>) {
        val requested = (input.manifest?.usesPermissions.orEmpty() + input.permissions.map { it.name }).map { it.substringAfterLast('.') }.toSet()
        for (g in sensitive) {
            val hits = g.perms.filter { it in requested }
            if (hits.isNotEmpty()) out += f(
                g.id, g.severity, "Permissions", g.title, hits.joinToString(", ") { "android.permission.$it" },
                g.impact, "Remove the permission unless the feature is core to the app, request it at the moment of use, and document it in the Play data-safety form.", g.ref,
            )
        }
        val dangerous = input.permissions.filter { it.risk == PermissionRisk.DANGEROUS }.map { it.name }
        if (dangerous.isNotEmpty()) out += f(
            "SEC-008", Severity.INFO, "Permissions", "Application requests ${dangerous.size} dangerous permission(s).",
            list(dangerous),
            "Dangerous permissions grant access to sensitive user data or device capabilities.",
            "Verify each one is required, request it with a runtime rationale, and degrade gracefully when it is denied.",
            "MASVS-PRIVACY-1 · CWE-250",
        )
    }

    // ---- Signing -----------------------------------------------------------------------------------

    private fun signingRules(input: SecurityInput, out: MutableList<SecurityFinding>) {
        val s = input.signing ?: return

        if (s.isDebugSigned) out += f(
            "SEC-009", Severity.CRITICAL, "Signing", "Application is signed with the Android debug certificate.",
            s.signers.firstOrNull { it.subjectDn.contains("CN=Android Debug") }?.subjectDn.orEmpty(),
            "The debug keystore is publicly known and unprotected — anyone can produce an update signed with the same identity, and Google Play rejects the app.",
            "Sign release builds with a protected release keystore (or Play App Signing); never with the generated debug.keystore.",
            "MASVS-RESILIENCE-2 · CWE-321",
        )

        if (!input.isBundle && s.schemesVerified.entries.none { (scheme, ok) -> ok && scheme != "v1" }) out += f(
            "SEC-010", Severity.MEDIUM, "Signing", "APK is not verified by Signature Scheme v2 or newer.",
            s.schemesVerified.entries.joinToString(", ") { "${it.key}=${it.value}" },
            "v1 (JAR) signing does not protect the whole archive; APKs can be tampered with (Janus, CVE-2017-13156) on devices that fall back to v1.",
            "Enable v2/v3 signing (default in the Android Gradle Plugin) and re-sign the release.",
            "MASVS-RESILIENCE-2 · CWE-347",
        )

        val weak = s.signers.filter { it.keyAlgorithm?.contains("RSA", ignoreCase = true) == true && (it.keySizeBits ?: 4096) < 2048 }
        if (weak.isNotEmpty()) out += f(
            "SEC-040", Severity.HIGH, "Signing", "Signing key is shorter than 2048 bits.",
            weak.joinToString(", ") { "${it.keyAlgorithm} ${it.keySizeBits} bits (${it.subjectDn})" },
            "Short RSA keys can be factored; an attacker who recovers the key can sign malicious updates as this app.",
            "Generate a new keystore with RSA 3072/4096 or EC P-256 and migrate using APK Signature Scheme v3 key rotation.",
            "MASVS-CRYPTO-1 · CWE-326",
        )
    }

    // ---- Secrets -----------------------------------------------------------------------------------------

    private val secretSeverity = mapOf(
        SecretKind.PRIVATE_KEY to Severity.CRITICAL,
        SecretKind.AWS_ACCESS_KEY to Severity.HIGH,
        SecretKind.STRIPE_KEY to Severity.HIGH,
        SecretKind.GITHUB_TOKEN to Severity.HIGH,
        SecretKind.SLACK_TOKEN to Severity.HIGH,
        SecretKind.BASIC_AUTH_URL to Severity.HIGH,
        SecretKind.BEARER_TOKEN to Severity.MEDIUM,
        SecretKind.JWT to Severity.MEDIUM,
        SecretKind.GENERIC_API_KEY to Severity.MEDIUM,
        SecretKind.GOOGLE_API_KEY to Severity.MEDIUM,
        SecretKind.FIREBASE_DATABASE_URL to Severity.LOW,
    )

    private fun secretRules(input: SecurityInput, out: MutableList<SecurityFinding>) {
        for ((kind, findings) in input.secrets.groupBy { it.kind }) {
            out += f(
                "SEC-050", secretSeverity[kind] ?: Severity.MEDIUM, "Secrets", "Hardcoded ${kind.label} (${findings.size}).",
                list(findings.map { "${it.maskedValue} in ${it.sourceFile}" }, 4),
                "Anything shipped in an APK can be extracted in minutes; a leaked credential gives attackers the same access as the app.",
                when (kind) {
                    SecretKind.GOOGLE_API_KEY -> "Restrict the key by package name + SHA-1 and by API in the Google Cloud console; rotate it if it is unrestricted."
                    SecretKind.FIREBASE_DATABASE_URL -> "Verify Firebase Realtime Database security rules deny unauthenticated reads and writes."
                    else -> "Remove the secret from the app, rotate it, and move the operation to a backend that holds the credential."
                },
                "MASVS-STORAGE-1 · CWE-798",
            )
        }
    }

    // ---- Files & code ----------------------------------------------------------------------------------------

    private val sensitiveFile = Regex("""(?i)\.(jks|keystore|p12|pfx|pem|key|env|sqlite|sqlite3|db|sql|bak|old|orig|swp)$|(^|/)(\.git|\.svn|\.env|id_rsa|credentials|secrets?)(/|$|\.)""")
    private val embeddedCode = Regex("""(?i)(^|/)assets/.*\.(apk|dex|jar|odex|oat)$""")

    private val devToolPackages = mapOf(
        "com.facebook.stetho" to "Stetho",
        "com.squareup.leakcanary" to "LeakCanary",
        "leakcanary" to "LeakCanary",
        "com.facebook.flipper" to "Flipper",
        "com.chuckerteam.chucker" to "Chucker",
        "com.readystatesoftware.chuck" to "Chuck",
        "com.facebook.react.devsupport" to "React Native dev support",
    )

    private fun fileAndCodeRules(input: SecurityInput, out: MutableList<SecurityFinding>) {
        val paths = input.files.map { it.path }

        val sensitiveFiles = paths.filter { !it.contains("META-INF/") && sensitiveFile.containsMatchIn(it) }
        if (sensitiveFiles.isNotEmpty()) out += f(
            "SEC-060", Severity.HIGH, "Files", "Sensitive files are packaged in the APK.",
            list(sensitiveFiles),
            "Keystores, private keys, environment files and database dumps bundled with the app are trivially extractable.",
            "Remove them from the APK (check src/main/assets, res/raw and build.gradle sourceSets); keep secrets on a server.",
            "MASVS-STORAGE-1 · CWE-312",
        )

        val embedded = paths.filter { embeddedCode.containsMatchIn(it) }
        if (embedded.isNotEmpty()) out += f(
            "SEC-061", Severity.MEDIUM, "Files", "Executable code is embedded as an asset.",
            list(embedded),
            "Bundled APK/DEX/JAR files suggest dynamic code loading, which bypasses Play review and can be tampered with if loaded from writable storage.",
            "Ship code in the main APK; if dynamic loading is unavoidable, verify signatures and load only from app-private, read-only locations.",
            "MASVS-CODE-4 · CWE-494",
        )

        if (paths.any { it.endsWith("flutter_assets/kernel_blob.bin") }) out += f(
            "SEC-062", Severity.HIGH, "Code", "Flutter debug (JIT) build.",
            "assets/flutter_assets/kernel_blob.bin is present — release builds are AOT-compiled and do not contain it.",
            "Debug Flutter builds ship readable kernel code, enable the Dart VM service for remote debugging and are far slower.",
            "Build with `flutter build apk --release` (or appbundle).",
            "MASVS-RESILIENCE-4 · CWE-489",
        )

        val tools = input.classes.mapNotNull { c -> devToolPackages.entries.firstOrNull { c.packageName.startsWith(it.key) }?.value }.distinct()
        if (tools.isNotEmpty()) out += f(
            "SEC-063", Severity.MEDIUM, "Code", "Debug / diagnostic tooling is packaged.",
            tools.joinToString(", "),
            "Inspection libraries expose network traffic, databases and view hierarchies to anyone who can attach — often over an unauthenticated local port.",
            "Use debugImplementation for these libraries so they are excluded from release builds.",
            "MASVS-RESILIENCE-4 · CWE-489",
        )

        val classes = input.classes
        if (classes.size >= 300) {
            val shortNames = classes.count { it.simpleName.length <= 2 }
            if (shortNames * 100 / classes.size < 5) out += f(
                "SEC-064", Severity.LOW, "Code", "Code does not appear to be obfuscated.",
                "$shortNames of ${classes.size} classes (${shortNames * 100 / classes.size}%) have short names — R8/ProGuard renaming is usually above 30%.",
                "Readable class and method names make reverse engineering and targeted tampering much easier.",
                "Enable minifyEnabled/isMinifyEnabled with R8 for release builds and keep rules as narrow as possible.",
                "MASVS-RESILIENCE-3 · CWE-656",
            )
        }

        input.metadata?.let { meta ->
            if (meta.abis.isNotEmpty() && meta.abis.none { it.contains("64") }) out += f(
                "SEC-065", Severity.LOW, "Code", "Native libraries are 32-bit only.",
                "ABIs: ${meta.abis.joinToString()}",
                "32-bit processes have much weaker address-space randomization and are no longer accepted by Google Play for new releases.",
                "Add arm64-v8a (and x86_64) builds of all native libraries.",
                "MASVS-RESILIENCE-3 · CWE-1104",
            )
        }
    }
}
