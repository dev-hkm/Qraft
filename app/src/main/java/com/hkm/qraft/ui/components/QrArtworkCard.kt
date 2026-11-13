package com.hkm.qraft.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.qraft.model.*
import com.hkm.qraft.qr.BarcodeEngine
import com.hkm.qraft.qr.LogoCatalog
import com.hkm.qraft.qr.QrEngine
import com.hkm.qraft.ui.theme.AppleSprings
import com.hkm.qraft.ui.theme.QraftThemeTokens
import io.github.alexzhirkevich.qrose.QrCodePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun QrArtworkPreview(
    data: String,
    design: QrDesignSpec,
    modifier: Modifier = Modifier,
    qrViewportSize: Dp = 212.dp,
    showCheckerboardForTransparent: Boolean = true
) {
    val context = LocalContext.current
    val safeData = data.ifBlank { "https://qraft.studio" }

    val logoBitmap by produceState<Bitmap?>(initialValue = null, key1 = design.logo) {
        value = withContext(Dispatchers.Default) {
            LogoCatalog.renderLogoBitmap(context, design.logo, outputPx = 280)
        }
    }

    val bgPhotoBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = design.background.mode,
        key2 = design.background.photoUri
    ) {
        value = if (design.background.mode == BackgroundMode.PHOTO && !design.background.photoUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                LogoCatalog.loadSampledUri(context, Uri.parse(design.background.photoUri), 600)
            }
        } else {
            null
        }
    }

    val logoPainter = remember(logoBitmap) {
        logoBitmap?.let { BitmapPainter(it.asImageBitmap()) }
    }

    val options = remember(design, logoPainter) {
        QrEngine.buildQrOptions(design, logoPainter)
    }

    val qrPainter = remember(safeData, options) {
        QrCodePainter(data = safeData, options = options)
    }

    val frame = design.outerFrame
    val frameCorner = frame.cornerRadiusDp.dp
    val framePad = (frame.effectivePaddingFraction() * 190f).dp

    val frameBorderModifier = when {
        frame.style == OuterFrameStyle.GRADIENT_BORDER -> Modifier.border(
            width = 4.dp,
            brush = Brush.linearGradient(
                listOf(Color(frame.accentColor), Color(frame.accentSecondaryColor))
            ),
            shape = RoundedCornerShape(frameCorner)
        )
        frame.style == OuterFrameStyle.NEON_GLOW -> Modifier.border(
            width = 2.5.dp,
            brush = Brush.linearGradient(
                listOf(Color(frame.accentColor), Color(frame.accentSecondaryColor))
            ),
            shape = RoundedCornerShape(frameCorner)
        )
        frame.style == OuterFrameStyle.MINIMAL_OUTLINE -> Modifier.border(
            width = 1.2.dp,
            color = Color(frame.accentColor),
            shape = RoundedCornerShape(frameCorner)
        )
        frame.style == OuterFrameStyle.GLASS_CARD -> Modifier.border(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.78f),
                    Color(frame.accentColor).copy(alpha = 0.34f)
                )
            ),
            shape = RoundedCornerShape(frameCorner)
        )
        frame.borderThicknessDp > 0f && frame.style != OuterFrameStyle.NONE -> Modifier.border(
            width = frame.borderThicknessDp.dp,
            color = Color(frame.accentColor),
            shape = RoundedCornerShape(frameCorner)
        )
        else -> Modifier
    }

    val label = frame.label.trim().ifBlank { "SCAN ME" }

    Box(
        modifier = modifier.padding(bottom = if (frame.style == OuterFrameStyle.SPEECH_BUBBLE) 12.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (frame.style == OuterFrameStyle.NONE) {
            QrCoreBox(
                qrPainter = qrPainter,
                design = design,
                bgPhotoBitmap = bgPhotoBitmap,
                size = qrViewportSize,
                cornerRadius = 18.dp,
                showCheckerboard = showCheckerboardForTransparent
            )
        } else {
            Surface(
                shape = RoundedCornerShape(frameCorner),
                color = if (frame.style == OuterFrameStyle.GLASS_CARD) {
                    Color(frame.frameFillColor).copy(alpha = 0.88f)
                } else {
                    Color(frame.frameFillColor)
                },
                shadowElevation = if (QraftThemeTokens.colors.isDark) 4.dp else 10.dp,
                modifier = Modifier.then(frameBorderModifier)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(
                        horizontal = if (frame.style == OuterFrameStyle.SCAN_BAR_HEADER) 0.dp else framePad,
                        vertical = if (frame.style == OuterFrameStyle.SCAN_BAR_HEADER) 0.dp else framePad
                    )
                ) {
                    if (frame.style == OuterFrameStyle.SCAN_BAR_HEADER) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(qrViewportSize + framePad * 2)
                                .background(Color(frame.accentColor))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                color = Color(frame.textColor),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(framePad))
                    }

                    if (frame.style == OuterFrameStyle.TOP_BADGE_CARD || frame.style == OuterFrameStyle.DOUBLE_PILL) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(frame.accentColor))
                                .padding(horizontal = 18.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = Color(frame.textColor),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(
                            horizontal = if (frame.style == OuterFrameStyle.SCAN_BAR_HEADER) framePad else 0.dp
                        )
                    ) {
                        QrCoreBox(
                            qrPainter = qrPainter,
                            design = design,
                            bgPhotoBitmap = bgPhotoBitmap,
                            size = qrViewportSize,
                            cornerRadius = if (frame.style == OuterFrameStyle.CIRCULAR_STAMP) {
                                qrViewportSize / 2
                            } else {
                                (frameCorner - 8.dp).coerceAtLeast(10.dp)
                            },
                            showCheckerboard = showCheckerboardForTransparent
                        )
                        if (frame.style == OuterFrameStyle.CIRCULAR_STAMP) {
                            Canvas(modifier = Modifier.size(qrViewportSize + 12.dp)) {
                                drawCircle(
                                    color = Color(frame.accentColor),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }
                        if (frame.style == OuterFrameStyle.CORNER_BRACKET) {
                            Canvas(modifier = Modifier.size(qrViewportSize + 10.dp)) {
                                val sw = 3.5.dp.toPx()
                                val leg = 20.dp.toPx()
                                val c = Color(frame.accentColor)
                                val r = size.width
                                val b = size.height
                                val p = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, leg); lineTo(0f, 0f); lineTo(leg, 0f)
                                    moveTo(r - leg, 0f); lineTo(r, 0f); lineTo(r, leg)
                                    moveTo(0f, b - leg); lineTo(0f, b); lineTo(leg, b)
                                    moveTo(r - leg, b); lineTo(r, b); lineTo(r, b - leg)
                                }
                                drawPath(
                                    path = p,
                                    color = c,
                                    style = Stroke(
                                        width = sw,
                                        cap = StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }

                    if (frame.style == OuterFrameStyle.SCAN_BAR_HEADER) {
                        Spacer(Modifier.height(framePad))
                    }

                    when (frame.style) {
                        OuterFrameStyle.TICKET_STUB -> {
                            Spacer(Modifier.height(10.dp))
                            Canvas(
                                modifier = Modifier
                                    .width(qrViewportSize)
                                    .height(2.dp)
                            ) {
                                drawLine(
                                    color = Color(frame.accentColor).copy(alpha = 0.45f),
                                    start = Offset(0f, size.height / 2f),
                                    end = Offset(size.width, size.height / 2f),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(14f, 10f),
                                        0f
                                    )
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(frame.accentColor))
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = Color(frame.textColor),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (frame.subtitle.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = frame.subtitle.trim(),
                                    color = Color(frame.accentColor),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        OuterFrameStyle.MINIMAL_OUTLINE -> {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = label,
                                color = Color(frame.accentColor),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OuterFrameStyle.BOTTOM_SCAN_BANNER,
                        OuterFrameStyle.SPEECH_BUBBLE,
                        OuterFrameStyle.WARM_POSTER,
                        OuterFrameStyle.DOUBLE_PILL,
                        OuterFrameStyle.GLASS_CARD,
                        OuterFrameStyle.CORNER_BRACKET,
                        OuterFrameStyle.NEON_GLOW,
                        OuterFrameStyle.CIRCULAR_STAMP -> {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(frame.accentColor),
                                                if (frame.style == OuterFrameStyle.WARM_POSTER || frame.style == OuterFrameStyle.NEON_GLOW) {
                                                    Color(frame.accentSecondaryColor)
                                                } else {
                                                    Color(frame.accentColor)
                                                }
                                            )
                                        )
                                    )
                                    .padding(horizontal = 20.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = if (frame.style == OuterFrameStyle.DOUBLE_PILL && frame.subtitle.isNotBlank()) {
                                        frame.subtitle.trim()
                                    } else {
                                        label
                                    },
                                    color = Color(frame.textColor),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (frame.style == OuterFrameStyle.WARM_POSTER && frame.subtitle.isNotBlank()) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = frame.subtitle.trim(),
                                    color = Color(frame.accentColor),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        OuterFrameStyle.POLAROID_STAMP -> {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = label,
                                color = Color(frame.accentColor),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.6.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> Unit
                    }
                }
            }
            if (frame.style == OuterFrameStyle.SPEECH_BUBBLE) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 10.dp)
                        .size(width = 24.dp, height = 12.dp)
                ) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                        close()
                    }
                    drawPath(path, color = Color(frame.frameFillColor))
                }
            }
        }
    }
}

@Composable
fun BarcodeArtworkPreview(
    spec: BarcodeDesignSpec,
    modifier: Modifier = Modifier,
    widthDp: Dp = 248.dp,
    showCheckerboardForTransparent: Boolean = true
) {
    val encoded = remember(spec.format, spec.data) {
        BarcodeEngine.encodeMatrix(spec)
    }
    val matrix = encoded.first
    val validation = encoded.second
    val isSquare2D = spec.format == BarcodeFormatType.AZTEC || spec.format == BarcodeFormatType.DATA_MATRIX
    val barHeight = when {
        isSquare2D -> widthDp * 0.72f
        spec.format == BarcodeFormatType.PDF_417 -> widthDp * 0.44f
        else -> widthDp * 0.36f
    }

    val coreCard: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (spec.transparentBg) {
                        Modifier
                    } else {
                        Modifier.background(Color(spec.backgroundColor))
                    }
                )
                .padding(horizontal = (widthDp * spec.quietMarginFraction.coerceIn(0.03f, 0.14f)), vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (spec.transparentBg && showCheckerboardForTransparent) {
                CheckerboardBackground(modifier = Modifier.matchParentSize())
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (matrix != null) {
                    Canvas(
                        modifier = Modifier
                            .width(if (isSquare2D) barHeight else widthDp - 36.dp)
                            .height(barHeight)
                    ) {
                        val cols = matrix.width
                        val rows = matrix.height
                        val barColor = Color(spec.foregroundColor)
                        if (!spec.format.is2D) {
                            val barW = size.width / cols.toFloat()
                            val midRow = rows / 2
                            var x = 0
                            while (x < cols) {
                                if (matrix.get(x, midRow)) {
                                    var end = x + 1
                                    while (end < cols && matrix.get(end, midRow)) end++
                                    val bx = x * barW
                                    val bw = (end - x) * barW
                                    if (spec.roundedBars) {
                                        val r = min(bw * 0.48f, size.height * 0.08f)
                                        drawRoundRect(
                                            color = barColor,
                                            topLeft = Offset(bx, 0f),
                                            size = Size(bw, size.height),
                                            cornerRadius = CornerRadius(r, r)
                                        )
                                    } else {
                                        drawRect(
                                            color = barColor,
                                            topLeft = Offset(bx, 0f),
                                            size = Size(bw + 0.4f, size.height)
                                        )
                                    }
                                    x = end
                                } else {
                                    x++
                                }
                            }
                        } else {
                            val boxW = if (isSquare2D) min(size.width, size.height) else size.width
                            val boxH = if (isSquare2D) boxW else size.height
                            val ox = (size.width - boxW) / 2f
                            val oy = (size.height - boxH) / 2f
                            val cellW = boxW / cols.toFloat()
                            val cellH = boxH / rows.toFloat()
                            val r = if (spec.roundedBars) min(cellW, cellH) * 0.34f else 0f
                            for (row in 0 until rows) {
                                for (col in 0 until cols) {
                                    if (matrix.get(col, row)) {
                                        val mx = ox + col * cellW
                                        val my = oy + row * cellH
                                        if (r > 0.5f) {
                                            drawRoundRect(
                                                color = barColor,
                                                topLeft = Offset(mx, my),
                                                size = Size(cellW, cellH),
                                                cornerRadius = CornerRadius(r, r)
                                            )
                                        } else {
                                            drawRect(
                                                color = barColor,
                                                topLeft = Offset(mx, my),
                                                size = Size(cellW + 0.4f, cellH + 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (spec.showText) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (validation.isValid) validation.normalizedData else spec.data.ifBlank { spec.format.defaultSample },
                        color = Color(spec.foregroundColor),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (spec.framed) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(spec.cornerRadiusDp.dp),
            color = Color(spec.frameFillColor),
            shadowElevation = if (QraftThemeTokens.colors.isDark) 4.dp else 10.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(14.dp)
            ) {
                if (spec.caption.isNotBlank()) {
                    Text(
                        text = spec.caption.trim(),
                        color = Color(spec.frameAccentColor),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                coreCard()
            }
        }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            coreCard()
        }
    }
}

@Composable
private fun QrCoreBox(
    qrPainter: QrCodePainter,
    design: QrDesignSpec,
    bgPhotoBitmap: Bitmap?,
    size: Dp,
    cornerRadius: Dp,
    showCheckerboard: Boolean
) {
    val quietPad = (size * design.quietZonePadding.coerceIn(0f, 0.24f))
    val shadowModifier = if (design.matrixShadowEnabled) {
        Modifier.shadow(elevation = 14.dp, shape = RoundedCornerShape(cornerRadius), clip = false)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(size)
            .then(shadowModifier)
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        when (design.background.mode) {
            BackgroundMode.TRANSPARENT -> {
                if (showCheckerboard) {
                    CheckerboardBackground(modifier = Modifier.fillMaxSize())
                }
            }
            BackgroundMode.SOLID -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(design.background.brush.primaryColor))
                )
            }
            BackgroundMode.GRADIENT -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(brush = QrEngine.buildComposeBrush(design.background.brush, this.size.minDimension))
                }
            }
            BackgroundMode.PHOTO -> {
                if (bgPhotoBitmap != null) {
                    Image(
                        bitmap = bgPhotoBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(design.background.photoOverlayColor)
                                .copy(alpha = design.background.photoOverlayAlpha.coerceIn(0f, 0.95f))
                        )
                )
            }
        }

        AnimatedContent(
            targetState = qrPainter,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.97f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.98f))
            },
            label = "qrMorph"
        ) { painter ->
            Image(
                painter = painter,
                contentDescription = "Live customized QR code",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(quietPad)
            )
        }
    }
}

@Composable
fun CheckerboardBackground(
    modifier: Modifier = Modifier,
    tileDp: Dp = 10.dp
) {
    val lightTile = if (QraftThemeTokens.colors.isDark) Color(0xFF282420) else Color(0xFFF3EFEA)
    val darkTile = if (QraftThemeTokens.colors.isDark) Color(0xFF1F1C19) else Color(0xFFE4DDD5)
    Canvas(modifier = modifier) {
        val tile = tileDp.toPx().coerceAtLeast(6f)
        val cols = (size.width / tile).roundToInt() + 1
        val rows = (size.height / tile).roundToInt() + 1
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                drawRect(
                    color = if ((r + c) % 2 == 0) lightTile else darkTile,
                    topLeft = Offset(c * tile, r * tile),
                    size = Size(tile, tile)
                )
            }
        }
    }
}

/**
 * Editorial monochrome vector QR mark matching the white/black/graphite aesthetic.
 */
@Composable
fun HeroQrArtworkIllustration(
    modifier: Modifier = Modifier
) {
    val colors = QraftThemeTokens.colors
    val cardFill = if (colors.isDark) Color(0xFF242429) else Color(0xFFFFFFFF)
    val shadowTone = if (colors.isDark) Color(0xFF0E0E10) else Color(0xFFDCDCE2)
    val primaryInk = if (colors.isDark) Color(0xFFFFFFFF) else Color(0xFF111113)
    val secondaryInk = if (colors.isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val subtleAccent = if (colors.isDark) Color(0xFF52525B) else Color(0xFFB4B4BE)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawArc(
            color = subtleAccent,
            startAngle = -55f,
            sweepAngle = 50f,
            useCenter = false,
            topLeft = Offset(w * 0.66f, h * 0.08f),
            size = Size(w * 0.28f, h * 0.34f),
            style = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
        )
        drawArc(
            color = subtleAccent,
            startAngle = 140f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = Offset(w * 0.02f, h * 0.50f),
            size = Size(w * 0.26f, h * 0.32f),
            style = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
        )

        drawCircle(color = primaryInk, radius = w * 0.032f, center = Offset(w * 0.16f, h * 0.18f))
        drawCircle(color = subtleAccent, radius = w * 0.038f, center = Offset(w * 0.86f, h * 0.78f))
        drawCircle(color = secondaryInk, radius = w * 0.028f, center = Offset(w * 0.50f, h * 0.90f))

        val cardLeft = w * 0.18f
        val cardTop = h * 0.16f
        val cardSize = w * 0.64f
        drawRoundRect(
            color = shadowTone,
            topLeft = Offset(cardLeft + w * 0.04f, cardTop + h * 0.05f),
            size = Size(cardSize, cardSize),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )
        drawRoundRect(
            color = cardFill,
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardSize, cardSize),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )

        val eyeSize = cardSize * 0.28f
        val pad = cardSize * 0.14f
        val e1 = Offset(cardLeft + pad, cardTop + pad)
        val e2 = Offset(cardLeft + cardSize - pad - eyeSize, cardTop + pad)
        val e3 = Offset(cardLeft + pad, cardTop + cardSize - pad - eyeSize)

        listOf(e1 to primaryInk, e2 to primaryInk, e3 to secondaryInk).forEach { (pos, tint) ->
            drawCircle(
                color = tint,
                radius = eyeSize * 0.44f,
                center = Offset(pos.x + eyeSize / 2f, pos.y + eyeSize / 2f),
                style = Stroke(width = eyeSize * 0.19f)
            )
            drawCircle(
                color = tint,
                radius = eyeSize * 0.17f,
                center = Offset(pos.x + eyeSize / 2f, pos.y + eyeSize / 2f)
            )
        }

        val dotBaseX = e2.x + eyeSize * 0.22f
        val dotBaseY = e3.y + eyeSize * 0.22f
        val step = eyeSize * 0.56f
        val r = eyeSize * 0.19f
        drawCircle(color = primaryInk, radius = r, center = Offset(dotBaseX, dotBaseY))
        drawCircle(color = subtleAccent, radius = r, center = Offset(dotBaseX + step, dotBaseY))
        drawCircle(color = secondaryInk, radius = r, center = Offset(dotBaseX, dotBaseY + step))
        drawCircle(color = primaryInk, radius = r * 1.15f, center = Offset(dotBaseX + step, dotBaseY + step))
    }
}
