package com.hkm.qraft.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.hkm.qraft.model.*
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.options.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

object QrEngine {

    fun effectiveEcc(design: QrDesignSpec): QrErrorCorrectionLevel {
        return when (design.ecc) {
            QrEccSetting.AUTO -> when {
                design.logo.sourceType != LogoSourceType.NONE -> QrErrorCorrectionLevel.High
                design.codeSilhouette != QrCodeSilhouette.SQUARE -> QrErrorCorrectionLevel.MediumHigh
                design.pixelStyle in setOf(
                    QrPixelStyle.STAR,
                    QrPixelStyle.HEART,
                    QrPixelStyle.CROSS
                ) -> QrErrorCorrectionLevel.MediumHigh
                else -> QrErrorCorrectionLevel.Medium
            }
            QrEccSetting.LOW -> QrErrorCorrectionLevel.Low
            QrEccSetting.MEDIUM -> QrErrorCorrectionLevel.Medium
            QrEccSetting.QUARTILE -> QrErrorCorrectionLevel.MediumHigh
            QrEccSetting.HIGH -> QrErrorCorrectionLevel.High
        }
    }

    fun effectiveZxingEcc(design: QrDesignSpec): ErrorCorrectionLevel {
        return when (effectiveEcc(design)) {
            QrErrorCorrectionLevel.Low -> ErrorCorrectionLevel.L
            QrErrorCorrectionLevel.Medium, QrErrorCorrectionLevel.Auto -> ErrorCorrectionLevel.M
            QrErrorCorrectionLevel.MediumHigh -> ErrorCorrectionLevel.Q
            QrErrorCorrectionLevel.High -> ErrorCorrectionLevel.H
        }
    }

    fun buildQrOptions(
        design: QrDesignSpec,
        logoPainter: Painter?
    ): QrOptions {
        val lightPixel = CustomQrShapes.lightPixelShape(design.lightPixelStyle)
        val shapes = QrShapes(
            code = CustomQrShapes.codeShape(design.codeSilhouette),
            darkPixel = CustomQrShapes.pixelShape(
                design.pixelStyle,
                design.pixelCornerRoundness,
                design.pixelSizeFactor
            ),
            lightPixel = lightPixel ?: object : QrPixelShape {
                override fun Path.path(
                    size: Float,
                    neighbors: io.github.alexzhirkevich.qrose.Neighbors
                ): Path = this
            },
            ball = CustomQrShapes.ballShape(design.eyeBallStyle, design.eyeBallRoundness),
            frame = CustomQrShapes.frameShape(design.eyeFrameStyle, design.eyeFrameRoundness),
            centralSymmetry = true
        )

        val darkBrush = buildQrBrush(design.darkBrush)
        val frameBrush = if (design.customEyeFrameColors) buildQrBrush(design.eyeFrameBrush) else darkBrush
        val ballBrush = if (design.customEyeBallColors) buildQrBrush(design.eyeBallBrush) else darkBrush
        val lightBrush = if (lightPixel != null) {
            QrBrush.solid(Color(design.lightPixelColor))
        } else {
            QrBrush.solid(Color.Transparent)
        }

        val colors = QrColors(
            dark = darkBrush,
            light = lightBrush,
            ball = ballBrush,
            frame = frameBrush
        )

        val logo = if (design.logo.sourceType != LogoSourceType.NONE && logoPainter != null) {
            val padding = when (design.logo.paddingStyle) {
                LogoPaddingStyle.NATURAL -> QrLogoPadding.Natural(design.logo.paddingFraction)
                LogoPaddingStyle.ACCURATE -> QrLogoPadding.Accurate(design.logo.paddingFraction)
                LogoPaddingStyle.NONE -> QrLogoPadding.Empty
            }
            QrLogo(
                painter = logoPainter,
                size = design.logo.sizeFraction.coerceIn(0.10f, 0.34f),
                padding = padding,
                shape = CustomQrShapes.logoShape(design.logo.clipShape)
            )
        } else {
            QrLogo()
        }

        // Photo and transparent backgrounds render cleanly with transparent QR background
        // so the outer container / exporter controls photo overlay scrim & checkerboard.
        val qrBackground = when (design.background.mode) {
            BackgroundMode.TRANSPARENT, BackgroundMode.PHOTO -> QrBackground()
            BackgroundMode.SOLID -> QrBackground(
                fill = SolidColor(Color(design.background.brush.primaryColor))
            )
            BackgroundMode.GRADIENT -> QrBackground(
                fill = buildComposeBrush(design.background.brush, 512f)
            )
        }

        return QrOptions(
            shapes = shapes,
            colors = colors,
            logo = logo,
            background = qrBackground,
            errorCorrectionLevel = effectiveEcc(design),
            fourEyed = design.fourthEyeEnabled
        )
    }

    fun buildQrBrush(config: BrushStyleConfig): QrBrush {
        return when (config.mode) {
            BrushMode.SOLID -> QrBrush.solid(Color(config.primaryColor))
            BrushMode.LINEAR_GRADIENT,
            BrushMode.RADIAL_GRADIENT,
            BrushMode.SWEEP_GRADIENT -> QrBrush.brush(QrBrushMode.Join) { size ->
                buildComposeBrush(config, size)
            }
        }
    }

    fun buildComposeBrush(config: BrushStyleConfig, size: Float): Brush {
        val stops = buildColorStops(config)
        return when (config.mode) {
            BrushMode.SOLID -> SolidColor(Color(config.primaryColor))
            BrushMode.LINEAR_GRADIENT -> {
                val angle = when (config.direction) {
                    GradientDirection.CUSTOM_ANGLE -> config.angleDegrees
                    else -> config.direction.angleDeg
                }
                val (start, end) = angleOffsets(size.coerceAtLeast(1f), angle)
                Brush.linearGradient(
                    colorStops = stops.toTypedArray(),
                    start = start,
                    end = end
                )
            }
            BrushMode.RADIAL_GRADIENT -> {
                val center = Offset(size / 2f, size / 2f)
                Brush.radialGradient(
                    colorStops = stops.toTypedArray(),
                    center = center,
                    radius = (size * 0.65f).coerceAtLeast(1f)
                )
            }
            BrushMode.SWEEP_GRADIENT -> {
                val sweepColors = if (config.tertiaryColor != null) {
                    listOf(
                        Color(config.primaryColor),
                        Color(config.secondaryColor),
                        Color(config.tertiaryColor),
                        Color(config.primaryColor)
                    )
                } else {
                    listOf(
                        Color(config.primaryColor),
                        Color(config.secondaryColor),
                        Color(config.primaryColor)
                    )
                }
                Brush.sweepGradient(
                    colors = sweepColors,
                    center = Offset(size / 2f, size / 2f)
                )
            }
        }
    }

    private fun buildColorStops(config: BrushStyleConfig): List<Pair<Float, Color>> {
        val c1 = Color(config.primaryColor)
        val c2 = Color(config.secondaryColor)
        val c3 = config.tertiaryColor?.let { Color(it) }
        return if (c3 != null) {
            listOf(0f to c1, 0.5f to c2, 1f to c3)
        } else {
            listOf(0f to c1, 1f to c2)
        }
    }

    private fun angleOffsets(size: Float, angleDeg: Float): Pair<Offset, Offset> {
        val rad = angleDeg * PI / 180.0
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        val half = size / 2f
        val start = Offset(half - dx * half, half - dy * half)
        val end = Offset(half + dx * half, half + dy * half)
        return start to end
    }

    fun renderQrCoreBitmap(
        context: Context,
        data: String,
        design: QrDesignSpec,
        sizePx: Int
    ): Bitmap {
        val safeData = data.ifBlank { "https://qraft.studio" }
        val logoBitmap = LogoCatalog.renderLogoBitmap(context, design.logo, outputPx = (sizePx * 0.36f).roundToInt().coerceAtLeast(120))
        val logoPainter = logoBitmap?.let { BitmapPainter(it.asImageBitmap()) }
        val options = buildQrOptions(design, logoPainter)

        val painter = QrCodePainter(data = safeData, options = options)

        val quietPx = (sizePx * design.quietZonePadding.coerceIn(0f, 0.24f)).roundToInt()
        val innerSize = (sizePx - quietPx * 2).coerceAtLeast(64)

        val qrAndroidBitmap = Bitmap.createBitmap(innerSize, innerSize, Bitmap.Config.ARGB_8888)
        val qrImageBitmap = qrAndroidBitmap.asImageBitmap()
        val composeCanvas = androidx.compose.ui.graphics.Canvas(qrImageBitmap)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = Size(innerSize.toFloat(), innerSize.toFloat())
        ) {
            with(painter) {
                draw(size)
            }
        }

        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(output)

        when (design.background.mode) {
            BackgroundMode.TRANSPARENT -> Unit
            BackgroundMode.SOLID -> {
                canvas.drawColor(design.background.brush.primaryColor.toInt())
            }
            BackgroundMode.GRADIENT -> {
                val bgAndroid = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                val bgImg = bgAndroid.asImageBitmap()
                val bgCompose = androidx.compose.ui.graphics.Canvas(bgImg)
                CanvasDrawScope().draw(
                    density = Density(1f),
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = bgCompose,
                    size = Size(sizePx.toFloat(), sizePx.toFloat())
                ) {
                    drawRect(brush = buildComposeBrush(design.background.brush, sizePx.toFloat()))
                }
                canvas.drawBitmap(bgImg.asAndroidBitmap(), 0f, 0f, null)
            }
            BackgroundMode.PHOTO -> {
                val photo = design.background.photoUri?.let {
                    LogoCatalog.loadSampledUri(context, Uri.parse(it), sizePx)
                }
                if (photo != null) {
                    val side = min(photo.width, photo.height)
                    val left = (photo.width - side) / 2
                    val top = (photo.height - side) / 2
                    val src = android.graphics.Rect(left, top, left + side, top + side)
                    val dst = android.graphics.Rect(0, 0, sizePx, sizePx)
                    canvas.drawBitmap(photo, src, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                } else {
                    canvas.drawColor(0xFFFFFFFF.toInt())
                }
                val scrimColor = Color(design.background.photoOverlayColor)
                    .copy(alpha = design.background.photoOverlayAlpha.coerceIn(0f, 0.95f))
                val scrimPaint = Paint().apply {
                    color = android.graphics.Color.argb(
                        (scrimColor.alpha * 255).roundToInt(),
                        (scrimColor.red * 255).roundToInt(),
                        (scrimColor.green * 255).roundToInt(),
                        (scrimColor.blue * 255).roundToInt()
                    )
                }
                canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), scrimPaint)
            }
        }

        canvas.drawBitmap(
            qrImageBitmap.asAndroidBitmap(),
            quietPx.toFloat(),
            quietPx.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        return output
    }

    fun renderFramedExportBitmap(
        context: Context,
        data: String,
        design: QrDesignSpec,
        targetWidthPx: Int = design.exportSizePx,
        forceOpaqueWhiteForJpeg: Boolean = false
    ): Bitmap {
        val frame = design.outerFrame
        if (frame.style == OuterFrameStyle.NONE) {
            val core = renderQrCoreBitmap(context, data, design, targetWidthPx)
            if (!forceOpaqueWhiteForJpeg || design.background.mode != BackgroundMode.TRANSPARENT) {
                return core
            }
            val opaque = Bitmap.createBitmap(targetWidthPx, targetWidthPx, Bitmap.Config.ARGB_8888)
            val c = AndroidCanvas(opaque)
            c.drawColor(0xFFFFFFFF.toInt())
            c.drawBitmap(core, 0f, 0f, null)
            return opaque
        }

        val w = targetWidthPx
        val extraBottom = when (frame.style) {
            OuterFrameStyle.BOTTOM_SCAN_BANNER,
            OuterFrameStyle.SPEECH_BUBBLE,
            OuterFrameStyle.POLAROID_STAMP,
            OuterFrameStyle.WARM_POSTER,
            OuterFrameStyle.DOUBLE_PILL -> (w * 0.19f).roundToInt()
            OuterFrameStyle.TOP_BADGE_CARD -> (w * 0.15f).roundToInt()
            else -> (w * 0.04f).roundToInt()
        }
        val extraTop = if (frame.style == OuterFrameStyle.TOP_BADGE_CARD || frame.style == OuterFrameStyle.DOUBLE_PILL) {
            (w * 0.12f).roundToInt()
        } else {
            0
        }

        val totalH = w + extraBottom + extraTop
        val bitmap = Bitmap.createBitmap(w, totalH, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)

        if (forceOpaqueWhiteForJpeg) {
            canvas.drawColor(0xFFFFFFFF.toInt())
        }

        val margin = w * 0.04f
        val cardRect = RectF(margin, margin, w - margin, totalH - margin)
        val cornerR = (frame.cornerRadiusDp / 360f) * w

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = frame.frameFillColor.toInt()
        }
        canvas.drawRoundRect(cardRect, cornerR, cornerR, cardPaint)

        if (frame.style == OuterFrameStyle.GRADIENT_BORDER) {
            val strokeW = max(w * 0.022f, 12f)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), totalH.toFloat(),
                    frame.accentColor.toInt(),
                    frame.accentSecondaryColor.toInt(),
                    Shader.TileMode.CLAMP
                )
            }
            val insetRect = RectF(
                cardRect.left + strokeW / 2f,
                cardRect.top + strokeW / 2f,
                cardRect.right - strokeW / 2f,
                cardRect.bottom - strokeW / 2f
            )
            canvas.drawRoundRect(insetRect, cornerR, cornerR, borderPaint)
        } else if (frame.borderThicknessDp > 0f) {
            val strokeW = (frame.borderThicknessDp / 360f) * w
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                color = frame.accentColor.toInt()
            }
            canvas.drawRoundRect(cardRect, cornerR, cornerR, borderPaint)
        }

        val qrPad = w * 0.085f
        val qrSize = (w - qrPad * 2f).roundToInt()
        val qrTop = (margin + extraTop + w * 0.045f).roundToInt()
        val qrCore = renderQrCoreBitmap(context, data, design, qrSize)
        val qrCorner = cornerR * 0.65f
        val qrSave = canvas.save()
        val qrClip = android.graphics.Path().apply {
            addRoundRect(
                RectF(qrPad, qrTop.toFloat(), qrPad + qrSize, qrTop + qrSize.toFloat()),
                qrCorner,
                qrCorner,
                android.graphics.Path.Direction.CW
            )
        }
        canvas.clipPath(qrClip)
        canvas.drawBitmap(qrCore, qrPad, qrTop.toFloat(), null)
        canvas.restoreToCount(qrSave)

        val label = frame.label.trim().ifBlank { "SCAN ME" }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = frame.textColor.toInt()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = w * 0.056f
            letterSpacing = 0.08f
        }

        when (frame.style) {
            OuterFrameStyle.BOTTOM_SCAN_BANNER,
            OuterFrameStyle.SPEECH_BUBBLE,
            OuterFrameStyle.WARM_POSTER,
            OuterFrameStyle.DOUBLE_PILL -> {
                val pillW = w * 0.56f
                val pillH = w * 0.115f
                val pillLeft = (w - pillW) / 2f
                val pillTop = qrTop + qrSize + w * 0.045f
                val pillRect = RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH)
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = frame.accentColor.toInt()
                }
                canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillPaint)
                val fm = textPaint.fontMetrics
                val baseline = pillRect.centerY() - (fm.ascent + fm.descent) / 2f
                canvas.drawText(label, pillRect.centerX(), baseline, textPaint)
            }
            OuterFrameStyle.TOP_BADGE_CARD -> {
                val pillW = w * 0.52f
                val pillH = w * 0.105f
                val pillLeft = (w - pillW) / 2f
                val pillTop = margin + w * 0.04f
                val pillRect = RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH)
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = frame.accentColor.toInt()
                }
                canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillPaint)
                val fm = textPaint.fontMetrics
                val baseline = pillRect.centerY() - (fm.ascent + fm.descent) / 2f
                canvas.drawText(label, pillRect.centerX(), baseline, textPaint)
            }
            OuterFrameStyle.POLAROID_STAMP -> {
                val stampPaint = Paint(textPaint).apply {
                    color = frame.accentColor.toInt()
                }
                val y = qrTop + qrSize + w * 0.115f
                canvas.drawText(label, w / 2f, y, stampPaint)
            }
            else -> Unit
        }

        return bitmap
    }

    fun verifyScannability(
        context: Context,
        data: String,
        design: QrDesignSpec
    ): ScannabilityReport {
        val safeData = data.ifBlank { "https://qraft.studio" }
        val zxingEcc = effectiveZxingEcc(design)
        val matrixSize = runCatching {
            Encoder.encode(safeData, zxingEcc).matrix.width
        }.getOrDefault(25)

        val bgForContrast = when (design.background.mode) {
            BackgroundMode.TRANSPARENT -> 0xFFFFFFFFL
            BackgroundMode.PHOTO -> design.background.photoOverlayColor
            else -> design.background.brush.primaryColor
        }
        val contrast = contrastRatio(design.darkBrush.primaryColor, bgForContrast)

        // Render clean core bitmap at 512px (with white backing if transparent) and test real ZXing decode
        val testDesign = if (design.background.mode == BackgroundMode.TRANSPARENT) {
            design.copy(
                background = design.background.copy(
                    mode = BackgroundMode.SOLID,
                    brush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFFFFFFFF)
                )
            )
        } else {
            design
        }

        val bitmap = renderQrCoreBitmap(context, safeData, testDesign, 512)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val reader = QRCodeReader()
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )

        val decoded = runCatching {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        }.getOrNull() ?: runCatching {
            reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), hints).text
        }.getOrNull()

        val isDecodedOk = decoded == safeData
        val warning = when {
            !isDecodedOk && contrast < 3.0 ->
                "Low color contrast (${"%.1f".format(contrast)}:1). Darken pixels or brighten background."
            !isDecodedOk && design.logo.sourceType != LogoSourceType.NONE && design.logo.sizeFraction > 0.25f ->
                "Logo may cover critical data. Set ECC to High or shrink logo below 24%."
            !isDecodedOk ->
                "Stylized modules reduced scan margin. Try thicker pixels or larger quiet zone."
            contrast < 3.2 ->
                "Scannable, though higher contrast (${"%.1f".format(contrast)}:1) improves speed in dim light."
            else -> null
        }

        return ScannabilityReport(
            isScannable = isDecodedOk,
            decodedPreview = decoded,
            matrixSize = matrixSize,
            effectiveEcc = zxingEcc.name,
            contrastRatio = contrast,
            warningMessage = warning
        )
    }

    fun exportSvg(data: String, design: QrDesignSpec): String {
        val safeData = data.ifBlank { "https://qraft.studio" }
        val qr = Encoder.encode(safeData, effectiveZxingEcc(design))
        val matrix = qr.matrix
        val count = matrix.width
        val cell = 20f
        val qrSize = count * cell
        val quiet = qrSize * design.quietZonePadding.coerceIn(0f, 0.24f)
        val totalQr = qrSize + quiet * 2f

        val hasBanner = design.outerFrame.style != OuterFrameStyle.NONE &&
            design.outerFrame.style != OuterFrameStyle.CLEAN_CARD &&
            design.outerFrame.style != OuterFrameStyle.GRADIENT_BORDER
        val extraH = if (hasBanner) totalQr * 0.18f else 0f
        val totalW = totalQr
        val totalH = totalQr + extraH

        val darkHex1 = toSvgHex(design.darkBrush.primaryColor)
        val darkHex2 = toSvgHex(design.darkBrush.secondaryColor)
        val bgHex1 = toSvgHex(design.background.brush.primaryColor)
        val bgHex2 = toSvgHex(design.background.brush.secondaryColor)

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine(
            """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${fmt(totalW)} ${fmt(totalH)}" width="1024" height="${(1024f * totalH / totalW).roundToInt()}">"""
        )
        sb.appendLine("  <defs>")
        if (design.darkBrush.mode != BrushMode.SOLID) {
            if (design.darkBrush.mode == BrushMode.RADIAL_GRADIENT) {
                sb.appendLine("""    <radialGradient id="qrDarkFill" cx="50%" cy="50%" r="60%">""")
                sb.appendLine("""      <stop offset="0%" stop-color="$darkHex1"/>""")
                sb.appendLine("""      <stop offset="100%" stop-color="$darkHex2"/>""")
                sb.appendLine("""    </radialGradient>""")
            } else {
                sb.appendLine("""    <linearGradient id="qrDarkFill" x1="0%" y1="0%" x2="100%" y2="100%">""")
                sb.appendLine("""      <stop offset="0%" stop-color="$darkHex1"/>""")
                sb.appendLine("""      <stop offset="100%" stop-color="$darkHex2"/>""")
                sb.appendLine("""    </linearGradient>""")
            }
        }
        if (design.background.mode == BackgroundMode.GRADIENT) {
            sb.appendLine("""    <linearGradient id="qrBgFill" x1="0%" y1="0%" x2="100%" y2="100%">""")
            sb.appendLine("""      <stop offset="0%" stop-color="$bgHex1"/>""")
            sb.appendLine("""      <stop offset="100%" stop-color="$bgHex2"/>""")
            sb.appendLine("""    </linearGradient>""")
        }
        sb.appendLine("  </defs>")

        // Background
        when (design.background.mode) {
            BackgroundMode.TRANSPARENT -> Unit
            BackgroundMode.SOLID -> sb.appendLine(
                """  <rect width="${fmt(totalW)}" height="${fmt(totalH)}" rx="32" fill="$bgHex1"/>"""
            )
            BackgroundMode.GRADIENT -> sb.appendLine(
                """  <rect width="${fmt(totalW)}" height="${fmt(totalH)}" rx="32" fill="url(#qrBgFill)"/>"""
            )
            BackgroundMode.PHOTO -> sb.appendLine(
                """  <rect width="${fmt(totalW)}" height="${fmt(totalH)}" rx="32" fill="#FFFFFF"/>"""
            )
        }

        val fillRef = if (design.darkBrush.mode == BrushMode.SOLID) darkHex1 else "url(#qrDarkFill)"
        val eyeFrameFill = if (design.customEyeFrameColors) toSvgHex(design.eyeFrameBrush.primaryColor) else fillRef
        val eyeBallFill = if (design.customEyeBallColors) toSvgHex(design.eyeBallBrush.primaryColor) else fillRef

        // Finder pattern coordinates (7x7 corners)
        val finderOrigins = mutableListOf(0 to 0, (count - 7) to 0, 0 to (count - 7))
        if (design.fourthEyeEnabled) {
            finderOrigins.add((count - 7) to (count - 7))
        }

        fun isInsideFinder(x: Int, y: Int): Boolean {
            return finderOrigins.any { (fx, fy) -> x in fx until (fx + 7) && y in fy until (fy + 7) }
        }

        val factor = design.pixelSizeFactor.coerceIn(0.45f, 1.0f)
        val modSize = cell * factor
        val modPad = (cell - modSize) / 2f

        sb.appendLine("""  <g transform="translate(${fmt(quiet)}, ${fmt(quiet)})">""")
        for (y in 0 until count) {
            for (x in 0 until count) {
                if (matrix.get(x, y).toInt() != 1 || isInsideFinder(x, y)) continue
                val px = x * cell + modPad
                val py = y * cell + modPad
                when (design.pixelStyle) {
                    QrPixelStyle.CIRCLE -> {
                        val cx = x * cell + cell / 2f
                        val cy = y * cell + cell / 2f
                        sb.appendLine("""    <circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(modSize / 2f)}" fill="$fillRef"/>""")
                    }
                    QrPixelStyle.DIAMOND -> {
                        val cx = x * cell + cell / 2f
                        val cy = y * cell + cell / 2f
                        val r = modSize / 2f
                        sb.appendLine("""    <polygon points="${fmt(cx)},${fmt(cy - r)} ${fmt(cx + r)},${fmt(cy)} ${fmt(cx)},${fmt(cy + r)} ${fmt(cx - r)},${fmt(cy)}" fill="$fillRef"/>""")
                    }
                    else -> {
                        val rx = if (design.pixelStyle == QrPixelStyle.SQUARE) 0f else modSize * design.pixelCornerRoundness
                        sb.appendLine("""    <rect x="${fmt(px)}" y="${fmt(py)}" width="${fmt(modSize)}" height="${fmt(modSize)}" rx="${fmt(rx)}" fill="$fillRef"/>""")
                    }
                }
            }
        }

        // Draw crisp finder eyes
        val frameSize = 7f * cell
        val ballSize = 3f * cell
        for ((fx, fy) in finderOrigins) {
            val ox = fx * cell
            val oy = fy * cell
            val outerR = when (design.eyeFrameStyle) {
                QrEyeFrameStyle.CIRCLE -> frameSize / 2f
                QrEyeFrameStyle.SQUARE -> 0f
                else -> frameSize * design.eyeFrameRoundness.coerceIn(0f, 0.45f)
            }
            val strokeW = cell
            sb.appendLine(
                """    <rect x="${fmt(ox + strokeW / 2f)}" y="${fmt(oy + strokeW / 2f)}" width="${fmt(frameSize - strokeW)}" height="${fmt(frameSize - strokeW)}" rx="${fmt((outerR - strokeW / 2f).coerceAtLeast(0f))}" fill="none" stroke="$eyeFrameFill" stroke-width="${fmt(strokeW)}"/>"""
            )
            val ballR = when (design.eyeBallStyle) {
                QrEyeBallStyle.CIRCLE -> ballSize / 2f
                QrEyeBallStyle.SQUARE -> 0f
                else -> ballSize * design.eyeBallRoundness.coerceIn(0f, 0.45f)
            }
            sb.appendLine(
                """    <rect x="${fmt(ox + 2f * cell)}" y="${fmt(oy + 2f * cell)}" width="${fmt(ballSize)}" height="${fmt(ballSize)}" rx="${fmt(ballR)}" fill="$eyeBallFill"/>"""
            )
        }
        sb.appendLine("  </g>")

        if (hasBanner) {
            val pillW = totalW * 0.56f
            val pillH = totalW * 0.11f
            val pillX = (totalW - pillW) / 2f
            val pillY = totalQr + extraH * 0.18f
            val accentHex = toSvgHex(design.outerFrame.accentColor)
            val textHex = toSvgHex(design.outerFrame.textColor)
            val safeLabel = design.outerFrame.label.ifBlank { "SCAN ME" }
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            sb.appendLine("""  <rect x="${fmt(pillX)}" y="${fmt(pillY)}" width="${fmt(pillW)}" height="${fmt(pillH)}" rx="${fmt(pillH / 2f)}" fill="$accentHex"/>""")
            sb.appendLine("""  <text x="${fmt(totalW / 2f)}" y="${fmt(pillY + pillH * 0.66f)}" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="${fmt(pillH * 0.48f)}" fill="$textHex">$safeLabel</text>""")
        }

        sb.appendLine("</svg>")
        return sb.toString()
    }

    fun contrastRatio(argbA: Long, argbB: Long): Double {
        val lumA = relativeLuminance(argbA)
        val lumB = relativeLuminance(argbB)
        val bright = max(lumA, lumB)
        val dark = min(lumA, lumB)
        return (bright + 0.05) / (dark + 0.05)
    }

    private fun relativeLuminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFFL).toDouble() / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = channel(16)
        val g = channel(8)
        val b = channel(0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun toSvgHex(argb: Long): String =
        String.format("#%06X", (argb and 0xFFFFFFL))

    private fun fmt(value: Float): String =
        String.format(java.util.Locale.US, "%.2f", value)
}
