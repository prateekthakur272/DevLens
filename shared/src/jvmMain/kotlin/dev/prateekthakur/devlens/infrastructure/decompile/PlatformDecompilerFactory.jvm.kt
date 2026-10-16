package dev.prateekthakur.devlens.infrastructure.decompile

import dev.prateekthakur.devlens.domain.decompile.ApkDecompiler

actual fun createApkDecompiler(): ApkDecompiler = JadxApkDecompiler()
