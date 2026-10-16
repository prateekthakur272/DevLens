package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.core.process.CommandExecutor
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.signing.SigningAnalyzer
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator

/** Analyzers that also understand `.aab` bundles (protobuf manifest, JAR signature). Desktop (JVM) only. */
expect fun createManifestAnalyzer(executor: CommandExecutor, sdkLocator: AndroidSdkLocator): ManifestAnalyzer
expect fun createSigningAnalyzer(executor: CommandExecutor, sdkLocator: AndroidSdkLocator): SigningAnalyzer
