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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.qraft.model.*
import com.hkm.qraft.qr.LogoCatalog
import com.hkm.qraft.qr.QrEngine
import com.hkm.qraft.ui.theme.AppleSprings
import com.hkm.qraft.ui.theme.QraftThemeTokens
import io.github.alexzhirkevich.qrose.QrCodePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    val frameBorderModifier = when {
        frame.style == OuterFrameStyle.GRADIENT_BORDER -> Modifier.border(
            width = 4.dp,
            brush = Brush.linearGradient(
                listOf(Color(frame.accentColor), Color(frame.accentSecondaryColor))
            ),
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
                modifier = Modifier
                    .then(frameBorderModifier)
                    .padding(0.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = if (
                            frame.style == OuterFrameStyle.CLEAN_CARD ||
                            frame.style == OuterFrameStyle.GRADIENT_BORDER
                        ) 16.dp else 14.dp
                    )
                ) {
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

                    Box(contentAlignment = Alignment.Center) {
                        QrCoreBox(
                            qrPainter = qrPainter,
                            design = design,
                            bgPhotoBitmap = bgPhotoBitmap,
                            size = qrViewportSize,
                            cornerRadius = (frameCorner - 8.dp).coerceAtLeast(10.dp),
                            showCheckerboard = showCheckerboardForTransparent
                        )
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

                    when (frame.style) {
                        OuterFrameStyle.BOTTOM_SCAN_BANNER,
                        OuterFrameStyle.SPEECH_BUBBLE,
                        OuterFrameStyle.WARM_POSTER,
                        OuterFrameStyle.DOUBLE_PILL,
                        OuterFrameStyle.GLASS_CARD,
                        OuterFrameStyle.CORNER_BRACKET -> {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(frame.accentColor),
                                                if (frame.style == OuterFrameStyle.WARM_POSTER) {
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
 * Custom flat-vector hero illustration matching the warm peach/orange/olive-green aesthetic
 * of the attached reference screenshot.
 */
@Composable
fun HeroQrArtworkIllustration(
    modifier: Modifier = Modifier
) {
    val colors = QraftThemeTokens.colors
    val cardFill = if (colors.isDark) Color(0xFF472C1B) else Color(0xFFFFFBF7)
    val orangeMain = Color(0xFFF16D22)
    val orangeDeep = if (colors.isDark) Color(0xFFFF9B5A) else Color(0xFF5C2E0B)
    val sageAccent = Color(0xFF83AB32)
    val softPeach = if (colors.isDark) Color(0xFF633B24) else Color(0xFFFAB589)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Motion & sparkle rays around the card (mirroring the bell vibration arcs in the screenshot!)
        drawArc(
            color = sageAccent,
            startAngle = -55f,
            sweepAngle = 50f,
            useCenter = false,
            topLeft = Offset(w * 0.66f, h * 0.08f),
            size = Size(w * 0.28f, h * 0.34f),
            style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
        )
        drawArc(
            color = sageAccent,
            startAngle = 140f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = Offset(w * 0.02f, h * 0.50f),
            size = Size(w * 0.26f, h * 0.32f),
            style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
        )

        // Small decorative confetti dots
        drawCircle(color = orangeMain, radius = w * 0.038f, center = Offset(w * 0.16f, h * 0.18f))
        drawCircle(color = softPeach, radius = w * 0.045f, center = Offset(w * 0.86f, h * 0.78f))
        drawCircle(color = sageAccent, radius = w * 0.032f, center = Offset(w * 0.50f, h * 0.90f))

        // Tilted card base
        val cardLeft = w * 0.18f
        val cardTop = h * 0.16f
        val cardSize = w * 0.64f
        drawRoundRect(
            color = softPeach,
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

        // Stylized finder eyes inside the card
        val eyeSize = cardSize * 0.28f
        val pad = cardSize * 0.14f
        val e1 = Offset(cardLeft + pad, cardTop + pad)
        val e2 = Offset(cardLeft + cardSize - pad - eyeSize, cardTop + pad)
        val e3 = Offset(cardLeft + pad, cardTop + cardSize - pad - eyeSize)

        listOf(e1 to orangeMain, e2 to orangeDeep, e3 to orangeDeep).forEach { (pos, tint) ->
            drawRoundRect(
                color = tint,
                topLeft = pos,
                size = Size(eyeSize, eyeSize),
                cornerRadius = CornerRadius(eyeSize * 0.28f, eyeSize * 0.28f),
                style = Stroke(width = eyeSize * 0.20f)
            )
            drawCircle(
                color = tint,
                radius = eyeSize * 0.18f,
                center = Offset(pos.x + eyeSize / 2f, pos.y + eyeSize / 2f)
            )
        }

        // Bottom-right colorful data dots
        val dotBaseX = e2.x + eyeSize * 0.22f
        val dotBaseY = e3.y + eyeSize * 0.22f
        val step = eyeSize * 0.56f
        val r = eyeSize * 0.20f
        drawCircle(color = orangeMain, radius = r, center = Offset(dotBaseX, dotBaseY))
        drawCircle(color = sageAccent, radius = r, center = Offset(dotBaseX + step, dotBaseY))
        drawCircle(color = orangeDeep, radius = r, center = Offset(dotBaseX, dotBaseY + step))
        drawCircle(color = orangeMain, radius = r, center = Offset(dotBaseX + step, dotBaseY + step))
    }
}
