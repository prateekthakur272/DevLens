package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.domain.manifest.ComponentInfo
import dev.prateekthakur.devlens.domain.manifest.CustomPermission
import dev.prateekthakur.devlens.domain.manifest.IntentFilterInfo
import dev.prateekthakur.devlens.domain.manifest.ManifestInfo
import dev.prateekthakur.devlens.domain.manifest.ProviderInfo

/** Maps a parsed AndroidManifest element tree (from aapt2 text output or a decoded protobuf manifest) to [ManifestInfo]. */
internal fun mapManifest(manifest: AaptXmlNode): ManifestInfo {
    val usesSdk = manifest.childrenNamed("uses-sdk").firstOrNull()
    val application = manifest.childrenNamed("application").firstOrNull()

    val targetSdk = usesSdk?.attr("targetSdkVersion")?.toIntOrNull()
    val debuggable = application?.attr("debuggable") == "true"
    val allowBackupAttr = application?.attr("allowBackup")
    val cleartextAttr = application?.attr("usesCleartextTraffic")

    return ManifestInfo(
        packageName = manifest.attr("package") ?: "",
        versionName = manifest.attr("versionName"),
        versionCode = manifest.attr("versionCode")?.toLongOrNull(),
        minSdk = usesSdk?.attr("minSdkVersion")?.toIntOrNull(),
        targetSdk = targetSdk,
        debuggable = debuggable,
        // Android default is true when unspecified.
        allowBackup = allowBackupAttr?.let { it == "true" } ?: true,
        // Platform default: cleartext allowed for targetSdk < 28, blocked otherwise, unless overridden.
        usesCleartextTraffic = cleartextAttr?.let { it == "true" } ?: ((targetSdk ?: 28) < 28),
        usesNetworkSecurityConfig = application?.attr("networkSecurityConfig") != null,
        usesPermissions = manifest.childrenNamed("uses-permission")
            .mapNotNull { it.attr("name") }
            .distinct(),
        activities = (application?.childrenNamed("activity").orEmpty() + application?.childrenNamed("activity-alias").orEmpty())
            .map { it.toComponentInfo() },
        services = application?.childrenNamed("service").orEmpty().map { it.toComponentInfo() },
        receivers = application?.childrenNamed("receiver").orEmpty().map { it.toComponentInfo() },
        providers = application?.childrenNamed("provider").orEmpty().map { it.toProviderInfo() },
        testOnly = application?.attr("testOnly") == "true",
        requestLegacyExternalStorage = application?.attr("requestLegacyExternalStorage") == "true",
        hasBackupRules = application?.attr("fullBackupContent") != null || application?.attr("dataExtractionRules") != null,
        customPermissions = manifest.childrenNamed("permission").mapNotNull { node ->
            val name = node.attr("name") ?: return@mapNotNull null
            CustomPermission(name, parseInt(node.attr("protectionLevel")) and 0xF)
        },
    )
}

private fun AaptXmlNode.toComponentInfo(): ComponentInfo {
    val intentFilterCount = childrenNamed("intent-filter").size
    val exported = attr("exported")?.let { it == "true" } ?: (intentFilterCount > 0)
    return ComponentInfo(
        name = attr("name") ?: "",
        exported = exported,
        permission = attr("permission"),
        intentFilterCount = intentFilterCount,
        enabled = attr("enabled")?.let { it == "true" } ?: true,
        launchMode = parseInt(attr("launchMode")),
        taskAffinity = attr("taskAffinity"),
        allowTaskReparenting = attr("allowTaskReparenting") == "true",
        intentFilters = childrenNamed("intent-filter").map { it.toIntentFilterInfo() },
    )
}

private fun AaptXmlNode.toIntentFilterInfo() = IntentFilterInfo(
    actions = childrenNamed("action").mapNotNull { it.attr("name") },
    categories = childrenNamed("category").mapNotNull { it.attr("name") },
    schemes = childrenNamed("data").mapNotNull { it.attr("scheme") }.distinct(),
    hosts = childrenNamed("data").mapNotNull { it.attr("host") }.distinct(),
    autoVerify = attr("autoVerify") == "true",
)

/** aapt2 prints ints as decimal (`3`) or hex (`0x00000002`). */
private fun parseInt(value: String?): Int {
    val v = value?.trim() ?: return 0
    return if (v.startsWith("0x", ignoreCase = true)) v.drop(2).toLongOrNull(16)?.toInt() ?: 0 else v.toIntOrNull() ?: 0
}

private fun AaptXmlNode.toProviderInfo(): ProviderInfo {
    val exported = attr("exported")?.let { it == "true" } ?: false
    return ProviderInfo(
        name = attr("name") ?: "",
        authority = attr("authorities") ?: "",
        exported = exported,
        readPermission = attr("readPermission") ?: attr("permission"),
        writePermission = attr("writePermission") ?: attr("permission"),
        grantUriPermissions = attr("grantUriPermissions") == "true",
    )
}
