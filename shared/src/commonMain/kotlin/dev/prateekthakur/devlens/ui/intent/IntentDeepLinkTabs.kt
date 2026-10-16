package dev.prateekthakur.devlens.ui.intent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.prateekthakur.devlens.domain.deeplink.DeepLinkTester
import dev.prateekthakur.devlens.domain.device.DeviceInfo
import dev.prateekthakur.devlens.domain.device.DeviceManager
import dev.prateekthakur.devlens.domain.intent.IntentSpec
import dev.prateekthakur.devlens.domain.intent.IntentTester
import dev.prateekthakur.devlens.ui.common.SectionCard
import dev.prateekthakur.devlens.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
private fun DeviceSelector(deviceManager: DeviceManager, selected: String?, onSelect: (String) -> Unit) {
    var devices by remember { mutableStateOf<List<DeviceInfo>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        deviceManager.listDevices().onSuccess {
            devices = it
            it.firstOrNull()?.let { d -> onSelect(d.serial) }
        }
    }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(selected ?: "No device") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            devices.forEach { device ->
                DropdownMenuItem(text = { Text(device.name ?: device.serial) }, onClick = { onSelect(device.serial); expanded = false })
            }
        }
    }
}

/** PRD §25 — construct and resolve/launch an intent. */
@Composable
fun IntentTab(intentTester: IntentTester, deviceManager: DeviceManager) {
    var serial by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf("android.intent.action.VIEW") }
    var dataUri by remember { mutableStateOf("") }
    var mimeType by remember { mutableStateOf("") }
    var component by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun spec() = IntentSpec(
        action = action.ifBlank { null },
        dataUri = dataUri.ifBlank { null },
        mimeType = mimeType.ifBlank { null },
        targetPackage = null,
        component = component.ifBlank { null },
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SectionCard {
            DeviceSelector(deviceManager, serial) { serial = it }
            OutlinedTextField(action, { action = it }, label = { Text("Action") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = Spacing.md))
            OutlinedTextField(dataUri, { dataUri = it }, label = { Text("Data URI") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = Spacing.md))
            OutlinedTextField(mimeType, { mimeType = it }, label = { Text("MIME type") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = Spacing.md))
            OutlinedTextField(component, { component = it }, label = { Text("Component (pkg/.Activity)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = Spacing.md))
            Row(modifier = Modifier.padding(top = Spacing.md)) {
                Button(onClick = {
                    val s = serial ?: return@Button
                    scope.launch {
                        intentTester.resolve(spec(), s)
                            .onSuccess { resultText = if (it.isEmpty()) "No matching component." else it.joinToString("\n") { r -> "${r.packageName}/${r.activityName}" } }
                            .onFailure { resultText = "Error: ${it.message}" }
                    }
                }) { Text("Resolve") }
                Button(onClick = {
                    val s = serial ?: return@Button
                    scope.launch {
                        intentTester.launch(spec(), s)
                            .onSuccess { resultText = it.ifBlank { "Launched." } }
                            .onFailure { resultText = "Error: ${it.message}" }
                    }
                }, modifier = Modifier.padding(start = Spacing.sm)) { Text("Launch") }
            }
        }
        if (resultText.isNotBlank()) {
            SectionCard(title = "Result", modifier = Modifier.padding(top = Spacing.lg)) {
                Text(resultText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** PRD §26 — resolve a custom-scheme / http(s) deep link against connected device apps. */
@Composable
fun DeepLinkTab(deepLinkTester: DeepLinkTester, deviceManager: DeviceManager) {
    var serial by remember { mutableStateOf<String?>(null) }
    var url by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        SectionCard {
            DeviceSelector(deviceManager, serial) { serial = it }
            OutlinedTextField(url, { url = it }, label = { Text("URL (https://... or myapp://...)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = Spacing.md))
            Button(
                onClick = {
                    val s = serial ?: return@Button
                    scope.launch {
                        deepLinkTester.resolve(url, s)
                            .onSuccess {
                                resultText = if (it.matchedPackage == null) {
                                    "No app resolves this link."
                                } else {
                                    "${it.matchedPackage} / ${it.matchedActivity}"
                                }
                            }
                            .onFailure { resultText = "Error: ${it.message}" }
                    }
                },
                modifier = Modifier.padding(top = Spacing.md),
            ) { Text("Resolve") }
        }
        if (resultText.isNotBlank()) {
            SectionCard(title = "Result", modifier = Modifier.padding(top = Spacing.lg)) {
                Text(resultText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
