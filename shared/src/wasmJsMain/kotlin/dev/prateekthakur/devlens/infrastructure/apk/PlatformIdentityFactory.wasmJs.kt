package dev.prateekthakur.devlens.infrastructure.apk

import dev.prateekthakur.devlens.core.process.CommandExecutor
import dev.prateekthakur.devlens.domain.apk.ApkIdentityReader
import dev.prateekthakur.devlens.domain.apk.AppIdentity
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator

actual fun createApkIdentityReader(executor: CommandExecutor, sdkLocator: AndroidSdkLocator): ApkIdentityReader = object : ApkIdentityReader {
    override suspend fun read(apkPath: String): Result<AppIdentity> =
        Result.failure(IllegalStateException("Reading the app icon is only available in the desktop app."))
}
