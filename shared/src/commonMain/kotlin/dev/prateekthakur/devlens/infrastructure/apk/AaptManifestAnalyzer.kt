package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.core.process.CommandExecutor
import dev.prateekthakur.devlens.domain.manifest.ComponentInfo
import dev.prateekthakur.devlens.domain.manifest.CustomPermission
import dev.prateekthakur.devlens.domain.manifest.IntentFilterInfo
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.manifest.ManifestInfo
import dev.prateekthakur.devlens.domain.manifest.ProviderInfo
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator

/**
 * PRD §14 — decodes AndroidManifest.xml by shelling out to `aapt2 dump xmltree`, the same
 * tool the Android build system uses, rather than hand-parsing the binary AXML format.
 */
class AaptManifestAnalyzer(
    private val executor: CommandExecutor = CommandExecutor(),
    private val sdkLocator: AndroidSdkLocator = AndroidSdkLocator(executor = executor),
) : ManifestAnalyzer {

    override suspend fun analyze(apkPath: String): Result<ManifestInfo> {
        val aapt2 = sdkLocator.findAapt2()
            ?: return Result.failure(
                IllegalStateException("aapt2 not found. Install Android SDK Build-Tools or configure the SDK path in Settings."),
            )

        val result = executor.run(aapt2, listOf("dump", "xmltree", apkPath, "--file", "AndroidManifest.xml"), timeoutMillis = 20_000)
        if (!result.isSuccess) {
            return Result.failure(IllegalStateException(result.stderr.ifBlank { "aapt2 dump xmltree failed" }))
        }

        val manifestNode = AaptXmlTreeParser.parse(result.stdout)
            ?: return Result.failure(IllegalStateException("Could not parse AndroidManifest.xml"))

        return Result.success(mapManifest(manifestNode))
    }
}
