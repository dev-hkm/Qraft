package com.hkm.qraft.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.hkm.qraft.model.QrDesignSpec
import kotlin.math.roundToInt

enum class PrintSheetLayout(
    val label: String,
    val cols: Int,
    val rows: Int,
    val description: String
) {
    TABLE_TENTS_2X2("2×2 Cards", 2, 2, "4 large cards per A4 page"),
    STICKERS_3X4("3×4 Stickers", 3, 4, "12 labels per A4 page"),
    GIFT_TAGS_2X5("2×5 Tags", 2, 5, "10 compact tags per A4 page")
}

data class PrintSheetItem(
    val title: String,
    val payload: String,
    val design: QrDesignSpec
)

object BatchSequenceExpander {

    fun expandSequence(
        prefix: String,
        suffix: String,
        start: Int,
        end: Int,
        zeroPad: Int = 1
    ): List<String> {
        val pad = zeroPad.coerceIn(1, 6)
        val step = if (end >= start) 1 else -1
        val count = kotlin.math.abs(end - start) + 1
        val clampedCount = count.coerceAtMost(120)
        val results = ArrayList<String>(clampedCount)
        var current = start
        repeat(clampedCount) {
            val numberStr = if (current >= 0) {
                current.toString().padStart(pad, '0')
            } else {
                "-" + (-current).toString().padStart(pad, '0')
            }
            results += "$prefix$numberStr$suffix"
            current += step
        }
        return results
    }

    fun parseImportedLines(rawText: String): List<String> {
        val rawLines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) return emptyList()

        val looksCsv = rawLines.any { it.contains(',') }
        val extracted = mutableListOf<String>()
        rawLines.forEachIndexed { index, line ->
            val token = if (looksCsv) {
                extractFirstCsvColumn(line)
            } else {
                line
            }.trim()
            if (index == 0 && isLikelyHeader(token)) return@forEachIndexed
            if (token.isNotEmpty()) extracted += token
        }
        return extracted.take(200)
    }

    private fun extractFirstCsvColumn(line: String): String {
        if (line.startsWith("\"")) {
            val closing = line.indexOf('"', startIndex = 1)
            if (closing > 1) return line.substring(1, closing)
        }
        return line.substringBefore(',').trim().trim('"')
    }

    private fun isLikelyHeader(token: String): Boolean {
        val lower = token.lowercase()
        return lower in setOf("url", "payload", "link", "qr", "text", "data", "code", "value")
    }
}

/**
 * Composes 300 DPI A4 printable sheets (2480 × 3508) containing tiled single QRs or heterogeneous
 * multi-QR print jobs with subtle cut guides and crisp labels.
 */
object PrintSheetEngine {

    fun tileSingleQr(
        title: String,
        payload: String,
        design: QrDesignSpec,
        layout: PrintSheetLayout
    ): List<PrintSheetItem> {
        val count = layout.cols * layout.rows
        val item = PrintSheetItem(
            title = title.ifBlank { "Qraft QR" },
            payload = payload,
            design = design
        )
        return List(count) { item }
    }

    fun renderPrintSheetPdfBytes(
        context: Context,
        items: List<PrintSheetItem>,
        layout: PrintSheetLayout,
        sheetTitle: String = "Qraft Print Sheet"
    ): ByteArray {
        val perPage = (layout.cols * layout.rows).coerceAtLeast(1)
        val totalPages = ((items.size + perPage - 1) / perPage).coerceAtLeast(1)
        return QrPdfWriter.writePages(totalPages) { pageIndex ->
            renderPrintSheetPageBitmap(
                context = context,
                items = items,
                layout = layout,
                pageIndex = pageIndex,
                totalPages = totalPages,
                sheetTitle = sheetTitle
            )
        }
    }

    fun renderPrintSheetPageBitmap(
        context: Context,
        items: List<PrintSheetItem>,
        layout: PrintSheetLayout,
        pageIndex: Int,
        totalPages: Int,
        sheetTitle: String = "Qraft Print Sheet",
        showCutGuides: Boolean = true
    ): Bitmap {
        val pageW = QrPdfWriter.A4_WIDTH_PX
        val pageH = QrPdfWriter.A4_HEIGHT_PX
        val bitmap = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFFFFFFFF.toInt())

        val sideMargin = 110f
        val topHeaderH = 195f
        val bottomFooterH = 110f
        val gridW = pageW - sideMargin * 2f
        val gridH = pageH - topHeaderH - bottomFooterH
        val cellGap = 36f
        val cellW = (gridW - cellGap * (layout.cols - 1)) / layout.cols
        val cellH = (gridH - cellGap * (layout.rows - 1)) / layout.rows

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E1D1B.toInt()
            textSize = 48f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF7B736C.toInt()
            textSize = 32f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(sheetTitle.ifBlank { "Qraft Print Sheet" }.take(38), sideMargin, 124f, headerPaint)
        canvas.drawText("${layout.label} • Page ${pageIndex + 1}/$totalPages", pageW - sideMargin, 124f, metaPaint)

        val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFD9D1C7.toInt()
            strokeWidth = 2.5f
            pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
        }
        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF231F1B.toInt()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val perPage = layout.cols * layout.rows
        val startIndex = pageIndex * perPage

        for (slot in 0 until perPage) {
            val itemIndex = startIndex + slot
            if (itemIndex >= items.size) break
            val item = items[itemIndex]
            val col = slot % layout.cols
            val row = slot / layout.cols

            val cellLeft = sideMargin + col * (cellW + cellGap)
            val cellTop = topHeaderH + row * (cellH + cellGap)
            val cellRect = RectF(cellLeft, cellTop, cellLeft + cellW, cellTop + cellH)

            if (showCutGuides) {
                canvas.drawRoundRect(cellRect, 26f, 26f, guidePaint)
            }

            val captionZone = minOf(84f, cellH * 0.14f)
            val innerPad = minOf(cellW, cellH) * 0.08f
            val availW = cellW - innerPad * 2f
            val availH = cellH - innerPad * 2f - captionZone
            val qrRenderTarget = minOf(availW, availH).roundToInt().coerceIn(220, 1200)

            val qrBmp = QrEngine.renderFramedExportBitmap(
                context = context,
                data = item.payload.ifBlank { "https://qraft.studio" },
                design = item.design,
                targetWidthPx = qrRenderTarget,
                forceOpaqueWhiteForJpeg = false
            )
            try {
                val scale = minOf(availW / qrBmp.width.toFloat(), availH / qrBmp.height.toFloat())
                val drawW = qrBmp.width * scale
                val drawH = qrBmp.height * scale
                val drawLeft = cellLeft + (cellW - drawW) / 2f
                val drawTop = cellTop + innerPad + (availH - drawH) / 2f
                canvas.drawBitmap(qrBmp, null, RectF(drawLeft, drawTop, drawLeft + drawW, drawTop + drawH), imgPaint)
            } finally {
                qrBmp.recycle()
            }

            val label = item.title.ifBlank { "QR #${itemIndex + 1}" }
            var textSize = (captionZone * 0.42f).coerceIn(22f, 38f)
            captionPaint.textSize = textSize
            val maxTextW = cellW * 0.86f
            while (textSize > 18f && captionPaint.measureText(label) > maxTextW) {
                textSize *= 0.9f
                captionPaint.textSize = textSize
            }
            val labelY = cellTop + cellH - innerPad * 0.6f
            canvas.drawText(label, cellRect.centerX(), labelY, captionPaint)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF9C948C.toInt()
            textAlign = Paint.Align.CENTER
            textSize = 28f
        }
        canvas.drawText("Generated at 300 DPI by Qraft QR Studio", pageW / 2f, pageH - 48f, footerPaint)
        return bitmap
    }
}
