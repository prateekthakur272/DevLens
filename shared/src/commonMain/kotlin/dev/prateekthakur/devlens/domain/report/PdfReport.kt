package dev.prateekthakur.devlens.domain.report

import dev.prateekthakur.devlens.domain.apk.ApkMetadata
import dev.prateekthakur.devlens.domain.manifest.ManifestInfo
import dev.prateekthakur.devlens.domain.permission.PermissionInfo
import dev.prateekthakur.devlens.domain.secrets.SecretFinding
import dev.prateekthakur.devlens.domain.security.SecurityFinding
import dev.prateekthakur.devlens.domain.signing.SigningInfo
import dev.prateekthakur.devlens.domain.size.SizeReport

/** Everything a finished APK analysis produced, in structured form, for rich (PDF) rendering. */
data class ApkReportData(
    val fileName: String,
    val appLabel: String?,
    val icon: ByteArray?,
    val path: String,
    val score: Int,
    val metadata: ApkMetadata?,
    val manifest: ManifestInfo?,
    val permissions: List<PermissionInfo>,
    val security: List<SecurityFinding>,
    val size: SizeReport?,
    val secrets: List<SecretFinding>,
    val signing: SigningInfo?,
    val classCount: Int,
    val methodCount: Int,
)

interface PdfReportWriter {
    suspend fun write(data: ApkReportData, path: String): Result<Unit>
}
