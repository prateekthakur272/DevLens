package dev.prateekthakur.devlens.infrastructure.report

import dev.prateekthakur.devlens.domain.permission.PermissionRisk
import dev.prateekthakur.devlens.domain.report.ApkReportData
import dev.prateekthakur.devlens.domain.report.PdfReportWriter
import dev.prateekthakur.devlens.domain.security.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.Color
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Renders an [ApkReportData] to a multi-page A4 PDF in the same flat, dense, monospace language as the app:
 * bordered panels, tables with severity edges, a stat strip and a severity bar. Uses the built-in PDF fonts.
 */
class PdfBoxReportWriter : PdfReportWriter {
    override suspend fun write(data: ApkReportData, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            PDDocument().use { doc ->
                PdfLayout(doc, data).render()
                doc.save(File(path))
            }
        }
    }
}

private val GREEN = Color(0x3D, 0xDC, 0x84)
private val INK = Color(0x1A, 0x1B, 0x20)
private val MUTED = Color(0x5F, 0x63, 0x6E)
private val LINE = Color(0xC1, 0xC9, 0xC1)
private val STRIP = Color(0xE6, 0xEB, 0xE6)
private val HEAD = Color(0xF1, 0xF4, 0xF1)
private val ZEBRA = Color(0xF6, 0xF8, 0xF6)
private val BLOCK_OFF = Color(0xE0, 0xE5, 0xE0)
private val OK_GREEN = Color(0x2E, 0x7D, 0x32)

private fun severityColor(s: Severity) = when (s) {
    Severity.CRITICAL -> Color(0xB7, 0x1C, 0x1C)
    Severity.HIGH -> Color(0xE5, 0x39, 0x35)
    Severity.MEDIUM -> Color(0xEF, 0x6C, 0x00)
    Severity.LOW -> Color(0xF9, 0xA8, 0x25)
    Severity.INFO -> Color(0x3D, 0x5A, 0xFE)
}

private fun severityShort(s: Severity) = when (s) {
    Severity.CRITICAL -> "CRIT"
    Severity.HIGH -> "HIGH"
    Severity.MEDIUM -> "MED"
    Severity.LOW -> "LOW"
    Severity.INFO -> "INFO"
}

private fun riskColor(r: PermissionRisk) = when (r) {
    PermissionRisk.DANGEROUS -> Color(0xD3, 0x2F, 0x2F)
    PermissionRisk.SPECIAL -> Color(0xE6, 0x51, 0x00)
    PermissionRisk.SIGNATURE -> Color(0x7B, 0x1F, 0xA2)
    PermissionRisk.NORMAL -> OK_GREEN
    PermissionRisk.UNKNOWN -> Color(0x75, 0x75, 0x75)
}

private fun tint(c: Color, amount: Float = 0.16f) = Color(
    (255 - (255 - c.red) * amount).toInt(),
    (255 - (255 - c.green) * amount).toInt(),
    (255 - (255 - c.blue) * amount).toInt(),
)

/** A table cell: text, optional tint tag, or a horizontal bar. */
private class Cell(val text: String = "", val color: Color = INK, val bold: Boolean = false, val tag: Color? = null, val bar: Float? = null)

private class Col(val title: String, val weight: Float)

private class PdfLayout(private val doc: PDDocument, private val data: ApkReportData) {
    private val mono = PDType1Font(Standard14Fonts.FontName.COURIER)
    private val monoBold = PDType1Font(Standard14Fonts.FontName.COURIER_BOLD)
    private val sansBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
    private val pageW = PDRectangle.A4.width
    private val pageH = PDRectangle.A4.height
    private val margin = 36f
    private val contentW = pageW - margin * 2
    private val top = pageH - 50f
    private val bottom = 48f
    private val fs = 8f          // body size
    private val lh = 10.8f       // body line height

    private lateinit var cs: PDPageContentStream
    private var y = 0f
    private var panelOpen = false

    fun render() {
        newPage(first = true)
        titleBlock()
        summary()
        findings()
        findingDetails()
        attackSurface()
        apkInfo()
        permissions()
        secrets()
        signing()
        sizes()
        closePanel()
        cs.close()
        addFooters()
    }

    // ---- primitives ----------------------------------------------------------

    private fun newPage(first: Boolean = false) {
        if (!first) {
            if (panelOpen) hline(margin, y, contentW)
            cs.close()
        }
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        cs = PDPageContentStream(doc, page)
        y = top
        if (!first) {
            fill(margin, pageH - 34f, contentW, 14f, STRIP)
            fill(margin + 6f, pageH - 30.5f, 6f, 6f, GREEN)
            text("DEVLENS - APK ANALYSIS REPORT", margin + 18f, pageH - 30f, 6.8f, sansBold, INK, spacing = 0.5f)
            val right = data.fileName.safe()
            text(right, margin + contentW - 6f - width(right, 7f, mono), pageH - 30f, 7f, mono, MUTED)
        }
    }

    private fun ensure(h: Float) {
        if (y - h < bottom) newPage()
    }

    private fun fill(x: Float, yy: Float, w: Float, h: Float, c: Color) {
        cs.setNonStrokingColor(c)
        cs.addRect(x, yy, w, h)
        cs.fill()
    }

    private fun hline(x: Float, yy: Float, w: Float, c: Color = LINE) = fill(x, yy, w, 0.6f, c)

    /** Left + right hairlines of a panel for one row of height [h] whose bottom is at [yBottom]. */
    private fun sides(yBottom: Float, h: Float) {
        fill(margin, yBottom, 0.6f, h, LINE)
        fill(margin + contentW - 0.6f, yBottom, 0.6f, h, LINE)
    }

    private fun String.safe(): String = buildString {
        for (ch in this@safe) {
            when {
                ch == '\t' -> append("  ")
                ch == '\n' || ch == '\r' -> append(' ')
                ch.code in 32..126 -> append(ch)
                else -> try {
                    mono.encode(ch.toString()); append(ch)
                } catch (_: Exception) {
                    append('?')
                }
            }
        }
    }

    private fun width(t: String, size: Float, f: PDType1Font) = f.getStringWidth(t) / 1000f * size

    private fun text(t: String, x: Float, yy: Float, size: Float, f: PDType1Font, c: Color, spacing: Float = 0f) {
        cs.beginText()
        cs.setFont(f, size)
        cs.setNonStrokingColor(c)
        cs.setCharacterSpacing(spacing)
        cs.newLineAtOffset(x, yy)
        cs.showText(t.safe())
        cs.endText()
    }

    /** Greedy word wrap that also hard-breaks tokens longer than the line (paths, hashes, class names). */
    private fun wrap(raw: String, size: Float, f: PDType1Font, maxW: Float): List<String> {
        val out = mutableListOf<String>()
        var line = ""
        for (word in raw.safe().split(' ').filter { it.isNotEmpty() }) {
            var w = word
            while (width(w, size, f) > maxW) {
                var cut = w.length
                while (cut > 1 && width(w.substring(0, cut), size, f) > maxW) cut--
                if (line.isNotEmpty()) { out += line; line = "" }
                out += w.substring(0, cut)
                w = w.substring(cut)
            }
            val candidate = if (line.isEmpty()) w else "$line $w"
            if (width(candidate, size, f) <= maxW) line = candidate else { out += line; line = w }
        }
        if (line.isNotEmpty()) out += line
        return out.ifEmpty { listOf("") }
    }

    // ---- panel + table + kv --------------------------------------------------

    private fun closePanel() {
        if (panelOpen) {
            hline(margin, y, contentW)
            panelOpen = false
            y -= 12f
        }
    }

    private fun panel(title: String, trailing: String? = null, minKeep: Float = 60f) {
        closePanel()
        ensure(minKeep + 18f)
        val h = 17f
        fill(margin, y - h, contentW, h, STRIP)
        hline(margin, y, contentW)
        hline(margin, y - h, contentW)
        sides(y - h, h)
        fill(margin + 7f, y - 11.5f, 6f, 6f, GREEN)
        text(title.uppercase(), margin + 18f, y - 11.5f, 7f, sansBold, INK, spacing = 0.5f)
        if (trailing != null) {
            val t = trailing.safe()
            text(t, margin + contentW - 7f - width(t, 7f, mono), y - 11.5f, 7f, mono, MUTED)
        }
        y -= h
        panelOpen = true
    }

    private fun kv(rows: List<Pair<String, String>>, labelW: Float = 112f) {
        for ((k, v) in rows) {
            val lines = wrap(v, fs, mono, contentW - labelW - 20f)
            val h = lines.size * lh + 3f
            ensure(h)
            sides(y - h, h)
            text(k, margin + 8f, y - 9f, 7.4f, mono, MUTED)
            lines.forEachIndexed { i, l -> text(l, margin + 8f + labelW, y - 9f - i * lh, fs, mono, INK) }
            y -= h
        }
        y -= 3f
        sides(y, 3f)
    }

    private fun table(cols: List<Col>, rows: List<List<Cell>>, accents: List<Color?> = emptyList(), empty: String = "-- none --") {
        val inner = contentW - 3f
        val total = cols.sumOf { it.weight.toDouble() }.toFloat()
        val widths = cols.map { inner * it.weight / total }
        // header
        ensure(30f)
        val hh = 13f
        fill(margin, y - hh, contentW, hh, HEAD)
        sides(y - hh, hh)
        var x = margin + 3f
        cols.forEachIndexed { i, c ->
            text(c.title.uppercase(), x + 5f, y - 9f, 6.4f, sansBold, MUTED, spacing = 0.4f)
            x += widths[i]
        }
        hline(margin, y - hh, contentW)
        y -= hh
        if (rows.isEmpty()) {
            ensure(14f)
            sides(y - 14f, 14f)
            text(empty, margin + 8f, y - 10f, fs, mono, MUTED)
            y -= 14f
            return
        }
        rows.forEachIndexed { r, cells ->
            val wrapped = cells.mapIndexed { i, cell ->
                if (cell.bar != null) listOf("") else wrap(cell.text, fs, if (cell.bold) monoBold else mono, widths[i] - 10f - (if (cell.tag != null) 6f else 0f))
            }
            val h = (wrapped.maxOf { it.size } * lh + 4f).coerceAtLeast(14f)
            ensure(h)
            if (r % 2 == 1) fill(margin, y - h, contentW, h, ZEBRA)
            sides(y - h, h)
            accents.getOrNull(r)?.let { fill(margin + 0.6f, y - h, 2.4f, h, it) }
            var cx = margin + 3f
            cells.forEachIndexed { i, cell ->
                val font = if (cell.bold) monoBold else mono
                when {
                    cell.bar != null -> {
                        fill(cx + 5f, y - 9f, widths[i] - 12f, 4.5f, BLOCK_OFF)
                        fill(cx + 5f, y - 9f, ((widths[i] - 12f) * cell.bar).coerceIn(0f, widths[i] - 12f), 4.5f, cell.color)
                    }
                    cell.tag != null -> {
                        val tw = width(cell.text, 7f, monoBold) + 8f
                        fill(cx + 5f, y - 11.5f, tw, 9.5f, tint(cell.tag))
                        text(cell.text, cx + 9f, y - 9f, 7f, monoBold, cell.color)
                    }
                    else -> wrapped[i].forEachIndexed { li, l -> text(l, cx + 5f, y - 9f - li * lh, fs, font, cell.color) }
                }
                cx += widths[i]
            }
            y -= h
        }
    }

    // ---- sections ----------------------------------------------------------------

    private fun titleBlock() {
        fill(margin, y - 7f, 7f, 7f, GREEN)
        text("DEVLENS - APK SECURITY REPORT", margin + 14f, y - 6.5f, 7.5f, sansBold, INK, spacing = 0.7f)
        val stamp = "generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        text(stamp, margin + contentW - width(stamp, 7.5f, mono), y - 6.5f, 7.5f, mono, MUTED)
        y -= 20f
        val iconImage = data.icon?.let { runCatching { PDImageXObject.createFromByteArray(doc, it, "icon") }.getOrNull() }
        val textX = if (iconImage != null) margin + 54f else margin
        if (iconImage != null) {
            fill(margin, y - 46f, 46f, 46f, LINE)
            fill(margin + 0.8f, y - 45.2f, 44.4f, 44.4f, Color.WHITE)
            cs.drawImage(iconImage, margin + 3f, y - 43f, 40f, 40f)
        }
        val title = data.appLabel ?: data.fileName
        text(title.safe(), textX, y - 16f, 17f, monoBold, INK)
        var ty = y - 30f
        if (data.appLabel != null) {
            text(data.fileName.safe(), textX, ty, 8.5f, mono, MUTED)
            ty -= 12f
        }
        data.metadata?.let {
            text("${it.packageName}   v${it.versionName} (${it.versionCode})".safe(), textX, ty, 8.5f, mono, MUTED)
            ty -= 12f
        }
        y = minOf(ty, y - 46f) - 4f
        for (l in wrap(data.path, 7.4f, mono, contentW)) {
            text(l, margin, y - 8f, 7.4f, mono, MUTED)
            y -= 10f
        }
        y -= 6f
        fill(margin, y, contentW, 1.4f, INK)
        fill(margin, y, 54f, 1.4f, GREEN)
        y -= 14f
    }

    private fun summary() {
        panel("Summary", minKeep = 110f)
        val findings = data.security
        val worst = findings.minByOrNull { it.severity.ordinal }?.severity
        val dangerous = data.permissions.count { it.risk == PermissionRisk.DANGEROUS }
        val exported = data.manifest?.let {
            it.activities.count { c -> c.exported } + it.services.count { c -> c.exported } +
                it.receivers.count { c -> c.exported } + it.providers.count { p -> p.exported }
        } ?: 0
        val scoreColor = when {
            data.score >= 85 -> OK_GREEN
            data.score >= 60 -> Color(0xE6, 0x51, 0x00)
            data.score >= 35 -> Color(0xD3, 0x2F, 0x2F)
            else -> Color(0xB7, 0x1C, 0x1C)
        }
        val h = 48f
        ensure(h + 40f)
        sides(y - h, h)
        val cellW = (contentW - 0.6f) / 5.6f
        val widths = listOf(cellW * 1.6f, cellW, cellW, cellW, cellW)
        var x = margin
        data class Stat(val label: String, val value: String, val color: Color, val foot: String)
        val stats = listOf(
            Stat("risk score", data.score.toString(), scoreColor, ""),
            Stat("findings", findings.size.toString(), worst?.let(::severityColor) ?: OK_GREEN, worst?.let { "worst: ${it.name.lowercase()}" } ?: "clean"),
            Stat("secrets", data.secrets.size.toString(), if (data.secrets.isEmpty()) INK else Color(0xD3, 0x2F, 0x2F), "in strings"),
            Stat("dangerous perms", dangerous.toString(), if (dangerous == 0) INK else Color(0xD3, 0x2F, 0x2F), "of ${data.permissions.size}"),
            Stat("exported", exported.toString(), if (exported == 0) INK else Color(0xE6, 0x51, 0x00), "components"),
        )
        stats.forEachIndexed { i, s ->
            if (i > 0) fill(x, y - h, 0.6f, h, LINE)
            text(s.label.uppercase(), x + 9f, y - 11f, 6.2f, sansBold, MUTED, spacing = 0.4f)
            text(s.value, x + 9f, y - 31f, 20f, monoBold, s.color)
            if (i == 0) {
                val blocks = 20
                val filled = (data.score / 100f * blocks).toInt()
                val bw = (widths[0] - 18f - (blocks - 1) * 1.5f) / blocks
                for (b in 0 until blocks) fill(x + 9f + b * (bw + 1.5f), y - 43f, bw, 4f, if (b < filled) scoreColor else BLOCK_OFF)
            } else {
                text(s.foot, x + 9f, y - 43f, 6.8f, mono, MUTED)
            }
            x += widths[i]
        }
        y -= h
        hline(margin, y, contentW)
        // severity distribution bar + legend
        val total = findings.size
        val bh = 34f
        sides(y - bh, bh)
        if (total == 0) {
            text("no findings", margin + 9f, y - 14f, fs, mono, MUTED)
        } else {
            var bx = margin + 9f
            val barW = contentW - 18f
            Severity.entries.forEach { sv ->
                val n = findings.count { it.severity == sv }
                if (n > 0) {
                    val w = barW * n / total
                    fill(bx, y - 14f, (w - 1.5f).coerceAtLeast(1f), 7f, severityColor(sv))
                    bx += w
                }
            }
            var lx = margin + 9f
            Severity.entries.forEach { sv ->
                val n = findings.count { it.severity == sv }
                fill(lx, y - 28f, 5f, 5f, severityColor(sv))
                val label = "${severityShort(sv)} $n"
                text(label, lx + 8f, y - 28.5f, 7f, mono, if (n == 0) MUTED else INK)
                lx += 8f + width(label, 7f, mono) + 14f
            }
        }
        y -= bh
    }

    private fun findings() {
        panel("Findings", "${data.security.size}", minKeep = 50f)
        table(
            listOf(Col("Sev", 0.9f), Col("Rule", 1.1f), Col("Category", 1.4f), Col("Finding", 4.6f), Col("Evidence", 3f)),
            data.security.map { f ->
                listOf(
                    Cell(severityShort(f.severity), severityColor(f.severity), tag = severityColor(f.severity)),
                    Cell(f.ruleId, MUTED),
                    Cell(f.category, MUTED),
                    Cell(f.finding),
                    Cell(f.evidence, MUTED),
                )
            },
            accents = data.security.map { severityColor(it.severity) },
            empty = "-- no findings --",
        )
    }

    private fun findingDetails() {
        if (data.security.isEmpty()) return
        panel("Finding details", "${data.security.size}", minKeep = 70f)
        data.security.forEach { f ->
            val c = severityColor(f.severity)
            ensure(52f)
            val hh = 14f
            fill(margin, y - hh, contentW, hh, HEAD)
            sides(y - hh, hh)
            fill(margin + 0.6f, y - hh, 2.4f, hh, c)
            val tw = width(severityShort(f.severity), 7f, monoBold) + 8f
            fill(margin + 9f, y - 11.5f, tw, 9.5f, tint(c))
            text(severityShort(f.severity), margin + 13f, y - 9f, 7f, monoBold, c)
            text(f.ruleId, margin + 18f + tw, y - 9f, 7.4f, mono, MUTED)
            hline(margin, y - hh, contentW)
            y -= hh
            for (l in wrap(f.finding, 8.6f, monoBold, contentW - 24f)) {
                ensure(lh + 2f)
                sides(y - lh - 1f, lh + 1f)
                text(l, margin + 8f, y - 9f, 8.6f, monoBold, INK)
                y -= lh + 1f
            }
            y -= 1f
            kv(listOf("category" to f.category, "standards" to (f.reference ?: ""), "evidence" to f.evidence, "impact" to f.impact, "recommendation" to f.recommendation).filter { it.second.isNotBlank() }, labelW = 82f)
            hline(margin, y, contentW)
        }
    }

    private fun attackSurface() {
        val m = data.manifest ?: return
        panel("Attack surface", "exported / total", minKeep = 90f)
        val dangerous = data.permissions.count { it.risk == PermissionRisk.DANGEROUS }
        val rows = listOf(
            Triple("activities", m.activities.count { it.exported }, m.activities.size),
            Triple("services", m.services.count { it.exported }, m.services.size),
            Triple("receivers", m.receivers.count { it.exported }, m.receivers.size),
            Triple("providers", m.providers.count { it.exported }, m.providers.size),
            Triple("dangerous permissions", dangerous, data.permissions.size),
        )
        table(
            listOf(Col("Component", 3f), Col("Exposed", 1.4f), Col("", 5f)),
            rows.map { (name, n, total) ->
                val frac = if (total == 0) 0f else n.toFloat() / total
                listOf(Cell(name), Cell("$n / $total", bold = n > 0), Cell(color = if (frac > 0.5f) Color(0xD3, 0x2F, 0x2F) else Color(0x2E, 0x7D, 0x5B), bar = frac))
            },
        )
        y -= 4f
        sides(y, 4f)
        kv(
            listOf(
                "debuggable" to m.debuggable.toString(),
                "allowBackup" to m.allowBackup.toString(),
                "usesCleartextTraffic" to m.usesCleartextTraffic.toString(),
                "networkSecurityConfig" to m.usesNetworkSecurityConfig.toString(),
            ),
            labelW = 130f,
        )
    }

    private fun apkInfo() {
        val m = data.metadata ?: return
        panel("APK information", minKeep = 60f)
        kv(
            listOf(
                "package" to m.packageName,
                "version" to "${m.versionName} (${m.versionCode})",
                "min / target sdk" to "${m.minSdk} / ${m.targetSdk}",
                "size" to "${bytes(m.sizeBytes)} (${m.sizeBytes} bytes)",
                "abis" to m.abis.joinToString().ifBlank { "none (no native code)" },
                "dex files" to m.dexFileCount.toString(),
                "native libs" to m.nativeLibraryCount.toString(),
                "classes / methods" to "${data.classCount} / ${data.methodCount}",
            ),
        )
    }

    private fun permissions() {
        if (data.permissions.isEmpty()) return
        panel("Permissions", "${data.permissions.size}", minKeep = 50f)
        val sorted = data.permissions.sortedBy { if (it.risk == PermissionRisk.DANGEROUS) -1 else it.risk.ordinal }
        table(
            listOf(Col("Risk", 1.5f), Col("Permission", 5f), Col("Description", 5f)),
            sorted.map { p -> listOf(Cell(p.risk.name, riskColor(p.risk), bold = true), Cell(p.name), Cell(p.description, MUTED)) },
            accents = sorted.map { if (it.risk == PermissionRisk.DANGEROUS) riskColor(it.risk) else null },
        )
    }

    private fun secrets() {
        if (data.secrets.isEmpty()) return
        panel("Possible secrets", "${data.secrets.size}", minKeep = 50f)
        table(
            listOf(Col("Kind", 3f), Col("Value (masked)", 4f), Col("Source", 3f)),
            data.secrets.map { s -> listOf(Cell(s.kind.label, Color(0xD3, 0x2F, 0x2F), bold = true), Cell(s.maskedValue), Cell(s.sourceFile, MUTED)) },
        )
    }

    private fun signing() {
        val s = data.signing ?: return
        panel("Signing", if (s.isDebugSigned) "DEBUG CERTIFICATE" else null, minKeep = 60f)
        val rows = mutableListOf(
            "verified" to s.verified.toString(),
            "schemes" to s.schemesVerified.entries.joinToString("  ") { "${it.key}:${if (it.value) "yes" else "no"}" },
        )
        s.signers.forEachIndexed { i, signer ->
            rows += "signer ${i + 1} subject" to signer.subjectDn
            rows += "signer ${i + 1} sha-256" to signer.sha256
            signer.keyAlgorithm?.let { rows += "signer ${i + 1} key" to "$it ${signer.keySizeBits ?: ""}".trim() }
        }
        kv(rows, labelW = 130f)
    }

    private fun sizes() {
        val s = data.size ?: return
        val total = s.totalBytes.coerceAtLeast(1)
        panel("Size by category", bytes(s.totalBytes), minKeep = 60f)
        table(
            listOf(Col("Category", 3f), Col("Size", 2f), Col("%", 1f), Col("", 5f)),
            s.byCategoryBytes.entries.sortedByDescending { it.value }.map { (cat, b) ->
                listOf(Cell(cat), Cell(bytes(b)), Cell("${b * 100 / total}"), Cell(color = Color(0x2E, 0x7D, 0x5B), bar = b.toFloat() / total))
            },
        )
        if (s.largestFiles.isNotEmpty()) {
            panel("Largest files", minKeep = 50f)
            table(
                listOf(Col("Size", 2f), Col("Path", 10f)),
                s.largestFiles.take(12).map { (p, b) -> listOf(Cell(bytes(b)), Cell(p, MUTED)) },
            )
        }
    }

    private fun addFooters() {
        val n = doc.numberOfPages
        for (i in 0 until n) {
            PDPageContentStream(doc, doc.getPage(i), PDPageContentStream.AppendMode.APPEND, true, true).use { f ->
                f.setNonStrokingColor(LINE)
                f.addRect(margin, 36f, contentW, 0.6f)
                f.fill()
                f.setNonStrokingColor(GREEN)
                f.addRect(margin, 36f, 22f, 0.6f)
                f.fill()
                f.beginText()
                f.setFont(mono, 7f)
                f.setNonStrokingColor(MUTED)
                f.newLineAtOffset(margin, 25f)
                f.showText("devlens - all analysis ran locally")
                f.endText()
                val label = "page ${i + 1}/$n"
                f.beginText()
                f.setFont(mono, 7f)
                f.setNonStrokingColor(MUTED)
                f.newLineAtOffset(margin + contentW - width(label, 7f, mono), 25f)
                f.showText(label)
                f.endText()
            }
        }
    }

    private fun bytes(b: Long): String = when {
        b < 1024 -> "$b B"
        b < 1024 * 1024 -> "${(b / 102.4).toLong() / 10.0} KB"
        b < 1024L * 1024 * 1024 -> "${(b / (1024 * 102.4)).toLong() / 10.0} MB"
        else -> "${(b / (1024.0 * 1024 * 102.4)).toLong() / 10.0} GB"
    }
}
