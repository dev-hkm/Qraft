package com.hkm.qraft.qr

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
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
            QrPixelStyle.OCTAGON -> pixelModifier { size, _ ->
                addPolygon(this, size, factor, sides = 8, rotationOffsetDeg = 22.5f)
            }
            QrPixelStyle.RING -> pixelModifier { size, _ -> addHollowRing(this, size, factor) }
            QrPixelStyle.WAVE_H -> pixelModifier { size, neighbors ->
                addConnectedPill(this, size, factor, horizontal = true, neighbors = neighbors)
            }
            QrPixelStyle.WAVE_V -> pixelModifier { size, neighbors ->
                addConnectedPill(this, size, factor, horizontal = false, neighbors = neighbors)
            }
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
            QrEyeFrameStyle.DOUBLE_RING -> frameModifier { size, _ ->
                // Exact ISO 1:1:3:1:1 outer circular ring + 4 non-overlapping diagonal orbital studs
                val ringT = size / 6.8f
                addOval(Rect(0f, 0f, size, size), Path.Direction.Clockwise)
                addOval(Rect(ringT, ringT, size - ringT, size - ringT), Path.Direction.CounterClockwise)
                val dotR = size * 0.052f
                val corner = size * 0.085f
                listOf(
                    corner to corner,
                    (size - corner) to corner,
                    corner to (size - corner),
                    (size - corner) to (size - corner)
                ).forEach { (dx, dy) ->
                    addOval(Rect(dx - dotR, dy - dotR, dx + dotR, dy + dotR), Path.Direction.Clockwise)
                }
            }
            QrEyeFrameStyle.BRACKET -> frameModifier { size, _ ->
                // Architectural caliper frame: exact 1:1:3:1:1 thickness at 0.50f centerlines with dual symmetrical caliper notches
                val t = size / 7f
                val notch = t * 0.26f
                val a1 = size * 0.26f
                val a2 = size * 0.40f
                val b1 = size * 0.60f
                val b2 = size * 0.74f
                moveTo(0f, 0f)
                lineTo(a1, 0f); lineTo(a1, notch); lineTo(a2, notch); lineTo(a2, 0f)
                lineTo(b1, 0f); lineTo(b1, notch); lineTo(b2, notch); lineTo(b2, 0f)
                lineTo(size, 0f)
                lineTo(size, a1); lineTo(size - notch, a1); lineTo(size - notch, a2); lineTo(size, a2)
                lineTo(size, b1); lineTo(size - notch, b1); lineTo(size - notch, b2); lineTo(size, b2)
                lineTo(size, size)
                lineTo(b2, size); lineTo(b2, size - notch); lineTo(b1, size - notch); lineTo(b1, size)
                lineTo(a2, size); lineTo(a2, size - notch); lineTo(a1, size - notch); lineTo(a1, size)
                lineTo(0f, size)
                lineTo(0f, b2); lineTo(notch, b2); lineTo(notch, b1); lineTo(0f, b1)
                lineTo(0f, a2); lineTo(notch, a2); lineTo(notch, a1); lineTo(0f, a1)
                close()
                val ir = t * 0.30f
                addRoundRect(
                    RoundRect(Rect(t, t, size - t, size - t), CornerRadius(ir, ir)),
                    Path.Direction.CounterClockwise
                )
            }
            QrEyeFrameStyle.HEXAGON -> frameModifier { size, _ ->
                val ringThickness = size / 6.8f
                addPolygon(this, size, 1.02f, sides = 6, rotationOffsetDeg = -30f)
                addPolygonCcw(this, size, (size - 2f * ringThickness) / size * 1.02f, sides = 6, rotationOffsetDeg = -30f)
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
            QrEyeBallStyle.OCTAGON -> ballModifier { size, _ ->
                addPolygon(this, size, 1.0f, sides = 8, rotationOffsetDeg = 22.5f)
            }
            QrEyeBallStyle.HEXAGON -> ballModifier { size, _ ->
                addPolygon(this, size, 1.0f, sides = 6, rotationOffsetDeg = -30f)
            }
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
            QrCodeSilhouette.CIRCLE -> ScannableSilhouetteShape(
                delegate = QrCodeShape.circle(padding = 1.32f),
                moatModules = 3
            )
            QrCodeSilhouette.HEXAGON -> ScannableSilhouetteShape(
                delegate = QrCodeShape.hexagon(rotation = 0f),
                moatModules = 2
            )
        }
    }

    private class ScannableSilhouetteShape(
        private val delegate: QrCodeShape,
        private val moatModules: Int
    ) : QrCodeShape {
        override val shapeSizeIncrease: Float
            get() = delegate.shapeSizeIncrease

        override fun QrCodeMatrix.transform(): QrCodeMatrix {
            val out = with(delegate) { this@transform.transform() }
            val coreSize = this.size
            val total = out.size
            val offset = (total - coreSize) / 2
            if (offset <= 0) return out

            val minClear = (offset - moatModules).coerceAtLeast(0)
            val maxClear = (offset + coreSize + moatModules).coerceAtMost(total)
            for (x in minClear until maxClear) {
                for (y in minClear until maxClear) {
                    val insideCore = x in offset until (offset + coreSize) &&
                        y in offset until (offset + coreSize)
                    if (!insideCore) {
                        out[x, y] = QrCodeMatrix.PixelType.Background
                    }
                }
            }
            return out
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
        val outerR = (size / 2f) * (factor * 1.14f).coerceIn(0.45f, 1.18f)
        val innerR = outerR * 0.72f
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
        // Non-overlapping single closed Swiss cross polygon (EvenOdd-safe, bold center for ZXing!)
        val scaled = size * factor.coerceAtLeast(0.90f)
        val pad = (size - scaled) / 2f
        val arm = scaled * 0.62f
        val inset = (scaled - arm) / 2f
        val x0 = pad
        val x1 = pad + inset
        val x2 = pad + inset + arm
        val x3 = pad + scaled
        val y0 = pad
        val y1 = pad + inset
        val y2 = pad + inset + arm
        val y3 = pad + scaled
        path.moveTo(x1, y0)
        path.lineTo(x2, y0)
        path.lineTo(x2, y1)
        path.lineTo(x3, y1)
        path.lineTo(x3, y2)
        path.lineTo(x2, y2)
        path.lineTo(x2, y3)
        path.lineTo(x1, y3)
        path.lineTo(x1, y2)
        path.lineTo(x0, y2)
        path.lineTo(x0, y1)
        path.lineTo(x1, y1)
        path.close()
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

    private fun addHollowRing(path: Path, size: Float, factor: Float) {
        // Bullseye ring: bold outer ring CW + hairline orbital moat CCW + 72% solid core CW so ZXing center-sampling sees full ink contrast at 512px.
        val scaled = size * factor.coerceIn(0.86f, 1.0f)
        val pad = (size - scaled) / 2f
        val innerPad = pad + scaled * 0.08f
        val corePad = pad + scaled * 0.14f
        path.addOval(Rect(pad, pad, size - pad, size - pad), Path.Direction.Clockwise)
        path.addOval(Rect(innerPad, innerPad, size - innerPad, size - innerPad), Path.Direction.CounterClockwise)
        path.addOval(Rect(corePad, corePad, size - corePad, size - corePad), Path.Direction.Clockwise)
    }

    private fun addConnectedPill(path: Path, size: Float, factor: Float, horizontal: Boolean, neighbors: Neighbors) {
        val thickness = size * factor
        val inset = (size - thickness) / 2f
        val r = thickness * 0.48f
        if (horizontal) {
            val left = if (neighbors.left) 0f else inset * 0.45f
            val right = if (neighbors.right) size else size - inset * 0.45f
            path.addRoundRect(
                RoundRect(
                    Rect(left, inset, right, size - inset),
                    CornerRadius(if (neighbors.left) 0f else r, if (neighbors.left) 0f else r)
                )
            )
        } else {
            val top = if (neighbors.top) 0f else inset * 0.45f
            val bottom = if (neighbors.bottom) size else size - inset * 0.45f
            path.addRoundRect(
                RoundRect(
                    Rect(inset, top, size - inset, bottom),
                    CornerRadius(r, r)
                )
            )
        }
    }

    private fun addPolygonCcw(
        path: Path,
        size: Float,
        factor: Float,
        sides: Int,
        rotationOffsetDeg: Float
    ) {
        val cx = size / 2f
        val cy = size / 2f
        val r = (size / 2f) * factor
        for (i in sides - 1 downTo 0) {
            val angle = (rotationOffsetDeg + i * (360f / sides)) * PI / 180.0
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == sides - 1) path.moveTo(x, y) else path.lineTo(x, y)
        }
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
