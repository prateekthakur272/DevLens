package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.core.process.CommandExecutor
import dev.prateekthakur.devlens.domain.apk.ApkIdentityReader
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator

/** Reading the icon needs real zip access — desktop (JVM) only. */
expect fun createApkIdentityReader(executor: CommandExecutor, sdkLocator: AndroidSdkLocator): ApkIdentityReader
