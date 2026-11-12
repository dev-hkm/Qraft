package com.hkm.qraft.data

import com.hkm.qraft.model.*

object PresetCatalog {

    val builtInPresets: List<SavedStylePreset> = listOf(
        SavedStylePreset(
            id = "preset_espresso",
            name = "Warm Espresso",
            subtitle = "Rounded pixels • Soft cream card",
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.ROUNDED,
                pixelCornerRoundness = 0.45f,
                eyeFrameStyle = QrEyeFrameStyle.ROUNDED,
                eyeFrameRoundness = 0.34f,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF5C2E0B,
                    secondaryColor = 0xFFB84D0E,
                    direction = GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT
                ),
                customEyeFrameColors = true,
                eyeFrameBrush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFF23170F),
                customEyeBallColors = true,
                eyeBallBrush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFFC4510C),
                background = BackgroundStyleConfig(
                    mode = BackgroundMode.SOLID,
                    brush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFFFFFFFF)
                ),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.BOTTOM_SCAN_BANNER,
                    label = "SCAN ME",
                    frameFillColor = 0xFFFFFBF7,
                    accentColor = 0xFF5C2E0B,
                    accentSecondaryColor = 0xFFF07C2E,
                    textColor = 0xFFFFFFFF
                )
            )
        ),
        SavedStylePreset(
            id = "preset_classic",
            name = "Classic Ink",
            subtitle = "Crisp minimal • Maximum contrast",
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.SQUARE,
                pixelCornerRoundness = 0f,
                eyeFrameStyle = QrEyeFrameStyle.SQUARE,
                eyeFrameRoundness = 0f,
                eyeBallStyle = QrEyeBallStyle.SQUARE,
                darkBrush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFF1A1917),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.CLEAN_CARD,
                    label = "SCAN",
                    frameFillColor = 0xFFFFFFFF
                )
            )
        ),
        SavedStylePreset(
            id = "preset_liquid_sage",
            name = "Sage Liquid",
            subtitle = "Organic liquid flow • Leaf eyes",
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.LIQUID,
                pixelCornerRoundness = 0.5f,
                eyeFrameStyle = QrEyeFrameStyle.LEAF_TL_BR,
                eyeBallStyle = QrEyeBallStyle.LEAF,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF2F5E1B,
                    secondaryColor = 0xFF4B7A18,
                    direction = GradientDirection.TOP_TO_BOTTOM
                ),
                customEyeFrameColors = true,
                eyeFrameBrush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFF3E6F20),
                customEyeBallColors = true,
                eyeBallBrush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFF56881C),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.TOP_BADGE_CARD,
                    label = "ECO PASS",
                    frameFillColor = 0xFFFAFCF5,
                    accentColor = 0xFF4F781C,
                    textColor = 0xFFFFFFFF
                )
            )
        ),
        SavedStylePreset(
            id = "preset_sunset_coral",
            name = "Sunset Coral",
            subtitle = "Circular dots • Warm gradient",
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.CIRCLE,
                pixelSizeFactor = 0.88f,
                eyeFrameStyle = QrEyeFrameStyle.CIRCLE,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFFD9385A,
                    secondaryColor = 0xFFD6541C,
                    direction = GradientDirection.BOTTOM_LEFT_TO_TOP_RIGHT
                ),
                customEyeFrameColors = true,
                eyeFrameBrush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFFC42D52),
                customEyeBallColors = true,
                eyeBallBrush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFFD94A26),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.GRADIENT_BORDER,
                    label = "OPEN LINK",
                    frameFillColor = 0xFFFFFFFF,
                    accentColor = 0xFFD9385A,
                    accentSecondaryColor = 0xFFF4873B,
                    textColor = 0xFFFFFFFF
                )
            )
        ),
        SavedStylePreset(
            id = "preset_ocean_glass",
            name = "Ocean Radial",
            subtitle = "Diamonds • Deep sapphire radial",
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.DIAMOND,
                eyeFrameStyle = QrEyeFrameStyle.SQUIRCLE,
                eyeBallStyle = QrEyeBallStyle.DIAMOND,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.RADIAL_GRADIENT,
                    primaryColor = 0xFF1456A0,
                    secondaryColor = 0xFF1898C4
                ),
                background = BackgroundStyleConfig(
                    mode = BackgroundMode.GRADIENT,
                    brush = BrushStyleConfig(
                        mode = BrushMode.LINEAR_GRADIENT,
                        primaryColor = 0xFFF8FCFF,
                        secondaryColor = 0xFFEAF4FC
                    )
                ),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.DOUBLE_PILL,
                    label = "CONNECT",
                    frameFillColor = 0xFFFFFFFF,
                    accentColor = 0xFF1456A0,
                    textColor = 0xFFFFFFFF
                )
            )
        ),
        SavedStylePreset(
            id = "preset_orchid_bloom",
            name = "Orchid Bloom",
            subtitle = "Soft stars • Plum & berry",
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.STAR,
                pixelSizeFactor = 1.0f,
                eyeFrameStyle = QrEyeFrameStyle.THREE_ROUND,
                eyeBallStyle = QrEyeBallStyle.STAR,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF561869,
                    secondaryColor = 0xFF9B2675,
                    direction = GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT
                ),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.SPEECH_BUBBLE,
                    label = "SCAN HERE",
                    frameFillColor = 0xFFFCF7FD,
                    accentColor = 0xFF7D2E8D,
                    textColor = 0xFFFFFFFF
                )
            )
        ),
        SavedStylePreset(
            id = "preset_circle_emblem",
            name = "Orbital Badge",
            subtitle = "Circular silhouette • Editorial frame",
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.CIRCLE,
                eyeFrameStyle = QrEyeFrameStyle.CIRCLE,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                codeSilhouette = QrCodeSilhouette.CIRCLE,
                fourthEyeEnabled = true,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.SOLID,
                    primaryColor = 0xFF232220
                ),
                customEyeBallColors = true,
                eyeBallBrush = BrushStyleConfig(
                    mode = BrushMode.SOLID,
                    primaryColor = 0xFFE76F25
                ),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.WARM_POSTER,
                    label = "QRAFT BADGE",
                    subtitle = "Point your camera",
                    frameFillColor = 0xFFFCD8BE,
                    accentColor = 0xFF5C2E0B,
                    textColor = 0xFFFFFFFF
                ),
                ecc = QrEccSetting.HIGH,
                quietZonePadding = 0.11f
            )
        ),
        SavedStylePreset(
            id = "preset_midnight_gold",
            name = "Obsidian Gold",
            subtitle = "Dark canvas • Warm amber ink",
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.ROUNDED,
                pixelCornerRoundness = 0.35f,
                eyeFrameStyle = QrEyeFrameStyle.CHAMFER,
                eyeBallStyle = QrEyeBallStyle.SQUIRCLE,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFFF7CA7E,
                    secondaryColor = 0xFFE88A3E,
                    direction = GradientDirection.LEFT_TO_RIGHT
                ),
                background = BackgroundStyleConfig(
                    mode = BackgroundMode.SOLID,
                    brush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFF191614)
                ),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.POLAROID_STAMP,
                    label = "VIP ACCESS",
                    frameFillColor = 0xFF231F1B,
                    accentColor = 0xFFE88A3E,
                    textColor = 0xFFF7EFE7
                )
            )
        ),
        SavedStylePreset(
            id = "preset_spectrum",
            name = "Apple Spectrum",
            subtitle = "Sweep rainbow • High-res bars",
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.VERTICAL_BARS,
                eyeFrameStyle = QrEyeFrameStyle.ROUNDED,
                eyeBallStyle = QrEyeBallStyle.ROUNDED,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.SWEEP_GRADIENT,
                    primaryColor = 0xFFE74C3C,
                    secondaryColor = 0xFF3498DB,
                    tertiaryColor = 0xFF9B59B6
                ),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.CLEAN_CARD,
                    label = "SPECTRUM",
                    frameFillColor = 0xFFFFFFFF
                )
            )
        )
    )

    fun starterItems(now: Long = System.currentTimeMillis()): List<QrCreationItem> = listOf(
        QrCreationItem(
            id = "starter_wifi",
            title = "Studio Guest Wi-Fi",
            type = QrContentType.WIFI,
            payload = QrPayloadFields(
                wifiSsid = "Qraft_Studio_5G",
                wifiPassword = "Espresso2026!",
                wifiSecurity = WifiSecurity.WPA
            ),
            design = builtInPresets[2].design.copy(
                logo = LogoStyleConfig(
                    sourceType = LogoSourceType.ICON,
                    iconKey = "wifi",
                    sizeFraction = 0.21f,
                    clipShape = LogoClipShape.CIRCLE,
                    paddingStyle = LogoPaddingStyle.NATURAL,
                    backingEnabled = true,
                    backingColor = 0xFFFFFFFF,
                    iconTintColor = 0xFF3E6F20
                ),
                ecc = QrEccSetting.HIGH
            ),
            isFavorite = true,
            isVerifiedScannable = true,
            createdAtMillis = now - 3600_000L * 2,
            updatedAtMillis = now - 3600_000L * 2
        ),
        QrCreationItem(
            id = "starter_portfolio",
            title = "Portfolio Website",
            type = QrContentType.URL,
            payload = QrPayloadFields(url = "https://qraft.studio/showcase"),
            design = builtInPresets[0].design.copy(
                logo = LogoStyleConfig(
                    sourceType = LogoSourceType.MONOGRAM,
                    monogramText = "QR",
                    monogramBgColor = 0xFF5C2E0B,
                    monogramTextColor = 0xFFFFFFFF,
                    sizeFraction = 0.20f,
                    clipShape = LogoClipShape.SQUIRCLE,
                    backingEnabled = true,
                    backingColor = 0xFFFFFFFF
                ),
                ecc = QrEccSetting.HIGH
            ),
            isFavorite = true,
            isVerifiedScannable = true,
            createdAtMillis = now - 3600_000L * 8,
            updatedAtMillis = now - 3600_000L * 8
        ),
        QrCreationItem(
            id = "starter_vcard",
            title = "Alex Designer Card",
            type = QrContentType.VCARD,
            payload = QrPayloadFields(
                firstName = "Alex",
                lastName = "Nguyen",
                organization = "Qraft Design Lab",
                jobTitle = "Lead Product Designer",
                phone = "+84 901 234 567",
                email = "alex@qraft.studio",
                website = "https://qraft.studio"
            ),
            design = builtInPresets[5].design,
            isFavorite = false,
            isVerifiedScannable = true,
            createdAtMillis = now - 86400_000L,
            updatedAtMillis = now - 86400_000L
        ),
        QrCreationItem(
            id = "starter_event",
            title = "Design Meetup Pass",
            type = QrContentType.EVENT,
            payload = QrPayloadFields(
                eventTitle = "Apple-Grade Craft Meetup",
                eventLocation = "District 1 Studio, HCMC",
                eventStart = "20261120T183000",
                eventEnd = "20261120T210000",
                eventDescription = "Bring your favorite QR sticker!"
            ),
            design = builtInPresets[3].design,
            isFavorite = false,
            isVerifiedScannable = true,
            createdAtMillis = now - 86400_000L * 2,
            updatedAtMillis = now - 86400_000L * 2
        )
    )
}
