package dev.prateekthakur.devlens.ui.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.prateekthakur.devlens.domain.ai.AiRequest
import dev.prateekthakur.devlens.domain.ai.AiService
import dev.prateekthakur.devlens.ui.common.ScreenHeader
import dev.prateekthakur.devlens.ui.common.SectionCard
import dev.prateekthakur.devlens.ui.common.Tag
import dev.prateekthakur.devlens.ui.theme.Spacing
import kotlinx.coroutines.launch

/** PRD §34-35 — AI is optional and local-first by default; no provider is configured until Settings opts in. */
@Composable
fun AiAssistantScreen(aiService: AiService) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("AI Assistant", "No cloud AI is configured — DevLens never sends data anywhere until you add a provider")

        SectionCard(modifier = Modifier.padding(top = Spacing.lg)) {
            OutlinedTextField(
                question,
                { question = it },
                label = { Text("Ask about a crash, permission, or build error") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    scope.launch {
                        aiService.explain(AiRequest(question = question, evidence = emptyMap())).onSuccess {
                            answer = it.answer
                            confidence = it.confidence.name
                        }
                    }
                },
                modifier = Modifier.padding(top = Spacing.md),
            ) { Text("Ask") }
        }

        if (answer.isNotBlank()) {
            SectionCard(modifier = Modifier.padding(top = Spacing.lg)) {
                Row {
                    Tag(confidence, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = Spacing.sm))
                    Text(answer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
