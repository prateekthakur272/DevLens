package dev.prateekthakur.devlens.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A tiny dependency-free line-icon set (no icon-font artifact resolves for this Compose
 * Multiplatform version) drawn to the same 24dp / ~2dp-stroke convention as Material Symbols.
 */
typealias NavIcon = @Composable (Modifier) -> Unit

private fun DrawScope.stroke(color: Color) = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
private fun IconCanvas(modifier: Modifier, draw: DrawScope.(Color) -> Unit) {
    val color = LocalContentColor.current
    Canvas(modifier = modifier) { draw(color) }
}

val DashboardIcon: NavIcon = { modifier ->
    IconCanvas(modifier) { color ->
        val s = stroke(color)
        val g = size.minDimension / 12f
        val cell = size.minDimension * 5f / 12f
        listOf(
            Offset(g, g), Offset(g * 2 + cell, g), Offset(g, g * 2 + cell), Offset(g * 2 + cell, g * 2 + cell),
        ).forEach { topLeft ->
            drawRoundRect(color, topLeft = topLeft, size = Size(cell, cell), cornerRadius = CornerRadius(cell * 0.25f), style = s)
        }
    }
}

val ProjectsIcon: NavIcon = { modifier ->
    IconCanvas(modifier) { color ->
        val s = stroke(color)
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.12f, h * 0.28f),
            size = Size(w * 0.76f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.06f),
            style = s,
        )
        drawLine(color, Offset(w * 0.12f, h * 0.28f), Offset(w * 0.4f, h * 0.16f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.4f, h * 0.16f), Offset(w * 0.55f, h * 0.28f), strokeWidth = s.width, cap = StrokeCap.Round)
    }
}

val ApkIcon: NavIcon = { modifier ->
    IconCanvas(modifier) { color ->
        val s = stroke(color)
        val w = size.width
        val h = size.height
        val top = Offset(w * 0.5f, h * 0.12f)
        val left = Offset(w * 0.14f, h * 0.32f)
        val right = Offset(w * 0.86f, h * 0.32f)
        val bottom = Offset(w * 0.5f, h * 0.9f)
        val bl = Offset(w * 0.14f, h * 0.68f)
        val br = Offset(w * 0.86f, h * 0.68f)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(br.x, br.y)
            lineTo(bottom.x, bottom.y); lineTo(bl.x, bl.y); lineTo(left.x, left.y); close()
        }
        drawPath(path, color, style = s)
        drawLine(color, left, right, strokeWidth = s.width)
        drawLine(color, Offset(w * 0.5f, h * 0.5f), bottom, strokeWidth = s.width)
    }
}

val DevicesIcon: NavIcon = { modifier ->
    IconCanvas(modifier) { color ->
        val s = stroke(color)
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.3f, h * 0.1f),
            size = Size(w * 0.4f, h * 0.8f),
            cornerRadius = CornerRadius(w * 0.08f),
            style = s,
        )
        drawLine(color, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.5f, h * 0.78f), strokeWidth = s.width * 1.8f, cap = StrokeCap.Round)
    }
}

val DebugIcon: NavIcon = { modifier ->
    IconCanvas(modifier) { color ->
        val s = stroke(color)
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.3f, h * 0.3f),
            size = Size(w * 0.4f, h * 0.5f),
            cornerRadius = CornerRadius(w * 0.15f),
            style = s,
        )
        drawLine(color, Offset(w * 0.5f, h * 0.3f), Offset(w * 0.5f, h * 0.16f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.3f, h * 0.45f), Offset(w * 0.14f, h * 0.4f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.45f), Offset(w * 0.86f, h * 0.4f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.3f, h * 0.6f), Offset(w * 0.14f, h * 0.6f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.6f), Offset(w * 0.86f, h * 0.6f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.3f, h * 0.75f), Offset(w * 0.14f, h * 0.8f), strokeWidth = s.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.75f), Offset(w * 0.86f, h * 0.8f), strokeWidth = s.width, cap = StrokeCap.Round)
    }
}

val FlutterIcon: NavIcon = { modifier ->
    IconCanvas(modifier) { color ->
        val s = stroke(color)
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.2f, h * 0.5f)
            lineTo(w * 0.55f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.15f)
            lineTo(w * 0.45f, h * 0.55f)
            lineTo(w * 0.85f, h * 0.95f)
            lineTo(w * 0.55f, h * 0.95f)
            lineTo(w * 0.3f, h * 0.7f)
        }
        drawPath(path, color, style = s)
    }
}

val ReportsIcon: NavIcon = { modifier ->
    IconCanvas(modifier) { color ->
        val s = stroke(color)
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.2f, h * 0.1f),
            size = Size(w * 0.6f, h * 0.8f),
            cornerRadius = CornerRadius(w * 0.06f),
            style = s,
        )
        listOf(0.32f, 0.48f, 0.64f).forEach { fy ->
            drawLine(color, Offset(w * 0.32f, h * fy), Offset(w * 0.68f, h * fy), strokeWidth = s.width, cap = StrokeCap.Round)
        }
    }
}

val AiIcon: NavIcon = { modifier ->
    IconCanvas(modifier) { color ->
        val s = stroke(color)
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, h * 0.08f)
            lineTo(cx + w * 0.08f, cy - h * 0.08f)
            lineTo(w * 0.92f, cy)
            lineTo(cx + w * 0.08f, cy + h * 0.08f)
            lineTo(cx, h * 0.92f)
            lineTo(cx - w * 0.08f, cy + h * 0.08f)
            lineTo(w * 0.08f, cy)
            lineTo(cx - w * 0.08f, cy - h * 0.08f)
            close()
        }
        drawPath(path, color, style = s)
    }
}

val SettingsIcon: NavIcon = { modifier ->
    IconCanvas(modifier) { color ->
        val s = stroke(color)
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.5f), style = s)
        drawCircle(color, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = s.width, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.09f, w * 0.09f))))
    }
}
