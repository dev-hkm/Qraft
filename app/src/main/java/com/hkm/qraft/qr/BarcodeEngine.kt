package com.hkm.qraft.qr

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.hkm.qraft.model.BarcodeDesignSpec
import com.hkm.qraft.model.BarcodeFormatType
import com.hkm.qraft.model.ScannabilityReport
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class BarcodeValidationResult(
    val isValid: Boolean,
    val normalizedData: String,
    val warningOrError: String? = null
)

object BarcodeEngine {

    fun toZxingFormat(type: BarcodeFormatType): BarcodeFormat = when (type) {
        BarcodeFormatType.CODE_128 -> BarcodeFormat.CODE_128
        BarcodeFormatType.EAN_13 -> BarcodeFormat.EAN_13
        BarcodeFormatType.EAN_8 -> BarcodeFormat.EAN_8
        BarcodeFormatType.UPC_A -> BarcodeFormat.UPC_A
        BarcodeFormatType.CODE_39 -> BarcodeFormat.CODE_39
        BarcodeFormatType.ITF -> BarcodeFormat.ITF
        BarcodeFormatType.CODABAR -> BarcodeFormat.CODABAR
        BarcodeFormatType.PDF_417 -> BarcodeFormat.PDF_417
        BarcodeFormatType.AZTEC -> BarcodeFormat.AZTEC
        BarcodeFormatType.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
    }

    fun normalizeAndValidate(format: BarcodeFormatType, rawInput: String): BarcodeValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return BarcodeValidationResult(false, format.defaultSample, "Enter content for ${format.label}")
        }
        return when (format) {
            BarcodeFormatType.EAN_13 -> {
                val digits = trimmed.filter { it.isDigit() }
                when (digits.length) {
                    12 -> BarcodeValidationResult(true, digits + gs1CheckDigit(digits))
                    13 -> {
                        val expected = gs1CheckDigit(digits.take(12))
                        if (digits[12] == expected) {
                            BarcodeValidationResult(true, digits)
                        } else {
                            BarcodeValidationResult(false, digits, "Checksum digit should be $expected (entered ${digits[12]})")
                        }
                    }
                    else -> BarcodeValidationResult(false, digits, "EAN-13 requires 12 or 13 digits (${digits.length} entered)")
                }
            }
            BarcodeFormatType.EAN_8 -> {
                val digits = trimmed.filter { it.isDigit() }
                when (digits.length) {
                    7 -> BarcodeValidationResult(true, digits + gs1CheckDigit(digits))
                    8 -> {
                        val expected = gs1CheckDigit(digits.take(7))
                        if (digits[7] == expected) {
                            BarcodeValidationResult(true, digits)
                        } else {
                            BarcodeValidationResult(false, digits, "Checksum digit should be $expected (entered ${digits[7]})")
                        }
                    }
                    else -> BarcodeValidationResult(false, digits, "EAN-8 requires 7 or 8 digits (${digits.length} entered)")
                }
            }
            BarcodeFormatType.UPC_A -> {
                val digits = trimmed.filter { it.isDigit() }
                when (digits.length) {
                    11 -> BarcodeValidationResult(true, digits + gs1CheckDigit(digits))
                    12 -> {
                        val expected = gs1CheckDigit(digits.take(11))
                        if (digits[11] == expected) {
                            BarcodeValidationResult(true, digits)
                        } else {
                            BarcodeValidationResult(false, digits, "UPC-A check digit should be $expected (entered ${digits[11]})")
                        }
                    }
                    else -> BarcodeValidationResult(false, digits, "UPC-A requires 11 or 12 digits (${digits.length} entered)")
                }
            }
            BarcodeFormatType.ITF -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.isEmpty() || digits.length % 2 != 0) {
                    BarcodeValidationResult(false, digits, "ITF requires an even number of digits (${digits.length})")
                } else {
                    BarcodeValidationResult(true, digits)
                }
            }
            BarcodeFormatType.CODE_39 -> {
                val upper = trimmed.uppercase()
                val allowed = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
                val invalid = upper.firstOrNull { it !in allowed }
                if (invalid != null) {
                    BarcodeValidationResult(false, upper, "Code 39 does not support character '$invalid'")
                } else {
                    BarcodeValidationResult(true, upper)
                }
            }
            BarcodeFormatType.CODABAR -> {
                val clean = trimmed.uppercase()
                val core = clean.trimStart('A', 'B', 'C', 'D').trimEnd('A', 'B', 'C', 'D')
                val allowedBody = "0123456789-$:/.+"
                if (core.isEmpty() || core.any { it !in allowedBody }) {
                    BarcodeValidationResult(false, clean, "Codabar supports 0-9 and - $ : / . + (with optional A-D guard)")
                } else {
                    BarcodeValidationResult(true, clean)
                }
            }
            BarcodeFormatType.CODE_128,
            BarcodeFormatType.PDF_417,
            BarcodeFormatType.AZTEC,
            BarcodeFormatType.DATA_MATRIX -> {
                BarcodeValidationResult(true, trimmed)
            }
        }
    }

    /**
     * Standard GS1 Mod-10 check digit weighted 3,1,3,1 from rightmost digit.
     */
    fun gs1CheckDigit(body: String): Char {
        var sum = 0
        for (i in body.indices) {
            val weight = if ((body.length - 1 - i) % 2 == 0) 3 else 1
            sum += (body[i] - '0') * weight
        }
        val mod = sum % 10
        return ('0' + ((10 - mod) % 10))
    }

    fun encodeMatrix(spec: BarcodeDesignSpec): Pair<BitMatrix?, BarcodeValidationResult> {
        val validation = normalizeAndValidate(spec.format, spec.data)
        val payload = if (validation.isValid) validation.normalizedData else spec.format.defaultSample
        return runCatching {
            val hints = mapOf(EncodeHintType.MARGIN to 0)
            val matrix = MultiFormatWriter().encode(
                payload,
                toZxingFormat(spec.format),
                1,
                1,
                hints
            )
            matrix to validation
        }.getOrElse { err ->
            val fallback = runCatching {
                MultiFormatWriter().encode(
                    spec.format.defaultSample,
                    toZxingFormat(spec.format),
                    1,
                    1,
                    mapOf(EncodeHintType.MARGIN to 0)
                )
            }.getOrNull()
            fallback to BarcodeValidationResult(
                isValid = false,
                normalizedData = validation.normalizedData,
                warningOrError = validation.warningOrError ?: err.localizedMessage ?: "Invalid barcode input"
            )
        }
    }

    fun renderBarcodeBitmap(
        spec: BarcodeDesignSpec,
        targetWidthPx: Int = spec.exportWidthPx,
        forceOpaqueWhiteForJpeg: Boolean = false
    ): Bitmap {
        var attemptW = targetWidthPx.coerceIn(512, 8192)
        var lastOom: OutOfMemoryError? = null
        while (attemptW >= 512) {
            try {
                return renderBarcodeBitmapExact(spec, attemptW, forceOpaqueWhiteForJpeg)
            } catch (oom: OutOfMemoryError) {
                lastOom = oom
                System.gc()
                if (attemptW <= 512) break
                attemptW = (attemptW / 2).coerceAtLeast(512)
            }
        }
        throw lastOom ?: OutOfMemoryError("Unable to allocate barcode bitmap")
    }

    private fun renderBarcodeBitmapExact(
        spec: BarcodeDesignSpec,
        w: Int,
        forceOpaqueWhiteForJpeg: Boolean
    ): Bitmap {
        val (matrixOrNull, validation) = encodeMatrix(spec)
        val isSquare2D = spec.format == BarcodeFormatType.AZTEC || spec.format == BarcodeFormatType.DATA_MATRIX
        val h = when {
            isSquare2D -> if (spec.framed || spec.caption.isNotBlank() || spec.showText) (w * 1.12f).roundToInt() else w
            spec.format == BarcodeFormatType.PDF_417 -> (w * 0.56f).roundToInt()
            else -> (w * (if (spec.framed) 0.56f else 0.46f)).roundToInt()
        }.coerceAtLeast(360)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        if (forceOpaqueWhiteForJpeg || !spec.transparentBg) {
            val bg = if (spec.framed) spec.frameFillColor.toInt() else spec.backgroundColor.toInt()
            canvas.drawColor(bg)
        }

        val outerPad = if (spec.framed) w * 0.045f else 0f
        val cardRect = RectF(outerPad, outerPad, w - outerPad, h - outerPad)
        val cornerR = (spec.cornerRadiusDp / 360f) * w * 0.5f

        if (spec.framed) {
            canvas.drawRoundRect(
                cardRect,
                cornerR,
                cornerR,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = spec.frameFillColor.toInt() }
            )
            canvas.drawRoundRect(
                cardRect,
                cornerR,
                cornerR,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = max(w * 0.0045f, 2f)
                    color = spec.frameAccentColor.toInt()
                    alpha = 60
                }
            )
        }

        val innerPad = outerPad + w * spec.quietMarginFraction.coerceIn(0.03f, 0.14f)
        val topCaptionSpace = if (spec.caption.isNotBlank()) h * 0.13f else 0f
        val bottomTextSpace = if (spec.showText) h * 0.16f else 0f

        if (spec.caption.isNotBlank()) {
            val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (spec.framed) spec.frameAccentColor.toInt() else spec.foregroundColor.toInt()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                letterSpacing = 0.08f
                textSize = (w * 0.036f).coerceIn(14f, w * 0.055f)
            }
            val fm = capPaint.fontMetrics
            canvas.drawText(
                spec.caption.trim(),
                w / 2f,
                outerPad + topCaptionSpace * 0.68f - (fm.ascent + fm.descent) / 2f,
                capPaint
            )
        }

        val contentLeft = innerPad
        val contentRight = w - innerPad
        val contentTop = outerPad + w * 0.05f + topCaptionSpace
        val contentBottom = (h - outerPad - w * 0.05f - bottomTextSpace).coerceAtLeast(contentTop + 60f)
        val contentW = contentRight - contentLeft
        val contentH = contentBottom - contentTop

        if (!spec.transparentBg) {
            val coreBgRect = RectF(
                contentLeft - w * 0.02f,
                contentTop - h * 0.02f,
                contentRight + w * 0.02f,
                contentBottom + h * 0.02f
            )
            canvas.drawRoundRect(
                coreBgRect,
                cornerR * 0.45f,
                cornerR * 0.45f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = spec.backgroundColor.toInt() }
            )
        }

        val matrix = matrixOrNull
        if (matrix != null) {
            val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = spec.foregroundColor.toInt()
            }
            val cols = matrix.width
            val rows = matrix.height
            if (!spec.format.is2D) {
                val barW = contentW / cols.toFloat()
                val midRow = rows / 2
                var x = 0
                while (x < cols) {
                    if (matrix.get(x, midRow)) {
                        var runEnd = x + 1
                        while (runEnd < cols && matrix.get(runEnd, midRow)) runEnd++
                        val left = contentLeft + x * barW
                        val right = contentLeft + runEnd * barW
                        if (spec.roundedBars) {
                            val r = min((right - left) * 0.48f, contentH * 0.08f)
                            canvas.drawRoundRect(RectF(left, contentTop, right, contentBottom), r, r, fgPaint)
                        } else {
                            canvas.drawRect(left, contentTop, right, contentBottom, fgPaint)
                        }
                        x = runEnd
                    } else {
                        x++
                    }
                }
            } else {
                val boxSize = if (isSquare2D) min(contentW, contentH) else contentW
                val startX = contentLeft + (contentW - boxSize) / 2f
                val boxH = if (isSquare2D) boxSize else contentH
                val startY = contentTop + (contentH - boxH) / 2f
                val cellW = boxSize / cols.toFloat()
                val cellH = boxH / rows.toFloat()
                val r = if (spec.roundedBars) min(cellW, cellH) * 0.34f else 0f
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        if (matrix.get(col, row)) {
                            val lx = startX + col * cellW
                            val ty = startY + row * cellH
                            if (r > 0.5f) {
                                canvas.drawRoundRect(
                                    RectF(lx, ty, lx + cellW, ty + cellH),
                                    r, r, fgPaint
                                )
                            } else {
                                canvas.drawRect(lx, ty, lx + cellW + 0.3f, ty + cellH + 0.3f, fgPaint)
                            }
                        }
                    }
                }
            }
        }

        if (spec.showText) {
            val displayLabel = if (validation.isValid) validation.normalizedData else spec.data.trim().ifBlank { spec.format.defaultSample }
            val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = spec.foregroundColor.toInt()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                letterSpacing = 0.10f
            }
            var sizePx = w * 0.042f
            val maxW = contentW * 0.94f
            txtPaint.textSize = sizePx
            while (txtPaint.measureText(displayLabel) > maxW && sizePx > w * 0.020f) {
                sizePx *= 0.9f
                txtPaint.textSize = sizePx
            }
            val fm = txtPaint.fontMetrics
            val ty = (contentBottom + (h - outerPad - contentBottom) * 0.54f) - (fm.ascent + fm.descent) / 2f
            canvas.drawText(displayLabel, w / 2f, ty, txtPaint)
        }

        return bitmap
    }

    fun exportBarcodeSvg(spec: BarcodeDesignSpec): String {
        val (matrix, validation) = encodeMatrix(spec)
        val isSquare2D = spec.format == BarcodeFormatType.AZTEC || spec.format == BarcodeFormatType.DATA_MATRIX
        val vw = spec.exportWidthPx.coerceIn(512, 8192).toFloat()
        val vh = when {
            isSquare2D -> if (spec.framed || spec.caption.isNotBlank() || spec.showText) vw * 1.12f else vw
            spec.format == BarcodeFormatType.PDF_417 -> vw * 0.56f
            else -> vw * (if (spec.framed) 0.56f else 0.46f)
        }
        val fgHex = toHex(spec.foregroundColor)
        val bgHex = toHex(spec.backgroundColor)
        val frameHex = toHex(spec.frameFillColor)
        val accentHex = toHex(spec.frameAccentColor)

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${vw.roundToInt()} ${vh.roundToInt()}" width="${vw.roundToInt()}" height="${vh.roundToInt()}">""")

        val outerPad = if (spec.framed) vw * 0.045f else 0f
        val cornerR = (spec.cornerRadiusDp / 360f) * vw * 0.5f
        if (spec.framed) {
            sb.appendLine("""  <rect x="$outerPad" y="$outerPad" width="${vw - outerPad * 2}" height="${vh - outerPad * 2}" rx="$cornerR" fill="$frameHex" stroke="$accentHex" stroke-opacity="0.25" stroke-width="${max(vw * 0.004f, 2f)}"/>""")
        } else if (!spec.transparentBg) {
            sb.appendLine("""  <rect x="0" y="0" width="$vw" height="$vh" fill="$bgHex"/>""")
        }

        val innerPad = outerPad + vw * spec.quietMarginFraction.coerceIn(0.03f, 0.14f)
        val topCaptionSpace = if (spec.caption.isNotBlank()) vh * 0.13f else 0f
        val bottomTextSpace = if (spec.showText) vh * 0.16f else 0f
        val cLeft = innerPad
        val cRight = vw - innerPad
        val cTop = outerPad + vw * 0.05f + topCaptionSpace
        val cBottom = (vh - outerPad - vw * 0.05f - bottomTextSpace).coerceAtLeast(cTop + 60f)
        val cW = cRight - cLeft
        val cH = cBottom - cTop

        if (matrix != null) {
            val cols = matrix.width
            val rows = matrix.height
            val pathData = StringBuilder()
            if (!spec.format.is2D) {
                val barW = cW / cols.toFloat()
                val mid = rows / 2
                var x = 0
                while (x < cols) {
                    if (matrix.get(x, mid)) {
                        var end = x + 1
                        while (end < cols && matrix.get(end, mid)) end++
                        val bx = cLeft + x * barW
                        val bw = (end - x) * barW
                        pathData.append("M${bx},${cTop}h${bw}v${cH}h${-bw}Z ")
                        x = end
                    } else {
                        x++
                    }
                }
            } else {
                val boxSize = if (isSquare2D) min(cW, cH) else cW
                val startX = cLeft + (cW - boxSize) / 2f
                val boxH = if (isSquare2D) boxSize else cH
                val startY = cTop + (cH - boxH) / 2f
                val cellW = boxSize / cols.toFloat()
                val cellH = boxH / rows.toFloat()
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (matrix.get(c, r)) {
                            val mx = startX + c * cellW
                            val my = startY + r * cellH
                            pathData.append("M${mx},${my}h${cellW}v${cellH}h${-cellW}Z ")
                        }
                    }
                }
            }
            sb.appendLine("""  <path d="$pathData" fill="$fgHex"/>""")
        }

        if (spec.caption.isNotBlank()) {
            val safeCap = escape(spec.caption.trim())
            sb.appendLine("""  <text x="${vw / 2f}" y="${outerPad + topCaptionSpace * 0.65f}" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="${vw * 0.036f}" fill="$accentHex">$safeCap</text>""")
        }
        if (spec.showText) {
            val txt = escape(if (validation.isValid) validation.normalizedData else spec.data.trim())
            val ty = cBottom + (vh - outerPad - cBottom) * 0.56f
            sb.appendLine("""  <text x="${vw / 2f}" y="$ty" text-anchor="middle" font-family="monospace" font-weight="bold" font-size="${vw * 0.038f}" letter-spacing="2" fill="$fgHex">$txt</text>""")
        }
        sb.appendLine("</svg>")
        return sb.toString()
    }

    fun verifyBarcodeScannable(spec: BarcodeDesignSpec): ScannabilityReport {
        val (matrix, validation) = encodeMatrix(spec)
        if (!validation.isValid) {
            return ScannabilityReport(
                isScannable = false,
                decodedPreview = null,
                matrixSize = matrix?.width ?: 0,
                effectiveEcc = spec.format.shortCode,
                contrastRatio = 21.0,
                warningMessage = validation.warningOrError ?: "Invalid ${spec.format.label} data."
            )
        }
        val bgRef = if (spec.transparentBg) 0xFFFFFFFFL else spec.backgroundColor
        val contrast = QrEngine.contrastRatio(spec.foregroundColor, bgRef)
        val rendered = runCatching {
            renderBarcodeBitmap(spec.copy(framed = false, showText = false, caption = ""), targetWidthPx = 720, forceOpaqueWhiteForJpeg = true)
        }.getOrNull() ?: return ScannabilityReport(
            isScannable = false,
            matrixSize = matrix?.width ?: 0,
            effectiveEcc = spec.format.shortCode,
            contrastRatio = contrast,
            warningMessage = "Barcode render failed."
        )

        val decoded = try {
            val w = rendered.width
            val h = rendered.height
            val pixels = IntArray(w * h)
            rendered.getPixels(pixels, 0, w, 0, 0, w, h)
            val source = RGBLuminanceSource(w, h, pixels)
            val hints = mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.POSSIBLE_FORMATS to listOf(toZxingFormat(spec.format))
            )
            val reader = MultiFormatReader()
            runCatching {
                reader.decode(BinaryBitmap(HybridBinarizer(source)), hints)
            }.getOrNull() ?: runCatching {
                reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), hints)
            }.getOrNull()
        } finally {
            rendered.recycle()
        }

        val lowContrast = contrast < 3.0
        val ok = decoded != null && !lowContrast
        return ScannabilityReport(
            isScannable = ok,
            decodedPreview = decoded?.text ?: validation.normalizedData,
            matrixSize = matrix?.width ?: 0,
            effectiveEcc = spec.format.shortCode,
            contrastRatio = contrast,
            warningMessage = when {
                lowContrast -> "Low bar-to-background contrast (${"%.1f".format(contrast)}:1). Darken bars or brighten background."
                decoded == null -> "Decoder verification unclear. Increase quiet zone or disable rounded bars."
                else -> null
            }
        )
    }

    private fun toHex(argb: Long): String = String.format("#%06X", (argb and 0x00FFFFFFL))

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
