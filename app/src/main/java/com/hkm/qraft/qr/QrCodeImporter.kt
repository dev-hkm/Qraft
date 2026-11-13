package com.hkm.qraft.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.hkm.qraft.model.AppPlatform
import com.hkm.qraft.model.PaymentMethod
import com.hkm.qraft.model.QrContentType
import com.hkm.qraft.model.QrPayloadFields
import com.hkm.qraft.model.SocialPlatform
import com.hkm.qraft.model.WifiSecurity
import java.net.URLDecoder

data class DecodedRawCode(
    val text: String,
    val format: BarcodeFormat
)

data class DecodedQrImport(
    val rawText: String,
    val type: QrContentType,
    val fields: QrPayloadFields,
    val suggestedTitle: String
)

/**
 * Decodes existing QR code & barcode images from Gallery/Shared images (including high-res 8K
 * framed exports) using multi-scale downsampling, center-ROI crops, inverted/binarizer passes,
 * and 90-degree rotations, then parses structured payloads back into editable Qraft fields.
 */
object QrCodeImporter {

    private val decodeHints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.ALSO_INVERTED to true,
        DecodeHintType.CHARACTER_SET to "UTF-8",
        DecodeHintType.POSSIBLE_FORMATS to listOf(
            BarcodeFormat.QR_CODE,
            BarcodeFormat.CODE_128,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A,
            BarcodeFormat.CODE_39,
            BarcodeFormat.ITF,
            BarcodeFormat.CODABAR,
            BarcodeFormat.PDF_417,
            BarcodeFormat.AZTEC,
            BarcodeFormat.DATA_MATRIX
        )
    )

    fun decodeFromUri(context: Context, uri: Uri): DecodedQrImport? {
        val raw = decodeFromUriAnyFormat(context, uri) ?: return null
        return parsePayloadToImport(raw.text)
    }

    fun decodeFromUriAnyFormat(context: Context, uri: Uri): DecodedRawCode? {
        for (targetMaxDim in intArrayOf(1400, 960, 640)) {
            val bitmap = try {
                LogoCatalog.loadSampledUri(context, uri, maxDim = targetMaxDim)
            } catch (_: OutOfMemoryError) {
                null
            } ?: continue
            try {
                decodeBitmapAnyFormat(bitmap)?.let { return it }
            } catch (_: OutOfMemoryError) {
                // Retry at next smaller sample dimension
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
        return null
    }

    fun decodeFromBitmap(bitmap: Bitmap): DecodedQrImport? {
        val raw = decodeBitmapAnyFormat(bitmap) ?: return null
        return parsePayloadToImport(raw.text)
    }

    fun decodeRawString(bitmap: Bitmap): String? = decodeBitmapAnyFormat(bitmap)?.text

    fun decodeBitmapAnyFormat(bitmap: Bitmap): DecodedRawCode? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null

        // 1. Direct pass on supplied bitmap
        decodeSinglePass(bitmap)?.let { return it }

        // 2. Multi-scale downsampling helps stylized dots / 4K / 8K PNGs fuse cleanly for ZXing
        val maxSide = maxOf(bitmap.width, bitmap.height)
        for (targetMax in intArrayOf(920, 620, 1280, 420)) {
            if (kotlin.math.abs(maxSide - targetMax) < 80) continue
            val scale = targetMax.toFloat() / maxSide.toFloat()
            val tw = (bitmap.width * scale).toInt().coerceAtLeast(160)
            val th = (bitmap.height * scale).toInt().coerceAtLeast(160)
            val scaled = try {
                Bitmap.createScaledBitmap(bitmap, tw, th, true)
            } catch (_: OutOfMemoryError) {
                null
            } ?: continue
            val result = try {
                decodeSinglePass(scaled)
            } finally {
                if (scaled !== bitmap && !scaled.isRecycled) scaled.recycle()
            }
            if (result != null) return result
        }

        // 3. Center & upper-center ROI crops (strips out ornate 8K outer frames & bottom "SCAN HERE" pills)
        if (bitmap.width >= 200 && bitmap.height >= 200) {
            val cropSpecs = listOf(
                Triple(0.09f, 0.06f, 0.82f), // removes outer frame border
                Triple(0.17f, 0.08f, 0.66f), // isolates QR area on bottom-banner frames
                Triple(0.17f, 0.17f, 0.66f)  // symmetric center 66%
            )
            for ((leftFrac, topFrac, boxFrac) in cropSpecs) {
                val cw = (bitmap.width * boxFrac).toInt().coerceIn(96, bitmap.width)
                val ch = (bitmap.height * boxFrac).toInt().coerceIn(96, bitmap.height)
                val cx = (bitmap.width * leftFrac).toInt().coerceIn(0, bitmap.width - cw)
                val cy = (bitmap.height * topFrac).toInt().coerceIn(0, bitmap.height - ch)
                val cropped = try {
                    Bitmap.createBitmap(bitmap, cx, cy, cw, ch)
                } catch (_: OutOfMemoryError) {
                    null
                } ?: continue
                val result = try {
                    decodeSinglePass(cropped) ?: if (maxOf(cw, ch) > 720) {
                        val s = 560f / maxOf(cw, ch).toFloat()
                        val smallCrop = Bitmap.createScaledBitmap(
                            cropped,
                            (cw * s).toInt().coerceAtLeast(120),
                            (ch * s).toInt().coerceAtLeast(120),
                            true
                        )
                        try {
                            decodeSinglePass(smallCrop)
                        } finally {
                            if (smallCrop !== cropped && !smallCrop.isRecycled) smallCrop.recycle()
                        }
                    } else null
                } finally {
                    if (cropped !== bitmap && !cropped.isRecycled) cropped.recycle()
                }
                if (result != null) return result
            }
        }

        // 4. Orthogonal rotations (useful for sideways gallery screenshots or 1D barcodes)
        val rotSource = if (maxSide > 1080) {
            val s = 920f / maxSide.toFloat()
            runCatching {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * s).toInt().coerceAtLeast(160),
                    (bitmap.height * s).toInt().coerceAtLeast(160),
                    true
                )
            }.getOrNull() ?: bitmap
        } else {
            bitmap
        }
        try {
            for (angle in floatArrayOf(90f, 180f, 270f)) {
                val matrix = Matrix().apply { postRotate(angle) }
                val rotated = try {
                    Bitmap.createBitmap(rotSource, 0, 0, rotSource.width, rotSource.height, matrix, true)
                } catch (_: OutOfMemoryError) {
                    null
                } ?: continue
                val result = try {
                    decodeSinglePass(rotated)
                } finally {
                    if (rotated !== rotSource && !rotated.isRecycled) rotated.recycle()
                }
                if (result != null) return result
            }
        } finally {
            if (rotSource !== bitmap && !rotSource.isRecycled) rotSource.recycle()
        }

        return null
    }

    private fun decodeSinglePass(bitmap: Bitmap): DecodedRawCode? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0 || bitmap.isRecycled) return null
        val pixels = try {
            IntArray(w * h)
        } catch (_: OutOfMemoryError) {
            return null
        }
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val baseSource = RGBLuminanceSource(w, h, pixels)
        val qrReader = QRCodeReader()
        val multiReader = MultiFormatReader()

        val sources = listOf(baseSource, baseSource.invert())
        for (source in sources) {
            for (binary in listOf(
                BinaryBitmap(HybridBinarizer(source)),
                BinaryBitmap(GlobalHistogramBinarizer(source))
            )) {
                try {
                    val res = qrReader.decode(binary, decodeHints)
                    if (!res.text.isNullOrEmpty()) {
                        return DecodedRawCode(res.text, BarcodeFormat.QR_CODE)
                    }
                } catch (_: Exception) {
                } finally {
                    qrReader.reset()
                }
                try {
                    val res = multiReader.decode(binary, decodeHints)
                    if (!res.text.isNullOrEmpty()) {
                        return DecodedRawCode(res.text, res.barcodeFormat)
                    }
                } catch (_: NotFoundException) {
                } catch (_: Exception) {
                } finally {
                    multiReader.reset()
                }
            }
        }
        return null
    }

    fun parsePayloadToImport(raw: String): DecodedQrImport {
        val trimmed = raw.trim()
        val (type, fields) = parsePayload(trimmed)
        return DecodedQrImport(
            rawText = trimmed,
            type = type,
            fields = fields,
            suggestedTitle = QrPayloadEncoder.defaultTitle(type, fields)
        )
    }

    fun parsePayload(raw: String): Pair<QrContentType, QrPayloadFields> {
        val text = raw.trim()
        if (text.startsWith("WIFI:", ignoreCase = true)) {
            return QrContentType.WIFI to parseWifi(text)
        }
        if (text.contains("BEGIN:VCARD", ignoreCase = true)) {
            return QrContentType.VCARD to parseVCard(text)
        }
        if (text.contains("BEGIN:VEVENT", ignoreCase = true)) {
            return QrContentType.EVENT to parseVEvent(text)
        }
        if (text.startsWith("geo:", ignoreCase = true)) {
            return QrContentType.GEO to parseGeo(text)
        }
        if (text.startsWith("mailto:", ignoreCase = true)) {
            return QrContentType.EMAIL to parseEmail(text)
        }
        if (text.startsWith("tel:", ignoreCase = true)) {
            return QrContentType.PHONE to QrPayloadFields(
                phoneNumber = urlDecode(text.substring(4)).trim()
            )
        }
        if (text.startsWith("smsto:", ignoreCase = true) || text.startsWith("sms:", ignoreCase = true)) {
            return QrContentType.SMS to parseSms(text)
        }
        parsePaymentOrNull(text)?.let { return QrContentType.PAYMENT to it }
        parseAppLinkOrNull(text)?.let { return QrContentType.APP_LINK to it }
        parseSocialOrNull(text)?.let { return QrContentType.SOCIAL to it }

        if (looksLikeUrl(text)) {
            val normalizedUrl = if (text.contains("://")) text else "https://$text"
            return QrContentType.URL to QrPayloadFields(url = normalizedUrl)
        }
        return QrContentType.TEXT to QrPayloadFields(text = text)
    }

    private fun parseWifi(raw: String): QrPayloadFields {
        val body = raw.substring(5)
        val tokens = splitEscapedSemicolons(body)
        var ssid = ""
        var password = ""
        var security = WifiSecurity.WPA
        var hidden = false
        for (token in tokens) {
            if (token.isBlank()) continue
            val colon = token.indexOf(':')
            if (colon <= 0) continue
            val key = token.substring(0, colon).uppercase()
            val value = unescapeWifi(token.substring(colon + 1))
            when (key) {
                "S" -> ssid = value
                "P" -> password = value
                "T" -> security = when {
                    value.equals("WEP", true) -> WifiSecurity.WEP
                    value.equals("nopass", true) || value.equals("NONE", true) || value.isBlank() -> WifiSecurity.NONE
                    else -> WifiSecurity.WPA
                }
                "H" -> hidden = value.equals("true", true) || value == "1"
            }
        }
        return QrPayloadFields(
            wifiSsid = ssid,
            wifiPassword = password,
            wifiSecurity = security,
            wifiHidden = hidden
        )
    }

    private fun splitEscapedSemicolons(input: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (ch in input) {
            if (escaped) {
                current.append('\\').append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == ';') {
                result += current.toString()
                current.clear()
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) result += current.toString()
        return result
    }

    private fun unescapeWifi(value: String): String {
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                sb.append(value[i + 1])
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun parseVCard(raw: String): QrPayloadFields {
        val unfolded = raw.replace("\r\n ", "").replace("\n ", "").replace("\r\n\t", "").replace("\n\t", "")
        var first = ""
        var last = ""
        var fullName = ""
        var org = ""
        var title = ""
        var phone = ""
        var email = ""
        var website = ""
        var street = ""
        var city = ""
        var country = ""
        var note = ""

        for (line in unfolded.lines()) {
            val idx = line.indexOf(':')
            if (idx <= 0) continue
            val keyPart = line.substring(0, idx).uppercase()
            val value = line.substring(idx + 1).trim()
            val baseKey = keyPart.substringBefore(';')
            when (baseKey) {
                "N" -> {
                    val parts = value.split(';')
                    last = parts.getOrNull(0)?.trim().orEmpty()
                    first = parts.getOrNull(1)?.trim().orEmpty()
                }
                "FN" -> fullName = value
                "ORG" -> org = value.replace(";", ", ").trim()
                "TITLE" -> title = value
                "TEL" -> if (phone.isBlank()) phone = value
                "EMAIL" -> if (email.isBlank()) email = value
                "URL" -> if (website.isBlank()) website = value
                "ADR" -> {
                    val parts = value.split(';')
                    street = parts.getOrNull(2)?.trim().orEmpty()
                    city = parts.getOrNull(3)?.trim().orEmpty()
                    country = parts.getOrNull(6)?.trim().orEmpty()
                }
                "NOTE" -> note = value
            }
        }
        if (first.isBlank() && last.isBlank() && fullName.isNotBlank()) {
            val split = fullName.trim().split(Regex("\\s+"), limit = 2)
            first = split.getOrNull(0).orEmpty()
            last = split.getOrNull(1).orEmpty()
        }
        return QrPayloadFields(
            firstName = first,
            lastName = last,
            organization = org,
            jobTitle = title,
            phone = phone,
            email = email,
            website = website,
            street = street,
            city = city,
            country = country,
            note = note
        )
    }

    private fun parseVEvent(raw: String): QrPayloadFields {
        var title = ""
        var location = ""
        var start = ""
        var end = ""
        var desc = ""
        for (line in raw.lines()) {
            val idx = line.indexOf(':')
            if (idx <= 0) continue
            val baseKey = line.substring(0, idx).substringBefore(';').uppercase()
            val value = line.substring(idx + 1).trim()
            when (baseKey) {
                "SUMMARY" -> title = value
                "LOCATION" -> location = value
                "DTSTART" -> start = value
                "DTEND" -> end = value
                "DESCRIPTION" -> desc = value
            }
        }
        return QrPayloadFields(
            eventTitle = title,
            eventLocation = location,
            eventStart = start,
            eventEnd = end,
            eventDescription = desc
        )
    }

    private fun parseGeo(raw: String): QrPayloadFields {
        val withoutScheme = raw.substring(4)
        val coordPart = withoutScheme.substringBefore('?')
        val queryPart = withoutScheme.substringAfter('?', "")
        val coords = coordPart.split(',')
        val lat = coords.getOrNull(0)?.trim().orEmpty()
        val lng = coords.getOrNull(1)?.trim().orEmpty()
        val labelMatch = Regex("\\(([^)]+)\\)").find(urlDecode(queryPart))
        val label = labelMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        return QrPayloadFields(latitude = lat, longitude = lng, geoLabel = label)
    }

    private fun parseEmail(raw: String): QrPayloadFields {
        val after = raw.substring(7)
        val to = urlDecode(after.substringBefore('?')).trim()
        val params = parseQueryParams(after.substringAfter('?', ""))
        return QrPayloadFields(
            emailTo = to,
            emailSubject = params["subject"].orEmpty(),
            emailBody = params["body"].orEmpty()
        )
    }

    private fun parseSms(raw: String): QrPayloadFields {
        val lower = raw.lowercase()
        val after = if (lower.startsWith("smsto:")) raw.substring(6) else raw.substring(4)
        return if (after.contains('?')) {
            val number = urlDecode(after.substringBefore('?')).trim()
            val params = parseQueryParams(after.substringAfter('?', ""))
            QrPayloadFields(smsNumber = number, smsMessage = params["body"].orEmpty())
        } else {
            val colon = after.indexOf(':')
            if (colon >= 0) {
                QrPayloadFields(
                    smsNumber = after.substring(0, colon).trim(),
                    smsMessage = after.substring(colon + 1).trim()
                )
            } else {
                QrPayloadFields(smsNumber = after.trim())
            }
        }
    }

    private fun parsePaymentOrNull(text: String): QrPayloadFields? {
        val lower = text.lowercase()
        return when {
            lower.startsWith("bitcoin:") -> {
                val after = text.substring(8)
                val target = after.substringBefore('?')
                val params = parseQueryParams(after.substringAfter('?', ""))
                QrPayloadFields(
                    paymentMethod = PaymentMethod.BITCOIN,
                    paymentTarget = target,
                    paymentAmount = params["amount"].orEmpty(),
                    paymentNote = params["message"].orEmpty()
                )
            }
            lower.startsWith("ethereum:") -> {
                val after = text.substring(9)
                val target = after.substringBefore('?')
                val params = parseQueryParams(after.substringAfter('?', ""))
                QrPayloadFields(
                    paymentMethod = PaymentMethod.ETHEREUM,
                    paymentTarget = target,
                    paymentAmount = params["value"] ?: params["amount"].orEmpty()
                )
            }
            lower.startsWith("solana:") -> {
                val after = text.substring(7)
                val target = after.substringBefore('?')
                val params = parseQueryParams(after.substringAfter('?', ""))
                QrPayloadFields(
                    paymentMethod = PaymentMethod.SOLANA,
                    paymentTarget = target,
                    paymentAmount = params["amount"].orEmpty(),
                    paymentNote = params["memo"].orEmpty()
                )
            }
            lower.startsWith("upi://pay") -> {
                val params = parseQueryParams(text.substringAfter('?', ""))
                QrPayloadFields(
                    paymentMethod = PaymentMethod.UPI,
                    paymentTarget = params["pa"].orEmpty(),
                    paymentAmount = params["am"].orEmpty(),
                    paymentNote = params["tn"].orEmpty()
                )
            }
            lower.startsWith("https://paypal.me/") || lower.startsWith("http://paypal.me/") -> {
                val path = text.substringAfter("paypal.me/").trim('/')
                val segments = path.split('/').filter { it.isNotBlank() }
                QrPayloadFields(
                    paymentMethod = PaymentMethod.PAYPAL,
                    paymentTarget = segments.getOrNull(0).orEmpty(),
                    paymentAmount = segments.getOrNull(1).orEmpty()
                )
            }
            else -> null
        }
    }

    private fun parseAppLinkOrNull(text: String): QrPayloadFields? {
        val lower = text.lowercase()
        return when {
            lower.contains("play.google.com/store/apps/details") -> {
                val params = parseQueryParams(text.substringAfter('?', ""))
                val pkg = params["id"].orEmpty().ifBlank { text }
                QrPayloadFields(appPlatform = AppPlatform.GOOGLE_PLAY, appTarget = pkg)
            }
            lower.contains("apps.apple.com/") -> {
                val idMatch = Regex("/id(\\d+)").find(text)?.groupValues?.getOrNull(1)
                QrPayloadFields(appPlatform = AppPlatform.APPLE_APP_STORE, appTarget = idMatch ?: text)
            }
            else -> null
        }
    }

    private fun parseSocialOrNull(text: String): QrPayloadFields? {
        val normalized = text.trim()
        for (platform in SocialPlatform.entries) {
            if (normalized.startsWith(platform.prefixUrl, ignoreCase = true)) {
                val handle = normalized.substring(platform.prefixUrl.length).trim('/')
                return QrPayloadFields(socialPlatform = platform, socialHandle = handle)
            }
        }
        return null
    }

    private fun looksLikeUrl(text: String): Boolean {
        if (text.contains('\n') || text.contains(' ')) return false
        val lower = text.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) return true
        return Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$").matches(text)
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        for (pair in query.split('&')) {
            val eq = pair.indexOf('=')
            if (eq > 0) {
                val k = urlDecode(pair.substring(0, eq)).lowercase()
                val v = urlDecode(pair.substring(eq + 1))
                map[k] = v
            }
        }
        return map
    }

    private fun urlDecode(value: String): String = try {
        URLDecoder.decode(value, "UTF-8")
    } catch (_: Exception) {
        value
    }
}
