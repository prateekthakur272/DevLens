package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.core.process.CommandExecutor
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.signing.ApksignerSigningAnalyzer
import dev.prateekthakur.devlens.domain.signing.SigningAnalyzer
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator

actual fun createManifestAnalyzer(executor: CommandExecutor, sdkLocator: AndroidSdkLocator): ManifestAnalyzer = AaptManifestAnalyzer(executor, sdkLocator)
actual fun createSigningAnalyzer(executor: CommandExecutor, sdkLocator: AndroidSdkLocator): SigningAnalyzer = ApksignerSigningAnalyzer(executor, sdkLocator)
