package dev.prateekthakur.devlens.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.prateekthakur.devlens.domain.device.DeviceConnectionState
import dev.prateekthakur.devlens.domain.device.DeviceInfo
import dev.prateekthakur.devlens.domain.device.DeviceManager
import dev.prateekthakur.devlens.ui.common.EmptyState
import dev.prateekthakur.devlens.ui.common.ErrorBanner
import dev.prateekthakur.devlens.ui.common.HoverableCard
import dev.prateekthakur.devlens.ui.common.PulsingDot
import dev.prateekthakur.devlens.ui.common.ScreenHeader
import dev.prateekthakur.devlens.ui.common.Tag
import dev.prateekthakur.devlens.ui.theme.Spacing
import kotlinx.coroutines.launch

/** PRD §19 — connected Android devices via ADB. */
@Composable
fun DevicesScreen(deviceManager: DeviceManager) {
    var devices by remember { mutableStateOf<List<DeviceInfo>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        refreshing = true
        deviceManager.listDevices()
            .onSuccess { devices = it; error = null }
            .onFailure { error = it.message }
        refreshing = false
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("Devices", "Connected via ADB — no root required") {
            Button(onClick = { scope.launch { refresh() } }, enabled = !refreshing) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Refresh")
                }
            }
        }

        error?.let { ErrorBanner(it) }

        if (devices.isEmpty() && error == null && !refreshing) {
            EmptyState("No device connected. Plug in a device with USB debugging enabled.")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.padding(top = Spacing.lg)) {
            items(devices) { device -> DeviceCard(device) }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceInfo) {
    val (tone, label) = when (device.state) {
        DeviceConnectionState.CONNECTED -> MaterialTheme.colorScheme.secondaryContainer to "Connected"
        DeviceConnectionState.UNAUTHORIZED -> MaterialTheme.colorScheme.errorContainer to "Unauthorized"
        DeviceConnectionState.OFFLINE -> MaterialTheme.colorScheme.surfaceContainerHighest to "Offline"
        DeviceConnectionState.NONE -> MaterialTheme.colorScheme.surfaceContainerHighest to "No device"
    }
    val onTone = when (device.state) {
        DeviceConnectionState.CONNECTED -> MaterialTheme.colorScheme.onSecondaryContainer
        DeviceConnectionState.UNAUTHORIZED -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    HoverableCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (device.state == DeviceConnectionState.CONNECTED) {
                    PulsingDot(MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = Spacing.sm))
                }
                Text(device.name ?: device.serial, style = MaterialTheme.typography.titleSmall)
            }
            Tag(label, tone, onTone)
        }
        Text(
            listOfNotNull(
                device.androidVersion?.let { "Android $it" },
                device.apiLevel?.let { "API $it" },
                device.manufacturer,
                device.serial,
            ).joinToString("  ·  "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}
