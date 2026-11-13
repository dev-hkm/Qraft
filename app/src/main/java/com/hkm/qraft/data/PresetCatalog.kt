package com.hkm.qraft.data

import com.hkm.qraft.model.*

object PresetCatalog {

    val builtInPresets: List<SavedStylePreset> = listOf(
        // 1 - Editorial Monochrome
        SavedStylePreset(
            id = "preset_swiss_mono",
            name = "Swiss Mono",
            subtitle = "Crisp minimal hairline matte",
            category = PresetCategory.MONOCHROME,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.SQUARE,
                pixelSizeFactor = 0.94f,
                eyeFrameStyle = QrEyeFrameStyle.SQUARE,
                eyeBallStyle = QrEyeBallStyle.SQUARE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF111113),
                background = BackgroundStyleConfig(BackgroundMode.SOLID, BrushStyleConfig(BrushMode.SOLID, 0xFFFFFFFF)),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE, "SCAN ME", "", 0xFFFFFFFF, 0xFF111113, 0xFF52525B, 0xFFFFFFFF)
            )
        ),
        // 2
        SavedStylePreset(
            id = "preset_apple_minimal",
            name = "Obsidian Clean",
            subtitle = "Soft rounded raw vector",
            category = PresetCategory.MONOCHROME,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.ROUNDED,
                pixelCornerRoundness = 0.42f,
                pixelSizeFactor = 0.92f,
                eyeFrameStyle = QrEyeFrameStyle.SQUIRCLE,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF3F3F46),
                background = BackgroundStyleConfig(BackgroundMode.SOLID, BrushStyleConfig(BrushMode.SOLID, 0xFFFFFFFF)),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE)
            )
        ),
        // 3
        SavedStylePreset(
            id = "preset_graphite_liquid",
            name = "Graphite Fluid",
            subtitle = "Connected organic ink drops",
            category = PresetCategory.MONOCHROME,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.LIQUID,
                pixelCornerRoundness = 0.45f,
                pixelSizeFactor = 0.95f,
                eyeFrameStyle = QrEyeFrameStyle.LEAF_TL_BR,
                eyeBallStyle = QrEyeBallStyle.LEAF,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF111113,
                    secondaryColor = 0xFF52525B,
                    direction = GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT
                ),
                outerFrame = OuterFrameConfig(OuterFrameStyle.CLEAN_CARD, "SCAN", "", 0xFFFFFFFF, 0xFF111113, 0xFF3F3F46, 0xFFFFFFFF)
            )
        ),
        // 4
        SavedStylePreset(
            id = "preset_hairline_gallery",
            name = "Gallery Hairline",
            subtitle = "Fine border museum print",
            category = PresetCategory.FRAMED,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.CIRCLE,
                pixelSizeFactor = 0.88f,
                eyeFrameStyle = QrEyeFrameStyle.CIRCLE,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF111113),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.MINIMAL_OUTLINE,
                    label = "SCAN TO VIEW",
                    frameFillColor = 0xFFFFFFFF,
                    accentColor = 0xFF111113,
                    textColor = 0xFFFFFFFF,
                    cornerRadiusDp = 20f
                )
            )
        ),
        // 5
        SavedStylePreset(
            id = "preset_noir_invert",
            name = "Noir Reverse",
            subtitle = "White matrix on jet black",
            category = PresetCategory.DARK,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.ROUNDED,
                pixelCornerRoundness = 0.36f,
                pixelSizeFactor = 0.90f,
                eyeFrameStyle = QrEyeFrameStyle.ROUNDED,
                eyeBallStyle = QrEyeBallStyle.SQUIRCLE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFFFFFFFF, 0xFFE4E4E7),
                background = BackgroundStyleConfig(BackgroundMode.SOLID, BrushStyleConfig(BrushMode.SOLID, 0xFF0A0A0B)),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.CLEAN_CARD,
                    label = "SCAN ME",
                    frameFillColor = 0xFF141416,
                    accentColor = 0xFFFFFFFF,
                    textColor = 0xFF0A0A0B
                )
            )
        ),
        // 6
        SavedStylePreset(
            id = "preset_cobalt_pulse",
            name = "Cobalt Gradient",
            subtitle = "Electric sapphire spectrum",
            category = PresetCategory.GRADIENT,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.ROUNDED,
                pixelCornerRoundness = 0.40f,
                pixelSizeFactor = 0.92f,
                eyeFrameStyle = QrEyeFrameStyle.SQUIRCLE,
                eyeBallStyle = QrEyeBallStyle.SQUIRCLE,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF1D4ED8,
                    secondaryColor = 0xFF06B6D4,
                    direction = GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT
                ),
                customEyeFrameColors = true,
                eyeFrameBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF0F172A, 0xFF1D4ED8),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE)
            )
        ),
        // 7
        SavedStylePreset(
            id = "preset_emerald_glass",
            name = "Emerald Frosted",
            subtitle = "Deep pine glass badge",
            category = PresetCategory.FRAMED,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.CIRCLE,
                pixelSizeFactor = 0.88f,
                eyeFrameStyle = QrEyeFrameStyle.LEAF_TL_BR,
                eyeBallStyle = QrEyeBallStyle.LEAF,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF065F46,
                    secondaryColor = 0xFF10B981
                ),
                background = BackgroundStyleConfig(
                    mode = BackgroundMode.GRADIENT,
                    brush = BrushStyleConfig(BrushMode.LINEAR_GRADIENT, 0xFFFFFFFF, 0xFFECFDF5)
                ),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.GLASS_CARD,
                    label = "CONNECT",
                    frameFillColor = 0xFFFAFDFB,
                    accentColor = 0xFF065F46,
                    accentSecondaryColor = 0xFF10B981,
                    textColor = 0xFFFFFFFF
                )
            )
        ),
        // 8
        SavedStylePreset(
            id = "preset_cyber_neon",
            name = "Cyber Cyan",
            subtitle = "Dark matrix + neon rim",
            category = PresetCategory.DARK,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.OCTAGON,
                pixelSizeFactor = 0.88f,
                eyeFrameStyle = QrEyeFrameStyle.CHAMFER,
                eyeBallStyle = QrEyeBallStyle.DIAMOND,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF22D3EE,
                    secondaryColor = 0xFF818CF8
                ),
                customEyeFrameColors = true,
                eyeFrameBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF38BDF8, 0xFF818CF8),
                background = BackgroundStyleConfig(BackgroundMode.SOLID, BrushStyleConfig(BrushMode.SOLID, 0xFF090D16)),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.NEON_GLOW,
                    label = "CYBER SCAN",
                    frameFillColor = 0xFF0F172A,
                    accentColor = 0xFF22D3EE,
                    accentSecondaryColor = 0xFF6366F1,
                    textColor = 0xFF090D16
                )
            )
        ),
        // 9
        SavedStylePreset(
            id = "preset_sunset_coral",
            name = "Sunset Coral",
            subtitle = "Warm coral to magenta flow",
            category = PresetCategory.GRADIENT,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.WAVE_H,
                pixelSizeFactor = 0.88f,
                eyeFrameStyle = QrEyeFrameStyle.THREE_ROUND,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFFE11D48,
                    secondaryColor = 0xFFF97316
                ),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE)
            )
        ),
        // 10
        SavedStylePreset(
            id = "preset_ticket_stub",
            name = "Ticket Pass",
            subtitle = "Perforated event stub card",
            category = PresetCategory.FRAMED,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.SQUARE,
                pixelSizeFactor = 0.92f,
                eyeFrameStyle = QrEyeFrameStyle.SQUARE,
                eyeBallStyle = QrEyeBallStyle.SQUARE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF18181B, 0xFF18181B),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.TICKET_STUB,
                    label = "ADMIT ONE",
                    subtitle = "PRESENT AT GATE",
                    frameFillColor = 0xFFFAFAFA,
                    accentColor = 0xFF18181B,
                    textColor = 0xFFFFFFFF,
                    cornerRadiusDp = 18f
                )
            )
        ),
        // 11
        SavedStylePreset(
            id = "preset_header_studio",
            name = "Studio Header",
            subtitle = "Editorial top banner stripe",
            category = PresetCategory.FRAMED,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.ROUNDED,
                pixelCornerRoundness = 0.35f,
                pixelSizeFactor = 0.90f,
                eyeFrameStyle = QrEyeFrameStyle.ROUNDED,
                eyeBallStyle = QrEyeBallStyle.ROUNDED,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF111113),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.SCAN_BAR_HEADER,
                    label = "DIGITAL PASS",
                    frameFillColor = 0xFFFFFFFF,
                    accentColor = 0xFF111113,
                    textColor = 0xFFFFFFFF,
                    cornerRadiusDp = 22f
                )
            )
        ),
        // 12
        SavedStylePreset(
            id = "preset_circular_seal",
            name = "Circle Seal",
            subtitle = "Circular silhouette + ring crest",
            category = PresetCategory.ORGANIC,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.CIRCLE,
                pixelSizeFactor = 0.88f,
                eyeFrameStyle = QrEyeFrameStyle.DOUBLE_RING,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                codeSilhouette = QrCodeSilhouette.CIRCLE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF52525B),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.CIRCULAR_STAMP,
                    label = "VERIFIED",
                    frameFillColor = 0xFFFFFFFF,
                    accentColor = 0xFF111113,
                    textColor = 0xFFFFFFFF
                ),
                ecc = QrEccSetting.QUARTILE
            )
        ),
        // 13
        SavedStylePreset(
            id = "preset_violet_spectrum",
            name = "Ultraviolet Spectrum",
            subtitle = "Sweep violet to rose ink",
            category = PresetCategory.GRADIENT,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.DIAMOND,
                pixelSizeFactor = 0.92f,
                eyeFrameStyle = QrEyeFrameStyle.SQUIRCLE,
                eyeBallStyle = QrEyeBallStyle.DIAMOND,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.SWEEP_GRADIENT,
                    primaryColor = 0xFF6D28D9,
                    secondaryColor = 0xFFEC4899,
                    tertiaryColor = 0xFF4F46E5
                ),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE)
            )
        ),
        // 14
        SavedStylePreset(
            id = "preset_obsidian_gold",
            name = "Obsidian Gold",
            subtitle = "Luxury black & champagne gold",
            category = PresetCategory.DARK,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.HEXAGON,
                pixelSizeFactor = 0.92f,
                eyeFrameStyle = QrEyeFrameStyle.HEXAGON,
                eyeBallStyle = QrEyeBallStyle.HEXAGON,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFFF5D06F,
                    secondaryColor = 0xFFD97706
                ),
                customEyeFrameColors = true,
                eyeFrameBrush = BrushStyleConfig(BrushMode.SOLID, 0xFFFDE68A, 0xFFF59E0B),
                background = BackgroundStyleConfig(BackgroundMode.SOLID, BrushStyleConfig(BrushMode.SOLID, 0xFF101012)),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.GRADIENT_BORDER,
                    label = "VIP ACCESS",
                    frameFillColor = 0xFF151518,
                    accentColor = 0xFFF5D06F,
                    accentSecondaryColor = 0xFFB45309,
                    textColor = 0xFF111113
                )
            )
        ),
        // 15
        SavedStylePreset(
            id = "preset_hollow_rings",
            name = "Mono Rings",
            subtitle = "Technical open-ring modules",
            category = PresetCategory.MONOCHROME,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.RING,
                pixelSizeFactor = 0.90f,
                eyeFrameStyle = QrEyeFrameStyle.DOUBLE_RING,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF111113),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE),
                ecc = QrEccSetting.HIGH
            )
        ),
        // 16
        SavedStylePreset(
            id = "preset_bracket_finder",
            name = "Camera Finder",
            subtitle = "Precision corner brackets",
            category = PresetCategory.FRAMED,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.SQUARE,
                pixelSizeFactor = 0.88f,
                eyeFrameStyle = QrEyeFrameStyle.BRACKET,
                eyeBallStyle = QrEyeBallStyle.CROSS,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF18181B, 0xFF18181B),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.CORNER_BRACKET,
                    label = "POINT CAMERA",
                    frameFillColor = 0xFFFFFFFF,
                    accentColor = 0xFF111113,
                    textColor = 0xFFFFFFFF
                )
            )
        ),
        // 17
        SavedStylePreset(
            id = "preset_ocean_radial",
            name = "Deep Radial",
            subtitle = "Radial indigo burst",
            category = PresetCategory.GRADIENT,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.CIRCLE,
                pixelSizeFactor = 0.90f,
                eyeFrameStyle = QrEyeFrameStyle.CIRCLE,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.RADIAL_GRADIENT,
                    primaryColor = 0xFF0EA5E9,
                    secondaryColor = 0xFF1E3A8A
                ),
                background = BackgroundStyleConfig(BackgroundMode.SOLID, BrushStyleConfig(BrushMode.SOLID, 0xFFF8FAFC)),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE)
            )
        ),
        // 18
        SavedStylePreset(
            id = "preset_wave_barcode_qr",
            name = "Vertical Bars",
            subtitle = "Linear architectural stripes",
            category = PresetCategory.MONOCHROME,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.VERTICAL_BARS,
                pixelSizeFactor = 0.84f,
                eyeFrameStyle = QrEyeFrameStyle.SQUARE,
                eyeBallStyle = QrEyeBallStyle.SQUARE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF18181B, 0xFF18181B),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE)
            )
        ),
        // 19
        SavedStylePreset(
            id = "preset_wave_horizontal",
            name = "Horizon Strands",
            subtitle = "Continuous horizontal bands",
            category = PresetCategory.ORGANIC,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.HORIZONTAL_BARS,
                pixelSizeFactor = 0.84f,
                eyeFrameStyle = QrEyeFrameStyle.ROUNDED,
                eyeBallStyle = QrEyeBallStyle.ROUNDED,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF0F172A,
                    secondaryColor = 0xFF334155
                ),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE)
            )
        ),
        // 20
        SavedStylePreset(
            id = "preset_polaroid_white",
            name = "Instant Print",
            subtitle = "Classic clean Polaroid border",
            category = PresetCategory.FRAMED,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.ROUNDED,
                pixelCornerRoundness = 0.32f,
                pixelSizeFactor = 0.90f,
                eyeFrameStyle = QrEyeFrameStyle.SQUIRCLE,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF111113),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.POLAROID_STAMP,
                    label = "MEMORY · 2026",
                    frameFillColor = 0xFFFFFFFF,
                    accentColor = 0xFF111113,
                    textColor = 0xFFFFFFFF,
                    cornerRadiusDp = 16f
                )
            )
        ),
        // 21
        SavedStylePreset(
            id = "preset_cherry_blossom",
            name = "Rose Velvet",
            subtitle = "Crisp crimson heart motif",
            category = PresetCategory.ORGANIC,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.HEART,
                pixelSizeFactor = 0.94f,
                eyeFrameStyle = QrEyeFrameStyle.LEAF_TR_BL,
                eyeBallStyle = QrEyeBallStyle.STAR,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF9F1239,
                    secondaryColor = 0xFFFB7185
                ),
                customEyeFrameColors = true,
                eyeFrameBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF881337, 0xFFE11D48),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE),
                ecc = QrEccSetting.HIGH
            )
        ),
        // 22
        SavedStylePreset(
            id = "preset_dark_emerald_matrix",
            name = "Terminal Green",
            subtitle = "Dark console phosphor matrix",
            category = PresetCategory.DARK,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.SQUARE,
                pixelSizeFactor = 0.86f,
                eyeFrameStyle = QrEyeFrameStyle.BRACKET,
                eyeBallStyle = QrEyeBallStyle.SQUARE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF34D399, 0xFF10B981),
                background = BackgroundStyleConfig(BackgroundMode.SOLID, BrushStyleConfig(BrushMode.SOLID, 0xFF06120E)),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.NEON_GLOW,
                    label = "EXEC // QR",
                    frameFillColor = 0xFF081A14,
                    accentColor = 0xFF34D399,
                    accentSecondaryColor = 0xFF059669,
                    textColor = 0xFF06120E
                )
            )
        ),
        // 23
        SavedStylePreset(
            id = "preset_hex_emblem",
            name = "Honeycomb Emblem",
            subtitle = "Hexagonal badge silhouette",
            category = PresetCategory.ORGANIC,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.HEXAGON,
                pixelSizeFactor = 0.94f,
                eyeFrameStyle = QrEyeFrameStyle.HEXAGON,
                eyeBallStyle = QrEyeBallStyle.HEXAGON,
                codeSilhouette = QrCodeSilhouette.HEXAGON,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFFB45309,
                    secondaryColor = 0xFFF59E0B
                ),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE),
                ecc = QrEccSetting.QUARTILE
            )
        ),
        // 24
        SavedStylePreset(
            id = "preset_bauhaus_red",
            name = "Bauhaus Signal",
            subtitle = "Black grid + crimson eyes",
            category = PresetCategory.MONOCHROME,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.SQUARE,
                pixelSizeFactor = 0.90f,
                eyeFrameStyle = QrEyeFrameStyle.SQUARE,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF111113),
                customEyeFrameColors = true,
                eyeFrameBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF111113),
                customEyeBallColors = true,
                eyeBallBrush = BrushStyleConfig(BrushMode.SOLID, 0xFFDC2626, 0xFFDC2626),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE)
            )
        ),
        // 25
        SavedStylePreset(
            id = "preset_titanium_slate",
            name = "Titanium Dark",
            subtitle = "Silver ink on matte slate",
            category = PresetCategory.DARK,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.WAVE_V,
                pixelSizeFactor = 0.86f,
                eyeFrameStyle = QrEyeFrameStyle.SQUIRCLE,
                eyeBallStyle = QrEyeBallStyle.OCTAGON,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFFF4F4F5,
                    secondaryColor = 0xFFA1A1AA
                ),
                background = BackgroundStyleConfig(BackgroundMode.SOLID, BrushStyleConfig(BrushMode.SOLID, 0xFF18181B)),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.MINIMAL_OUTLINE,
                    label = "TITANIUM PRO",
                    frameFillColor = 0xFF18181B,
                    accentColor = 0xFFE4E4E7,
                    textColor = 0xFFFFFFFF
                )
            )
        ),
        // 26
        SavedStylePreset(
            id = "preset_arctic_frost",
            name = "Arctic Frosted",
            subtitle = "Crisp glacier blue & frost",
            category = PresetCategory.GRADIENT,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.ROUNDED,
                pixelCornerRoundness = 0.45f,
                pixelSizeFactor = 0.90f,
                lightPixelStyle = QrLightPixelStyle.DOT,
                lightPixelColor = 0x1A0284C7,
                eyeFrameStyle = QrEyeFrameStyle.THREE_ROUND,
                eyeBallStyle = QrEyeBallStyle.SQUIRCLE,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF0369A1,
                    secondaryColor = 0xFF38BDF8
                ),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE)
            )
        ),
        // 27
        SavedStylePreset(
            id = "preset_shield_secure",
            name = "Shield Vault",
            subtitle = "Armor shield grid + high ECC",
            category = PresetCategory.ORGANIC,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.SHIELD,
                pixelSizeFactor = 0.92f,
                eyeFrameStyle = QrEyeFrameStyle.CHAMFER,
                eyeBallStyle = QrEyeBallStyle.OCTAGON,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF1E293B, 0xFF475569),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE),
                ecc = QrEccSetting.HIGH
            )
        ),
        // 28
        SavedStylePreset(
            id = "preset_twin_tag",
            name = "Twin Badge",
            subtitle = "Top title + bottom CTA pill",
            category = PresetCategory.FRAMED,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.ROUNDED,
                pixelCornerRoundness = 0.38f,
                pixelSizeFactor = 0.92f,
                eyeFrameStyle = QrEyeFrameStyle.ROUNDED,
                eyeBallStyle = QrEyeBallStyle.CIRCLE,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF111113),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.DOUBLE_PILL,
                    label = "QRAFT STUDIO",
                    subtitle = "SCAN NOW",
                    frameFillColor = 0xFFFFFFFF,
                    accentColor = 0xFF111113,
                    textColor = 0xFFFFFFFF
                )
            )
        ),
        // 29
        SavedStylePreset(
            id = "preset_starlight",
            name = "Starfield Ink",
            subtitle = "Sparkle stars in deep plum",
            category = PresetCategory.ORGANIC,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.STAR,
                pixelSizeFactor = 0.96f,
                eyeFrameStyle = QrEyeFrameStyle.SQUIRCLE,
                eyeBallStyle = QrEyeBallStyle.STAR,
                darkBrush = BrushStyleConfig(
                    mode = BrushMode.LINEAR_GRADIENT,
                    primaryColor = 0xFF581C87,
                    secondaryColor = 0xFFC026D3
                ),
                outerFrame = OuterFrameConfig(OuterFrameStyle.NONE),
                ecc = QrEccSetting.HIGH
            )
        ),
        // 30
        SavedStylePreset(
            id = "preset_speech_bubble",
            name = "Speech Callout",
            subtitle = "Interactive bubble prompt",
            category = PresetCategory.FRAMED,
            design = QrDesignSpec(
                pixelStyle = QrPixelStyle.CROSS,
                pixelSizeFactor = 0.92f,
                eyeFrameStyle = QrEyeFrameStyle.ROUNDED,
                eyeBallStyle = QrEyeBallStyle.CROSS,
                darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF18181B, 0xFF3F3F46),
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.SPEECH_BUBBLE,
                    label = "SCAN FOR LINK",
                    frameFillColor = 0xFFFFFFFF,
                    accentColor = 0xFF111113,
                    textColor = 0xFFFFFFFF
                ),
                ecc = QrEccSetting.QUARTILE
            )
        )
    )

    fun starterItems(now: Long = System.currentTimeMillis()): List<QrCreationItem> = listOf(
        QrCreationItem(
            id = "starter_wifi",
            title = "Guest Studio Wi-Fi",
            type = QrContentType.WIFI,
            payload = QrPayloadFields(
                wifiSsid = "Qraft_Guest_5G",
                wifiPassword = "studio-craft-2026",
                wifiSecurity = WifiSecurity.WPA
            ),
            design = builtInPresets[3].design,
            isFavorite = true,
            isVerifiedScannable = true,
            createdAtMillis = now - 3600_000L * 2,
            updatedAtMillis = now - 3600_000L * 2
        ),
        QrCreationItem(
            id = "starter_url",
            title = "Qraft Portfolio",
            type = QrContentType.URL,
            payload = QrPayloadFields(url = "https://qraft.studio/design"),
            design = builtInPresets[1].design.copy(
                logo = LogoStyleConfig(
                    sourceType = LogoSourceType.MONOGRAM,
                    monogramText = "QR",
                    monogramBgColor = 0xFF111113,
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
            id = "starter_barcode",
            title = "Inventory Code 128",
            type = QrContentType.TEXT,
            payload = QrPayloadFields(text = "QRAFT-PRO-2026"),
            design = builtInPresets[0].design,
            isFavorite = false,
            isVerifiedScannable = true,
            createdAtMillis = now - 3600_000L * 14,
            updatedAtMillis = now - 3600_000L * 14,
            codeKind = CodeKind.BARCODE,
            barcode = BarcodeDesignSpec(
                format = BarcodeFormatType.CODE_128,
                data = "QRAFT-PRO-2026",
                caption = "QRAFT INVENTORY",
                framed = true
            )
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
        )
    )
}
