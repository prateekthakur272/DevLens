package dev.prateekthakur.devlens.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.prateekthakur.devlens.domain.device.DeviceInfo
import dev.prateekthakur.devlens.domain.device.DeviceManager
import dev.prateekthakur.devlens.domain.logcat.LogEntry
import dev.prateekthakur.devlens.domain.logcat.LogLevel
import dev.prateekthakur.devlens.domain.logcat.LogcatSource
import dev.prateekthakur.devlens.ui.common.EmptyState
import dev.prateekthakur.devlens.ui.common.PulsingDot
import dev.prateekthakur.devlens.ui.theme.Spacing
import dev.prateekthakur.devlens.ui.theme.logLevelColor
import kotlinx.coroutines.launch

/** PRD §21 — real-time Logcat streaming, filtering and search. */
@Composable
fun LogsScreen(deviceManager: DeviceManager, logcatSource: LogcatSource) {
    var devices by remember { mutableStateOf<List<DeviceInfo>>(emptyList()) }
    var selectedSerial by remember { mutableStateOf<String?>(null) }
    var deviceMenuExpanded by remember { mutableStateOf(false) }
    var streaming by remember { mutableStateOf(false) }
    var minLevel by remember { mutableStateOf(LogLevel.VERBOSE) }
    var levelMenuExpanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val entries = remember { mutableStateListOf<LogEntry>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        deviceManager.listDevices().onSuccess {
            devices = it
            selectedSerial = it.firstOrNull()?.serial
        }
    }

    fun start() {
        val serial = selectedSerial ?: return
        entries.clear()
        streaming = true
        scope.launch {
            logcatSource.start(serial) { entry ->
                entries.add(entry)
                if (entries.size > 5000) entries.removeAt(0) // bounded buffer — PRD §42
            }
        }
    }

    fun stop() {
        streaming = false
        scope.launch { logcatSource.stop() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box {
                OutlinedButton(onClick = { deviceMenuExpanded = true }) {
                    Text(selectedSerial ?: "No device")
                }
                DropdownMenu(expanded = deviceMenuExpanded, onDismissRequest = { deviceMenuExpanded = false }) {
                    devices.forEach { device ->
                        DropdownMenuItem(text = { Text(device.name ?: device.serial) }, onClick = {
                            selectedSerial = device.serial
                            deviceMenuExpanded = false
                        })
                    }
                }
            }
            Box(modifier = Modifier.padding(start = Spacing.sm)) {
                OutlinedButton(onClick = { levelMenuExpanded = true }) { Text("Min: ${minLevel.name}") }
                DropdownMenu(expanded = levelMenuExpanded, onDismissRequest = { levelMenuExpanded = false }) {
                    LogLevel.entries.forEach { level ->
                        DropdownMenuItem(text = { Text(level.name) }, onClick = {
                            minLevel = level
                            levelMenuExpanded = false
                        })
                    }
                }
            }
            if (!streaming) {
                Button(onClick = { start() }, modifier = Modifier.padding(start = Spacing.sm)) { Text("Start") }
            } else {
                Button(onClick = { stop() }, modifier = Modifier.padding(start = Spacing.sm)) { Text("Stop") }
            }
            OutlinedButton(onClick = { entries.clear() }, modifier = Modifier.padding(start = Spacing.sm)) { Text("Clear") }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search (tag or message)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
        )

        val filtered = entries.filter {
            it.level.ordinal >= minLevel.ordinal &&
                (search.isBlank() || it.tag.contains(search, true) || it.message.contains(search, true))
        }

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(bottom = Spacing.sm)) {
            if (streaming) {
                PulsingDot(MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = Spacing.sm))
                Text("LIVE  ·  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
            Text(
                "${filtered.size} / ${entries.size} lines",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (entries.isEmpty()) {
            EmptyState("No log lines yet. Select a device and press Start.")
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
                .padding(Spacing.md),
        ) {
            items(filtered) { entry ->
                Text(
                    "${entry.timestamp} ${entry.pid}/${entry.tid} ${entry.level.name.first()} ${entry.tag}: ${entry.message}",
                    color = logLevelColor(entry.level),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(vertical = 1.dp),
                )
            }
        }
    }
}
