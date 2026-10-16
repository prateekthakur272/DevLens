package dev.prateekthakur.devlens.ui.crashes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.prateekthakur.devlens.domain.crash.CauseConfidence
import dev.prateekthakur.devlens.domain.crash.CrashAnalyzer
import dev.prateekthakur.devlens.domain.crash.CrashReport
import dev.prateekthakur.devlens.ui.common.ErrorBanner
import dev.prateekthakur.devlens.ui.common.LabeledValue
import dev.prateekthakur.devlens.ui.common.ScreenHeader
import dev.prateekthakur.devlens.ui.common.SectionCard
import dev.prateekthakur.devlens.ui.common.Tag
import dev.prateekthakur.devlens.ui.theme.Spacing

/** PRD §23 — paste a stack trace / crash log and extract root cause with confidence labels. */
@Composable
fun CrashesScreen(crashAnalyzer: CrashAnalyzer) {
    var rawLog by remember { mutableStateOf("") }
    var report by remember { mutableStateOf<CrashReport?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("Crash Analyzer", "Paste a stack trace or a Logcat crash dump")

        SectionCard(modifier = Modifier.padding(top = Spacing.lg)) {
            OutlinedTextField(
                value = rawLog,
                onValueChange = { rawLog = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                label = { Text("Stack trace") },
            )
            Button(
                onClick = {
                    crashAnalyzer.analyze(rawLog)
                        .onSuccess { report = it; error = null }
                        .onFailure { error = it.message; report = null }
                },
                modifier = Modifier.padding(top = Spacing.md),
            ) {
                Text("Analyze")
            }
        }

        error?.let { ErrorBanner(it) }

        report?.let { r ->
            SectionCard(modifier = Modifier.padding(top = Spacing.lg)) {
                Text("${r.exceptionType}: ${r.exceptionMessage}", style = MaterialTheme.typography.titleMedium)
                LabeledValue("Thread", r.threadName)
                LabeledValue("Process", r.processName)
            }
            SectionCard(title = "Causes", modifier = Modifier.padding(top = Spacing.lg)) {
                r.causes.forEach { cause ->
                    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (container, content) = confidenceTone(cause.confidence)
                            Tag(confidenceLabel(cause.confidence), container, content, modifier = Modifier.padding(end = Spacing.sm))
                            Text(cause.description, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(cause.evidence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.xs))
                    }
                }
            }
            if (r.stackTrace.isNotEmpty()) {
                SectionCard(title = "Stack trace", modifier = Modifier.padding(top = Spacing.lg)) {
                    r.stackTrace.forEach { frame ->
                        Text("at $frame", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun confidenceTone(confidence: CauseConfidence) = when (confidence) {
    CauseConfidence.ROOT_CAUSE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    CauseConfidence.LIKELY_CAUSE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    CauseConfidence.POSSIBLE_CAUSE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
}

private fun confidenceLabel(confidence: CauseConfidence) = when (confidence) {
    CauseConfidence.ROOT_CAUSE -> "Root Cause"
    CauseConfidence.LIKELY_CAUSE -> "Likely Cause"
    CauseConfidence.POSSIBLE_CAUSE -> "Possible Cause"
}
