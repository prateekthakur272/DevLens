package dev.prateekthakur.devlens.domain.permission

/** PRD §15 — permission classification. */
enum class PermissionRisk { NORMAL, DANGEROUS, SIGNATURE, SPECIAL, UNKNOWN }

data class PermissionInfo(
    val name: String,
    val risk: PermissionRisk,
    val description: String,
    val declared: Boolean,
    val requested: Boolean,
    val granted: Boolean? = null,
)

interface PermissionAnalyzer {
    suspend fun analyze(apkPath: String): Result<List<PermissionInfo>>
}

/** Classifies the permissions declared in the manifest against [PermissionRiskDatabase]. */
class PermissionClassifier(
    private val manifestAnalyzer: dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer,
) : PermissionAnalyzer {

    override suspend fun analyze(apkPath: String): Result<List<PermissionInfo>> {
        val manifest = manifestAnalyzer.analyze(apkPath).getOrElse { return Result.failure(it) }
        val infos = manifest.usesPermissions.map { name ->
            val (risk, description) = PermissionRiskDatabase.classify(name)
            PermissionInfo(
                name = name,
                risk = risk,
                description = description,
                declared = true,
                requested = true,
            )
        }
        return Result.success(infos)
    }
}
