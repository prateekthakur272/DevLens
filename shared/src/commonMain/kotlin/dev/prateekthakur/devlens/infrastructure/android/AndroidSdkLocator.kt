package dev.prateekthakur.devlens.infrastructure.android

import dev.prateekthakur.devlens.core.io.PlatformEnv
import dev.prateekthakur.devlens.core.io.PlatformFiles
import dev.prateekthakur.devlens.core.process.CommandExecutor

/**
 * Locates the Android SDK, ADB and build-tools (aapt2) without requiring them on PATH.
 * DevLens never modifies the SDK — read-only discovery only (PRD §11, §40).
 */
class AndroidSdkLocator(
    private val files: PlatformFiles = PlatformFiles(),
    private val executor: CommandExecutor = CommandExecutor(),
    private val sdkOverride: () -> String? = { null },
) {
    private var cachedSdkRoot: String? = null
    private var sdkRootResolved = false

    /** Forget the cached SDK location so the next lookup re-detects (after a settings change). */
    fun invalidate() {
        sdkRootResolved = false
        cachedSdkRoot = null
    }

    suspend fun sdkRoot(): String? {
        if (sdkRootResolved) return cachedSdkRoot
        sdkRootResolved = true

        sdkOverride()?.takeIf { files.exists(it) }?.let {
            cachedSdkRoot = it
            return it
        }

        val fromEnv = listOfNotNull(
            PlatformEnv.get("ANDROID_HOME"),
            PlatformEnv.get("ANDROID_SDK_ROOT"),
        ).firstOrNull { files.exists(it) }
        if (fromEnv != null) {
            cachedSdkRoot = fromEnv
            return fromEnv
        }

        val home = PlatformEnv.get("HOME") ?: PlatformEnv.get("USERPROFILE")
        val candidates = listOfNotNull(
            home?.let { files.join(it, "Library", "Android", "sdk") },
            home?.let { files.join(it, "Android", "Sdk") },
            home?.let { files.join(it, "AppData", "Local", "Android", "Sdk") },
        )
        cachedSdkRoot = candidates.firstOrNull { files.exists(it) }
        return cachedSdkRoot
    }

    suspend fun findAdb(): String? {
        if (sdkOverride() == null) executor.which("adb")?.let { return it }
        val root = sdkRoot()
        root?.let { existingExecutable(files.join(it, "platform-tools"), "adb") }?.let { return it }
        return if (sdkOverride() != null) executor.which("adb") else null
    }

    suspend fun findAapt2(): String? = findBuildTool("aapt2")

    suspend fun findApksigner(): String? = findBuildTool("apksigner")

    private suspend fun findBuildTool(name: String): String? {
        // A user-chosen SDK wins over whatever happens to be on PATH.
        if (sdkOverride() == null) executor.which(name)?.let { return it }
        val root = sdkRoot() ?: return if (sdkOverride() != null) executor.which(name) else null
        val buildToolsDir = files.join(root, "build-tools")
        val versions = files.listEntries(buildToolsDir)
            .filter { it.isDirectory }
            .map { it.name }
            .sortedDescending()
        for (version in versions) {
            existingExecutable(files.join(buildToolsDir, version), name)?.let { return it }
        }
        return if (sdkOverride() != null) executor.which(name) else null
    }

    private suspend fun existingExecutable(dir: String, name: String): String? {
        val windows = files.join(dir, "$name.exe")
        if (files.exists(windows)) return windows
        val unix = files.join(dir, name)
        if (files.exists(unix)) return unix
        return null
    }
}
