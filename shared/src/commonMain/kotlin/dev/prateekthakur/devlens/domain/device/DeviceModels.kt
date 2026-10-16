package dev.prateekthakur.devlens.domain.device

/** PRD §19 — connected Android device state. */
enum class DeviceConnectionState { CONNECTED, UNAUTHORIZED, OFFLINE, NONE }

data class DeviceInfo(
    val serial: String,
    val name: String?,
    val state: DeviceConnectionState,
    val androidVersion: String? = null,
    val apiLevel: Int? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val abi: String? = null,
)

/** PRD §19-20 — device discovery and ADB operations. */
interface DeviceManager {
    suspend fun listDevices(): Result<List<DeviceInfo>>
}
