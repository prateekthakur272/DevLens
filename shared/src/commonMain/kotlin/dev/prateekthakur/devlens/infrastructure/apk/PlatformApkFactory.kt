package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.domain.apk.ApkAnalyzer
import dev.prateekthakur.devlens.domain.dex.DexClassAnalyzer
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.secrets.ApkSecretsAnalyzer
import dev.prateekthakur.devlens.domain.size.SizeAnalyzer

/** Zip-based APK structure/size/dex/strings reading needs real filesystem zip access — desktop (JVM) only. */
expect fun createZipBasedApkAnalyzer(manifestAnalyzer: ManifestAnalyzer): ApkAnalyzer
expect fun createZipBasedSizeAnalyzer(): SizeAnalyzer
expect fun createDexClassAnalyzer(): DexClassAnalyzer
expect fun createApkSecretsAnalyzer(): ApkSecretsAnalyzer
