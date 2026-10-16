package dev.prateekthakur.devlens.domain.apk

/** PRD §12 — APK/AAB metadata surfaced by the Inspector. */
data class ApkMetadata(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val sizeBytes: Long,
    val abis: List<String>,
    val dexFileCount: Int,
    val nativeLibraryCount: Int,
)

/** PRD §13 — one entry in the extracted APK file tree. */
data class ApkFileEntry(
    val path: String,
    val sizeBytes: Long,
    val category: ApkFileCategory,
)

enum class ApkFileCategory { MANIFEST, DEX, RESOURCES, RES, ASSETS, NATIVE_LIB, META_INF, UNKNOWN }

/** Analyzes an APK/AAB as untrusted input (PRD §40) — never executes it. */
interface ApkAnalyzer {
    suspend fun analyze(apkPath: String): Result<ApkMetadata>
    suspend fun listFiles(apkPath: String): Result<List<ApkFileEntry>>
}
