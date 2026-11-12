package com.hkm.qraft.qr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.hkm.qraft.model.ExportImageFormat
import com.hkm.qraft.model.QrDesignSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ExportResult(
    val success: Boolean,
    val message: String,
    val uri: Uri? = null
)

object QrExporter {

    suspend fun saveToDevice(
        context: Context,
        title: String,
        data: String,
        design: QrDesignSpec,
        format: ExportImageFormat
    ): ExportResult = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = sanitizeFilename(title)
            val fileName = "${cleanName}_${System.currentTimeMillis()}.${format.extension}"

            when (format) {
                ExportImageFormat.PNG,
                ExportImageFormat.JPEG,
                ExportImageFormat.WEBP -> {
                    val bitmap = QrEngine.renderFramedExportBitmap(
                        context = context,
                        data = data,
                        design = design,
                        targetWidthPx = design.exportSizePx,
                        forceOpaqueWhiteForJpeg = format == ExportImageFormat.JPEG
                    )
                    val uri = try {
                        saveBitmapToGallery(context, bitmap, fileName, format)
                    } finally {
                        bitmap.recycle()
                    }
                    ExportResult(
                        success = uri != null,
                        message = if (uri != null) {
                            "Saved ${design.exportSizePx}px ${format.extension.uppercase()} to Pictures/Qraft"
                        } else {
                            "Could not save image"
                        },
                        uri = uri
                    )
                }
                ExportImageFormat.SVG -> {
                    val svg = QrEngine.exportSvg(data, design)
                    val uri = saveDocumentBytes(context, svg.toByteArray(Charsets.UTF_8), fileName, format.mimeType)
                    ExportResult(
                        success = uri != null,
                        message = if (uri != null) "Saved SVG vector to Documents/Qraft" else "Could not save SVG",
                        uri = uri
                    )
                }
                ExportImageFormat.PDF -> {
                    val pdfBytes = renderPdfBytes(context, title, data, design)
                    val uri = saveDocumentBytes(context, pdfBytes, fileName, format.mimeType)
                    ExportResult(
                        success = uri != null,
                        message = if (uri != null) "Saved print PDF to Documents/Qraft" else "Could not save PDF",
                        uri = uri
                    )
                }
            }
        }.getOrElse { err ->
            ExportResult(false, "Export failed: ${err.localizedMessage ?: "unknown error"}")
        }
    }

    suspend fun shareQr(
        context: Context,
        title: String,
        data: String,
        design: QrDesignSpec,
        format: ExportImageFormat
    ): ExportResult = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = "${sanitizeFilename(title)}.${format.extension}"
            val outFile = File(dir, fileName)

            when (format) {
                ExportImageFormat.PNG,
                ExportImageFormat.JPEG,
                ExportImageFormat.WEBP -> {
                    val bitmap = QrEngine.renderFramedExportBitmap(
                        context = context,
                        data = data,
                        design = design,
                        targetWidthPx = design.exportSizePx,
                        forceOpaqueWhiteForJpeg = format == ExportImageFormat.JPEG
                    )
                    try {
                        FileOutputStream(outFile).use { fos ->
                            bitmap.compress(compressFormatFor(format), 96, fos)
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
                ExportImageFormat.SVG -> {
                    outFile.writeText(QrEngine.exportSvg(data, design))
                }
                ExportImageFormat.PDF -> {
                    outFile.writeBytes(renderPdfBytes(context, title, data, design))
                }
            }

            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = format.mimeType
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share $title (${format.extension.uppercase()})")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
            ExportResult(true, "Opened share sheet", shareUri)
        }.getOrElse { err ->
            ExportResult(false, "Share failed: ${err.localizedMessage ?: "unknown error"}")
        }
    }

    fun copyPayloadToClipboard(context: Context, label: String, payload: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, payload))
    }

    suspend fun copyImageToClipboard(
        context: Context,
        title: String,
        data: String,
        design: QrDesignSpec
    ): ExportResult = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(dir, "clipboard_${sanitizeFilename(title)}.png")
            val clipWidth = design.exportSizePx.coerceIn(512, 2048)
            val bitmap = QrEngine.renderFramedExportBitmap(
                context = context,
                data = data,
                design = design,
                targetWidthPx = clipWidth,
                forceOpaqueWhiteForJpeg = false
            )
            try {
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
            } finally {
                bitmap.recycle()
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            withContext(Dispatchers.Main) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newUri(context.contentResolver, title.ifBlank { "Qraft QR" }, uri))
            }
            ExportResult(true, "Copied QR image (${clipWidth}px) to clipboard", uri)
        }.getOrElse { err ->
            ExportResult(false, "Copy image failed: ${err.localizedMessage ?: "unknown error"}")
        }
    }

    fun renderPdfBytes(
        context: Context,
        title: String,
        data: String,
        design: QrDesignSpec
    ): ByteArray {
        return QrPdfWriter.writeSinglePage {
            renderSinglePrintPageBitmap(context, title, data, design)
        }
    }

    fun renderSinglePrintPageBitmap(
        context: Context,
        title: String,
        data: String,
        design: QrDesignSpec
    ): Bitmap {
        val pageW = QrPdfWriter.A4_WIDTH_PX
        val pageH = QrPdfWriter.A4_HEIGHT_PX
        val pageBitmap = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(pageBitmap)
        canvas.drawColor(0xFFFAF7F4.toInt())

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E1D1B.toInt()
            textAlign = Paint.Align.CENTER
            textSize = 92f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF7B736C.toInt()
            textAlign = Paint.Align.CENTER
            textSize = 42f
        }

        canvas.drawText(title.ifBlank { "Qraft Studio QR" }, pageW / 2f, 340f, titlePaint)
        canvas.drawText(data.take(80), pageW / 2f, 430f, subtitlePaint)

        val qrBmp = QrEngine.renderFramedExportBitmap(
            context = context,
            data = data,
            design = design,
            targetWidthPx = 1920,
            forceOpaqueWhiteForJpeg = false
        )
        val availableH = 2380f
        val drawScale = minOf(1f, availableH / qrBmp.height.toFloat())
        val drawW = qrBmp.width * drawScale
        val drawH = qrBmp.height * drawScale
        val left = (pageW - drawW) / 2f
        val top = 560f + ((availableH - drawH).coerceAtLeast(0f) / 2f)
        val dst = RectF(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(qrBmp, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        qrBmp.recycle()

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8B837B.toInt()
            textAlign = Paint.Align.CENTER
            textSize = 36f
        }
        canvas.drawText(
            "300 DPI Print Ready • Qraft QR Studio",
            pageW / 2f,
            pageH - 160f,
            footerPaint
        )
        return pageBitmap
    }

    private fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        format: ExportImageFormat
    ): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Qraft")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { os ->
                bitmap.compress(compressFormatFor(format), 96, os)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } else {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "Qraft"
            ).apply { mkdirs() }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { os ->
                bitmap.compress(compressFormatFor(format), 96, os)
            }
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        }
    }

    private fun saveDocumentBytes(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/Qraft")
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return null
            resolver.openOutputStream(uri)?.use { os -> os.write(bytes) }
            values.clear()
            values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } else {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "Qraft"
            ).apply { mkdirs() }
            val outFile = File(dir, fileName)
            outFile.writeBytes(bytes)
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        }
    }

    private fun compressFormatFor(format: ExportImageFormat): Bitmap.CompressFormat {
        return when (format) {
            ExportImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ExportImageFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            else -> Bitmap.CompressFormat.PNG
        }
    }

    private fun sanitizeFilename(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_-]+"), "_").trim('_')
        return cleaned.take(32).ifBlank { "qraft_qr" }
    }
}
