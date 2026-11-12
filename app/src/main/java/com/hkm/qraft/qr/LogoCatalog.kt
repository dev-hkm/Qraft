package com.hkm.qraft.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.hkm.qraft.model.LogoClipShape
import com.hkm.qraft.model.LogoSourceType
import com.hkm.qraft.model.LogoStyleConfig
import kotlin.math.min

data class LogoIconChoice(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object LogoCatalog {
    val icons: List<LogoIconChoice> = listOf(
        LogoIconChoice("qr", "QR Mark", Icons.Rounded.QrCode2),
        LogoIconChoice("wifi", "Wi-Fi", Icons.Rounded.Wifi),
        LogoIconChoice("link", "Link", Icons.Rounded.Link),
        LogoIconChoice("globe", "Web", Icons.Rounded.Language),
        LogoIconChoice("card", "Contact", Icons.Rounded.Badge),
        LogoIconChoice("mail", "Email", Icons.Rounded.Email),
        LogoIconChoice("phone", "Phone", Icons.Rounded.Phone),
        LogoIconChoice("location", "Location", Icons.Rounded.LocationOn),
        LogoIconChoice("coffee", "Cafe", Icons.Rounded.Coffee),
        LogoIconChoice("shop", "Store", Icons.Rounded.Storefront),
        LogoIconChoice("bag", "Shop", Icons.Rounded.ShoppingBag),
        LogoIconChoice("pay", "Payment", Icons.Rounded.Payments),
        LogoIconChoice("heart", "Heart", Icons.Rounded.Favorite),
        LogoIconChoice("star", "Star", Icons.Rounded.Star),
        LogoIconChoice("spark", "Sparkle", Icons.Rounded.AutoAwesome),
        LogoIconChoice("verified", "Verified", Icons.Rounded.Verified),
        LogoIconChoice("camera", "Camera", Icons.Rounded.PhotoCamera),
        LogoIconChoice("music", "Music", Icons.Rounded.MusicNote),
        LogoIconChoice("event", "Event", Icons.Rounded.Event),
        LogoIconChoice("lock", "Secure", Icons.Rounded.Lock),
        LogoIconChoice("code", "Code", Icons.Rounded.Code),
        LogoIconChoice("gift", "Gift", Icons.Rounded.CardGiftcard),
        LogoIconChoice("rocket", "Launch", Icons.Rounded.RocketLaunch),
        LogoIconChoice("home", "Home", Icons.Rounded.Home)
    )

    val popularEmojis: List<String> = listOf(
        "⚡", "☕", "✨", "🔥", "💎", "🎵",
        "📍", "🎁", "🚀", "🌿", "🧡", "🛍️",
        "📱", "💳", "🎉", "🔒", "📸", "⭐"
    )

    fun iconForKey(key: String): ImageVector =
        icons.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.QrCode2

    fun renderLogoBitmap(
        context: Context,
        config: LogoStyleConfig,
        outputPx: Int = 360
    ): Bitmap? {
        if (config.sourceType == LogoSourceType.NONE) return null

        val bitmap = Bitmap.createBitmap(outputPx, outputPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val size = outputPx.toFloat()

        val clipPath = buildClipAndroidPath(config.clipShape, size)
        canvas.save()
        canvas.clipPath(clipPath)

        // Backing fill
        if (config.backingEnabled) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = if (config.sourceType == LogoSourceType.MONOGRAM) {
                    config.monogramBgColor.toInt()
                } else {
                    config.backingColor.toInt()
                }
            }
            canvas.drawRect(0f, 0f, size, size, bgPaint)
        } else if (config.sourceType == LogoSourceType.MONOGRAM) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = config.monogramBgColor.toInt()
            }
            canvas.drawRect(0f, 0f, size, size, bgPaint)
        }

        when (config.sourceType) {
            LogoSourceType.GALLERY -> {
                val uriStr = config.galleryUri
                val photo = uriStr?.let { loadSampledUri(context, Uri.parse(it), outputPx) }
                if (photo != null) {
                    val inset = if (config.backingEnabled) size * 0.07f else 0f
                    val dest = RectF(inset, inset, size - inset, size - inset)
                    val src = centerCropRect(photo.width, photo.height)
                    val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    canvas.drawBitmap(photo, src, dest, bmpPaint)
                } else {
                    drawMonogram(canvas, "IMG", config.iconTintColor.toInt(), size)
                }
            }
            LogoSourceType.ICON -> {
                drawVectorIconOnCanvas(
                    canvas = canvas,
                    vector = iconForKey(config.iconKey),
                    tint = config.iconTintColor.toInt(),
                    size = size
                )
            }
            LogoSourceType.EMOJI -> {
                val emoji = config.emoji.ifBlank { "⚡" }
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    textSize = size * 0.56f
                }
                val fm = textPaint.fontMetrics
                val baseline = size / 2f - (fm.ascent + fm.descent) / 2f
                canvas.drawText(emoji, size / 2f, baseline, textPaint)
            }
            LogoSourceType.MONOGRAM -> {
                val text = config.monogramText.trim().take(4).ifBlank { "QR" }
                drawMonogram(canvas, text, config.monogramTextColor.toInt(), size)
            }
            LogoSourceType.NONE -> Unit
        }

        canvas.restore()

        if (config.borderEnabled) {
            val strokeW = (size * 0.045f).coerceAtLeast(3f)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                color = config.borderColor.toInt()
            }
            val inset = strokeW / 2f
            val borderPath = buildClipAndroidPath(config.clipShape, size - strokeW, offset = inset)
            canvas.drawPath(borderPath, borderPaint)
        }

        return bitmap
    }

    private fun drawMonogram(canvas: Canvas, text: String, color: Int, size: Float) {
        val factor = when (text.length) {
            1 -> 0.52f
            2 -> 0.42f
            3 -> 0.32f
            else -> 0.25f
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            textSize = size * factor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = -0.02f
        }
        val fm = paint.fontMetrics
        val y = size / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, size / 2f, y, paint)
    }

    private fun drawVectorIconOnCanvas(
        canvas: Canvas,
        vector: ImageVector,
        tint: Int,
        size: Float
    ) {
        val pad = size * 0.19f
        val inner = size - pad * 2f
        val sx = inner / vector.viewportWidth
        val sy = inner / vector.viewportHeight
        val scale = min(sx, sy)
        val dx = pad + (inner - vector.viewportWidth * scale) / 2f
        val dy = pad + (inner - vector.viewportHeight * scale) / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = tint
        }

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        drawVectorGroup(canvas, vector.root, paint)
        canvas.restore()
    }

    private fun drawVectorGroup(
        canvas: Canvas,
        group: androidx.compose.ui.graphics.vector.VectorGroup,
        paint: Paint
    ) {
        canvas.save()
        canvas.translate(group.translationX, group.translationY)
        canvas.rotate(group.rotation, group.pivotX, group.pivotY)
        canvas.scale(group.scaleX, group.scaleY, group.pivotX, group.pivotY)
        for (i in 0 until group.size) {
            when (val node = group[i]) {
                is androidx.compose.ui.graphics.vector.VectorGroup -> drawVectorGroup(canvas, node, paint)
                is androidx.compose.ui.graphics.vector.VectorPath -> {
                    val composePath = Path()
                    val parser = androidx.compose.ui.graphics.vector.PathParser()
                    parser.addPathNodes(node.pathData)
                    parser.toPath(composePath)
                    val androidPath = composePath.asAndroidPath()
                    androidPath.fillType = if (node.pathFillType == androidx.compose.ui.graphics.PathFillType.EvenOdd) {
                        android.graphics.Path.FillType.EVEN_ODD
                    } else {
                        android.graphics.Path.FillType.WINDING
                    }
                    canvas.drawPath(androidPath, paint)
                }
            }
        }
        canvas.restore()
    }

    private fun buildClipAndroidPath(
        clipShape: LogoClipShape,
        size: Float,
        offset: Float = 0f
    ): android.graphics.Path {
        val path = android.graphics.Path()
        val rect = RectF(offset, offset, offset + size, offset + size)
        when (clipShape) {
            LogoClipShape.CIRCLE -> {
                path.addOval(rect, android.graphics.Path.Direction.CW)
            }
            LogoClipShape.ROUNDED -> {
                val r = size * 0.24f
                path.addRoundRect(rect, r, r, android.graphics.Path.Direction.CW)
            }
            LogoClipShape.SQUIRCLE -> {
                val r = size * 0.36f
                path.addRoundRect(rect, r, r, android.graphics.Path.Direction.CW)
            }
            LogoClipShape.SQUARE -> {
                path.addRect(rect, android.graphics.Path.Direction.CW)
            }
            LogoClipShape.DIAMOND -> {
                val cx = offset + size / 2f
                val cy = offset + size / 2f
                val half = size / 2f
                path.moveTo(cx, cy - half)
                path.lineTo(cx + half, cy)
                path.lineTo(cx, cy + half)
                path.lineTo(cx - half, cy)
                path.close()
            }
        }
        return path
    }

    fun loadSampledUri(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
            var sample = 1
            while (boundsOpts.outWidth / sample > maxDim * 2 || boundsOpts.outHeight / sample > maxDim * 2) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            }
        }.getOrNull()
    }

    private fun centerCropRect(w: Int, h: Int): android.graphics.Rect {
        val side = min(w, h)
        val left = (w - side) / 2
        val top = (h - side) / 2
        return android.graphics.Rect(left, top, left + side, top + side)
    }
}
