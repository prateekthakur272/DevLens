package dev.prateekthakur.devlens.domain.signing

import dev.prateekthakur.devlens.core.process.CommandExecutor
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator

/** PRD §16/§40 — "who signed this APK, and with what". */
data class SignerCertificate(
    val subjectDn: String,
    val sha256: String,
    val sha1: String,
    val keyAlgorithm: String?,
    val keySizeBits: Int?,
)

data class SigningInfo(
    val verified: Boolean,
    val schemesVerified: Map<String, Boolean>,
    val signers: List<SignerCertificate>,
) {
    val isDebugSigned: Boolean get() = signers.any { it.subjectDn.contains("CN=Android Debug") }
    val highestSchemeVerified: String?
        get() = schemesVerified.entries.filter { it.value }.maxByOrNull { it.key }?.key
}

interface SigningAnalyzer {
    suspend fun analyze(apkPath: String): Result<SigningInfo>
}

/** PRD §44 — wraps `apksigner verify --print-certs -v`, the same tool `gradle assembleRelease` uses. */
class ApksignerSigningAnalyzer(
    private val executor: CommandExecutor = CommandExecutor(),
    private val sdkLocator: AndroidSdkLocator = AndroidSdkLocator(executor = executor),
) : SigningAnalyzer {

    private val schemeRegex = Regex("""Verified using (v\S+) scheme[^:]*:\s*(true|false)""")
    private val signerLineRegex = Regex("""^V\d(?:\.\d)? Signer(?: #\d+)?: (.+)$""")

    override suspend fun analyze(apkPath: String): Result<SigningInfo> {
        val apksigner = sdkLocator.findApksigner()
            ?: return Result.failure(IllegalStateException("apksigner not found. Install Android SDK Build-Tools or configure the SDK path in Settings."))

        val result = executor.run(apksigner, listOf("verify", "--print-certs", "-v", apkPath), timeoutMillis = 20_000)
        val output = result.stdout + "\n" + result.stderr

        val schemes = linkedMapOf<String, Boolean>()
        schemeRegex.findAll(output).forEach { m -> schemes[m.groupValues[1]] = m.groupValues[2] == "true" }

        data class Partial(var dn: String? = null, var sha256: String? = null, var sha1: String? = null, var alg: String? = null, var bits: Int? = null)
        val signers = linkedMapOf<String, Partial>()

        for (rawLine in output.lineSequence()) {
            val match = signerLineRegex.find(rawLine.trim()) ?: continue
            val rest = match.groupValues[1]
            when {
                rest.startsWith("certificate DN:") -> {
                    val dn = rest.removePrefix("certificate DN:").trim()
                    signers.getOrPut(dn) { Partial() }.dn = dn
                }
                rest.startsWith("certificate SHA-256 digest:") -> lastSigner(signers)?.sha256 = rest.substringAfterLast(":").trim()
                rest.startsWith("certificate SHA-1 digest:") -> lastSigner(signers)?.sha1 = rest.substringAfterLast(":").trim()
                rest.startsWith("key algorithm:") -> lastSigner(signers)?.alg = rest.substringAfterLast(":").trim()
                rest.startsWith("key size (bits):") -> lastSigner(signers)?.bits = rest.substringAfterLast(":").trim().toIntOrNull()
            }
        }

        val verified = output.contains("Verifies") && !output.contains("DOES NOT VERIFY")

        return Result.success(
            SigningInfo(
                verified = verified,
                schemesVerified = schemes,
                signers = signers.values.mapNotNull { p ->
                    val dn = p.dn ?: return@mapNotNull null
                    SignerCertificate(
                        subjectDn = dn,
                        sha256 = p.sha256 ?: "",
                        sha1 = p.sha1 ?: "",
                        keyAlgorithm = p.alg,
                        keySizeBits = p.bits,
                    )
                },
            ),
        )
    }

    private fun <V> lastSigner(map: LinkedHashMap<String, V>): V? = map.values.lastOrNull()
}
