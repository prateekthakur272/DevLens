package dev.prateekthakur.devlens.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.prateekthakur.devlens.ui.theme.Spacing

/** Placeholder for MVP+ screens not yet implemented (PRD §60 — build the first slice, not everything at once). */
@Composable
fun ComingSoonScreen(title: String, description: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title)
        SectionCard(modifier = Modifier.padding(top = Spacing.lg)) {
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
