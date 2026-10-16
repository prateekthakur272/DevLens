package dev.prateekthakur.devlens.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.prateekthakur.devlens.ui.theme.Spacing

/**
 * Fixed, edge-to-edge tab bar with an underline indicator. When there is room the tabs share the
 * width equally; on narrow windows they keep their natural width and the row scrolls.
 */
@Composable
fun EdgeTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badges: List<Pair<String, Color>?> = emptyList(),
) {
    val cs = MaterialTheme.colorScheme
    Surface(modifier = modifier.fillMaxWidth(), shape = RectangleShape, color = cs.surfaceContainerLow) {
        Column {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val equalWidth = maxWidth >= 104.dp * tabs.size
                Row(modifier = if (equalWidth) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState())) {
                    tabs.forEachIndexed { index, title ->
                        val selected = index == selectedIndex
                        val textColor by animateColorAsState(if (selected) cs.onSurface else cs.onSurfaceVariant, tween(200))
                        val indicator by animateColorAsState(if (selected) cs.primary else cs.primary.copy(alpha = 0f), tween(200))
                        Box(
                            modifier = (if (equalWidth) Modifier.weight(1f) else Modifier)
                                .height(32.dp)
                                .clickable { onSelect(index) }
                                .padding(horizontal = Spacing.md),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = textColor,
                                maxLines = 1,
                                softWrap = false,
                            )
                            badges.getOrNull(index)?.let { (label, color) ->
                                Text(
                                    label,
                                    color = color,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 6.dp).background(color.copy(alpha = 0.14f)).padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                            }
                            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(indicator))
                        }
                    }
                }
            }
            HorizontalDivider(color = cs.outlineVariant)
        }
    }
}
