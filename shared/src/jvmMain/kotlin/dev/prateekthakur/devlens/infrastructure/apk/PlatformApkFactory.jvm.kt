package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.domain.apk.ApkAnalyzer
import dev.prateekthakur.devlens.domain.dex.DexClassAnalyzer
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.secrets.ApkSecretsAnalyzer
import dev.prateekthakur.devlens.domain.size.SizeAnalyzer
import dev.prateekthakur.devlens.infrastructure.apk.dex.ApkDexClassAnalyzer
import dev.prateekthakur.devlens.infrastructure.apk.secrets.ApkSecretsAnalyzerImpl

actual fun createZipBasedApkAnalyzer(manifestAnalyzer: ManifestAnalyzer): ApkAnalyzer = ZipApkAnalyzer(manifestAnalyzer)
actual fun createZipBasedSizeAnalyzer(): SizeAnalyzer = ZipSizeAnalyzer()
actual fun createDexClassAnalyzer(): DexClassAnalyzer = ApkDexClassAnalyzer()
actual fun createApkSecretsAnalyzer(): ApkSecretsAnalyzer = ApkSecretsAnalyzerImpl()
