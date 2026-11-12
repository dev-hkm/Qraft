package com.hkm.qraft.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hkm.qraft.model.ThemeMode

@Immutable
data class PastelPair(
    val container: Color,
    val content: Color
)

@Immutable
data class QraftColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val activePillBg: Color,
    val activePillContent: Color,
    val espressoAccent: Color,
    val espressoAccentContent: Color,
    val heroBannerBg: Color,
    val heroBannerTitle: Color,
    val heroBannerBody: Color,
    val timelineDotActive: Color,
    val timelineLine: Color,
    val successBg: Color,
    val successFg: Color,
    val warningBg: Color,
    val warningFg: Color,
    val dangerBg: Color,
    val dangerFg: Color,
    val pastels: List<PastelPair>
) {
    fun pastelAt(index: Int): PastelPair = pastels[index.mod(pastels.size)]
}

private val LightQraftColors = QraftColors(
    isDark = false,
    background = Color(0xFFFAF7F4),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF2ECE6),
    cardBorder = Color(0xFFEFE8E1),
    textPrimary = Color(0xFF1E1D1B),
    textSecondary = Color(0xFF7B736C),
    textTertiary = Color(0xFFA79E96),
    activePillBg = Color(0xFF232220),
    activePillContent = Color(0xFFFFFFFF),
    espressoAccent = Color(0xFF5C2E0B),
    espressoAccentContent = Color(0xFFFFFFFF),
    heroBannerBg = Color(0xFFFCD8BE),
    heroBannerTitle = Color(0xFF23170F),
    heroBannerBody = Color(0xFF6A4D3B),
    timelineDotActive = Color(0xFFF07C2E),
    timelineLine = Color(0xFFDCD3CB),
    successBg = Color(0xFFE4F0C8),
    successFg = Color(0xFF4F7616),
    warningBg = Color(0xFFFFEAD4),
    warningFg = Color(0xFFB55011),
    dangerBg = Color(0xFFFCE0DE),
    dangerFg = Color(0xFFC43B33),
    pastels = listOf(
        PastelPair(Color(0xFFFCE3D2), Color(0xFFE76F25)), // Peach
        PastelPair(Color(0xFFE4F0C8), Color(0xFF6E9725)), // Sage
        PastelPair(Color(0xFFF5DCF1), Color(0xFFBC59AF)), // Orchid
        PastelPair(Color(0xFFFBE7D3), Color(0xFFD67B35)), // Sand
        PastelPair(Color(0xFFDCEEFF), Color(0xFF3E83C4))  // Sky
    )
)

private val DarkQraftColors = QraftColors(
    isDark = true,
    background = Color(0xFF12100E),
    surface = Color(0xFF1E1B18),
    surfaceMuted = Color(0xFF2B2622),
    cardBorder = Color(0xFF322C27),
    textPrimary = Color(0xFFF5EFEA),
    textSecondary = Color(0xFFA79E95),
    textTertiary = Color(0xFF756C65),
    activePillBg = Color(0xFFF5EFEA),
    activePillContent = Color(0xFF181512),
    espressoAccent = Color(0xFFD87A38),
    espressoAccentContent = Color(0xFFFFFFFF),
    heroBannerBg = Color(0xFF312117),
    heroBannerTitle = Color(0xFFFCE6D4),
    heroBannerBody = Color(0xFFCBB19D),
    timelineDotActive = Color(0xFFF28E49),
    timelineLine = Color(0xFF3D3630),
    successBg = Color(0xFF232F19),
    successFg = Color(0xFF9AC74D),
    warningBg = Color(0xFF3B2413),
    warningFg = Color(0xFFF39C59),
    dangerBg = Color(0xFF3D1E1C),
    dangerFg = Color(0xFFF07B73),
    pastels = listOf(
        PastelPair(Color(0xFF372419), Color(0xFFF29352)),
        PastelPair(Color(0xFF232F19), Color(0xFF9AC74D)),
        PastelPair(Color(0xFF342031), Color(0xFFDE84D1)),
        PastelPair(Color(0xFF36281A), Color(0xFFEA9C57)),
        PastelPair(Color(0xFF1D2B38), Color(0xFF6FB1ED))
    )
)

val LocalQraftColors = staticCompositionLocalOf { LightQraftColors }

private val QraftTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
)

@Composable
private fun animateThemeColor(target: Color): Color {
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "themeColor"
    )
    return color
}

@Composable
fun QraftTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val raw = if (useDark) DarkQraftColors else LightQraftColors
    val animatedColors = raw.copy(
        background = animateThemeColor(raw.background),
        surface = animateThemeColor(raw.surface),
        surfaceMuted = animateThemeColor(raw.surfaceMuted),
        cardBorder = animateThemeColor(raw.cardBorder),
        textPrimary = animateThemeColor(raw.textPrimary),
        textSecondary = animateThemeColor(raw.textSecondary),
        activePillBg = animateThemeColor(raw.activePillBg),
        activePillContent = animateThemeColor(raw.activePillContent),
        espressoAccent = animateThemeColor(raw.espressoAccent),
        heroBannerBg = animateThemeColor(raw.heroBannerBg),
        heroBannerTitle = animateThemeColor(raw.heroBannerTitle),
        heroBannerBody = animateThemeColor(raw.heroBannerBody)
    )

    val m3Scheme = if (useDark) {
        darkColorScheme(
            primary = animatedColors.espressoAccent,
            onPrimary = animatedColors.espressoAccentContent,
            background = animatedColors.background,
            onBackground = animatedColors.textPrimary,
            surface = animatedColors.surface,
            onSurface = animatedColors.textPrimary,
            surfaceVariant = animatedColors.surfaceMuted,
            onSurfaceVariant = animatedColors.textSecondary,
            outline = animatedColors.cardBorder
        )
    } else {
        lightColorScheme(
            primary = animatedColors.espressoAccent,
            onPrimary = animatedColors.espressoAccentContent,
            background = animatedColors.background,
            onBackground = animatedColors.textPrimary,
            surface = animatedColors.surface,
            onSurface = animatedColors.textPrimary,
            surfaceVariant = animatedColors.surfaceMuted,
            onSurfaceVariant = animatedColors.textSecondary,
            outline = animatedColors.cardBorder
        )
    }

    CompositionLocalProvider(LocalQraftColors provides animatedColors) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography = QraftTypography,
            content = content
        )
    }
}

object QraftThemeTokens {
    val colors: QraftColors
        @Composable get() = LocalQraftColors.current
}
