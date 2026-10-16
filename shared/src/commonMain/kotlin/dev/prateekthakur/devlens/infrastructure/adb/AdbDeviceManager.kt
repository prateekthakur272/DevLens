package dev.prateekthakur.devlens.infrastructure.adb

import dev.prateekthakur.devlens.core.process.CommandExecutor
import dev.prateekthakur.devlens.domain.device.DeviceConnectionState
import dev.prateekthakur.devlens.domain.device.DeviceInfo
import dev.prateekthakur.devlens.domain.device.DeviceManager
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator

/** PRD §19-20 — device discovery via `adb devices -l`, plus per-device `getprop` lookups. */
class AdbDeviceManager(
    private val executor: CommandExecutor = CommandExecutor(),
    private val sdkLocator: AndroidSdkLocator = AndroidSdkLocator(executor = executor),
) : DeviceManager {

    override suspend fun listDevices(): Result<List<DeviceInfo>> {
        val adb = sdkLocator.findAdb()
            ?: return Result.failure(IllegalStateException("ADB not found. Configure the SDK path in Settings."))
        val result = executor.run(adb, listOf("devices", "-l"))
        if (!result.isSuccess) {
            return Result.failure(IllegalStateException(result.stderr.ifBlank { "adb devices failed" }))
        }

        val devices = mutableListOf<DeviceInfo>()
        result.stdout.lineSequence()
            .drop(1) // "List of devices attached"
            .filter { it.isNotBlank() }
            .forEach { line -> parseDeviceLine(adb, line)?.let { devices += it } }

        return Result.success(devices)
    }

    private suspend fun parseDeviceLine(adb: String, line: String): DeviceInfo? {
        val tokens = line.trim().split(Regex("\\s+"))
        if (tokens.size < 2) return null
        val serial = tokens[0]
        val state = when (tokens[1]) {
            "device" -> DeviceConnectionState.CONNECTED
            "unauthorized" -> DeviceConnectionState.UNAUTHORIZED
            "offline" -> DeviceConnectionState.OFFLINE
            else -> DeviceConnectionState.NONE
        }

        val extras = tokens.drop(2).associate { token ->
            val (key, value) = token.split(":", limit = 2).let { it.getOrElse(0) { "" } to it.getOrElse(1) { "" } }
            key to value
        }

        var androidVersion: String? = null
        var apiLevel: Int? = null
        if (state == DeviceConnectionState.CONNECTED) {
            androidVersion = getProp(adb, serial, "ro.build.version.release")
            apiLevel = getProp(adb, serial, "ro.build.version.sdk")?.toIntOrNull()
        }

        return DeviceInfo(
            serial = serial,
            name = extras["model"] ?: extras["device"],
            state = state,
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            manufacturer = extras["manufacturer"],
            model = extras["model"],
        )
    }

    private suspend fun getProp(adb: String, serial: String, prop: String): String? {
        val result = executor.run(adb, listOf("-s", serial, "shell", "getprop", prop))
        return result.stdout.trim().ifBlank { null }
    }
}
