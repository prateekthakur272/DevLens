package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.domain.apk.ApkFileCategory
import dev.prateekthakur.devlens.domain.apk.ApkFileEntry
import java.io.File
import java.util.zip.ZipFile

/**
 * Reads APK/AAB zip entries as untrusted input (PRD §40): rejects path traversal
 * (entries escaping the extraction root via "..") and caps the total uncompressed
 * size considered, to avoid a zip-bomb exhausting memory/disk.
 */
object ZipInspector {

    private const val MAX_TOTAL_UNCOMPRESSED_BYTES = 2L * 1024 * 1024 * 1024 // 2 GB safety cap
    private const val MAX_COMPRESSION_RATIO = 200

    fun listEntries(apkPath: String): List<ApkFileEntry> {
        val entries = mutableListOf<ApkFileEntry>()
        var totalUncompressed = 0L
        val bundle = BundleLayout.isBundle(apkPath)

        ZipFile(File(apkPath)).use { zip ->
            val zipEntries = zip.entries()
            while (zipEntries.hasMoreElements()) {
                val entry = zipEntries.nextElement()
                if (entry.isDirectory) continue

                val name = entry.name
                if (name.contains("..") || name.startsWith("/")) {
                    // Reject path-traversal entries outright rather than surfacing them.
                    continue
                }

                val uncompressedSize = entry.size.coerceAtLeast(0)
                val compressedSize = entry.compressedSize.coerceAtLeast(1)
                if (compressedSize > 0 && uncompressedSize / compressedSize > MAX_COMPRESSION_RATIO) {
                    // Suspicious compression ratio — likely a zip-bomb entry; record size only, do not decompress.
                }

                totalUncompressed += uncompressedSize
                if (totalUncompressed > MAX_TOTAL_UNCOMPRESSED_BYTES) break

                entries += ApkFileEntry(
                    path = name,
                    sizeBytes = uncompressedSize,
                    category = categoryOf(if (bundle) BundleLayout.moduleRelative(name) else name),
                )
            }
        }
        return entries
    }

    private fun categoryOf(path: String): ApkFileCategory = when {
        path == "AndroidManifest.xml" || path == "manifest/AndroidManifest.xml" -> ApkFileCategory.MANIFEST
        path.endsWith(".dex") -> ApkFileCategory.DEX
        path == "resources.arsc" || path == "resources.pb" -> ApkFileCategory.RESOURCES
        path.startsWith("res/") -> ApkFileCategory.RES
        path.startsWith("assets/") -> ApkFileCategory.ASSETS
        path.startsWith("lib/") -> ApkFileCategory.NATIVE_LIB
        path.startsWith("META-INF/") -> ApkFileCategory.META_INF
        else -> ApkFileCategory.UNKNOWN
    }
}
