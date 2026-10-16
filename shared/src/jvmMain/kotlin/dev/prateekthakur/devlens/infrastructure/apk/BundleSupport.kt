package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.manifest.ManifestInfo
import dev.prateekthakur.devlens.domain.signing.SignerCertificate
import dev.prateekthakur.devlens.domain.signing.SigningAnalyzer
import dev.prateekthakur.devlens.domain.signing.SigningInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.util.jar.JarFile
import java.util.zip.ZipFile

/** Android App Bundles (.aab) are zips with a per-module layout (`base/dex`, `base/manifest`, `base/lib`, …). */
object BundleLayout {
    fun isBundle(path: String) = path.endsWith(".aab", ignoreCase = true)

    /** Path inside a module, e.g. `base/dex/classes.dex` -> `dex/classes.dex`; non-module entries are returned as-is. */
    fun moduleRelative(path: String): String {
        val rest = path.substringAfter('/', "")
        val known = listOf("manifest/", "dex/", "res/", "assets/", "lib/", "root/", "resources.pb")
        return if (rest.isNotEmpty() && known.any { rest.startsWith(it) }) rest else path
    }
}

/** Reads the manifest of an `.aab` from its protobuf-encoded `base/manifest/AndroidManifest.xml`; defers to aapt2 for APKs. */
class BundleAwareManifestAnalyzer(private val apkAnalyzer: ManifestAnalyzer) : ManifestAnalyzer {
    override suspend fun analyze(apkPath: String): Result<ManifestInfo> {
        if (!BundleLayout.isBundle(apkPath)) return apkAnalyzer.analyze(apkPath)
        return withContext(Dispatchers.IO) {
            runCatching {
                val bytes = ZipFile(File(apkPath)).use { zip ->
                    val entry = zip.getEntry("base/manifest/AndroidManifest.xml")
                        ?: error("Not a valid app bundle: base/manifest/AndroidManifest.xml is missing.")
                    zip.getInputStream(entry).use { it.readBytes() }
                }
                val root = ProtoXmlParser.parse(bytes) ?: error("Could not decode the bundle's AndroidManifest.xml.")
                mapManifest(root)
            }
        }
    }
}

/**
 * Bundles are signed with a plain JAR signature by the upload key (apksigner does not apply). The certificate is
 * read from a signed entry with [JarFile], which also verifies the signature while the entry is read.
 */
class BundleAwareSigningAnalyzer(private val apksigner: SigningAnalyzer) : SigningAnalyzer {
    override suspend fun analyze(apkPath: String): Result<SigningInfo> {
        if (!BundleLayout.isBundle(apkPath)) return apksigner.analyze(apkPath)
        return withContext(Dispatchers.IO) { runCatching { readJarSignature(File(apkPath)) } }
    }

    private fun readJarSignature(file: File): SigningInfo {
        JarFile(file, true).use { jar ->
            // The signature covers every entry, so reading the smallest signed one is enough to obtain its certificate.
            val probe = jar.entries().asSequence()
                .filter { !it.isDirectory && !it.name.startsWith("META-INF/") }
                .minByOrNull { it.size.takeIf { s -> s >= 0 } ?: Long.MAX_VALUE }
                ?: return SigningInfo(false, emptyMap(), emptyList())
            jar.getInputStream(probe).use { it.readBytes() }
            val certs = probe.certificates?.filterIsInstance<X509Certificate>().orEmpty()
            if (certs.isEmpty()) return SigningInfo(false, emptyMap(), emptyList())
            val leaf = certs.first()
            val key = leaf.publicKey
            val bits = when (key) {
                is RSAPublicKey -> key.modulus.bitLength()
                is ECPublicKey -> key.params.order.bitLength()
                else -> null
            }
            fun digest(alg: String) = MessageDigest.getInstance(alg).digest(leaf.encoded).joinToString("") { "%02x".format(it) }
            return SigningInfo(
                verified = true,
                schemesVerified = mapOf("v1" to true),
                signers = listOf(SignerCertificate(leaf.subjectX500Principal.name, digest("SHA-256"), digest("SHA-1"), key.algorithm, bits)),
            )
        }
    }
}
