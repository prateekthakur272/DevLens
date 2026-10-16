package dev.prateekthakur.devlens.domain.size

/** PRD §17 — APK size breakdown. */
data class SizeReport(
    val totalBytes: Long,
    val byCategoryBytes: Map<String, Long>,
    val largestFiles: List<Pair<String, Long>>,
)

interface SizeAnalyzer {
    suspend fun analyze(apkPath: String): Result<SizeReport>
}
