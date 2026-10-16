package dev.prateekthakur.devlens.domain.manifest

/** PRD §14 — parsed AndroidManifest.xml contents. */
data class ManifestInfo(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val debuggable: Boolean,
    val allowBackup: Boolean,
    val usesCleartextTraffic: Boolean,
    val usesNetworkSecurityConfig: Boolean,
    val usesPermissions: List<String>,
    val activities: List<ComponentInfo>,
    val services: List<ComponentInfo>,
    val receivers: List<ComponentInfo>,
    val providers: List<ProviderInfo>,
    val testOnly: Boolean = false,
    val requestLegacyExternalStorage: Boolean = false,
    /** True when the app declares `fullBackupContent` or `dataExtractionRules`, i.e. scopes what gets backed up. */
    val hasBackupRules: Boolean = false,
    val customPermissions: List<CustomPermission> = emptyList(),
)

/** A permission the app itself defines; [protectionLevel] is the raw base value (0 normal, 1 dangerous, 2 signature, 3 signatureOrSystem). */
data class CustomPermission(val name: String, val protectionLevel: Int)

/** One `<intent-filter>` of a component. */
data class IntentFilterInfo(
    val actions: List<String>,
    val categories: List<String>,
    val schemes: List<String>,
    val hosts: List<String>,
    val autoVerify: Boolean,
)

data class ComponentInfo(
    val name: String,
    val exported: Boolean,
    val permission: String?,
    val intentFilterCount: Int,
    val enabled: Boolean = true,
    val launchMode: Int = 0,
    val taskAffinity: String? = null,
    val allowTaskReparenting: Boolean = false,
    val intentFilters: List<IntentFilterInfo> = emptyList(),
) {
    /** The app's launcher entry point (MAIN + LAUNCHER) — expected to be exported. */
    val isLauncher: Boolean
        get() = intentFilters.any { "android.intent.action.MAIN" in it.actions && "android.intent.category.LAUNCHER" in it.categories }
}

data class ProviderInfo(
    val name: String,
    val authority: String,
    val exported: Boolean,
    val readPermission: String?,
    val writePermission: String?,
    val grantUriPermissions: Boolean = false,
)

interface ManifestAnalyzer {
    suspend fun analyze(apkPath: String): Result<ManifestInfo>
}
