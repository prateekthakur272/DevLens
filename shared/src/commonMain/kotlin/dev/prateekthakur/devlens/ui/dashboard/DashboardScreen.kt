package dev.prateekthakur.devlens.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.prateekthakur.devlens.core.model.ToolCheck
import dev.prateekthakur.devlens.core.model.ToolStatus
import dev.prateekthakur.devlens.domain.environment.EnvironmentDoctor
import dev.prateekthakur.devlens.ui.common.HoverableCard
import dev.prateekthakur.devlens.ui.common.LoadingBlock
import dev.prateekthakur.devlens.ui.common.RingGauge
import dev.prateekthakur.devlens.ui.common.ScreenHeader
import dev.prateekthakur.devlens.ui.common.SectionCard
import dev.prateekthakur.devlens.ui.common.Tag
import dev.prateekthakur.devlens.ui.theme.Spacing
import dev.prateekthakur.devlens.ui.theme.toolStatusTone

/** PRD §10 — overview of the developer environment. */
@Composable
fun DashboardScreen(environmentDoctor: EnvironmentDoctor) {
    var checks by remember { mutableStateOf<List<ToolCheck>?>(null) }

    LaunchedEffect(Unit) {
        checks = environmentDoctor.checkAll()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("Dashboard", "Your local Android/Flutter developer environment at a glance")

        val results = checks
        if (results == null) {
            LoadingBlock()
        } else {
            val readyCount = results.count { it.status == ToolStatus.READY }
            val health = readyCount.toFloat() / results.size.coerceAtLeast(1)

            SectionCard(modifier = Modifier.padding(top = Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingGauge(
                        percent = health,
                        label = "ready",
                        color = when {
                            health >= 0.99f -> MaterialTheme.colorScheme.primary
                            health >= 0.5f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        },
                    )
                    Column(modifier = Modifier.padding(start = Spacing.xl)) {
                        Text("Environment health", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$readyCount of ${results.size} tools ready to go",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxSize().padding(top = Spacing.lg),
            ) {
                items(results) { check -> ToolCard(check) }
            }
        }
    }
}

@Composable
private fun ToolCard(check: ToolCheck) {
    val tone = toolStatusTone(check.status)
    HoverableCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(check.name, style = MaterialTheme.typography.titleSmall)
            Tag(statusLabel(check.status), tone.container, tone.content)
        }
        check.version?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.xs))
        }
        check.path?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.xs))
        }
        check.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.xs))
        }
    }
}

private fun statusLabel(status: ToolStatus) = when (status) {
    ToolStatus.READY -> "Ready"
    ToolStatus.WARNING -> "Warning"
    ToolStatus.NOT_FOUND -> "Not found"
}
