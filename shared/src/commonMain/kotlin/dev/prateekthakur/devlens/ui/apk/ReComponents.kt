package dev.prateekthakur.devlens.ui.apk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.prateekthakur.devlens.domain.security.Severity

/** Flat, dense building blocks for the analysis views — closer to a reverse-engineering tool than a dashboard. */
internal val ReShape = RoundedCornerShape(2.dp)

@Composable
internal fun Mono(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
    size: TextUnit = 12.sp,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = size,
        lineHeight = size * 1.4f,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/** Bordered, square panel with a small caps title strip. */
@Composable
internal fun RePanel(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth().border(1.dp, cs.outlineVariant, ReShape).background(cs.surfaceContainerLow, ReShape)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(cs.surfaceContainerHigh).padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.padding(end = 7.dp).size(7.dp).background(cs.primary))
            Text(title.uppercase(), color = cs.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            Box(modifier = Modifier.weight(1f))
            if (trailing != null) Mono(trailing, color = cs.onSurfaceVariant, size = 11.sp)
        }
        HorizontalDivider(color = cs.outlineVariant)
        content()
    }
}

@Composable
internal fun ReKv(label: String, value: String, labelWidth: Dp = 130.dp) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)) {
        Mono(label, modifier = Modifier.width(labelWidth), color = MaterialTheme.colorScheme.onSurfaceVariant, size = 11.5.sp)
        Mono(value, modifier = Modifier.weight(1f))
    }
}

internal data class ReCol(val title: String, val weight: Float = 1f, val width: Dp? = null)

@Composable
private fun ReHeader(cols: List<ReCol>) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(modifier = Modifier.fillMaxWidth().background(cs.surfaceContainer)) {
            Box(modifier = Modifier.width(3.dp))
            cols.forEach { c ->
                Box(modifier = (if (c.width != null) Modifier.width(c.width) else Modifier.weight(c.weight)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(c.title.uppercase(), color = cs.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, maxLines = 1)
                }
            }
        }
        HorizontalDivider(color = cs.outlineVariant)
    }
}

@Composable
private fun ReRowView(cols: List<ReCol>, index: Int, selected: Boolean, accent: Color?, onClick: (() -> Unit)?, cell: @Composable (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val bg = when {
        selected -> cs.primary.copy(alpha = 0.16f)
        hovered -> cs.surfaceContainerHigh
        index % 2 == 1 -> cs.surfaceContainer
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(bg)
            .hoverable(source)
            .let { if (onClick != null) it.clickable(interactionSource = source, indication = null, onClick = onClick) else it },
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(accent ?: Color.Transparent))
        cols.forEachIndexed { i, c ->
            Box(modifier = (if (c.width != null) Modifier.width(c.width) else Modifier.weight(c.weight)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                cell(i)
            }
        }
    }
}

/** Table that fills its parent and scrolls its own rows — for full-page views. */
@Composable
internal fun <T> ReTable(
    cols: List<ReCol>,
    rows: List<T>,
    modifier: Modifier = Modifier,
    selected: T? = null,
    onSelect: ((T) -> Unit)? = null,
    emptyText: String = "— none —",
    accent: ((T) -> Color?)? = null,
    cell: @Composable (T, Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxSize().border(1.dp, cs.outlineVariant, ReShape).background(cs.surfaceContainerLow, ReShape)) {
        ReHeader(cols)
        if (rows.isEmpty()) {
            Mono(emptyText, color = cs.onSurfaceVariant, modifier = Modifier.padding(10.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(rows) { index, row ->
                    ReRowView(cols, index, selected = row == selected, accent = accent?.invoke(row), onClick = onSelect?.let { f -> { f(row) } }) { i -> cell(row, i) }
                }
            }
        }
    }
}

/** Same look, but sized to its content — for tables embedded in a scrolling page. */
@Composable
internal fun <T> ReStaticTable(
    cols: List<ReCol>,
    rows: List<T>,
    selected: T? = null,
    onSelect: ((T) -> Unit)? = null,
    emptyText: String = "— none —",
    accent: ((T) -> Color?)? = null,
    cell: @Composable (T, Int) -> Unit,
) {
    Column {
        ReHeader(cols)
        if (rows.isEmpty()) {
            Mono(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(10.dp))
        }
        rows.forEachIndexed { index, row ->
            ReRowView(cols, index, selected = row == selected, accent = accent?.invoke(row), onClick = onSelect?.let { f -> { f(row) } }) { i -> cell(row, i) }
        }
    }
}

internal fun severityShort(s: Severity) = when (s) {
    Severity.CRITICAL -> "CRIT"
    Severity.HIGH -> "HIGH"
    Severity.MEDIUM -> "MED"
    Severity.LOW -> "LOW"
    Severity.INFO -> "INFO"
}

/** Square, tinted severity tag, e.g. `HIGH`. */
@Composable
internal fun SevLabel(s: Severity) {
    val c = severityChartColors.getValue(s)
    Box(modifier = Modifier.background(c.copy(alpha = 0.16f), ReShape).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Mono(severityShort(s), color = if (s == Severity.LOW) Color(0xFF8A5A00) else c, bold = true, size = 10.5.sp)
    }
}

/** Small square on/off button used for filters. */
@Composable
internal fun ReToggle(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .border(1.dp, if (selected) accent else cs.outlineVariant, ReShape)
            .background(if (selected) accent.copy(alpha = 0.16f) else Color.Transparent, ReShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Mono(label, size = 11.sp, bold = selected)
    }
}

internal val ReGap = Arrangement.spacedBy(10.dp)

/** Segmented meter: [fraction] of [segments] square blocks filled (animated). */
@Composable
internal fun SegMeter(fraction: Float, color: Color, modifier: Modifier = Modifier, segments: Int = 20) {
    val animated by androidx.compose.animation.core.animateFloatAsState(fraction.coerceIn(0f, 1f), androidx.compose.animation.core.tween(700))
    val filled = (animated * segments).toInt()
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(segments) { i ->
            Box(modifier = Modifier.weight(1f).height(6.dp).background(if (i < filled) color else MaterialTheme.colorScheme.surfaceContainerHigh))
        }
    }
}

/** One cell of a stat strip: small caps label, large monospace value, optional footer. */
@Composable
internal fun RowScope.StatCell(label: String, value: String, color: Color, modifier: Modifier = Modifier, footer: @Composable () -> Unit = {}) {
    Column(modifier = modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, maxLines = 1)
        Mono(value, color = color, bold = true, size = 24.sp, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
        footer()
    }
}
