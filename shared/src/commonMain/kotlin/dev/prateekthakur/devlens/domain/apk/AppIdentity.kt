package dev.prateekthakur.devlens.domain.apk

/** What the app calls itself: its launcher label and icon (always normalized to PNG bytes). */
class AppIdentity(val label: String?, val iconPng: ByteArray?)

interface ApkIdentityReader {
    suspend fun read(apkPath: String): Result<AppIdentity>
}
