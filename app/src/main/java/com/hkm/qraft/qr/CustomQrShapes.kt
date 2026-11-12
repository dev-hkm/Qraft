package com.hkm.qraft.qr

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import com.hkm.qraft.model.*
import io.github.alexzhirkevich.qrose.Neighbors
import io.github.alexzhirkevich.qrose.options.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CustomQrShapes {

    private inline fun pixelModifier(
        crossinline block: Path.(size: Float, neighbors: Neighbors) -> Unit
    ): QrPixelShape = object : QrPixelShape {
        override fun Path.path(size: Float, neighbors: Neighbors): Path {
            block(size, neighbors)
            return this
        }
    }

    private inline fun frameModifier(
        crossinline block: Path.(size: Float, neighbors: Neighbors) -> Unit
    ): QrFrameShape = object : QrFrameShape {
        override fun Path.path(size: Float, neighbors: Neighbors): Path {
            block(size, neighbors)
            return this
        }
    }

    private inline fun ballModifier(
        crossinline block: Path.(size: Float, neighbors: Neighbors) -> Unit
    ): QrBallShape = object : QrBallShape {
        override fun Path.path(size: Float, neighbors: Neighbors): Path {
            block(size, neighbors)
            return this
        }
    }

    private inline fun logoModifier(
        crossinline block: Path.(size: Float, neighbors: Neighbors) -> Unit
    ): QrLogoShape = object : QrLogoShape {
        override fun Path.path(size: Float, neighbors: Neighbors): Path {
            block(size, neighbors)
            return this
        }
    }

    fun pixelShape(style: QrPixelStyle, roundness: Float, sizeFactor: Float): QrPixelShape {
        val factor = sizeFactor.coerceIn(0.45f, 1.0f)
        val radius = roundness.coerceIn(0f, 0.5f)
        return when (style) {
            QrPixelStyle.SQUARE -> QrPixelShape.square(factor)
            QrPixelStyle.ROUNDED -> pixelModifier { size, _ ->
                val scaled = size * factor
                val pad = (size - scaled) / 2f
                val r = scaled * radius
                addRoundRect(
                    RoundRect(
                        Rect(pad, pad, pad + scaled, pad + scaled),
                        CornerRadius(r, r)
                    )
                )
            }
            QrPixelStyle.CIRCLE -> QrPixelShape.circle(factor)
            QrPixelStyle.LIQUID -> QrPixelShape.roundCorners(radius.coerceAtLeast(0.36f))
            QrPixelStyle.VERTICAL_BARS -> QrPixelShape.verticalLines(factor)
            QrPixelStyle.HORIZONTAL_BARS -> QrPixelShape.horizontalLines(factor)
            QrPixelStyle.DIAMOND -> pixelModifier { size, _ -> addDiamond(this, size, factor) }
            QrPixelStyle.STAR -> pixelModifier { size, _ -> addSparkleStar(this, size, factor) }
            QrPixelStyle.CROSS -> pixelModifier { size, _ -> addRoundedPlus(this, size, factor) }
            QrPixelStyle.HEART -> pixelModifier { size, _ -> addHeart(this, size, factor) }
            QrPixelStyle.HEXAGON -> pixelModifier { size, _ ->
                addPolygon(this, size, factor, sides = 6, rotationOffsetDeg = -30f)
            }
            QrPixelStyle.SHIELD -> pixelModifier { size, _ -> addShield(this, size, factor) }
        }
    }

    fun lightPixelShape(style: QrLightPixelStyle): QrPixelShape? {
        return when (style) {
            QrLightPixelStyle.NONE -> null
            QrLightPixelStyle.DOT -> QrPixelShape.circle(0.38f)
            QrLightPixelStyle.ROUNDED -> QrPixelShape.roundCorners(0.3f)
            QrLightPixelStyle.SQUARE -> QrPixelShape.square(0.55f)
        }
    }

    fun frameShape(style: QrEyeFrameStyle, roundness: Float): QrFrameShape {
        val r = roundness.coerceIn(0f, 0.5f)
        return when (style) {
            QrEyeFrameStyle.SQUARE -> QrFrameShape.square()
            QrEyeFrameStyle.ROUNDED -> QrFrameShape.roundCorners(r)
            QrEyeFrameStyle.CIRCLE -> QrFrameShape.circle()
            QrEyeFrameStyle.LEAF_TL_BR -> QrFrameShape.roundCorners(
                corner = r.coerceAtLeast(0.38f),
                topLeft = true,
                bottomLeft = false,
                topRight = false,
                bottomRight = true
            )
            QrEyeFrameStyle.LEAF_TR_BL -> QrFrameShape.roundCorners(
                corner = r.coerceAtLeast(0.38f),
                topLeft = false,
                bottomLeft = true,
                topRight = true,
                bottomRight = false
            )
            QrEyeFrameStyle.THREE_ROUND -> QrFrameShape.roundCorners(
                corner = r.coerceAtLeast(0.38f),
                topLeft = true,
                bottomLeft = true,
                topRight = true,
                bottomRight = false
            )
            QrEyeFrameStyle.CHAMFER -> frameModifier { size, _ ->
                val ringThickness = size / 7f
                addChamferRect(this, 0f, size, size * 0.18f)
                addChamferRectCcw(this, ringThickness, size - ringThickness, (size - 2 * ringThickness) * 0.16f)
            }
            QrEyeFrameStyle.SQUIRCLE -> frameModifier { size, _ ->
                val ringThickness = size / 7f
                val outerR = size * 0.36f
                val innerR = (size - 2 * ringThickness) * 0.32f
                addRoundRect(
                    RoundRect(Rect(0f, 0f, size, size), CornerRadius(outerR, outerR)),
                    Path.Direction.Clockwise
                )
                addRoundRect(
                    RoundRect(
                        Rect(ringThickness, ringThickness, size - ringThickness, size - ringThickness),
                        CornerRadius(innerR, innerR)
                    ),
                    Path.Direction.CounterClockwise
                )
            }
        }
    }

    fun ballShape(style: QrEyeBallStyle, roundness: Float): QrBallShape {
        val r = roundness.coerceIn(0f, 0.5f)
        return when (style) {
            QrEyeBallStyle.SQUARE -> QrBallShape.square()
            QrEyeBallStyle.ROUNDED -> QrBallShape.roundCorners(r)
            QrEyeBallStyle.CIRCLE -> QrBallShape.circle()
            QrEyeBallStyle.DIAMOND -> ballModifier { size, _ -> addDiamond(this, size, 1.0f) }
            QrEyeBallStyle.LEAF -> QrBallShape.roundCorners(
                radius = r.coerceAtLeast(0.42f),
                topLeft = true,
                bottomLeft = false,
                topRight = false,
                bottomRight = true
            )
            QrEyeBallStyle.STAR -> ballModifier { size, _ -> addSparkleStar(this, size, 1.05f) }
            QrEyeBallStyle.SQUIRCLE -> QrBallShape.roundCorners(0.36f)
            QrEyeBallStyle.CROSS -> ballModifier { size, _ -> addRoundedPlus(this, size, 0.96f) }
        }
    }

    fun logoShape(style: LogoClipShape): QrLogoShape {
        return when (style) {
            LogoClipShape.CIRCLE -> QrLogoShape.circle()
            LogoClipShape.ROUNDED -> QrLogoShape.roundCorners(0.26f)
            LogoClipShape.SQUARE -> QrLogoShape.rect(1f, 0f)
            LogoClipShape.SQUIRCLE -> QrLogoShape.roundCorners(0.38f)
            LogoClipShape.DIAMOND -> logoModifier { size, _ -> addDiamond(this, size, 1.0f) }
        }
    }

    fun codeShape(silhouette: QrCodeSilhouette): QrCodeShape {
        return when (silhouette) {
            QrCodeSilhouette.SQUARE -> QrCodeShape.Default
            QrCodeSilhouette.CIRCLE -> QrCodeShape.circle(padding = 1.08f)
            QrCodeSilhouette.HEXAGON -> QrCodeShape.hexagon(rotation = 0f)
        }
    }

    private fun addDiamond(path: Path, size: Float, factor: Float) {
        val half = size / 2f
        val r = half * factor.coerceIn(0.3f, 1.15f)
        path.moveTo(half, half - r)
        path.lineTo(half + r, half)
        path.lineTo(half, half + r)
        path.lineTo(half - r, half)
        path.close()
    }

    private fun addSparkleStar(path: Path, size: Float, factor: Float) {
        val cx = size / 2f
        val cy = size / 2f
        val outerR = (size / 2f) * factor.coerceIn(0.4f, 1.15f)
        val innerR = outerR * 0.65f
        val points = 5
        for (i in 0 until points * 2) {
            val angle = (-90.0 + i * (180.0 / points)) * PI / 180.0
            val rad = if (i % 2 == 0) outerR else innerR
            val x = cx + (rad * cos(angle)).toFloat()
            val y = cy + (rad * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun addRoundedPlus(path: Path, size: Float, factor: Float) {
        val scaled = size * factor
        val pad = (size - scaled) / 2f
        val arm = scaled * 0.36f
        val armOffset = (scaled - arm) / 2f
        val r = arm * 0.35f
        path.addRoundRect(
            RoundRect(
                Rect(pad + armOffset, pad, pad + armOffset + arm, pad + scaled),
                CornerRadius(r, r)
            )
        )
        path.addRoundRect(
            RoundRect(
                Rect(pad, pad + armOffset, pad + scaled, pad + armOffset + arm),
                CornerRadius(r, r)
            )
        )
    }

    private fun addHeart(path: Path, size: Float, factor: Float) {
        val scaled = size * factor
        val ox = (size - scaled) / 2f
        val oy = (size - scaled) / 2f
        val w = scaled
        val h = scaled
        path.moveTo(ox + w * 0.5f, oy + h * 0.90f)
        path.cubicTo(
            ox + w * 0.08f, oy + h * 0.62f,
            ox + w * 0.02f, oy + h * 0.25f,
            ox + w * 0.27f, oy + h * 0.14f
        )
        path.cubicTo(
            ox + w * 0.41f, oy + h * 0.08f,
            ox + w * 0.50f, oy + h * 0.22f,
            ox + w * 0.50f, oy + h * 0.29f
        )
        path.cubicTo(
            ox + w * 0.50f, oy + h * 0.22f,
            ox + w * 0.59f, oy + h * 0.08f,
            ox + w * 0.73f, oy + h * 0.14f
        )
        path.cubicTo(
            ox + w * 0.98f, oy + h * 0.25f,
            ox + w * 0.92f, oy + h * 0.62f,
            ox + w * 0.50f, oy + h * 0.90f
        )
        path.close()
    }

    private fun addPolygon(
        path: Path,
        size: Float,
        factor: Float,
        sides: Int,
        rotationOffsetDeg: Float
    ) {
        val cx = size / 2f
        val cy = size / 2f
        val r = (size / 2f) * factor
        for (i in 0 until sides) {
            val angle = (rotationOffsetDeg + i * (360f / sides)) * PI / 180.0
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun addShield(path: Path, size: Float, factor: Float) {
        val s = size * factor
        val ox = (size - s) / 2f
        val oy = (size - s) / 2f
        path.moveTo(ox + s * 0.12f, oy + s * 0.12f)
        path.lineTo(ox + s * 0.88f, oy + s * 0.12f)
        path.lineTo(ox + s * 0.88f, oy + s * 0.52f)
        path.quadraticTo(ox + s * 0.88f, oy + s * 0.80f, ox + s * 0.50f, oy + s * 0.94f)
        path.quadraticTo(ox + s * 0.12f, oy + s * 0.80f, ox + s * 0.12f, oy + s * 0.52f)
        path.close()
    }

    private fun addChamferRect(path: Path, min: Float, max: Float, cut: Float) {
        path.moveTo(min + cut, min)
        path.lineTo(max - cut, min)
        path.lineTo(max, min + cut)
        path.lineTo(max, max - cut)
        path.lineTo(max - cut, max)
        path.lineTo(min + cut, max)
        path.lineTo(min, max - cut)
        path.lineTo(min, min + cut)
        path.close()
    }

    private fun addChamferRectCcw(path: Path, min: Float, max: Float, cut: Float) {
        path.moveTo(min + cut, min)
        path.lineTo(min, min + cut)
        path.lineTo(min, max - cut)
        path.lineTo(min + cut, max)
        path.lineTo(max - cut, max)
        path.lineTo(max, max - cut)
        path.lineTo(max, min + cut)
        path.lineTo(max - cut, min)
        path.close()
    }
}
