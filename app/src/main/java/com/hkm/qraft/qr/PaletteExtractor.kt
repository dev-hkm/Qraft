package com.hkm.qraft.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ExtractedBrandPalette(
    val primaryDarkArgb: Long,
    val secondaryAccentArgb: Long,
    val lightCanvasArgb: Long,
    val frameAccentArgb: Long,
    val contrastOnCanvas: Double
)

/**
 * Extracts harmonious, high-contrast brand colors from an uploaded logo or background photo.
 * Ensures the primary QR foreground achieves WCAG AA/scannable luminance contrast against the canvas.
 */
object PaletteExtractor {

    private data class ColorBucket(
        var count: Int = 0,
        var weightSum: Float = 0f,
        var rSum: Long = 0,
        var gSum: Long = 0,
        var bSum: Long = 0
    ) {
        fun averageColor(): Int = AndroidColor.rgb(
            (rSum / count).toInt().coerceIn(0, 255),
            (gSum / count).toInt().coerceIn(0, 255),
            (bSum / count).toInt().coerceIn(0, 255)
        )
    }

    fun extractFromUri(context: Context, uri: Uri): ExtractedBrandPalette? {
        val bitmap = LogoCatalog.loadSampledUri(context, uri, maxDim = 160) ?: return null
        return try {
            extractFromBitmap(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    fun extractFromBitmap(bitmap: Bitmap): ExtractedBrandPalette {
        val targetMax = 48
        val sampled = if (bitmap.width > targetMax || bitmap.height > targetMax) {
            val scale = targetMax.toFloat() / max(bitmap.width, bitmap.height).toFloat()
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).roundToInt().coerceAtLeast(1),
                (bitmap.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        val clusters = try {
            val buckets = mutableMapOf<Int, ColorBucket>()
            val w = sampled.width
            val h = sampled.height
            val pixels = IntArray(w * h)
            sampled.getPixels(pixels, 0, w, 0, 0, w, h)
            val hsv = FloatArray(3)

            for (px in pixels) {
                val alpha = AndroidColor.alpha(px)
                if (alpha < 120) continue
                val r = AndroidColor.red(px)
                val g = AndroidColor.green(px)
                val b = AndroidColor.blue(px)
                AndroidColor.RGBToHSV(r, g, b, hsv)
                val sat = hsv[1]
                val brightness = hsv[2]
                // Skip near-pure-white highlight paper background
                if (brightness > 0.94f && sat < 0.10f) continue

                val key = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
                val weight = 1f + sat * 2.4f + if (brightness in 0.14f..0.82f) 0.5f else 0f
                val bucket = buckets.getOrPut(key) { ColorBucket() }
                bucket.count += 1
                bucket.weightSum += weight
                bucket.rSum += r
                bucket.gSum += g
                bucket.bSum += b
            }

            buckets.values
                .sortedByDescending { it.weightSum }
                .map { it.averageColor() }
        } finally {
            if (sampled !== bitmap) sampled.recycle()
        }

        if (clusters.isEmpty()) {
            return ExtractedBrandPalette(
                primaryDarkArgb = 0xFF1F1D1B,
                secondaryAccentArgb = 0xFFEA580C,
                lightCanvasArgb = 0xFFFFFFFF,
                frameAccentArgb = 0xFF5A3320,
                contrastOnCanvas = QrEngine.contrastRatio(0xFF1F1D1B, 0xFFFFFFFF)
            )
        }

        // Pick a rich chromatic primary if the top cluster is nearly neutral black/gray
        val topChromatic = clusters.firstOrNull { colorSaturation(it) > 0.24f } ?: clusters.first()
        val rawPrimary = topChromatic
        val rawSecondary = clusters.firstOrNull { candidate ->
            colorDistance(candidate, rawPrimary) > 78 && colorSaturation(candidate) > 0.18f
        } ?: clusters.firstOrNull { candidate ->
            colorDistance(candidate, rawPrimary) > 55
        } ?: shiftHue(rawPrimary, 28f)

        val lightCanvas = buildTintedPaperCanvas(rawPrimary)
        val lightCanvasArgb = lightCanvas.toUnsignedArgbLong()

        val primaryDark = darkenUntilScannable(rawPrimary, lightCanvasArgb, minRatio = 5.4)
        val secondaryDark = darkenUntilScannable(rawSecondary, lightCanvasArgb, minRatio = 4.5)
        val frameAccent = saturateAndDeepenForPill(rawSecondary)

        val primaryLong = primaryDark.toUnsignedArgbLong()
        return ExtractedBrandPalette(
            primaryDarkArgb = primaryLong,
            secondaryAccentArgb = secondaryDark.toUnsignedArgbLong(),
            lightCanvasArgb = lightCanvasArgb,
            frameAccentArgb = frameAccent.toUnsignedArgbLong(),
            contrastOnCanvas = QrEngine.contrastRatio(primaryLong, lightCanvasArgb)
        )
    }

    private fun buildTintedPaperCanvas(seedColor: Int): Int {
        // Blend 94% crisp white with 6% seed hue for cohesive Apple editorial warmth
        val r = (255 * 0.94f + AndroidColor.red(seedColor) * 0.06f).roundToInt().coerceIn(240, 255)
        val g = (255 * 0.94f + AndroidColor.green(seedColor) * 0.06f).roundToInt().coerceIn(240, 255)
        val b = (255 * 0.94f + AndroidColor.blue(seedColor) * 0.06f).roundToInt().coerceIn(240, 255)
        return AndroidColor.rgb(r, g, b)
    }

    private fun darkenUntilScannable(color: Int, backgroundArgb: Long, minRatio: Double): Int {
        var current = color
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(current, hsv)
        if (hsv[2] > 0.65f) {
            hsv[2] = 0.62f
            current = AndroidColor.HSVToColor(hsv)
        }
        var guard = 0
        while (QrEngine.contrastRatio(current.toUnsignedArgbLong(), backgroundArgb) < minRatio && guard < 24) {
            hsv[2] = (hsv[2] * 0.84f).coerceAtLeast(0.06f)
            hsv[1] = min(1f, hsv[1] * 1.04f)
            current = AndroidColor.HSVToColor(hsv)
            guard++
        }
        return if (QrEngine.contrastRatio(current.toUnsignedArgbLong(), backgroundArgb) < minRatio) {
            AndroidColor.rgb(0x1C, 0x1A, 0x18)
        } else {
            current
        }
    }

    private fun saturateAndDeepenForPill(color: Int): Int {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color, hsv)
        hsv[1] = min(0.92f, max(hsv[1], 0.42f))
        hsv[2] = hsv[2].coerceIn(0.32f, 0.72f)
        return AndroidColor.HSVToColor(hsv)
    }

    private fun shiftHue(color: Int, deltaHue: Float): Int {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + deltaHue) % 360f
        hsv[1] = max(hsv[1], 0.55f)
        hsv[2] = hsv[2].coerceIn(0.28f, 0.62f)
        return AndroidColor.HSVToColor(hsv)
    }

    private fun colorSaturation(color: Int): Float {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color, hsv)
        return hsv[1]
    }

    private fun colorDistance(a: Int, b: Int): Int {
        return abs(AndroidColor.red(a) - AndroidColor.red(b)) +
            abs(AndroidColor.green(a) - AndroidColor.green(b)) +
            abs(AndroidColor.blue(a) - AndroidColor.blue(b))
    }

    private fun Int.toUnsignedArgbLong(): Long = this.toLong() and 0xFFFFFFFFL
}
