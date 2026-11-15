package com.hkm.qraft.qr

import com.hkm.qraft.model.AppPlatform
import com.hkm.qraft.model.PaymentMethod
import com.hkm.qraft.model.QrContentType
import com.hkm.qraft.model.QrPayloadFields
import com.hkm.qraft.model.WifiSecurity

object QrPayloadEncoder {

    fun encode(type: QrContentType, fields: QrPayloadFields): String {
        return when (type) {
            QrContentType.URL -> encodeUrl(fields.url)
            QrContentType.TEXT -> fields.text.ifBlank { "Hello from Qraft" }
            QrContentType.WIFI -> encodeWifi(fields)
            QrContentType.VCARD -> encodeVCard(fields)
            QrContentType.EMAIL -> encodeEmail(fields)
            QrContentType.PHONE -> "tel:${fields.phoneNumber.trim()}"
            QrContentType.SMS -> encodeSms(fields)
            QrContentType.GEO -> encodeGeo(fields)
            QrContentType.EVENT -> encodeEvent(fields)
            QrContentType.SOCIAL -> encodeSocial(fields)
            QrContentType.PAYMENT -> encodePayment(fields)
            QrContentType.APP_LINK -> encodeAppLink(fields)
        }
    }

    fun defaultTitle(type: QrContentType, fields: QrPayloadFields): String {
        return when (type) {
            QrContentType.URL -> fields.url
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
                .ifBlank { "Website Link" }
            QrContentType.WIFI -> fields.wifiSsid.ifBlank { "Guest Wi-Fi" }
            QrContentType.VCARD -> listOf(fields.firstName, fields.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { fields.organization.ifBlank { "Contact Card" } }
            QrContentType.TEXT -> fields.text.lines().firstOrNull()?.take(28)?.ifBlank { "Text Note" } ?: "Text Note"
            QrContentType.EMAIL -> fields.emailTo.ifBlank { "Email Draft" }
            QrContentType.PHONE -> fields.phoneNumber.ifBlank { "Phone Number" }
            QrContentType.SMS -> fields.smsNumber.ifBlank { "SMS Quick Text" }
            QrContentType.GEO -> fields.geoLabel.ifBlank { "${fields.latitude}, ${fields.longitude}" }
            QrContentType.EVENT -> fields.eventTitle.ifBlank { "Calendar Invite" }
            QrContentType.SOCIAL -> "${fields.socialPlatform.label}: ${fields.socialHandle.ifBlank { "profile" }}"
            QrContentType.PAYMENT -> "${fields.paymentMethod.label} Pay"
            QrContentType.APP_LINK -> fields.appTarget.ifBlank { fields.appPlatform.label }
        }
    }

    fun summarySubtitle(type: QrContentType, fields: QrPayloadFields): String {
        return when (type) {
            QrContentType.URL -> encodeUrl(fields.url)
            QrContentType.WIFI -> "${fields.wifiSecurity.label}${if (fields.wifiHidden) " • Hidden" else ""}"
            QrContentType.VCARD -> listOfNotNull(
                fields.jobTitle.takeIf { it.isNotBlank() },
                fields.organization.takeIf { it.isNotBlank() },
                fields.phone.takeIf { it.isNotBlank() }
            ).joinToString(" • ").ifBlank { "vCard 3.0 Contact" }
            QrContentType.TEXT -> "${fields.text.length} chars"
            QrContentType.EMAIL -> fields.emailSubject.ifBlank { fields.emailTo.ifBlank { "mailto link" } }
            QrContentType.PHONE -> fields.phoneNumber.ifBlank { "Tap to call" }
            QrContentType.SMS -> fields.smsMessage.take(36).ifBlank { fields.smsNumber }
            QrContentType.GEO -> "${fields.latitude}, ${fields.longitude}"
            QrContentType.EVENT -> fields.eventLocation.ifBlank { fields.eventStart }
            QrContentType.SOCIAL -> encodeSocial(fields)
            QrContentType.PAYMENT -> buildString {
                append(fields.paymentMethod.label)
                if (fields.paymentAmount.isNotBlank()) {
                    append(" • ")
                    append(fields.paymentAmount)
                }
            }
            QrContentType.APP_LINK -> encodeAppLink(fields)
        }
    }

    fun validate(type: QrContentType, fields: QrPayloadFields): String? {
        return when (type) {
            QrContentType.URL -> if (fields.url.isBlank()) "Enter a website URL" else null
            QrContentType.TEXT -> if (fields.text.isBlank()) "Enter text content" else null
            QrContentType.WIFI -> if (fields.wifiSsid.isBlank()) "Enter the Wi-Fi network name (SSID)" else null
            QrContentType.VCARD -> if (
                fields.firstName.isBlank() &&
                fields.lastName.isBlank() &&
                fields.organization.isBlank() &&
                fields.phone.isBlank() &&
                fields.email.isBlank()
            ) "Enter at least a name, organization, phone, or email" else null
            QrContentType.EMAIL -> if (fields.emailTo.isBlank()) "Enter a recipient email address" else null
            QrContentType.PHONE -> if (fields.phoneNumber.isBlank()) "Enter a phone number" else null
            QrContentType.SMS -> if (fields.smsNumber.isBlank()) "Enter a recipient phone number" else null
            QrContentType.GEO -> if (fields.latitude.isBlank() || fields.longitude.isBlank()) "Enter latitude and longitude" else null
            QrContentType.EVENT -> if (fields.eventTitle.isBlank()) "Enter an event title" else null
            QrContentType.SOCIAL -> if (fields.socialHandle.isBlank()) "Enter a username or URL" else null
            QrContentType.PAYMENT -> if (fields.paymentTarget.isBlank()) "Enter a payment address or handle" else null
            QrContentType.APP_LINK -> if (fields.appTarget.isBlank()) "Enter a package ID, App ID, or link" else null
        }
    }

    private fun encodeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "https://qraft.studio"
        return if (trimmed.contains("://")) trimmed else "https://$trimmed"
    }

    private fun escapeWifi(value: String): String =
        value.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
            .replace("\"", "\\\"")

    private fun encodeWifi(fields: QrPayloadFields): String {
        val ssid = escapeWifi(fields.wifiSsid.trim())
        val pass = if (fields.wifiSecurity == WifiSecurity.NONE) "" else escapeWifi(fields.wifiPassword)
        val hidden = if (fields.wifiHidden) "true" else "false"
        return buildString {
            append("WIFI:T:")
            append(fields.wifiSecurity.protocolToken)
            append(";S:")
            append(ssid)
            append(";P:")
            append(pass)
            append(";H:")
            append(hidden)
            append(";;")
        }
    }

    private fun escapeStructuredText(raw: String): String =
        raw.trim()
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "")

    private fun encodeVCard(fields: QrPayloadFields): String {
        val lines = mutableListOf(
            "BEGIN:VCARD",
            "VERSION:3.0"
        )
        val first = escapeStructuredText(fields.firstName)
        val last = escapeStructuredText(fields.lastName)
        val fullName = listOf(fields.firstName.trim(), fields.lastName.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
        lines += "N:$last;$first;;;"
        if (fullName.isNotBlank()) lines += "FN:${escapeStructuredText(fullName)}"
        if (fields.organization.isNotBlank()) lines += "ORG:${escapeStructuredText(fields.organization)}"
        if (fields.jobTitle.isNotBlank()) lines += "TITLE:${escapeStructuredText(fields.jobTitle)}"
        if (fields.phone.isNotBlank()) lines += "TEL;TYPE=CELL:${fields.phone.trim()}"
        if (fields.email.isNotBlank()) lines += "EMAIL;TYPE=INTERNET:${fields.email.trim()}"
        if (fields.website.isNotBlank()) lines += "URL:${encodeUrl(fields.website)}"
        if (fields.street.isNotBlank() || fields.city.isNotBlank() || fields.country.isNotBlank()) {
            lines += "ADR;TYPE=WORK:;;${escapeStructuredText(fields.street)};${escapeStructuredText(fields.city)};;;${escapeStructuredText(fields.country)}"
        }
        if (fields.note.isNotBlank()) lines += "NOTE:${escapeStructuredText(fields.note)}"
        lines += "END:VCARD"
        return lines.joinToString("\n")
    }

    private fun encodeEmail(fields: QrPayloadFields): String {
        val to = fields.emailTo.trim()
        val params = buildList {
            if (fields.emailSubject.isNotBlank()) add("subject=${uriEncode(fields.emailSubject)}")
            if (fields.emailBody.isNotBlank()) add("body=${uriEncode(fields.emailBody)}")
        }
        return if (params.isEmpty()) "mailto:$to" else "mailto:$to?${params.joinToString("&")}"
    }

    private fun encodeSms(fields: QrPayloadFields): String {
        val num = fields.smsNumber.trim()
        val body = fields.smsMessage.trim()
        return if (body.isEmpty()) "smsto:$num" else "smsto:$num:$body"
    }

    private fun encodeGeo(fields: QrPayloadFields): String {
        val lat = fields.latitude.trim().ifBlank { "0" }
        val lng = fields.longitude.trim().ifBlank { "0" }
        val label = fields.geoLabel.trim()
        return if (label.isEmpty()) {
            "geo:$lat,$lng"
        } else {
            "geo:$lat,$lng?q=$lat,$lng(${uriEncode(label)})"
        }
    }

    private fun encodeEvent(fields: QrPayloadFields): String {
        return buildList {
            add("BEGIN:VCALENDAR")
            add("VERSION:2.0")
            add("BEGIN:VEVENT")
            add("SUMMARY:${escapeStructuredText(fields.eventTitle)}")
            if (fields.eventLocation.isNotBlank()) add("LOCATION:${escapeStructuredText(fields.eventLocation)}")
            if (fields.eventStart.isNotBlank()) add("DTSTART:${fields.eventStart.trim()}")
            if (fields.eventEnd.isNotBlank()) add("DTEND:${fields.eventEnd.trim()}")
            if (fields.eventDescription.isNotBlank()) add("DESCRIPTION:${escapeStructuredText(fields.eventDescription)}")
            add("END:VEVENT")
            add("END:VCALENDAR")
        }.joinToString("\n")
    }

    private fun encodeSocial(fields: QrPayloadFields): String {
        val handle = fields.socialHandle.trim()
        if (handle.startsWith("http://") || handle.startsWith("https://")) return handle
        val clean = handle.removePrefix("@")
        return if (clean.isBlank()) fields.socialPlatform.prefixUrl else "${fields.socialPlatform.prefixUrl}$clean"
    }

    private fun encodePayment(fields: QrPayloadFields): String {
        val target = fields.paymentTarget.trim()
        val amount = fields.paymentAmount.trim()
        val note = fields.paymentNote.trim()
        return when (fields.paymentMethod) {
            PaymentMethod.PAYPAL -> {
                val base = if (target.startsWith("http")) target else "https://paypal.me/${target.removePrefix("@")}"
                if (amount.isNotBlank()) "$base/$amount" else base
            }
            PaymentMethod.BITCOIN -> buildUriWithQuery("bitcoin:$target", listOfNotNull(
                amount.takeIf { it.isNotBlank() }?.let { "amount=$it" },
                note.takeIf { it.isNotBlank() }?.let { "message=${uriEncode(it)}" }
            ))
            PaymentMethod.ETHEREUM -> buildUriWithQuery("ethereum:$target", listOfNotNull(
                amount.takeIf { it.isNotBlank() }?.let { "value=$it" }
            ))
            PaymentMethod.SOLANA -> buildUriWithQuery("solana:$target", listOfNotNull(
                amount.takeIf { it.isNotBlank() }?.let { "amount=$it" },
                note.takeIf { it.isNotBlank() }?.let { "memo=${uriEncode(it)}" }
            ))
            PaymentMethod.UPI -> buildUriWithQuery("upi://pay", listOfNotNull(
                "pa=${uriEncode(target)}",
                amount.takeIf { it.isNotBlank() }?.let { "am=$it" },
                note.takeIf { it.isNotBlank() }?.let { "tn=${uriEncode(it)}" }
            ))
            PaymentMethod.CUSTOM -> target.ifBlank { "https://pay.example.com" }
        }
    }

    private fun encodeAppLink(fields: QrPayloadFields): String {
        val target = fields.appTarget.trim()
        if (target.startsWith("http://") || target.startsWith("https://") || target.contains("://")) {
            return target
        }
        return when (fields.appPlatform) {
            AppPlatform.GOOGLE_PLAY -> "https://play.google.com/store/apps/details?id=$target"
            AppPlatform.APPLE_APP_STORE -> "https://apps.apple.com/app/${target.removePrefix("id").let { "id$it" }}"
            AppPlatform.UNIVERSAL -> target.ifBlank { "https://qraft.studio/app" }
        }
    }

    private fun buildUriWithQuery(base: String, params: List<String>): String =
        if (params.isEmpty()) base else "$base?${params.joinToString("&")}"

    private fun uriEncode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
