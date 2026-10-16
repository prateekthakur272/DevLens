package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.domain.size.SizeAnalyzer
import dev.prateekthakur.devlens.domain.size.SizeReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** PRD §17 — APK size breakdown by category, largest files first. */
class ZipSizeAnalyzer : SizeAnalyzer {

    override suspend fun analyze(apkPath: String): Result<SizeReport> = withContext(Dispatchers.IO) {
        runCatching {
            val entries = ZipInspector.listEntries(apkPath)
            val byCategory = entries.groupBy { it.category.name }.mapValues { (_, v) -> v.sumOf { it.sizeBytes } }
            SizeReport(
                totalBytes = entries.sumOf { it.sizeBytes },
                byCategoryBytes = byCategory,
                largestFiles = entries.sortedByDescending { it.sizeBytes }.take(25).map { it.path to it.sizeBytes },
            )
        }
    }
}
