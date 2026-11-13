package com.hkm.qraft.model

import java.util.UUID

enum class CodeKind(val label: String) {
    QR("QR Code"),
    BARCODE("Barcode")
}

enum class BarcodeFormatType(
    val label: String,
    val shortCode: String,
    val zxingName: String,
    val is2D: Boolean,
    val helperText: String,
    val defaultSample: String
) {
    CODE_128("Code 128", "C128", "CODE_128", false, "Any ASCII text or SKU identifier", "QRAFT-PRO-2026"),
    EAN_13("EAN-13", "EAN13", "EAN_13", false, "12 digits (auto checksum) or 13 digits", "893456312014"),
    EAN_8("EAN-8", "EAN8", "EAN_8", false, "7 digits (auto checksum) or 8 digits", "9638507"),
    UPC_A("UPC-A", "UPCA", "UPC_A", false, "11 digits (auto checksum) or 12 digits", "03600029145"),
    CODE_39("Code 39", "C39", "CODE_39", false, "Uppercase A-Z, 0-9, - . $ / + % space", "QRAFT-05"),
    ITF("ITF-14", "ITF", "ITF", false, "Even count of digits (e.g. 14 digits)", "12345678901231"),
    CODABAR("Codabar", "CDBR", "CODABAR", false, "Digits 0-9 with A-D start/stop", "A40156B"),
    PDF_417("PDF417", "PDF417", "PDF_417", true, "High-density 2D stacked transit/ID code", "QRAFT STUDIO PASS 2026"),
    AZTEC("Aztec", "AZTEC", "AZTEC", true, "Compact 2D square boarding pass matrix", "https://qraft.studio/ticket"),
    DATA_MATRIX("Data Matrix", "DM", "DATA_MATRIX", true, "Compact industrial & logistics 2D grid", "SN-2026-QRAFT-8K")
}

data class BarcodeDesignSpec(
    val format: BarcodeFormatType = BarcodeFormatType.CODE_128,
    val data: String = "QRAFT-PRO-2026",
    val foregroundColor: Long = 0xFF111113,
    val backgroundColor: Long = 0xFFFFFFFF,
    val transparentBg: Boolean = false,
    val roundedBars: Boolean = false,
    val showText: Boolean = true,
    val caption: String = "",
    val quietMarginFraction: Float = 0.08f,
    val framed: Boolean = false,
    val frameFillColor: Long = 0xFFFFFFFF,
    val frameAccentColor: Long = 0xFF111113,
    val frameTextColor: Long = 0xFFFFFFFF,
    val cornerRadiusDp: Float = 24f,
    val exportWidthPx: Int = 4096
)

enum class QrContentType(
    val title: String,
    val shortCode: String,
    val description: String,
    val pastelIndex: Int
) {
    URL("Website URL", "WEB", "Links, landing pages & portals", 0),
    WIFI("Wi-Fi Pass", "WIFI", "Instant network auto-join", 1),
    VCARD("Contact Card", "CARD", "Full vCard 3.0 business card", 2),
    TEXT("Plain Text", "TXT", "Notes, codes & messages", 3),
    EMAIL("Email Draft", "MAIL", "Pre-addressed email composer", 4),
    PHONE("Phone Call", "CALL", "One-tap dialer number", 0),
    SMS("SMS Message", "SMS", "Pre-filled text message", 1),
    GEO("Location Pin", "GEO", "Map coordinates & place tag", 2),
    EVENT("Calendar Event", "DATE", "iCal meeting or invite", 3),
    SOCIAL("Social Profile", "SOC", "Instagram, TikTok, X, GitHub…", 4),
    PAYMENT("Payment & Crypto", "PAY", "PayPal, BTC, ETH, SOL, UPI", 0),
    APP_LINK("App & Deep Link", "APP", "Play Store, App Store, URI", 1)
}

enum class WifiSecurity(val label: String, val protocolToken: String) {
    WPA("WPA / WPA2 / WPA3", "WPA"),
    WEP("WEP", "WEP"),
    NONE("Open (No Password)", "nopass")
}

enum class SocialPlatform(
    val label: String,
    val prefixUrl: String,
    val placeholder: String
) {
    INSTAGRAM("Instagram", "https://instagram.com/", "username"),
    TIKTOK("TikTok", "https://www.tiktok.com/@", "username"),
    YOUTUBE("YouTube", "https://youtube.com/@", "channel"),
    X_TWITTER("X / Twitter", "https://x.com/", "handle"),
    FACEBOOK("Facebook", "https://facebook.com/", "profile"),
    LINKEDIN("LinkedIn", "https://linkedin.com/in/", "username"),
    GITHUB("GitHub", "https://github.com/", "username"),
    TELEGRAM("Telegram", "https://t.me/", "username"),
    WHATSAPP("WhatsApp", "https://wa.me/", "84901234567"),
    THREADS("Threads", "https://www.threads.net/@", "username")
}

enum class PaymentMethod(val label: String, val hint: String) {
    PAYPAL("PayPal.me", "username"),
    BITCOIN("Bitcoin", "bc1q... address"),
    ETHEREUM("Ethereum", "0x... address"),
    SOLANA("Solana", "Solana address"),
    UPI("UPI / Wallet URI", "name@bank"),
    CUSTOM("Custom Payment URI", "https://pay.example.com/...")
}

enum class AppPlatform(val label: String, val hint: String) {
    GOOGLE_PLAY("Google Play", "com.example.app"),
    APPLE_APP_STORE("Apple App Store", "id1234567890"),
    UNIVERSAL("Universal / Deep Link", "myapp://open/item")
}

data class QrPayloadFields(
    val url: String = "https://qraft.studio",
    val text: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: WifiSecurity = WifiSecurity.WPA,
    val wifiHidden: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val organization: String = "",
    val jobTitle: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val street: String = "",
    val city: String = "",
    val country: String = "",
    val note: String = "",
    val emailTo: String = "",
    val emailSubject: String = "",
    val emailBody: String = "",
    val phoneNumber: String = "",
    val smsNumber: String = "",
    val smsMessage: String = "",
    val latitude: String = "10.7769",
    val longitude: String = "106.7009",
    val geoLabel: String = "",
    val eventTitle: String = "",
    val eventLocation: String = "",
    val eventStart: String = "20261115T090000",
    val eventEnd: String = "20261115T103000",
    val eventDescription: String = "",
    val socialPlatform: SocialPlatform = SocialPlatform.INSTAGRAM,
    val socialHandle: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.PAYPAL,
    val paymentTarget: String = "",
    val paymentAmount: String = "",
    val paymentNote: String = "",
    val appPlatform: AppPlatform = AppPlatform.GOOGLE_PLAY,
    val appTarget: String = ""
)

enum class QrPixelStyle(val label: String) {
    SQUARE("Square"),
    ROUNDED("Rounded"),
    CIRCLE("Dots"),
    LIQUID("Liquid"),
    DIAMOND("Diamond"),
    STAR("Star"),
    VERTICAL_BARS("Vertical"),
    HORIZONTAL_BARS("Horizontal"),
    CROSS("Plus"),
    HEART("Heart"),
    HEXAGON("Hexagon"),
    SHIELD("Shield"),
    OCTAGON("Octagon"),
    RING("Ring"),
    WAVE_H("Pill H"),
    WAVE_V("Pill V")
}

enum class QrLightPixelStyle(val label: String) {
    NONE("Hidden"),
    DOT("Micro Dot"),
    ROUNDED("Soft Tile"),
    SQUARE("Grid Square")
}

enum class QrEyeFrameStyle(val label: String) {
    SQUARE("Sharp"),
    ROUNDED("Rounded"),
    CIRCLE("Circle"),
    LEAF_TL_BR("Leaf A"),
    LEAF_TR_BL("Leaf B"),
    CHAMFER("Chamfer"),
    SQUIRCLE("Squircle"),
    THREE_ROUND("Drop"),
    DOUBLE_RING("Dual Ring"),
    BRACKET("Bracket"),
    HEXAGON("Hex Ring")
}

enum class QrEyeBallStyle(val label: String) {
    SQUARE("Square"),
    ROUNDED("Rounded"),
    CIRCLE("Circle"),
    DIAMOND("Diamond"),
    LEAF("Leaf"),
    STAR("Star"),
    SQUIRCLE("Squircle"),
    CROSS("Cross"),
    OCTAGON("Octagon"),
    HEXAGON("Hexagon")
}

enum class QrCodeSilhouette(val label: String) {
    SQUARE("Square"),
    CIRCLE("Circular"),
    HEXAGON("Hexagon")
}

enum class BrushMode(val label: String) {
    SOLID("Solid"),
    LINEAR_GRADIENT("Linear"),
    RADIAL_GRADIENT("Radial"),
    SWEEP_GRADIENT("Spectrum")
}

enum class GradientDirection(val label: String, val angleDeg: Float) {
    TOP_TO_BOTTOM("Vertical", 90f),
    LEFT_TO_RIGHT("Horizontal", 0f),
    TOP_LEFT_TO_BOTTOM_RIGHT("Diagonal ↘", 45f),
    BOTTOM_LEFT_TO_TOP_RIGHT("Diagonal ↗", 315f),
    CUSTOM_ANGLE("Custom", 135f)
}

data class BrushStyleConfig(
    val mode: BrushMode = BrushMode.SOLID,
    val primaryColor: Long = 0xFF111113,
    val secondaryColor: Long = 0xFF52525B,
    val tertiaryColor: Long? = null,
    val direction: GradientDirection = GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT,
    val angleDegrees: Float = 45f
)

enum class BackgroundMode(val label: String) {
    SOLID("Solid"),
    GRADIENT("Gradient"),
    TRANSPARENT("Transparent"),
    PHOTO("Photo")
}

data class BackgroundStyleConfig(
    val mode: BackgroundMode = BackgroundMode.SOLID,
    val brush: BrushStyleConfig = BrushStyleConfig(
        mode = BrushMode.SOLID,
        primaryColor = 0xFFFFFFFF,
        secondaryColor = 0xFFF4F4F6
    ),
    val photoUri: String? = null,
    val photoOverlayAlpha: Float = 0.72f,
    val photoOverlayColor: Long = 0xFFFFFFFF
)

enum class LogoSourceType(val label: String) {
    NONE("None"),
    GALLERY("Photo"),
    ICON("Icon"),
    EMOJI("Emoji"),
    MONOGRAM("Badge")
}

enum class LogoClipShape(val label: String) {
    CIRCLE("Circle"),
    ROUNDED("Rounded"),
    SQUARE("Square"),
    SQUIRCLE("Squircle"),
    DIAMOND("Diamond")
}

enum class LogoPaddingStyle(val label: String) {
    NATURAL("Natural"),
    ACCURATE("Accurate"),
    NONE("Overlay")
}

data class LogoStyleConfig(
    val sourceType: LogoSourceType = LogoSourceType.NONE,
    val galleryUri: String? = null,
    val iconKey: String = "qr",
    val emoji: String = "✦",
    val monogramText: String = "QR",
    val monogramBgColor: Long = 0xFF111113,
    val monogramTextColor: Long = 0xFFFFFFFF,
    val sizeFraction: Float = 0.22f,
    val clipShape: LogoClipShape = LogoClipShape.CIRCLE,
    val paddingStyle: LogoPaddingStyle = LogoPaddingStyle.NATURAL,
    val paddingFraction: Float = 0.14f,
    val backingEnabled: Boolean = true,
    val backingColor: Long = 0xFFFFFFFF,
    val borderEnabled: Boolean = true,
    val borderColor: Long = 0xFFE4E4EA,
    val iconTintColor: Long = 0xFF111113,
    val rotationDeg: Float? = null,
    val borderWidthFraction: Float? = null
) {
    fun effectiveRotationDeg(): Float = rotationDeg ?: 0f
    fun effectiveBorderWidthFraction(): Float = (borderWidthFraction ?: 0.045f).coerceIn(0.015f, 0.12f)
}

enum class OuterFrameStyle(val label: String) {
    NONE("No Frame"),
    CLEAN_CARD("Clean Card"),
    MINIMAL_OUTLINE("Hairline"),
    BOTTOM_SCAN_BANNER("Scan Banner"),
    TOP_BADGE_CARD("Top Tag"),
    SCAN_BAR_HEADER("Header Bar"),
    SPEECH_BUBBLE("Callout"),
    GRADIENT_BORDER("Gradient Ring"),
    POLAROID_STAMP("Polaroid"),
    DOUBLE_PILL("Twin Pill"),
    WARM_POSTER("Poster"),
    GLASS_CARD("Glass Card"),
    CORNER_BRACKET("Viewfinder"),
    TICKET_STUB("Ticket Stub"),
    NEON_GLOW("Dark Glow"),
    CIRCULAR_STAMP("Circle Seal")
}

data class OuterFrameConfig(
    val style: OuterFrameStyle = OuterFrameStyle.NONE,
    val label: String = "SCAN ME",
    val subtitle: String = "",
    val frameFillColor: Long = 0xFFFFFFFF,
    val accentColor: Long = 0xFF111113,
    val accentSecondaryColor: Long = 0xFF52525B,
    val textColor: Long = 0xFFFFFFFF,
    val cornerRadiusDp: Float = 26f,
    val borderThicknessDp: Float = 0f,
    val framePaddingFraction: Float? = null
) {
    fun effectivePaddingFraction(): Float = (framePaddingFraction ?: 0.085f).coerceIn(0.04f, 0.16f)
}

enum class QrEccSetting(val label: String, val code: String, val ratioText: String) {
    AUTO("Auto", "AUTO", "Smart"),
    LOW("Low (L)", "L", "7%"),
    MEDIUM("Medium (M)", "M", "15%"),
    QUARTILE("Quartile (Q)", "Q", "25%"),
    HIGH("High (H)", "H", "30%")
}

data class QrDesignSpec(
    val pixelStyle: QrPixelStyle = QrPixelStyle.ROUNDED,
    val pixelCornerRoundness: Float = 0.42f,
    val pixelSizeFactor: Float = 0.92f,
    val lightPixelStyle: QrLightPixelStyle = QrLightPixelStyle.NONE,
    val lightPixelColor: Long = 0x22000000,
    val eyeFrameStyle: QrEyeFrameStyle = QrEyeFrameStyle.ROUNDED,
    val eyeFrameRoundness: Float = 0.30f,
    val eyeBallStyle: QrEyeBallStyle = QrEyeBallStyle.CIRCLE,
    val eyeBallRoundness: Float = 0.35f,
    val codeSilhouette: QrCodeSilhouette = QrCodeSilhouette.SQUARE,
    val fourthEyeEnabled: Boolean = false,
    val darkBrush: BrushStyleConfig = BrushStyleConfig(
        mode = BrushMode.SOLID,
        primaryColor = 0xFF111113,
        secondaryColor = 0xFF52525B
    ),
    val customEyeFrameColors: Boolean = false,
    val eyeFrameBrush: BrushStyleConfig = BrushStyleConfig(
        mode = BrushMode.SOLID,
        primaryColor = 0xFF111113,
        secondaryColor = 0xFF52525B
    ),
    val customEyeBallColors: Boolean = false,
    val eyeBallBrush: BrushStyleConfig = BrushStyleConfig(
        mode = BrushMode.SOLID,
        primaryColor = 0xFF111113,
        secondaryColor = 0xFF52525B
    ),
    val background: BackgroundStyleConfig = BackgroundStyleConfig(),
    val logo: LogoStyleConfig = LogoStyleConfig(),
    val outerFrame: OuterFrameConfig = OuterFrameConfig(),
    val ecc: QrEccSetting = QrEccSetting.AUTO,
    val quietZonePadding: Float = 0.10f,
    val exportSizePx: Int = 4096,
    val matrixShadowEnabled: Boolean = false
)

data class QrCreationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: QrContentType,
    val payload: QrPayloadFields,
    val design: QrDesignSpec,
    val isFavorite: Boolean = false,
    val isVerifiedScannable: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val codeKind: CodeKind? = null,
    val barcode: BarcodeDesignSpec? = null,
    val folder: String? = null,
    val tags: List<String>? = null,
    val isArchived: Boolean = false
) {
    fun resolvedCodeKind(): CodeKind = codeKind ?: CodeKind.QR
    fun resolvedBarcode(): BarcodeDesignSpec = barcode?.sanitized() ?: BarcodeDesignSpec()
    fun resolvedFolder(): String = (folder as String?)?.trim().orEmpty()
    @Suppress("UNCHECKED_CAST")
    fun resolvedTags(): List<String> = (tags as? List<String>)
        ?.filterNotNull()
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        .orEmpty()
}

enum class PresetCategory(val label: String) {
    ALL("All"),
    MONOCHROME("Monochrome"),
    GRADIENT("Gradient"),
    DARK("Dark & Neon"),
    FRAMED("Framed Cards"),
    ORGANIC("Geometric & Fluid")
}

data class SavedStylePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val design: QrDesignSpec,
    val isBuiltIn: Boolean = true,
    val category: PresetCategory? = null
) {
    fun resolvedCategory(): PresetCategory = category ?: PresetCategory.MONOCHROME
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class ExportImageFormat(val extension: String, val mimeType: String, val label: String) {
    PNG("png", "image/png", "PNG (Lossless)"),
    JPEG("jpg", "image/jpeg", "JPEG (Compact)"),
    WEBP("webp", "image/webp", "WebP (Modern)"),
    SVG("svg", "image/svg+xml", "SVG (Vector)"),
    PDF("pdf", "application/pdf", "PDF (Print)")
}

data class ScannabilityReport(
    val isScannable: Boolean,
    val decodedPreview: String? = null,
    val matrixSize: Int = 25,
    val effectiveEcc: String = "M",
    val contrastRatio: Double = 21.0,
    val warningMessage: String? = null
)

/**
 * Defensive post-deserialization sanitizers.
 * Gson can bypass Kotlin default values and leave non-null fields/enums as null at runtime
 * when loading JSON saved by older app versions or imported backup files.
 */
@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
private inline fun <T : Any> T?.orDefault(default: T): T = this ?: default

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
private fun String?.orEmptySafe(fallback: String = ""): String = this ?: fallback

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
fun QrPayloadFields?.sanitized(): QrPayloadFields {
    val src = this ?: return QrPayloadFields()
    return QrPayloadFields(
        url = src.url.orEmptySafe("https://qraft.studio"),
        text = src.text.orEmptySafe(),
        wifiSsid = src.wifiSsid.orEmptySafe(),
        wifiPassword = src.wifiPassword.orEmptySafe(),
        wifiSecurity = src.wifiSecurity.orDefault(WifiSecurity.WPA),
        wifiHidden = src.wifiHidden,
        firstName = src.firstName.orEmptySafe(),
        lastName = src.lastName.orEmptySafe(),
        organization = src.organization.orEmptySafe(),
        jobTitle = src.jobTitle.orEmptySafe(),
        phone = src.phone.orEmptySafe(),
        email = src.email.orEmptySafe(),
        website = src.website.orEmptySafe(),
        street = src.street.orEmptySafe(),
        city = src.city.orEmptySafe(),
        country = src.country.orEmptySafe(),
        note = src.note.orEmptySafe(),
        emailTo = src.emailTo.orEmptySafe(),
        emailSubject = src.emailSubject.orEmptySafe(),
        emailBody = src.emailBody.orEmptySafe(),
        phoneNumber = src.phoneNumber.orEmptySafe(),
        smsNumber = src.smsNumber.orEmptySafe(),
        smsMessage = src.smsMessage.orEmptySafe(),
        latitude = src.latitude.orEmptySafe("10.7769"),
        longitude = src.longitude.orEmptySafe("106.7009"),
        geoLabel = src.geoLabel.orEmptySafe(),
        eventTitle = src.eventTitle.orEmptySafe(),
        eventLocation = src.eventLocation.orEmptySafe(),
        eventStart = src.eventStart.orEmptySafe("20261115T090000"),
        eventEnd = src.eventEnd.orEmptySafe("20261115T103000"),
        eventDescription = src.eventDescription.orEmptySafe(),
        socialPlatform = src.socialPlatform.orDefault(SocialPlatform.INSTAGRAM),
        socialHandle = src.socialHandle.orEmptySafe(),
        paymentMethod = src.paymentMethod.orDefault(PaymentMethod.PAYPAL),
        paymentTarget = src.paymentTarget.orEmptySafe(),
        paymentAmount = src.paymentAmount.orEmptySafe(),
        paymentNote = src.paymentNote.orEmptySafe(),
        appPlatform = src.appPlatform.orDefault(AppPlatform.GOOGLE_PLAY),
        appTarget = src.appTarget.orEmptySafe()
    )
}

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
fun BrushStyleConfig?.sanitized(defaultPrimary: Long = 0xFF111113): BrushStyleConfig {
    val src = this ?: return BrushStyleConfig(primaryColor = defaultPrimary)
    return BrushStyleConfig(
        mode = src.mode.orDefault(BrushMode.SOLID),
        primaryColor = src.primaryColor,
        secondaryColor = src.secondaryColor,
        tertiaryColor = src.tertiaryColor,
        direction = src.direction.orDefault(GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT),
        angleDegrees = if (src.angleDegrees.isFinite()) src.angleDegrees else 45f
    )
}

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
fun BackgroundStyleConfig?.sanitized(): BackgroundStyleConfig {
    val src = this ?: return BackgroundStyleConfig()
    return BackgroundStyleConfig(
        mode = src.mode.orDefault(BackgroundMode.SOLID),
        brush = src.brush.sanitized(defaultPrimary = 0xFFFFFFFF),
        photoUri = src.photoUri,
        photoOverlayAlpha = src.photoOverlayAlpha.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.72f,
        photoOverlayColor = src.photoOverlayColor
    )
}

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
fun LogoStyleConfig?.sanitized(): LogoStyleConfig {
    val src = this ?: return LogoStyleConfig()
    return LogoStyleConfig(
        sourceType = src.sourceType.orDefault(LogoSourceType.NONE),
        galleryUri = src.galleryUri,
        iconKey = src.iconKey.orEmptySafe("qr"),
        emoji = src.emoji.orEmptySafe("✦"),
        monogramText = src.monogramText.orEmptySafe("QR"),
        monogramBgColor = src.monogramBgColor,
        monogramTextColor = src.monogramTextColor,
        sizeFraction = src.sizeFraction.takeIf { it.isFinite() }?.coerceIn(0.10f, 0.36f) ?: 0.22f,
        clipShape = src.clipShape.orDefault(LogoClipShape.CIRCLE),
        paddingStyle = src.paddingStyle.orDefault(LogoPaddingStyle.NATURAL),
        paddingFraction = src.paddingFraction.takeIf { it.isFinite() }?.coerceIn(0f, 0.35f) ?: 0.14f,
        backingEnabled = src.backingEnabled,
        backingColor = src.backingColor,
        borderEnabled = src.borderEnabled,
        borderColor = src.borderColor,
        iconTintColor = src.iconTintColor,
        rotationDeg = src.rotationDeg?.takeIf { it.isFinite() },
        borderWidthFraction = src.borderWidthFraction?.takeIf { it.isFinite() }
    )
}

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
fun OuterFrameConfig?.sanitized(): OuterFrameConfig {
    val src = this ?: return OuterFrameConfig()
    return OuterFrameConfig(
        style = src.style.orDefault(OuterFrameStyle.NONE),
        label = src.label.orEmptySafe("SCAN ME"),
        subtitle = src.subtitle.orEmptySafe(),
        frameFillColor = src.frameFillColor,
        accentColor = src.accentColor,
        accentSecondaryColor = src.accentSecondaryColor,
        textColor = src.textColor,
        cornerRadiusDp = src.cornerRadiusDp.takeIf { it.isFinite() }?.coerceIn(0f, 64f) ?: 26f,
        borderThicknessDp = src.borderThicknessDp.takeIf { it.isFinite() }?.coerceIn(0f, 24f) ?: 0f,
        framePaddingFraction = src.framePaddingFraction?.takeIf { it.isFinite() }
    )
}

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
fun QrDesignSpec?.sanitized(): QrDesignSpec {
    val src = this ?: return QrDesignSpec()
    return QrDesignSpec(
        pixelStyle = src.pixelStyle.orDefault(QrPixelStyle.ROUNDED),
        pixelCornerRoundness = src.pixelCornerRoundness.takeIf { it.isFinite() }?.coerceIn(0f, 0.5f) ?: 0.42f,
        pixelSizeFactor = src.pixelSizeFactor.takeIf { it.isFinite() }?.coerceIn(0.35f, 1.0f) ?: 0.92f,
        lightPixelStyle = src.lightPixelStyle.orDefault(QrLightPixelStyle.NONE),
        lightPixelColor = src.lightPixelColor,
        eyeFrameStyle = src.eyeFrameStyle.orDefault(QrEyeFrameStyle.ROUNDED),
        eyeFrameRoundness = src.eyeFrameRoundness.takeIf { it.isFinite() }?.coerceIn(0f, 0.5f) ?: 0.30f,
        eyeBallStyle = src.eyeBallStyle.orDefault(QrEyeBallStyle.CIRCLE),
        eyeBallRoundness = src.eyeBallRoundness.takeIf { it.isFinite() }?.coerceIn(0f, 0.5f) ?: 0.35f,
        codeSilhouette = src.codeSilhouette.orDefault(QrCodeSilhouette.SQUARE),
        fourthEyeEnabled = src.fourthEyeEnabled,
        darkBrush = src.darkBrush.sanitized(),
        customEyeFrameColors = src.customEyeFrameColors,
        eyeFrameBrush = src.eyeFrameBrush.sanitized(),
        customEyeBallColors = src.customEyeBallColors,
        eyeBallBrush = src.eyeBallBrush.sanitized(),
        background = src.background.sanitized(),
        logo = src.logo.sanitized(),
        outerFrame = src.outerFrame.sanitized(),
        ecc = src.ecc.orDefault(QrEccSetting.AUTO),
        quietZonePadding = src.quietZonePadding.takeIf { it.isFinite() }?.coerceIn(0.02f, 0.30f) ?: 0.10f,
        exportSizePx = src.exportSizePx.coerceIn(512, 8192),
        matrixShadowEnabled = src.matrixShadowEnabled
    )
}

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
fun BarcodeDesignSpec?.sanitized(): BarcodeDesignSpec {
    val src = this ?: return BarcodeDesignSpec()
    return BarcodeDesignSpec(
        format = src.format.orDefault(BarcodeFormatType.CODE_128),
        data = src.data.orEmptySafe("QRAFT-PRO-2026"),
        foregroundColor = src.foregroundColor,
        backgroundColor = src.backgroundColor,
        transparentBg = src.transparentBg,
        roundedBars = src.roundedBars,
        showText = src.showText,
        caption = src.caption.orEmptySafe(),
        quietMarginFraction = src.quietMarginFraction.takeIf { it.isFinite() }?.coerceIn(0.02f, 0.25f) ?: 0.08f,
        framed = src.framed,
        frameFillColor = src.frameFillColor,
        frameAccentColor = src.frameAccentColor,
        frameTextColor = src.frameTextColor,
        cornerRadiusDp = src.cornerRadiusDp.takeIf { it.isFinite() }?.coerceIn(0f, 64f) ?: 24f,
        exportWidthPx = src.exportWidthPx.coerceIn(512, 8192)
    )
}

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS", "UNCHECKED_CAST")
fun QrCreationItem.sanitized(): QrCreationItem = QrCreationItem(
    id = id.orEmptySafe(UUID.randomUUID().toString()),
    title = title.orEmptySafe("Qraft Code"),
    type = type.orDefault(QrContentType.URL),
    payload = payload.sanitized(),
    design = design.sanitized(),
    isFavorite = isFavorite,
    isVerifiedScannable = isVerifiedScannable,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    codeKind = codeKind ?: CodeKind.QR,
    barcode = barcode?.sanitized(),
    folder = (folder as String?)?.trim()?.takeIf { it.isNotEmpty() },
    tags = (tags as? List<String>)?.filterNotNull()?.map { it.trim() }?.filter { it.isNotBlank() }?.distinct(),
    isArchived = isArchived
)

@Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
fun SavedStylePreset.sanitized(): SavedStylePreset = SavedStylePreset(
    id = id.orEmptySafe(UUID.randomUUID().toString()),
    name = name.orEmptySafe("Custom Preset"),
    subtitle = subtitle.orEmptySafe("Saved style"),
    design = design.sanitized(),
    isBuiltIn = isBuiltIn,
    category = category ?: PresetCategory.MONOCHROME
)
