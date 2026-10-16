package dev.prateekthakur.devlens.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.prateekthakur.devlens.domain.crash.CrashAnalyzer
import dev.prateekthakur.devlens.domain.deeplink.DeepLinkTester
import dev.prateekthakur.devlens.domain.device.DeviceManager
import dev.prateekthakur.devlens.domain.intent.IntentTester
import dev.prateekthakur.devlens.domain.logcat.LogcatSource
import dev.prateekthakur.devlens.ui.crashes.CrashesScreen
import dev.prateekthakur.devlens.ui.intent.DeepLinkTab
import dev.prateekthakur.devlens.ui.intent.IntentTab
import dev.prateekthakur.devlens.ui.logs.LogsScreen

private val tabs = listOf("Logs", "Crashes", "Intents", "Deep Links")

/** PRD §21-26 — Logcat, crash analysis, intent testing and deep-link testing. */
@Composable
fun DebugScreen(
    deviceManager: DeviceManager,
    logcatSource: LogcatSource,
    crashAnalyzer: CrashAnalyzer,
    intentTester: IntentTester,
    deepLinkTester: DeepLinkTester,
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(top = dev.prateekthakur.devlens.ui.theme.Spacing.lg)) {
            when (selectedTab) {
                0 -> LogsScreen(deviceManager, logcatSource)
                1 -> CrashesScreen(crashAnalyzer)
                2 -> IntentTab(intentTester, deviceManager)
                3 -> DeepLinkTab(deepLinkTester, deviceManager)
            }
        }
    }
}
