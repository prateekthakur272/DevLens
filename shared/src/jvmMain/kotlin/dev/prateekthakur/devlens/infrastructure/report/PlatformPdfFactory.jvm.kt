package dev.prateekthakur.devlens.infrastructure.report

import dev.prateekthakur.devlens.domain.report.PdfReportWriter

actual fun createPdfReportWriter(): PdfReportWriter = PdfBoxReportWriter()
