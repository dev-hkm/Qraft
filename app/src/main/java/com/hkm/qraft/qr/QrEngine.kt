package com.hkm.qraft.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
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
                    QrPixelStyle.CROSS,
                    QrPixelStyle.RING
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
        var attemptPx = sizePx.coerceIn(128, 8192)
        while (true) {
            try {
                return renderQrCoreBitmapExact(context, data, design, attemptPx)
            } catch (oom: OutOfMemoryError) {
                if (attemptPx <= 256) throw oom
                System.gc()
                attemptPx = (attemptPx / 2).coerceAtLeast(256)
            }
        }
    }

    private fun renderQrCoreBitmapExact(
        context: Context,
        data: String,
        design: QrDesignSpec,
        clampedPx: Int
    ): Bitmap {
        val safeData = data.ifBlank { "https://qraft.studio" }
        val logoBitmap = LogoCatalog.renderLogoBitmap(
            context,
            design.logo,
            outputPx = (clampedPx * 0.28f).roundToInt().coerceIn(120, 1024)
        )
        val logoPainter = logoBitmap?.let { BitmapPainter(it.asImageBitmap()) }
        val options = buildQrOptions(design, logoPainter)

        val painter = QrCodePainter(data = safeData, options = options)

        val quietPx = (clampedPx * design.quietZonePadding.coerceIn(0f, 0.24f)).roundToInt()
        val innerSize = (clampedPx - quietPx * 2).coerceAtLeast(64)

        val output = Bitmap.createBitmap(clampedPx, clampedPx, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(output)

        when (design.background.mode) {
            BackgroundMode.TRANSPARENT -> Unit
            BackgroundMode.SOLID -> {
                canvas.drawColor(design.background.brush.primaryColor.toInt())
            }
            BackgroundMode.GRADIENT -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    val shader = buildNativeShader(design.background.brush, clampedPx.toFloat(), clampedPx.toFloat())
                    if (shader != null) {
                        this.shader = shader
                    } else {
                        color = design.background.brush.primaryColor.toInt()
                    }
                }
                canvas.drawRect(0f, 0f, clampedPx.toFloat(), clampedPx.toFloat(), bgPaint)
            }
            BackgroundMode.PHOTO -> {
                val photo = design.background.photoUri?.let {
                    LogoCatalog.loadSampledUri(context, Uri.parse(it), clampedPx.coerceAtMost(2048))
                }
                if (photo != null) {
                    val side = min(photo.width, photo.height)
                    val left = (photo.width - side) / 2
                    val top = (photo.height - side) / 2
                    val src = android.graphics.Rect(left, top, left + side, top + side)
                    val dst = android.graphics.Rect(0, 0, clampedPx, clampedPx)
                    canvas.drawBitmap(photo, src, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    photo.recycle()
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
                canvas.drawRect(0f, 0f, clampedPx.toFloat(), clampedPx.toFloat(), scrimPaint)
            }
        }

        // Single-bitmap compose draw: avoids dual 268MB buffer allocation at 8K (8192px)
        val composeCanvas = androidx.compose.ui.graphics.Canvas(output.asImageBitmap())
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = Size(clampedPx.toFloat(), clampedPx.toFloat())
        ) {
            translate(left = quietPx.toFloat(), top = quietPx.toFloat()) {
                with(painter) {
                    draw(Size(innerSize.toFloat(), innerSize.toFloat()))
                }
            }
        }
        logoBitmap?.recycle()
        return output
    }

    private fun buildNativeShader(config: BrushStyleConfig, width: Float, height: Float): Shader? {
        val c1 = config.primaryColor.toInt()
        val c2 = config.secondaryColor.toInt()
        val c3 = config.tertiaryColor?.toInt()
        val colors = if (c3 != null) intArrayOf(c1, c2, c3) else intArrayOf(c1, c2)
        val positions = if (c3 != null) floatArrayOf(0f, 0.5f, 1f) else floatArrayOf(0f, 1f)
        val w = width.coerceAtLeast(1f)
        val h = height.coerceAtLeast(1f)
        val minDim = min(w, h)

        return when (config.mode) {
            BrushMode.SOLID -> null
            BrushMode.LINEAR_GRADIENT -> {
                val angle = when (config.direction) {
                    GradientDirection.CUSTOM_ANGLE -> config.angleDegrees
                    else -> config.direction.angleDeg
                }
                val (start, end) = angleOffsets(minDim, angle)
                val sx = (w - minDim) / 2f + start.x
                val sy = (h - minDim) / 2f + start.y
                val ex = (w - minDim) / 2f + end.x
                val ey = (h - minDim) / 2f + end.y
                LinearGradient(sx, sy, ex, ey, colors, positions, Shader.TileMode.CLAMP)
            }
            BrushMode.RADIAL_GRADIENT -> {
                RadialGradient(
                    w / 2f,
                    h / 2f,
                    (minDim * 0.65f).coerceAtLeast(1f),
                    colors,
                    positions,
                    Shader.TileMode.CLAMP
                )
            }
            BrushMode.SWEEP_GRADIENT -> {
                val sweepColors = if (c3 != null) intArrayOf(c1, c2, c3, c1) else intArrayOf(c1, c2, c1)
                SweepGradient(w / 2f, h / 2f, sweepColors, null)
            }
        }
    }

    fun renderFramedExportBitmap(
        context: Context,
        data: String,
        design: QrDesignSpec,
        targetWidthPx: Int = design.exportSizePx,
        forceOpaqueWhiteForJpeg: Boolean = false
    ): Bitmap {
        var attemptW = targetWidthPx.coerceIn(128, 8192)
        while (true) {
            try {
                return renderFramedExportBitmapExact(context, data, design, attemptW, forceOpaqueWhiteForJpeg)
            } catch (oom: OutOfMemoryError) {
                if (attemptW <= 256) throw oom
                System.gc()
                attemptW = (attemptW / 2).coerceAtLeast(256)
            }
        }
    }

    private fun renderFramedExportBitmapExact(
        context: Context,
        data: String,
        design: QrDesignSpec,
        safeW: Int,
        forceOpaqueWhiteForJpeg: Boolean
    ): Bitmap {
        val frame = design.outerFrame
        if (frame.style == OuterFrameStyle.NONE) {
            val core = renderQrCoreBitmap(context, data, design, safeW)
            if (!forceOpaqueWhiteForJpeg || design.background.mode != BackgroundMode.TRANSPARENT) {
                return core
            }
            val opaque = Bitmap.createBitmap(safeW, safeW, Bitmap.Config.ARGB_8888)
            val c = AndroidCanvas(opaque)
            c.drawColor(0xFFFFFFFF.toInt())
            c.drawBitmap(core, 0f, 0f, null)
            core.recycle()
            return opaque
        }

        val w = safeW
        val hasSubtitle = (frame.style == OuterFrameStyle.WARM_POSTER || frame.style == OuterFrameStyle.TICKET_STUB) &&
            frame.subtitle.isNotBlank()
        val extraBottom = when (frame.style) {
            OuterFrameStyle.BOTTOM_SCAN_BANNER,
            OuterFrameStyle.GLASS_CARD,
            OuterFrameStyle.CORNER_BRACKET,
            OuterFrameStyle.NEON_GLOW,
            OuterFrameStyle.CIRCULAR_STAMP -> (w * 0.19f).roundToInt()
            OuterFrameStyle.TICKET_STUB -> (w * (if (hasSubtitle) 0.24f else 0.20f)).roundToInt()
            OuterFrameStyle.MINIMAL_OUTLINE -> (w * 0.13f).roundToInt()
            OuterFrameStyle.SPEECH_BUBBLE -> (w * 0.25f).roundToInt()
            OuterFrameStyle.POLAROID_STAMP -> (w * 0.19f).roundToInt()
            OuterFrameStyle.WARM_POSTER -> (w * (if (hasSubtitle) 0.24f else 0.19f)).roundToInt()
            OuterFrameStyle.DOUBLE_PILL -> (w * 0.19f).roundToInt()
            OuterFrameStyle.TOP_BADGE_CARD,
            OuterFrameStyle.SCAN_BAR_HEADER -> (w * 0.13f).roundToInt()
            else -> (w * 0.045f).roundToInt()
        }
        val extraTop = when (frame.style) {
            OuterFrameStyle.TOP_BADGE_CARD,
            OuterFrameStyle.DOUBLE_PILL,
            OuterFrameStyle.SCAN_BAR_HEADER -> (w * 0.135f).roundToInt()
            else -> 0
        }

        val totalH = w + extraBottom + extraTop
        val bitmap = Bitmap.createBitmap(w, totalH, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)

        if (forceOpaqueWhiteForJpeg) {
            canvas.drawColor(0xFFFFFFFF.toInt())
        }

        val margin = w * 0.04f
        val tailHeight = if (frame.style == OuterFrameStyle.SPEECH_BUBBLE) w * 0.06f else 0f
        val cardBottom = totalH - margin - tailHeight
        val cardRect = RectF(margin, margin, w - margin, cardBottom)
        val cornerR = (frame.cornerRadiusDp / 360f) * w

        val cardPath = android.graphics.Path().apply {
            addRoundRect(cardRect, cornerR, cornerR, android.graphics.Path.Direction.CW)
        }

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (frame.style == OuterFrameStyle.GLASS_CARD) {
                (frame.frameFillColor.toInt() and 0x00FFFFFF) or (232 shl 24)
            } else {
                frame.frameFillColor.toInt()
            }
        }
        canvas.drawPath(cardPath, cardPaint)

        // Frame perimeter decorations
        when (frame.style) {
            OuterFrameStyle.GLASS_CARD -> {
                val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        cardRect.left, cardRect.top, cardRect.left, cardRect.bottom,
                        0x44FFFFFF, 0x05FFFFFF, Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(cardRect, cornerR, cornerR, sheenPaint)
                val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = max(w * 0.008f, 3f)
                    shader = LinearGradient(
                        cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                        0xCCFFFFFF.toInt(),
                        ((frame.accentColor and 0x00FFFFFFL) or 0x55000000L).toInt(),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(cardRect, cornerR, cornerR, rimPaint)
            }
            OuterFrameStyle.MINIMAL_OUTLINE -> {
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = max(w * 0.0045f, 2f)
                    color = frame.accentColor.toInt()
                }
                canvas.drawRoundRect(cardRect, cornerR, cornerR, linePaint)
            }
            OuterFrameStyle.NEON_GLOW -> {
                val glowStroke = max(w * 0.026f, 12f)
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = glowStroke
                    alpha = 64
                    color = frame.accentColor.toInt()
                }
                val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = max(w * 0.011f, 5f)
                    shader = LinearGradient(
                        cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                        frame.accentColor.toInt(), frame.accentSecondaryColor.toInt(), Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(cardRect, cornerR, cornerR, glowPaint)
                canvas.drawRoundRect(cardRect, cornerR, cornerR, innerBorder)
            }
            OuterFrameStyle.SCAN_BAR_HEADER -> {
                val barH = w * 0.145f
                val saveHdr = canvas.save()
                canvas.clipPath(cardPath)
                val hdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = frame.accentColor.toInt()
                }
                canvas.drawRect(cardRect.left, cardRect.top, cardRect.right, cardRect.top + barH, hdrPaint)
                val hdrText = frame.label.trim().ifBlank { "SCAN ME" }
                val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = frame.textColor.toInt()
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    letterSpacing = 0.09f
                }
                fitTextSize(txtPaint, hdrText, cardRect.width() * 0.78f, w * 0.048f, w * 0.024f)
                val fm = txtPaint.fontMetrics
                canvas.drawText(
                    hdrText,
                    cardRect.centerX(),
                    cardRect.top + barH / 2f - (fm.ascent + fm.descent) / 2f,
                    txtPaint
                )
                canvas.restoreToCount(saveHdr)
            }
            OuterFrameStyle.GRADIENT_BORDER -> {
                val strokeW = max(w * 0.022f, 12f)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    shader = LinearGradient(
                        0f, 0f, w.toFloat(), cardBottom,
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
            }
            else -> if (frame.borderThicknessDp > 0f) {
                val strokeW = (frame.borderThicknessDp / 360f) * w
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    color = frame.accentColor.toInt()
                }
                canvas.drawRoundRect(cardRect, cornerR, cornerR, borderPaint)
            }
        }

        if (frame.style == OuterFrameStyle.SPEECH_BUBBLE) {
            val tailHalfW = w * 0.065f
            val overlap = 3f
            val tailPath = android.graphics.Path().apply {
                moveTo(w / 2f - tailHalfW, cardBottom - overlap)
                lineTo(w / 2f + tailHalfW, cardBottom - overlap)
                lineTo(w / 2f, cardBottom + tailHeight)
                close()
            }
            canvas.drawPath(tailPath, cardPaint)
        }

        val qrPad = w * frame.effectivePaddingFraction()
        val qrSize = (w - qrPad * 2f).roundToInt().coerceAtLeast(64)
        val qrTop = (margin + extraTop + w * 0.045f).roundToInt()
        val qrCorner = if (frame.style == OuterFrameStyle.CIRCULAR_STAMP) qrSize / 2f else cornerR * 0.65f
        val qrRect = RectF(qrPad, qrTop.toFloat(), qrPad + qrSize, qrTop + qrSize.toFloat())
        if (design.matrixShadowEnabled) {
            val outerGlow = RectF(
                qrRect.left - w * 0.008f,
                qrRect.top + w * 0.008f,
                qrRect.right + w * 0.008f,
                qrRect.bottom + w * 0.022f
            )
            canvas.drawRoundRect(
                outerGlow,
                qrCorner + w * 0.01f,
                qrCorner + w * 0.01f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x12000000 }
            )
            val innerShadow = RectF(
                qrRect.left - w * 0.003f,
                qrRect.top + w * 0.004f,
                qrRect.right + w * 0.003f,
                qrRect.bottom + w * 0.012f
            )
            canvas.drawRoundRect(
                innerShadow,
                qrCorner + w * 0.004f,
                qrCorner + w * 0.004f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1A000000 }
            )
        }
        if (frame.style == OuterFrameStyle.CORNER_BRACKET) {
            val inset = w * 0.018f
            val left = qrRect.left - inset
            val top = qrRect.top - inset
            val right = qrRect.right + inset
            val bottom = qrRect.bottom + inset
            val leg = w * 0.085f
            val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(w * 0.016f, 6f)
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = frame.accentColor.toInt()
            }
            val bp = android.graphics.Path().apply {
                moveTo(left, top + leg); lineTo(left, top); lineTo(left + leg, top)
                moveTo(right - leg, top); lineTo(right, top); lineTo(right, top + leg)
                moveTo(left, bottom - leg); lineTo(left, bottom); lineTo(left + leg, bottom)
                moveTo(right - leg, bottom); lineTo(right, bottom); lineTo(right, bottom - leg)
            }
            canvas.drawPath(bp, bracketPaint)
        }
        if (frame.style == OuterFrameStyle.CIRCULAR_STAMP) {
            val sealRadius = qrSize / 2f + w * 0.020f
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(w * 0.011f, 4f)
                color = frame.accentColor.toInt()
            }
            canvas.drawCircle(qrRect.centerX(), qrRect.centerY(), sealRadius, ringPaint)
        }

        // Render QR core directly onto `canvas` inside `qrRect` — zero double-bitmap OOM overhead at 8K!
        val safeData = data.ifBlank { "https://qraft.studio" }
        val logoBitmap = LogoCatalog.renderLogoBitmap(
            context,
            design.logo,
            outputPx = (qrSize * 0.28f).roundToInt().coerceIn(120, 1024)
        )
        val logoPainter = logoBitmap?.let { BitmapPainter(it.asImageBitmap()) }
        val options = buildQrOptions(design, logoPainter)
        val painter = QrCodePainter(data = safeData, options = options)
        val quietPx = (qrSize * design.quietZonePadding.coerceIn(0f, 0.24f)).roundToInt()
        val innerSize = (qrSize - quietPx * 2).coerceAtLeast(64)

        val qrSave = canvas.save()
        val qrClip = android.graphics.Path().apply {
            addRoundRect(qrRect, qrCorner, qrCorner, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(qrClip)
        when (design.background.mode) {
            BackgroundMode.TRANSPARENT -> Unit
            BackgroundMode.SOLID -> {
                canvas.drawRect(qrRect, Paint().apply { color = design.background.brush.primaryColor.toInt() })
            }
            BackgroundMode.GRADIENT -> {
                canvas.save()
                canvas.translate(qrRect.left, qrRect.top)
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    val shader = buildNativeShader(design.background.brush, qrSize.toFloat(), qrSize.toFloat())
                    if (shader != null) this.shader = shader else color = design.background.brush.primaryColor.toInt()
                }
                canvas.drawRect(0f, 0f, qrSize.toFloat(), qrSize.toFloat(), bgPaint)
                canvas.restore()
            }
            BackgroundMode.PHOTO -> {
                val photo = design.background.photoUri?.let {
                    LogoCatalog.loadSampledUri(context, Uri.parse(it), qrSize.coerceAtMost(2048))
                }
                if (photo != null) {
                    val side = min(photo.width, photo.height)
                    val left = (photo.width - side) / 2
                    val top = (photo.height - side) / 2
                    canvas.drawBitmap(
                        photo,
                        android.graphics.Rect(left, top, left + side, top + side),
                        qrRect,
                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    )
                    photo.recycle()
                } else {
                    canvas.drawRect(qrRect, Paint().apply { color = 0xFFFFFFFF.toInt() })
                }
                val scrimColor = Color(design.background.photoOverlayColor)
                    .copy(alpha = design.background.photoOverlayAlpha.coerceIn(0f, 0.95f))
                canvas.drawRect(
                    qrRect,
                    Paint().apply {
                        color = android.graphics.Color.argb(
                            (scrimColor.alpha * 255).roundToInt(),
                            (scrimColor.red * 255).roundToInt(),
                            (scrimColor.green * 255).roundToInt(),
                            (scrimColor.blue * 255).roundToInt()
                        )
                    }
                )
            }
        }
        val composeCanvas = androidx.compose.ui.graphics.Canvas(bitmap.asImageBitmap())
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = Size(w.toFloat(), totalH.toFloat())
        ) {
            translate(left = qrPad + quietPx, top = qrTop.toFloat() + quietPx) {
                with(painter) {
                    draw(Size(innerSize.toFloat(), innerSize.toFloat()))
                }
            }
        }
        canvas.restoreToCount(qrSave)
        logoBitmap?.recycle()

        val label = frame.label.trim().ifBlank { "SCAN ME" }
        val bottomText = if (frame.style == OuterFrameStyle.DOUBLE_PILL && frame.subtitle.isNotBlank()) {
            frame.subtitle.trim()
        } else {
            label
        }

        fun drawPillBadge(
            text: String,
            pillRect: RectF,
            useDualGradient: Boolean
        ) {
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                if (useDualGradient) {
                    shader = LinearGradient(
                        pillRect.left, pillRect.centerY(),
                        pillRect.right, pillRect.centerY(),
                        frame.accentColor.toInt(),
                        frame.accentSecondaryColor.toInt(),
                        Shader.TileMode.CLAMP
                    )
                } else {
                    color = frame.accentColor.toInt()
                }
            }
            canvas.drawRoundRect(pillRect, pillRect.height() / 2f, pillRect.height() / 2f, pillPaint)

            val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = frame.textColor.toInt()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                letterSpacing = 0.07f
            }
            fitTextSize(pillTextPaint, text, pillRect.width() * 0.84f, w * 0.052f, w * 0.026f)
            val fm = pillTextPaint.fontMetrics
            val baseline = pillRect.centerY() - (fm.ascent + fm.descent) / 2f
            canvas.drawText(text, pillRect.centerX(), baseline, pillTextPaint)
        }

        when (frame.style) {
            OuterFrameStyle.BOTTOM_SCAN_BANNER,
            OuterFrameStyle.SPEECH_BUBBLE,
            OuterFrameStyle.GLASS_CARD,
            OuterFrameStyle.CORNER_BRACKET,
            OuterFrameStyle.NEON_GLOW,
            OuterFrameStyle.CIRCULAR_STAMP -> {
                val pillW = w * 0.64f
                val pillH = w * 0.112f
                val pillLeft = (w - pillW) / 2f
                val pillTop = qrTop + qrSize + w * 0.042f
                drawPillBadge(
                    label,
                    RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH),
                    frame.style == OuterFrameStyle.NEON_GLOW
                )
            }
            OuterFrameStyle.TICKET_STUB -> {
                val seamY = qrTop + qrSize + w * 0.030f
                val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = max(w * 0.005f, 2f)
                    color = frame.accentColor.toInt()
                    alpha = 110
                    pathEffect = DashPathEffect(floatArrayOf(w * 0.024f, w * 0.016f), 0f)
                }
                canvas.drawLine(cardRect.left + w * 0.06f, seamY, cardRect.right - w * 0.06f, seamY, dashPaint)
                val pillW = w * 0.62f
                val pillH = w * 0.104f
                val pillLeft = (w - pillW) / 2f
                val pillTop = seamY + w * 0.026f
                drawPillBadge(label, RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH), false)
                if (hasSubtitle) {
                    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = frame.accentColor.toInt()
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    }
                    fitTextSize(subPaint, frame.subtitle.trim(), w * 0.80f, w * 0.036f, w * 0.020f)
                    canvas.drawText(frame.subtitle.trim(), w / 2f, pillTop + pillH + w * 0.046f, subPaint)
                }
            }
            OuterFrameStyle.MINIMAL_OUTLINE -> {
                val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = frame.accentColor.toInt()
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    letterSpacing = 0.14f
                }
                fitTextSize(capPaint, label, w * 0.80f, w * 0.042f, w * 0.022f)
                canvas.drawText(label, w / 2f, qrTop + qrSize + w * 0.088f, capPaint)
            }
            OuterFrameStyle.WARM_POSTER -> {
                val pillW = w * 0.66f
                val pillH = w * 0.112f
                val pillLeft = (w - pillW) / 2f
                val pillTop = qrTop + qrSize + w * 0.038f
                drawPillBadge(label, RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH), true)
                if (hasSubtitle) {
                    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = frame.accentColor.toInt()
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    }
                    fitTextSize(subPaint, frame.subtitle.trim(), w * 0.80f, w * 0.038f, w * 0.022f)
                    canvas.drawText(frame.subtitle.trim(), w / 2f, pillTop + pillH + w * 0.052f, subPaint)
                }
            }
            OuterFrameStyle.DOUBLE_PILL -> {
                val topW = w * 0.58f
                val topH = w * 0.104f
                val topLeft = (w - topW) / 2f
                val topTop = margin + w * 0.038f
                drawPillBadge(label, RectF(topLeft, topTop, topLeft + topW, topTop + topH), false)

                val botW = w * 0.62f
                val botH = w * 0.108f
                val botLeft = (w - botW) / 2f
                val botTop = qrTop + qrSize + w * 0.042f
                drawPillBadge(bottomText, RectF(botLeft, botTop, botLeft + botW, botTop + botH), false)
            }
            OuterFrameStyle.TOP_BADGE_CARD -> {
                val pillW = w * 0.58f
                val pillH = w * 0.105f
                val pillLeft = (w - pillW) / 2f
                val pillTop = margin + w * 0.04f
                drawPillBadge(label, RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH), false)
            }
            OuterFrameStyle.POLAROID_STAMP -> {
                val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = frame.accentColor.toInt()
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    letterSpacing = 0.08f
                }
                fitTextSize(stampPaint, label, w * 0.82f, w * 0.056f, w * 0.028f)
                val y = qrTop + qrSize + w * 0.115f
                canvas.drawText(label, w / 2f, y, stampPaint)
            }
            OuterFrameStyle.SCAN_BAR_HEADER -> Unit
            OuterFrameStyle.GRADIENT_BORDER, OuterFrameStyle.NONE, OuterFrameStyle.CLEAN_CARD -> Unit
        }

        return bitmap
    }

    private fun fitTextSize(
        paint: Paint,
        text: String,
        maxWidth: Float,
        preferredSize: Float,
        minSize: Float
    ) {
        var size = preferredSize
        paint.textSize = size
        while (size > minSize && paint.measureText(text) > maxWidth) {
            size *= 0.92f
            paint.textSize = size
        }
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
        bitmap.recycle()

        val source = RGBLuminanceSource(width, height, pixels)
        val reader = QRCodeReader()
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.ALSO_INVERTED to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )

        val decoded = runCatching {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        }.getOrNull() ?: runCatching {
            reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), hints).text
        }.getOrNull() ?: runCatching {
            val inverted = source.invert()
            reader.decode(BinaryBitmap(HybridBinarizer(inverted)), hints).text
        }.getOrNull() ?: runCatching {
            val inverted = source.invert()
            reader.decode(BinaryBitmap(GlobalHistogramBinarizer(inverted)), hints).text
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
        val qrCoreDim = count * cell
        val quiet = qrCoreDim * design.quietZonePadding.coerceIn(0f, 0.24f)
        val innerQrDim = qrCoreDim + quiet * 2f

        val frame = design.outerFrame
        val framed = frame.style != OuterFrameStyle.NONE
        val hasSubtitle = (frame.style == OuterFrameStyle.WARM_POSTER || frame.style == OuterFrameStyle.TICKET_STUB) &&
            frame.subtitle.isNotBlank()
        val extraBottom = when (frame.style) {
            OuterFrameStyle.NONE -> 0f
            OuterFrameStyle.BOTTOM_SCAN_BANNER,
            OuterFrameStyle.GLASS_CARD,
            OuterFrameStyle.CORNER_BRACKET,
            OuterFrameStyle.NEON_GLOW,
            OuterFrameStyle.CIRCULAR_STAMP -> innerQrDim * 0.20f
            OuterFrameStyle.TICKET_STUB -> innerQrDim * (if (hasSubtitle) 0.25f else 0.21f)
            OuterFrameStyle.MINIMAL_OUTLINE -> innerQrDim * 0.14f
            OuterFrameStyle.SPEECH_BUBBLE -> innerQrDim * 0.27f
            OuterFrameStyle.POLAROID_STAMP -> innerQrDim * 0.20f
            OuterFrameStyle.WARM_POSTER -> innerQrDim * (if (hasSubtitle) 0.25f else 0.20f)
            OuterFrameStyle.DOUBLE_PILL -> innerQrDim * 0.20f
            OuterFrameStyle.TOP_BADGE_CARD,
            OuterFrameStyle.SCAN_BAR_HEADER -> innerQrDim * 0.14f
            OuterFrameStyle.CLEAN_CARD,
            OuterFrameStyle.GRADIENT_BORDER -> innerQrDim * 0.06f
        }
        val extraTop = if (
            frame.style == OuterFrameStyle.TOP_BADGE_CARD ||
            frame.style == OuterFrameStyle.DOUBLE_PILL ||
            frame.style == OuterFrameStyle.SCAN_BAR_HEADER
        ) {
            innerQrDim * 0.14f
        } else {
            0f
        }
        val cardMargin = if (framed) innerQrDim * 0.045f else 0f
        val qrPad = if (framed) innerQrDim * frame.effectivePaddingFraction() else 0f
        val tailH = if (frame.style == OuterFrameStyle.SPEECH_BUBBLE) innerQrDim * 0.065f else 0f
        val totalW = innerQrDim + qrPad * 2f
        val qrOriginX = qrPad
        val qrOriginY = if (framed) cardMargin + extraTop + innerQrDim * 0.045f else 0f
        val totalH = qrOriginY + innerQrDim + (if (framed) cardMargin * 0.4f else 0f) + extraBottom

        val exportW = design.exportSizePx.coerceIn(512, 8192)
        val exportH = (exportW * totalH / totalW).roundToInt()

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine(
            """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${fmt(totalW)} ${fmt(totalH)}" width="$exportW" height="$exportH">"""
        )
        sb.appendLine("  <defs>")
        appendSvgBrushDef(sb, "qrDarkFill", design.darkBrush)
        if (design.customEyeFrameColors) {
            appendSvgBrushDef(sb, "qrEyeFrameFill", design.eyeFrameBrush)
        }
        if (design.customEyeBallColors) {
            appendSvgBrushDef(sb, "qrEyeBallFill", design.eyeBallBrush)
        }
        if (design.background.mode == BackgroundMode.GRADIENT) {
            appendSvgBrushDef(sb, "qrBgFill", design.background.brush)
        }
        if (frame.style == OuterFrameStyle.GRADIENT_BORDER ||
            frame.style == OuterFrameStyle.WARM_POSTER ||
            frame.style == OuterFrameStyle.NEON_GLOW
        ) {
            sb.appendLine("""    <linearGradient id="qrFrameAccent" x1="0%" y1="0%" x2="100%" y2="0%">""")
            sb.appendLine("""      <stop offset="0%" stop-color="${toSvgHex(frame.accentColor)}"/>""")
            sb.appendLine("""      <stop offset="100%" stop-color="${toSvgHex(frame.accentSecondaryColor)}"/>""")
            sb.appendLine("""    </linearGradient>""")
        }
        val qrCorner = if (frame.style == OuterFrameStyle.CIRCULAR_STAMP) {
            innerQrDim / 2f
        } else {
            ((frame.cornerRadiusDp / 360f) * totalW * 0.65f).coerceAtLeast(16f)
        }
        sb.appendLine("""    <clipPath id="qrCoreClip">""")
        sb.appendLine("""      <rect x="${fmt(qrOriginX)}" y="${fmt(qrOriginY)}" width="${fmt(innerQrDim)}" height="${fmt(innerQrDim)}" rx="${fmt(qrCorner)}"/>""")
        sb.appendLine("""    </clipPath>""")
        sb.appendLine("  </defs>")

        // Outer Frame card
        if (framed) {
            val cardCorner = (frame.cornerRadiusDp / 360f) * totalW
            val cardBottom = totalH - cardMargin - tailH
            val cardFill = toSvgHex(frame.frameFillColor)
            sb.appendLine(
                """  <rect x="${fmt(cardMargin)}" y="${fmt(cardMargin)}" width="${fmt(totalW - cardMargin * 2f)}" height="${fmt(cardBottom - cardMargin)}" rx="${fmt(cardCorner)}" fill="$cardFill"/>"""
            )
            if (frame.style == OuterFrameStyle.SPEECH_BUBBLE) {
                val tailHalfW = totalW * 0.065f
                val cx = totalW / 2f
                sb.appendLine(
                    """  <polygon points="${fmt(cx - tailHalfW)},${fmt(cardBottom - 2f)} ${fmt(cx + tailHalfW)},${fmt(cardBottom - 2f)} ${fmt(cx)},${fmt(cardBottom + tailH)}" fill="$cardFill"/>"""
                )
            }
            if (frame.style == OuterFrameStyle.GRADIENT_BORDER || frame.style == OuterFrameStyle.NEON_GLOW) {
                val strokeW = max(totalW * (if (frame.style == OuterFrameStyle.NEON_GLOW) 0.012f else 0.022f), 8f)
                sb.appendLine(
                    """  <rect x="${fmt(cardMargin + strokeW / 2f)}" y="${fmt(cardMargin + strokeW / 2f)}" width="${fmt(totalW - cardMargin * 2f - strokeW)}" height="${fmt(cardBottom - cardMargin - strokeW)}" rx="${fmt(cardCorner)}" fill="none" stroke="url(#qrFrameAccent)" stroke-width="${fmt(strokeW)}"/>"""
                )
            } else if (frame.style == OuterFrameStyle.MINIMAL_OUTLINE) {
                val strokeW = max(totalW * 0.0045f, 2f)
                sb.appendLine(
                    """  <rect x="${fmt(cardMargin)}" y="${fmt(cardMargin)}" width="${fmt(totalW - cardMargin * 2f)}" height="${fmt(cardBottom - cardMargin)}" rx="${fmt(cardCorner)}" fill="none" stroke="${toSvgHex(frame.accentColor)}" stroke-width="${fmt(strokeW)}"/>"""
                )
            } else if (frame.style == OuterFrameStyle.GLASS_CARD) {
                val strokeW = max(totalW * 0.008f, 3f)
                sb.appendLine(
                    """  <rect x="${fmt(cardMargin + strokeW / 2f)}" y="${fmt(cardMargin + strokeW / 2f)}" width="${fmt(totalW - cardMargin * 2f - strokeW)}" height="${fmt(cardBottom - cardMargin - strokeW)}" rx="${fmt(cardCorner)}" fill="none" stroke="${toSvgHex(frame.accentColor)}" stroke-opacity="0.35" stroke-width="${fmt(strokeW)}"/>"""
                )
            } else if (frame.style == OuterFrameStyle.SCAN_BAR_HEADER) {
                val barH = totalW * 0.145f
                val safeHdr = escapeXml(frame.label.trim().ifBlank { "SCAN ME" })
                sb.appendLine(
                    """  <rect x="${fmt(cardMargin)}" y="${fmt(cardMargin)}" width="${fmt(totalW - cardMargin * 2f)}" height="${fmt(barH)}" rx="${fmt(cardCorner)}" fill="${toSvgHex(frame.accentColor)}"/>"""
                )
                sb.appendLine(
                    """  <text x="${fmt(totalW / 2f)}" y="${fmt(cardMargin + barH * 0.65f)}" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="${fmt(totalW * 0.044f)}" letter-spacing="1.6" fill="${toSvgHex(frame.textColor)}">$safeHdr</text>"""
                )
            } else if (frame.borderThicknessDp > 0f) {
                val strokeW = (frame.borderThicknessDp / 360f) * totalW
                sb.appendLine(
                    """  <rect x="${fmt(cardMargin)}" y="${fmt(cardMargin)}" width="${fmt(totalW - cardMargin * 2f)}" height="${fmt(cardBottom - cardMargin)}" rx="${fmt(cardCorner)}" fill="none" stroke="${toSvgHex(frame.accentColor)}" stroke-width="${fmt(strokeW)}"/>"""
                )
            }
            if (frame.style == OuterFrameStyle.CIRCULAR_STAMP) {
                sb.appendLine(
                    """  <circle cx="${fmt(qrOriginX + innerQrDim / 2f)}" cy="${fmt(qrOriginY + innerQrDim / 2f)}" r="${fmt(innerQrDim / 2f + totalW * 0.020f)}" fill="none" stroke="${toSvgHex(frame.accentColor)}" stroke-width="${fmt(max(totalW * 0.011f, 4f))}"/>"""
                )
            }
            if (frame.style == OuterFrameStyle.CORNER_BRACKET) {
                val inset = totalW * 0.018f
                val l = qrOriginX - inset
                val t = qrOriginY - inset
                val r = qrOriginX + innerQrDim + inset
                val b = qrOriginY + innerQrDim + inset
                val leg = totalW * 0.085f
                val sw = max(totalW * 0.015f, 5f)
                val accentHex = toSvgHex(frame.accentColor)
                val pathD = "M ${fmt(l)} ${fmt(t + leg)} L ${fmt(l)} ${fmt(t)} L ${fmt(l + leg)} ${fmt(t)} " +
                    "M ${fmt(r - leg)} ${fmt(t)} L ${fmt(r)} ${fmt(t)} L ${fmt(r)} ${fmt(t + leg)} " +
                    "M ${fmt(l)} ${fmt(b - leg)} L ${fmt(l)} ${fmt(b)} L ${fmt(l + leg)} ${fmt(b)} " +
                    "M ${fmt(r - leg)} ${fmt(b)} L ${fmt(r)} ${fmt(b)} L ${fmt(r)} ${fmt(b - leg)}"
                sb.appendLine(
                    """  <path d="$pathD" fill="none" stroke="$accentHex" stroke-width="${fmt(sw)}" stroke-linecap="round" stroke-linejoin="round"/>"""
                )
            }
        }

        if (design.matrixShadowEnabled) {
            val sPad = innerQrDim * 0.008f
            val sDrop = innerQrDim * 0.02f
            sb.appendLine(
                """  <rect x="${fmt(qrOriginX - sPad)}" y="${fmt(qrOriginY + sDrop * 0.4f)}" width="${fmt(innerQrDim + sPad * 2f)}" height="${fmt(innerQrDim + sDrop * 0.7f)}" rx="${fmt(qrCorner + sPad)}" fill="#000000" fill-opacity="0.08"/>"""
            )
            sb.appendLine(
                """  <rect x="${fmt(qrOriginX - sPad * 0.4f)}" y="${fmt(qrOriginY + sDrop * 0.2f)}" width="${fmt(innerQrDim + sPad * 0.8f)}" height="${fmt(innerQrDim + sDrop * 0.35f)}" rx="${fmt(qrCorner)}" fill="#000000" fill-opacity="0.10"/>"""
            )
        }

        // QR core background
        val bgRect = """x="${fmt(qrOriginX)}" y="${fmt(qrOriginY)}" width="${fmt(innerQrDim)}" height="${fmt(innerQrDim)}""""
        val clipAttr = """clip-path="url(#qrCoreClip)""""
        when (design.background.mode) {
            BackgroundMode.TRANSPARENT -> Unit
            BackgroundMode.SOLID -> sb.appendLine(
                """  <rect $bgRect $clipAttr fill="${toSvgHex(design.background.brush.primaryColor)}"/>"""
            )
            BackgroundMode.GRADIENT -> sb.appendLine(
                """  <rect $bgRect $clipAttr fill="url(#qrBgFill)"/>"""
            )
            BackgroundMode.PHOTO -> sb.appendLine(
                """  <rect $bgRect $clipAttr fill="#FFFFFF"/>"""
            )
        }

        val fillRef = if (design.darkBrush.mode == BrushMode.SOLID) toSvgHex(design.darkBrush.primaryColor) else "url(#qrDarkFill)"
        val eyeFrameFill = if (design.customEyeFrameColors) {
            if (design.eyeFrameBrush.mode == BrushMode.SOLID) toSvgHex(design.eyeFrameBrush.primaryColor) else "url(#qrEyeFrameFill)"
        } else fillRef
        val eyeBallFill = if (design.customEyeBallColors) {
            if (design.eyeBallBrush.mode == BrushMode.SOLID) toSvgHex(design.eyeBallBrush.primaryColor) else "url(#qrEyeBallFill)"
        } else fillRef
        val lightFillRef = toSvgHex(design.lightPixelColor)
        val lightAlpha = (((design.lightPixelColor ushr 24) and 0xFFL).toFloat() / 255f).coerceIn(0.08f, 1.0f)

        val finderOrigins = mutableListOf(0 to 0, (count - 7) to 0, 0 to (count - 7))
        if (design.fourthEyeEnabled) {
            finderOrigins.add((count - 7) to (count - 7))
        }
        fun isInsideFinder(x: Int, y: Int): Boolean =
            finderOrigins.any { (fx, fy) -> x in fx until (fx + 7) && y in fy until (fy + 7) }

        val logoExclusionFrac = if (design.logo.sourceType != LogoSourceType.NONE) {
            (design.logo.sizeFraction + design.logo.paddingFraction * 0.25f).coerceIn(0.12f, 0.34f)
        } else 0f
        val logoRadiusCells = (count * logoExclusionFrac) / 2f
        val centerCell = count / 2f
        fun isInsideLogoZone(x: Int, y: Int): Boolean {
            if (logoExclusionFrac <= 0f) return false
            val dx = (x + 0.5f) - centerCell
            val dy = (y + 0.5f) - centerCell
            return when (design.logo.clipShape) {
                LogoClipShape.CIRCLE -> (dx * dx + dy * dy) < (logoRadiusCells * logoRadiusCells)
                LogoClipShape.DIAMOND -> (kotlin.math.abs(dx) + kotlin.math.abs(dy)) < logoRadiusCells * 1.15f
                else -> kotlin.math.abs(dx) < logoRadiusCells && kotlin.math.abs(dy) < logoRadiusCells
            }
        }

        val factor = design.pixelSizeFactor.coerceIn(0.45f, 1.0f)
        val darkPathData = StringBuilder()
        val lightPathData = StringBuilder()

        for (y in 0 until count) {
            for (x in 0 until count) {
                if (isInsideFinder(x, y) || isInsideLogoZone(x, y)) continue
                val bit = matrix.get(x, y).toInt() == 1
                val ox = qrOriginX + quiet + x * cell
                val oy = qrOriginY + quiet + y * cell
                if (bit) {
                    appendSvgPixelShape(
                        sb = darkPathData,
                        style = design.pixelStyle,
                        ox = ox,
                        oy = oy,
                        cell = cell,
                        factor = factor,
                        roundness = design.pixelCornerRoundness
                    )
                } else if (design.lightPixelStyle != QrLightPixelStyle.NONE) {
                    val lightFactor = when (design.lightPixelStyle) {
                        QrLightPixelStyle.DOT -> 0.38f
                        QrLightPixelStyle.ROUNDED -> 0.58f
                        QrLightPixelStyle.SQUARE -> 0.55f
                        QrLightPixelStyle.NONE -> 0f
                    }
                    val lightStyle = when (design.lightPixelStyle) {
                        QrLightPixelStyle.DOT -> QrPixelStyle.CIRCLE
                        QrLightPixelStyle.ROUNDED -> QrPixelStyle.ROUNDED
                        else -> QrPixelStyle.SQUARE
                    }
                    appendSvgPixelShape(
                        sb = lightPathData,
                        style = lightStyle,
                        ox = ox,
                        oy = oy,
                        cell = cell,
                        factor = lightFactor,
                        roundness = 0.30f
                    )
                }
            }
        }

        if (lightPathData.isNotEmpty()) {
            sb.appendLine("""  <path d="$lightPathData" fill="$lightFillRef" opacity="${fmt(lightAlpha)}"/>""")
        }
        if (darkPathData.isNotEmpty()) {
            sb.appendLine("""  <path d="$darkPathData" fill="$fillRef"/>""")
        }

        // Crisp vector finder eye frames & balls
        val eyeFramePath = StringBuilder()
        val eyeBallPath = StringBuilder()
        for ((fx, fy) in finderOrigins) {
            val ox = qrOriginX + quiet + fx * cell
            val oy = qrOriginY + quiet + fy * cell
            appendSvgEyeFrame(eyeFramePath, design.eyeFrameStyle, ox, oy, cell, design.eyeFrameRoundness)
            appendSvgEyeBall(eyeBallPath, design.eyeBallStyle, ox + 2f * cell, oy + 2f * cell, 3f * cell, design.eyeBallRoundness)
        }
        sb.appendLine("""  <path d="$eyeFramePath" fill="$eyeFrameFill" fill-rule="evenodd"/>""")
        sb.appendLine("""  <path d="$eyeBallPath" fill="$eyeBallFill"/>""")

        // Center Logo Badge (vector backing + monogram/emoji if configured)
        if (design.logo.sourceType != LogoSourceType.NONE) {
            val logoDim = qrCoreDim * design.logo.sizeFraction.coerceIn(0.10f, 0.32f)
            val lcx = qrOriginX + quiet + qrCoreDim / 2f
            val lcy = qrOriginY + quiet + qrCoreDim / 2f
            val lx = lcx - logoDim / 2f
            val ly = lcy - logoDim / 2f
            val backingFill = if (design.logo.backingEnabled) toSvgHex(design.logo.backingColor) else "none"
            val backingStroke = if (design.logo.borderEnabled) toSvgHex(design.logo.borderColor) else "none"
            val backingStrokeW = if (design.logo.borderEnabled) fmt(logoDim * 0.045f) else "0"
            when (design.logo.clipShape) {
                LogoClipShape.CIRCLE -> sb.appendLine(
                    """  <circle cx="${fmt(lcx)}" cy="${fmt(lcy)}" r="${fmt(logoDim / 2f)}" fill="$backingFill" stroke="$backingStroke" stroke-width="$backingStrokeW"/>"""
                )
                LogoClipShape.DIAMOND -> sb.appendLine(
                    """  <polygon points="${fmt(lcx)},${fmt(ly)} ${fmt(lx + logoDim)},${fmt(lcy)} ${fmt(lcx)},${fmt(ly + logoDim)} ${fmt(lx)},${fmt(lcy)}" fill="$backingFill" stroke="$backingStroke" stroke-width="$backingStrokeW"/>"""
                )
                else -> {
                    val lr = when (design.logo.clipShape) {
                        LogoClipShape.SQUARE -> 0f
                        LogoClipShape.SQUIRCLE -> logoDim * 0.36f
                        else -> logoDim * 0.24f
                    }
                    sb.appendLine(
                        """  <rect x="${fmt(lx)}" y="${fmt(ly)}" width="${fmt(logoDim)}" height="${fmt(logoDim)}" rx="${fmt(lr)}" fill="$backingFill" stroke="$backingStroke" stroke-width="$backingStrokeW"/>"""
                    )
                }
            }
            if (design.logo.sourceType == LogoSourceType.MONOGRAM) {
                val txt = escapeXml(design.logo.monogramText.trim().take(4).ifBlank { "QR" })
                sb.appendLine(
                    """  <text x="${fmt(lcx)}" y="${fmt(lcy + logoDim * 0.14f)}" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="${fmt(logoDim * 0.38f)}" fill="${toSvgHex(design.logo.monogramTextColor)}">$txt</text>"""
                )
            } else if (design.logo.sourceType == LogoSourceType.EMOJI) {
                val em = escapeXml(design.logo.emoji.ifBlank { "⚡" })
                sb.appendLine(
                    """  <text x="${fmt(lcx)}" y="${fmt(lcy + logoDim * 0.18f)}" text-anchor="middle" font-size="${fmt(logoDim * 0.54f)}">$em</text>"""
                )
            }
        }

        // Outer Frame badges & labels
        if (framed) {
            val safeLabel = escapeXml(frame.label.trim().ifBlank { "SCAN ME" })
            val accentHex = toSvgHex(frame.accentColor)
            val textHex = toSvgHex(frame.textColor)
            val pillFill = if (frame.style == OuterFrameStyle.WARM_POSTER) "url(#qrFrameAccent)" else accentHex

            fun appendSvgPill(text: String, py: Float, widthFrac: Float, heightFrac: Float) {
                val pw = totalW * widthFrac
                val ph = totalW * heightFrac
                val px = (totalW - pw) / 2f
                val fontSize = (ph * 0.46f).coerceAtMost(pw * 0.84f / text.length.coerceAtLeast(1) * 1.45f)
                sb.appendLine("""  <rect x="${fmt(px)}" y="${fmt(py)}" width="${fmt(pw)}" height="${fmt(ph)}" rx="${fmt(ph / 2f)}" fill="$pillFill"/>""")
                sb.appendLine(
                    """  <text x="${fmt(totalW / 2f)}" y="${fmt(py + ph * 0.65f)}" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="${fmt(fontSize)}" letter-spacing="1.2" fill="$textHex">$text</text>"""
                )
            }

            when (frame.style) {
                OuterFrameStyle.BOTTOM_SCAN_BANNER,
                OuterFrameStyle.SPEECH_BUBBLE,
                OuterFrameStyle.GLASS_CARD,
                OuterFrameStyle.CORNER_BRACKET,
                OuterFrameStyle.NEON_GLOW,
                OuterFrameStyle.CIRCULAR_STAMP -> {
                    appendSvgPill(safeLabel, qrOriginY + innerQrDim + innerQrDim * 0.042f, 0.60f, 0.105f)
                }
                OuterFrameStyle.TICKET_STUB -> {
                    val seamY = qrOriginY + innerQrDim + innerQrDim * 0.030f
                    sb.appendLine(
                        """  <line x1="${fmt(cardMargin + totalW * 0.05f)}" y1="${fmt(seamY)}" x2="${fmt(totalW - cardMargin - totalW * 0.05f)}" y2="${fmt(seamY)}" stroke="$accentHex" stroke-opacity="0.45" stroke-width="${fmt(max(totalW * 0.0045f, 2f))}" stroke-dasharray="${fmt(totalW * 0.025f)} ${fmt(totalW * 0.015f)}"/>"""
                    )
                    val py = seamY + innerQrDim * 0.024f
                    appendSvgPill(safeLabel, py, 0.60f, 0.098f)
                    if (hasSubtitle) {
                        val safeSub = escapeXml(frame.subtitle.trim())
                        sb.appendLine(
                            """  <text x="${fmt(totalW / 2f)}" y="${fmt(py + totalW * 0.135f)}" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="${fmt(totalW * 0.030f)}" fill="$accentHex">$safeSub</text>"""
                        )
                    }
                }
                OuterFrameStyle.MINIMAL_OUTLINE -> {
                    val y = qrOriginY + innerQrDim + innerQrDim * 0.088f
                    sb.appendLine(
                        """  <text x="${fmt(totalW / 2f)}" y="${fmt(y)}" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="${fmt(totalW * 0.036f)}" letter-spacing="2.4" fill="$accentHex">$safeLabel</text>"""
                    )
                }
                OuterFrameStyle.WARM_POSTER -> {
                    val py = qrOriginY + innerQrDim + innerQrDim * 0.036f
                    appendSvgPill(safeLabel, py, 0.62f, 0.102f)
                    if (hasSubtitle) {
                        val safeSub = escapeXml(frame.subtitle.trim())
                        sb.appendLine(
                            """  <text x="${fmt(totalW / 2f)}" y="${fmt(py + totalW * 0.145f)}" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="${fmt(totalW * 0.032f)}" fill="$accentHex">$safeSub</text>"""
                        )
                    }
                }
                OuterFrameStyle.TOP_BADGE_CARD -> {
                    appendSvgPill(safeLabel, cardMargin + innerQrDim * 0.038f, 0.54f, 0.098f)
                }
                OuterFrameStyle.DOUBLE_PILL -> {
                    appendSvgPill(safeLabel, cardMargin + innerQrDim * 0.036f, 0.54f, 0.096f)
                    val bottomTxt = escapeXml(frame.subtitle.trim().ifBlank { frame.label.trim().ifBlank { "SCAN ME" } })
                    appendSvgPill(bottomTxt, qrOriginY + innerQrDim + innerQrDim * 0.042f, 0.58f, 0.100f)
                }
                OuterFrameStyle.POLAROID_STAMP -> {
                    val y = qrOriginY + innerQrDim + innerQrDim * 0.11f
                    sb.appendLine(
                        """  <text x="${fmt(totalW / 2f)}" y="${fmt(y)}" text-anchor="middle" font-family="sans-serif" font-weight="bold" font-size="${fmt(totalW * 0.048f)}" letter-spacing="1.8" fill="$accentHex">$safeLabel</text>"""
                    )
                }
                else -> Unit
            }
        }

        sb.appendLine("</svg>")
        return sb.toString()
    }

    private fun appendSvgBrushDef(sb: StringBuilder, id: String, brush: BrushStyleConfig) {
        if (brush.mode == BrushMode.SOLID) return
        val c1 = toSvgHex(brush.primaryColor)
        val c2 = toSvgHex(brush.secondaryColor)
        val c3 = brush.tertiaryColor?.let { toSvgHex(it) }
        if (brush.mode == BrushMode.RADIAL_GRADIENT) {
            sb.appendLine("""    <radialGradient id="$id" cx="50%" cy="50%" r="60%">""")
            sb.appendLine("""      <stop offset="0%" stop-color="$c1"/>""")
            if (c3 != null) {
                sb.appendLine("""      <stop offset="50%" stop-color="$c2"/>""")
                sb.appendLine("""      <stop offset="100%" stop-color="$c3"/>""")
            } else {
                sb.appendLine("""      <stop offset="100%" stop-color="$c2"/>""")
            }
            sb.appendLine("""    </radialGradient>""")
        } else {
            val angle = if (brush.direction == GradientDirection.CUSTOM_ANGLE) brush.angleDegrees else brush.direction.angleDeg
            val rad = angle * PI / 180.0
            val dx = (cos(rad) * 50.0)
            val dy = (sin(rad) * 50.0)
            sb.appendLine(
                """    <linearGradient id="$id" x1="${(50.0 - dx).roundToInt()}%" y1="${(50.0 - dy).roundToInt()}%" x2="${(50.0 + dx).roundToInt()}%" y2="${(50.0 + dy).roundToInt()}%">"""
            )
            sb.appendLine("""      <stop offset="0%" stop-color="$c1"/>""")
            if (c3 != null) {
                sb.appendLine("""      <stop offset="50%" stop-color="$c2"/>""")
                sb.appendLine("""      <stop offset="100%" stop-color="$c3"/>""")
            } else {
                sb.appendLine("""      <stop offset="100%" stop-color="$c2"/>""")
            }
            sb.appendLine("""    </linearGradient>""")
        }
    }

    private fun appendSvgPixelShape(
        sb: StringBuilder,
        style: QrPixelStyle,
        ox: Float,
        oy: Float,
        cell: Float,
        factor: Float,
        roundness: Float
    ) {
        val f = factor.coerceIn(0.45f, 1.0f)
        val scaled = cell * f
        val pad = (cell - scaled) / 2f
        val x = ox + pad
        val y = oy + pad
        val cx = ox + cell / 2f
        val cy = oy + cell / 2f

        when (style) {
            QrPixelStyle.SQUARE -> {
                sb.append("M${fmt(x)},${fmt(y)}h${fmt(scaled)}v${fmt(scaled)}h${fmt(-scaled)}Z ")
            }
            QrPixelStyle.ROUNDED, QrPixelStyle.LIQUID -> {
                val rFrac = if (style == QrPixelStyle.LIQUID) roundness.coerceAtLeast(0.38f) else roundness.coerceIn(0f, 0.49f)
                appendSvgRoundRect(sb, x, y, scaled, scaled, scaled * rFrac)
            }
            QrPixelStyle.CIRCLE -> {
                val r = scaled / 2f
                sb.append("M${fmt(cx - r)},${fmt(cy)}a${fmt(r)},${fmt(r)} 0 1,0 ${fmt(r * 2f)},0a${fmt(r)},${fmt(r)} 0 1,0 ${fmt(-r * 2f)},0Z ")
            }
            QrPixelStyle.VERTICAL_BARS -> {
                val w = scaled
                appendSvgRoundRect(sb, ox + (cell - w) / 2f, oy, w, cell, w * 0.42f)
            }
            QrPixelStyle.HORIZONTAL_BARS -> {
                val h = scaled
                appendSvgRoundRect(sb, ox, oy + (cell - h) / 2f, cell, h, h * 0.42f)
            }
            QrPixelStyle.DIAMOND -> {
                val r = scaled / 2f
                sb.append("M${fmt(cx)},${fmt(cy - r)}L${fmt(cx + r)},${fmt(cy)}L${fmt(cx)},${fmt(cy + r)}L${fmt(cx - r)},${fmt(cy)}Z ")
            }
            QrPixelStyle.STAR -> {
                val outerR = scaled / 2f
                val innerR = outerR * 0.65f
                for (i in 0 until 10) {
                    val angle = (-90.0 + i * 36.0) * PI / 180.0
                    val rad = if (i % 2 == 0) outerR else innerR
                    val px = cx + (rad * cos(angle)).toFloat()
                    val py = cy + (rad * sin(angle)).toFloat()
                    sb.append(if (i == 0) "M${fmt(px)},${fmt(py)}" else "L${fmt(px)},${fmt(py)}")
                }
                sb.append("Z ")
            }
            QrPixelStyle.CROSS -> {
                val arm = scaled * 0.36f
                val off = (scaled - arm) / 2f
                val r = arm * 0.35f
                appendSvgRoundRect(sb, x + off, y, arm, scaled, r)
                appendSvgRoundRect(sb, x, y + off, scaled, arm, r)
            }
            QrPixelStyle.HEART -> {
                val w = scaled
                val h = scaled
                sb.append("M${fmt(x + w * 0.5f)},${fmt(y + h * 0.90f)}")
                sb.append("C${fmt(x + w * 0.08f)},${fmt(y + h * 0.62f)} ${fmt(x + w * 0.02f)},${fmt(y + h * 0.25f)} ${fmt(x + w * 0.27f)},${fmt(y + h * 0.14f)}")
                sb.append("C${fmt(x + w * 0.41f)},${fmt(y + h * 0.08f)} ${fmt(x + w * 0.50f)},${fmt(y + h * 0.22f)} ${fmt(x + w * 0.50f)},${fmt(y + h * 0.29f)}")
                sb.append("C${fmt(x + w * 0.50f)},${fmt(y + h * 0.22f)} ${fmt(x + w * 0.59f)},${fmt(y + h * 0.08f)} ${fmt(x + w * 0.73f)},${fmt(y + h * 0.14f)}")
                sb.append("C${fmt(x + w * 0.98f)},${fmt(y + h * 0.25f)} ${fmt(x + w * 0.92f)},${fmt(y + h * 0.62f)} ${fmt(x + w * 0.50f)},${fmt(y + h * 0.90f)}Z ")
            }
            QrPixelStyle.HEXAGON -> {
                val r = scaled / 2f
                for (i in 0 until 6) {
                    val angle = (-30.0 + i * 60.0) * PI / 180.0
                    val px = cx + (r * cos(angle)).toFloat()
                    val py = cy + (r * sin(angle)).toFloat()
                    sb.append(if (i == 0) "M${fmt(px)},${fmt(py)}" else "L${fmt(px)},${fmt(py)}")
                }
                sb.append("Z ")
            }
            QrPixelStyle.SHIELD -> {
                val s = scaled
                sb.append("M${fmt(x + s * 0.12f)},${fmt(y + s * 0.12f)}")
                sb.append("L${fmt(x + s * 0.88f)},${fmt(y + s * 0.12f)}")
                sb.append("L${fmt(x + s * 0.88f)},${fmt(y + s * 0.52f)}")
                sb.append("Q${fmt(x + s * 0.88f)},${fmt(y + s * 0.80f)} ${fmt(x + s * 0.50f)},${fmt(y + s * 0.94f)}")
                sb.append("Q${fmt(x + s * 0.12f)},${fmt(y + s * 0.80f)} ${fmt(x + s * 0.12f)},${fmt(y + s * 0.52f)}Z ")
            }
            QrPixelStyle.OCTAGON -> {
                val r = scaled / 2f
                for (i in 0 until 8) {
                    val angle = (22.5 + i * 45.0) * PI / 180.0
                    val px = cx + (r * cos(angle)).toFloat()
                    val py = cy + (r * sin(angle)).toFloat()
                    sb.append(if (i == 0) "M${fmt(px)},${fmt(py)}" else "L${fmt(px)},${fmt(py)}")
                }
                sb.append("Z ")
            }
            QrPixelStyle.RING -> {
                val ro = scaled / 2f
                val ri = ro * 0.44f
                sb.append("M${fmt(cx - ro)},${fmt(cy)}a${fmt(ro)},${fmt(ro)} 0 1,0 ${fmt(ro * 2f)},0a${fmt(ro)},${fmt(ro)} 0 1,0 ${fmt(-ro * 2f)},0Z ")
                sb.append("M${fmt(cx - ri)},${fmt(cy)}a${fmt(ri)},${fmt(ri)} 0 1,1 ${fmt(ri * 2f)},0a${fmt(ri)},${fmt(ri)} 0 1,1 ${fmt(-ri * 2f)},0Z ")
            }
            QrPixelStyle.WAVE_H -> {
                appendSvgRoundRect(sb, ox, oy + (cell - scaled) / 2f, cell, scaled, scaled * 0.48f)
            }
            QrPixelStyle.WAVE_V -> {
                appendSvgRoundRect(sb, ox + (cell - scaled) / 2f, oy, scaled, cell, scaled * 0.48f)
            }
        }
    }

    private fun appendSvgEyeFrame(
        sb: StringBuilder,
        style: QrEyeFrameStyle,
        ox: Float,
        oy: Float,
        cell: Float,
        roundness: Float
    ) {
        val outerSize = 7f * cell
        val innerSize = 5f * cell
        val r = roundness.coerceIn(0f, 0.45f)
        when (style) {
            QrEyeFrameStyle.SQUARE -> {
                sb.append("M${fmt(ox)},${fmt(oy)}h${fmt(outerSize)}v${fmt(outerSize)}h${fmt(-outerSize)}Z ")
                sb.append("M${fmt(ox + cell)},${fmt(oy + cell)}h${fmt(innerSize)}v${fmt(innerSize)}h${fmt(-innerSize)}Z ")
            }
            QrEyeFrameStyle.CIRCLE, QrEyeFrameStyle.DOUBLE_RING -> {
                val cx = ox + outerSize / 2f
                val cy = oy + outerSize / 2f
                val r1 = outerSize / 2f
                val r2 = innerSize / 2f
                sb.append("M${fmt(cx - r1)},${fmt(cy)}a${fmt(r1)},${fmt(r1)} 0 1,0 ${fmt(r1 * 2f)},0a${fmt(r1)},${fmt(r1)} 0 1,0 ${fmt(-r1 * 2f)},0Z ")
                sb.append("M${fmt(cx - r2)},${fmt(cy)}a${fmt(r2)},${fmt(r2)} 0 1,0 ${fmt(r2 * 2f)},0a${fmt(r2)},${fmt(r2)} 0 1,0 ${fmt(-r2 * 2f)},0Z ")
            }
            QrEyeFrameStyle.CHAMFER -> {
                val c1 = outerSize * 0.18f
                val c2 = innerSize * 0.16f
                appendSvgChamfer(sb, ox, oy, outerSize, c1)
                appendSvgChamfer(sb, ox + cell, oy + cell, innerSize, c2)
            }
            QrEyeFrameStyle.SQUIRCLE -> {
                appendSvgRoundRect(sb, ox, oy, outerSize, outerSize, outerSize * 0.36f)
                appendSvgRoundRect(sb, ox + cell, oy + cell, innerSize, innerSize, innerSize * 0.32f)
            }
            QrEyeFrameStyle.LEAF_TL_BR -> {
                val cr1 = outerSize * r.coerceAtLeast(0.38f)
                val cr2 = innerSize * r.coerceAtLeast(0.34f)
                appendSvgSelectiveRoundRect(sb, ox, oy, outerSize, outerSize, cr1, tl = true, tr = false, br = true, bl = false)
                appendSvgSelectiveRoundRect(sb, ox + cell, oy + cell, innerSize, innerSize, cr2, tl = true, tr = false, br = true, bl = false)
            }
            QrEyeFrameStyle.LEAF_TR_BL -> {
                val cr1 = outerSize * r.coerceAtLeast(0.38f)
                val cr2 = innerSize * r.coerceAtLeast(0.34f)
                appendSvgSelectiveRoundRect(sb, ox, oy, outerSize, outerSize, cr1, tl = false, tr = true, br = false, bl = true)
                appendSvgSelectiveRoundRect(sb, ox + cell, oy + cell, innerSize, innerSize, cr2, tl = false, tr = true, br = false, bl = true)
            }
            QrEyeFrameStyle.THREE_ROUND -> {
                val cr1 = outerSize * r.coerceAtLeast(0.38f)
                val cr2 = innerSize * r.coerceAtLeast(0.34f)
                appendSvgSelectiveRoundRect(sb, ox, oy, outerSize, outerSize, cr1, tl = true, tr = true, br = false, bl = true)
                appendSvgSelectiveRoundRect(sb, ox + cell, oy + cell, innerSize, innerSize, cr2, tl = true, tr = true, br = false, bl = true)
            }
            QrEyeFrameStyle.ROUNDED, QrEyeFrameStyle.BRACKET, QrEyeFrameStyle.HEXAGON -> {
                appendSvgRoundRect(sb, ox, oy, outerSize, outerSize, outerSize * r)
                appendSvgRoundRect(sb, ox + cell, oy + cell, innerSize, innerSize, innerSize * r * 0.85f)
            }
        }
    }

    private fun appendSvgEyeBall(
        sb: StringBuilder,
        style: QrEyeBallStyle,
        ox: Float,
        oy: Float,
        size: Float,
        roundness: Float
    ) {
        val r = roundness.coerceIn(0f, 0.45f)
        when (style) {
            QrEyeBallStyle.SQUARE -> sb.append("M${fmt(ox)},${fmt(oy)}h${fmt(size)}v${fmt(size)}h${fmt(-size)}Z ")
            QrEyeBallStyle.ROUNDED -> appendSvgRoundRect(sb, ox, oy, size, size, size * r)
            QrEyeBallStyle.SQUIRCLE -> appendSvgRoundRect(sb, ox, oy, size, size, size * 0.36f)
            QrEyeBallStyle.CIRCLE -> appendSvgPixelShape(sb, QrPixelStyle.CIRCLE, ox, oy, size, 1f, 0f)
            QrEyeBallStyle.DIAMOND -> appendSvgPixelShape(sb, QrPixelStyle.DIAMOND, ox, oy, size, 1f, 0f)
            QrEyeBallStyle.STAR -> appendSvgPixelShape(sb, QrPixelStyle.STAR, ox, oy, size, 1.05f, 0f)
            QrEyeBallStyle.CROSS -> appendSvgPixelShape(sb, QrPixelStyle.CROSS, ox, oy, size, 0.96f, 0f)
            QrEyeBallStyle.OCTAGON -> appendSvgPixelShape(sb, QrPixelStyle.OCTAGON, ox, oy, size, 1f, 0f)
            QrEyeBallStyle.HEXAGON -> appendSvgPixelShape(sb, QrPixelStyle.HEXAGON, ox, oy, size, 1f, 0f)
            QrEyeBallStyle.LEAF -> appendSvgSelectiveRoundRect(
                sb, ox, oy, size, size, size * r.coerceAtLeast(0.42f),
                tl = true, tr = false, br = true, bl = false
            )
        }
    }

    private fun appendSvgRoundRect(sb: StringBuilder, x: Float, y: Float, w: Float, h: Float, radius: Float) {
        val r = radius.coerceIn(0f, min(w, h) / 2f)
        if (r <= 0.1f) {
            sb.append("M${fmt(x)},${fmt(y)}h${fmt(w)}v${fmt(h)}h${fmt(-w)}Z ")
            return
        }
        appendSvgSelectiveRoundRect(sb, x, y, w, h, r, tl = true, tr = true, br = true, bl = true)
    }

    private fun appendSvgSelectiveRoundRect(
        sb: StringBuilder,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        tl: Boolean,
        tr: Boolean,
        br: Boolean,
        bl: Boolean
    ) {
        val maxR = min(w, h) / 2f
        val rTL = if (tl) radius.coerceIn(0f, maxR) else 0f
        val rTR = if (tr) radius.coerceIn(0f, maxR) else 0f
        val rBR = if (br) radius.coerceIn(0f, maxR) else 0f
        val rBL = if (bl) radius.coerceIn(0f, maxR) else 0f
        sb.append("M${fmt(x + rTL)},${fmt(y)}")
        sb.append("L${fmt(x + w - rTR)},${fmt(y)}")
        if (rTR > 0f) sb.append("A${fmt(rTR)},${fmt(rTR)} 0 0 1 ${fmt(x + w)},${fmt(y + rTR)}")
        sb.append("L${fmt(x + w)},${fmt(y + h - rBR)}")
        if (rBR > 0f) sb.append("A${fmt(rBR)},${fmt(rBR)} 0 0 1 ${fmt(x + w - rBR)},${fmt(y + h)}")
        sb.append("L${fmt(x + rBL)},${fmt(y + h)}")
        if (rBL > 0f) sb.append("A${fmt(rBL)},${fmt(rBL)} 0 0 1 ${fmt(x)},${fmt(y + h - rBL)}")
        sb.append("L${fmt(x)},${fmt(y + rTL)}")
        if (rTL > 0f) sb.append("A${fmt(rTL)},${fmt(rTL)} 0 0 1 ${fmt(x + rTL)},${fmt(y)}")
        sb.append("Z ")
    }

    private fun appendSvgChamfer(sb: StringBuilder, x: Float, y: Float, size: Float, cut: Float) {
        sb.append("M${fmt(x + cut)},${fmt(y)}")
        sb.append("L${fmt(x + size - cut)},${fmt(y)}")
        sb.append("L${fmt(x + size)},${fmt(y + cut)}")
        sb.append("L${fmt(x + size)},${fmt(y + size - cut)}")
        sb.append("L${fmt(x + size - cut)},${fmt(y + size)}")
        sb.append("L${fmt(x + cut)},${fmt(y + size)}")
        sb.append("L${fmt(x)},${fmt(y + size - cut)}")
        sb.append("L${fmt(x)},${fmt(y + cut)}Z ")
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

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
