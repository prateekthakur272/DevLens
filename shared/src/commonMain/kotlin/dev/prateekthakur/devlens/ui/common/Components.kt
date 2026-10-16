package dev.prateekthakur.devlens.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.prateekthakur.devlens.ui.theme.Spacing

/** Page title + optional subtitle + trailing actions, used at the top of every screen. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), content = actions)
    }
}

/** A titled, elevated content block — the base building unit for every panel of data. */
@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Spacing.sm))
            }
            content()
        }
    }
}

@Composable
fun LabeledValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = Spacing.sm))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm), color = MaterialTheme.colorScheme.outlineVariant)
}

/** A small rounded, filled label — used for severity/risk/status tags. */
@Composable
fun Tag(text: String, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
        )
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.padding(Spacing.lg))
}

@Composable
fun ErrorBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(Spacing.md))
    }
}

@Composable
fun LoadingBlock(message: String = "Working…") {
    Row(modifier = Modifier.padding(Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.padding(end = Spacing.md))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}
