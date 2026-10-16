package dev.prateekthakur.devlens.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.prateekthakur.devlens.ui.theme.Spacing

/** A [SectionCard] that lifts and brightens slightly on mouse hover — desktop apps should feel alive to the cursor. */
@Composable
fun HoverableCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(if (hovered && !pressed) 3.dp else 0.dp)
    val container by animateColorAsState(
        if (hovered) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        tween(150),
    )
    val scale by animateFloatAsState(if (pressed) 0.99f else 1f, animationSpec = tween(120))

    var interactionModifier: Modifier = Modifier.hoverable(interactionSource).scale(scale)
    if (onClick != null) {
        interactionModifier = interactionModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    }

    Card(
        modifier = modifier.fillMaxWidth().then(interactionModifier),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), content = content)
    }
}

/** A tap-to-expand card: the title row is always visible, the detail content animates open/closed. */
@Composable
fun ExpandableCard(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    detail: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, animationSpec = tween(200))

    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Chevron(modifier = Modifier.rotate(rotation).padding(end = Spacing.sm))
                Row(modifier = Modifier.fillMaxWidth()) { title() }
            }
            if (expanded) {
                Column(modifier = Modifier.padding(top = Spacing.sm)) { detail() }
            }
        }
    }
}

@Composable
private fun Chevron(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(16.dp)) {
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.2f)
            lineTo(size.width * 0.7f, size.height * 0.5f)
            lineTo(size.width * 0.3f, size.height * 0.8f)
        }
        drawPath(path, color, style = stroke)
    }
}

/** Small pulsing dot used to indicate a live/streaming state. */
@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
    )
    Canvas(modifier = modifier.size(10.dp)) {
        drawCircle(color.copy(alpha = alpha), radius = size.minDimension / 2)
    }
}

/** A minimal interactive donut chart — tap a segment (or its legend row) to highlight it. */
@Composable
fun DonutChart(
    data: List<Triple<String, Long, Color>>,
    modifier: Modifier = Modifier,
    holeFraction: Float = 0.62f,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    val total = data.sumOf { it.second }.coerceAtLeast(1)
    val sweepAnim = remember { Animatable(0f) }
    LaunchedEffect(data) { sweepAnim.animateTo(1f, tween(700)) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(180.dp)) {
            var startAngle = -90f
            val strokeWidth = size.minDimension * (1 - holeFraction) / 2
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            data.forEachIndexed { index, (_, value, color) ->
                val sweep = (value.toDouble() / total * 360.0).toFloat() * sweepAnim.value
                val isSelected = selected == index
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth * (if (isSelected) 1.15f else 1f), cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }
        Column(modifier = Modifier.padding(start = Spacing.lg)) {
            data.forEachIndexed { index, (label, value, color) ->
                val isSelected = selected == index
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 3.dp)
                        .clickable { selected = if (isSelected) null else index },
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) { drawCircle(color) }
                    Text(
                        "  $label — ${percentString(value, total)}%",
                        style = if (isSelected) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun percentString(value: Long, total: Long): String {
    val pct = value.toDouble() / total * 100
    val rounded = (pct * 10).toInt() / 10.0
    return rounded.toString()
}

/** A ring gauge (e.g. "environment health") that sweeps in on first composition. */
@Composable
fun RingGauge(percent: Float, label: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(percent) { animated.animateTo(percent, tween(900)) }
    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(96.dp)) {
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            drawArc(color = color.copy(alpha = 0.18f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(color = color, startAngle = -90f, sweepAngle = 360f * animated.value, useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(animated.value * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
