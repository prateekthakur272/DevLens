package dev.prateekthakur.devlens.infrastructure.report

import dev.prateekthakur.devlens.domain.report.ApkReportData
import dev.prateekthakur.devlens.domain.report.PdfReportWriter

actual fun createPdfReportWriter(): PdfReportWriter = object : PdfReportWriter {
    override suspend fun write(data: ApkReportData, path: String): Result<Unit> =
        Result.failure(IllegalStateException("PDF export is only available in the desktop app."))
}
