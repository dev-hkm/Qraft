package com.hkm.qraft.model

import java.util.UUID

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
    SHIELD("Soft Shield")
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
    CIRCLE("Ring"),
    LEAF_TL_BR("Leaf A"),
    LEAF_TR_BL("Leaf B"),
    CHAMFER("Chamfer"),
    SQUIRCLE("Squircle"),
    THREE_ROUND("Drop")
}

enum class QrEyeBallStyle(val label: String) {
    SQUARE("Square"),
    ROUNDED("Rounded"),
    CIRCLE("Circle"),
    DIAMOND("Diamond"),
    LEAF("Leaf"),
    STAR("Star"),
    SQUIRCLE("Squircle"),
    CROSS("Cross")
}

enum class QrCodeSilhouette(val label: String) {
    SQUARE("Classic Square"),
    CIRCLE("Circular Badge"),
    HEXAGON("Hexagon Emblem")
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
    val primaryColor: Long = 0xFF1E1D1B,
    val secondaryColor: Long = 0xFFE86F28,
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
        secondaryColor = 0xFFFFEFE3
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
    val emoji: String = "⚡",
    val monogramText: String = "QR",
    val monogramBgColor: Long = 0xFF5C2E0B,
    val monogramTextColor: Long = 0xFFFFFFFF,
    val sizeFraction: Float = 0.22f,
    val clipShape: LogoClipShape = LogoClipShape.CIRCLE,
    val paddingStyle: LogoPaddingStyle = LogoPaddingStyle.NATURAL,
    val paddingFraction: Float = 0.14f,
    val backingEnabled: Boolean = true,
    val backingColor: Long = 0xFFFFFFFF,
    val borderEnabled: Boolean = true,
    val borderColor: Long = 0xFFF0EBE6,
    val iconTintColor: Long = 0xFF5C2E0B
)

enum class OuterFrameStyle(val label: String) {
    NONE("No Frame"),
    CLEAN_CARD("Apple Card"),
    BOTTOM_SCAN_BANNER("Scan Banner"),
    TOP_BADGE_CARD("Top Tag"),
    SPEECH_BUBBLE("Callout"),
    GRADIENT_BORDER("Gradient Ring"),
    POLAROID_STAMP("Polaroid"),
    DOUBLE_PILL("Twin Pill"),
    WARM_POSTER("Poster")
}

data class OuterFrameConfig(
    val style: OuterFrameStyle = OuterFrameStyle.CLEAN_CARD,
    val label: String = "SCAN ME",
    val subtitle: String = "",
    val frameFillColor: Long = 0xFFFFFFFF,
    val accentColor: Long = 0xFF5C2E0B,
    val accentSecondaryColor: Long = 0xFFF07C2E,
    val textColor: Long = 0xFFFFFFFF,
    val cornerRadiusDp: Float = 26f,
    val borderThicknessDp: Float = 0f
)

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
        primaryColor = 0xFF1E1D1B,
        secondaryColor = 0xFFE86F28
    ),
    val customEyeFrameColors: Boolean = false,
    val eyeFrameBrush: BrushStyleConfig = BrushStyleConfig(
        mode = BrushMode.SOLID,
        primaryColor = 0xFF5C2E0B,
        secondaryColor = 0xFFF07C2E
    ),
    val customEyeBallColors: Boolean = false,
    val eyeBallBrush: BrushStyleConfig = BrushStyleConfig(
        mode = BrushMode.SOLID,
        primaryColor = 0xFFF07C2E,
        secondaryColor = 0xFF5C2E0B
    ),
    val background: BackgroundStyleConfig = BackgroundStyleConfig(),
    val logo: LogoStyleConfig = LogoStyleConfig(),
    val outerFrame: OuterFrameConfig = OuterFrameConfig(),
    val ecc: QrEccSetting = QrEccSetting.AUTO,
    val quietZonePadding: Float = 0.10f,
    val exportSizePx: Int = 2048
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
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class SavedStylePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val design: QrDesignSpec,
    val isBuiltIn: Boolean = true
)

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
