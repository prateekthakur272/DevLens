package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.core.process.CommandExecutor
import dev.prateekthakur.devlens.domain.apk.ApkIdentityReader
import dev.prateekthakur.devlens.domain.apk.AppIdentity
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Finds the app label and launcher icon. `aapt2 dump badging` names the icon per density; adaptive icons
 * point at XML, so when no raster is named we scan res/ for the same base name (or its foreground layer).
 */
class AaptApkIdentityReader(
    private val executor: CommandExecutor,
    private val sdkLocator: AndroidSdkLocator,
) : ApkIdentityReader {

    private val raster = setOf("png", "webp", "jpg", "jpeg")

    override suspend fun read(apkPath: String): Result<AppIdentity> = withContext(Dispatchers.IO) {
        runCatching {
            var label: String? = null
            var named = emptyList<Pair<Int, String>>()
            var appIcon: String? = null

            if (BundleLayout.isBundle(apkPath)) {
                // aapt2 cannot dump bundles; take the icon reference and a literal label from the protobuf manifest.
                ZipFile(apkPath).use { zip ->
                    zip.getEntry("base/manifest/AndroidManifest.xml")?.let { e ->
                        val app = ProtoXmlParser.parse(zip.getInputStream(e).use { it.readBytes() })?.childrenNamed("application")?.firstOrNull()
                        appIcon = app?.attr("icon")?.takeIf { it.isNotBlank() }
                        label = app?.attr("label")?.takeIf { it.isNotBlank() && !it.startsWith("@") }
                    }
                }
            } else sdkLocator.findAapt2()?.let { aapt2 ->
                val out = executor.run(aapt2, listOf("dump", "badging", apkPath), timeoutMillis = 20_000).stdout
                label = Regex("application-label:'(.*)'").find(out)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                named = Regex("application-icon-(\\d+):'([^']+)'").findAll(out).map { it.groupValues[1].toInt() to it.groupValues[2] }.toList()
                appIcon = Regex("application: label='[^']*' icon='([^']*)'").find(out)?.groupValues?.get(1)
            }

            val bytes = ZipFile(apkPath).use { zip ->
                bundleIcon(zip, appIcon)?.let { return@use it }
                val direct = named.filter { it.second.substringAfterLast('.').lowercase() in raster }
                    .maxByOrNull { it.first }?.second?.let { zip.getEntry(it) }
                val entry = direct ?: run {
                    val base = (appIcon ?: named.firstOrNull()?.second)?.substringAfterLast('/')?.substringBeforeLast('.')
                    val names = listOfNotNull(base, base?.let { "${it}_foreground" }, "ic_launcher", "ic_launcher_foreground").distinct()
                    names.firstNotNullOfOrNull { n -> bestRaster(zip, n) }
                }
                entry?.let { zip.getInputStream(it).use { s -> s.readBytes() } }
            }
            AppIdentity(label, bytes?.let(::toPng))
        }
    }

    /** Bundles: resolve `@mipmap/ic_launcher` through resources.pb (paths are often shortened), preferring the largest raster. */
    private fun bundleIcon(zip: ZipFile, iconRef: String?): ByteArray? {
        val ref = iconRef?.removePrefix("@")?.takeIf { it.isNotBlank() } ?: return null
        val query = when {
            ref.startsWith("id:0x") -> ref.removePrefix("id:0x").toLongOrNull(16)?.let { ResourceTableReader.Query(resourceId = it) }
            ref.contains('/') -> ResourceTableReader.Query(typeName = ref.substringBefore('/'), entryName = ref.substringAfter('/'))
            else -> null
        } ?: return null
        val table = zip.getEntry("base/resources.pb")?.let { e -> zip.getInputStream(e).use { it.readBytes() } } ?: return null
        val paths = runCatching { ResourceTableReader.filePaths(table, query) }.getOrDefault(emptyList())
        val entry = paths
            .filter { it.substringAfterLast('.').lowercase() in raster }
            .mapNotNull { zip.getEntry("base/$it") }
            .maxByOrNull { it.size }
            ?: return null
        return zip.getInputStream(entry).use { it.readBytes() }.let(::toPng)
    }

    private fun density(path: String) = when {
        "xxxhdpi" in path -> 6
        "xxhdpi" in path -> 5
        "xhdpi" in path -> 4
        "hdpi" in path -> 3
        "mdpi" in path -> 2
        "ldpi" in path -> 1
        else -> 0
    }

    private fun bestRaster(zip: ZipFile, baseName: String): ZipEntry? =
        zip.entries().asSequence()
            .filter { !it.isDirectory && (it.name.startsWith("res/") || it.name.contains("/res/")) }
            .filter { it.name.substringAfterLast('.').lowercase() in raster && it.name.substringAfterLast('/').substringBeforeLast('.') == baseName }
            .maxByOrNull { density(it.name) }

    /** Any supported format (incl. WebP) -> PNG, so the UI and the PDF writer only deal with one format. */
    private fun toPng(bytes: ByteArray): ByteArray? = runCatching {
        Image.makeFromEncoded(bytes).use { it.encodeToData(EncodedImageFormat.PNG)?.bytes }
    }.getOrNull()
}
