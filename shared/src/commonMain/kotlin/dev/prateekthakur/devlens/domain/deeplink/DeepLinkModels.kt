package dev.prateekthakur.devlens.domain.deeplink

/** PRD §26 — deep link / App Link resolution. */
data class DeepLinkResolution(
    val url: String,
    val matchedPackage: String?,
    val matchedActivity: String?,
    val verified: Boolean? = null,
)

interface DeepLinkTester {
    suspend fun resolve(url: String, serial: String): Result<DeepLinkResolution>
}

/** PRD §26 — resolves a deep link / App Link the same way IntentTester resolves a VIEW intent. */
class AdbDeepLinkTester(
    private val intentTester: dev.prateekthakur.devlens.domain.intent.IntentTester,
) : DeepLinkTester {

    override suspend fun resolve(url: String, serial: String): Result<DeepLinkResolution> {
        val spec = dev.prateekthakur.devlens.domain.intent.IntentSpec(
            action = "android.intent.action.VIEW",
            dataUri = url,
            mimeType = null,
            targetPackage = null,
            component = null,
        )
        val resolutions = intentTester.resolve(spec, serial).getOrElse { return Result.failure(it) }
        val match = resolutions.firstOrNull()
        return Result.success(
            DeepLinkResolution(
                url = url,
                matchedPackage = match?.packageName,
                matchedActivity = match?.activityName,
                verified = null,
            ),
        )
    }
}
