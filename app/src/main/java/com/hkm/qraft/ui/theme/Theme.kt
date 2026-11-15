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
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF1F1F4),
    cardBorder = Color(0xFFE6E6EA),
    textPrimary = Color(0xFF111113),
    textSecondary = Color(0xFF686870),
    textTertiary = Color(0xFF9C9CA4),
    activePillBg = Color(0xFF111113),
    activePillContent = Color(0xFFFFFFFF),
    espressoAccent = Color(0xFF111113),
    espressoAccentContent = Color(0xFFFFFFFF),
    heroBannerBg = Color(0xFFF0F0F4),
    heroBannerTitle = Color(0xFF111113),
    heroBannerBody = Color(0xFF5A5A62),
    timelineDotActive = Color(0xFF111113),
    timelineLine = Color(0xFFDCDCE2),
    successBg = Color(0xFFE7F5EC),
    successFg = Color(0xFF1B7A43),
    warningBg = Color(0xFFFFF4E5),
    warningFg = Color(0xFFB45309),
    dangerBg = Color(0xFFFDECEC),
    dangerFg = Color(0xFFB91C1C),
    pastels = listOf(
        PastelPair(Color(0xFF18181B), Color(0xFFFFFFFF)),
        PastelPair(Color(0xFFF1F1F5), Color(0xFF18181B)),
        PastelPair(Color(0xFFE4E4EA), Color(0xFF111113)),
        PastelPair(Color(0xFF27272A), Color(0xFFF4F4F5)),
        PastelPair(Color(0xFFEBEBF0), Color(0xFF27272A))
    )
)

private val DarkQraftColors = QraftColors(
    isDark = true,
    background = Color(0xFF0A0A0B),
    surface = Color(0xFF151518),
    surfaceMuted = Color(0xFF202024),
    cardBorder = Color(0xFF2B2B31),
    textPrimary = Color(0xFFF8F8FA),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF8E8E96),
    activePillBg = Color(0xFFF8F8FA),
    activePillContent = Color(0xFF0A0A0B),
    espressoAccent = Color(0xFFF8F8FA),
    espressoAccentContent = Color(0xFF0A0A0B),
    heroBannerBg = Color(0xFF17171B),
    heroBannerTitle = Color(0xFFFFFFFF),
    heroBannerBody = Color(0xFFA8A8B2),
    timelineDotActive = Color(0xFFF8F8FA),
    timelineLine = Color(0xFF2D2D34),
    successBg = Color(0xFF15291D),
    successFg = Color(0xFF6EE7A0),
    warningBg = Color(0xFF2F2312),
    warningFg = Color(0xFFFBBF24),
    dangerBg = Color(0xFF331819),
    dangerFg = Color(0xFFF87171),
    pastels = listOf(
        PastelPair(Color(0xFFF4F4F5), Color(0xFF111113)),
        PastelPair(Color(0xFF2C2C34), Color(0xFFF4F4F5)),
        PastelPair(Color(0xFF34343D), Color(0xFFE4E4E7)),
        PastelPair(Color(0xFFE4E4E7), Color(0xFF18181B)),
        PastelPair(Color(0xFF282830), Color(0xFFE4E4E7))
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
        espressoAccentContent = animateThemeColor(raw.espressoAccentContent),
        heroBannerBg = animateThemeColor(raw.heroBannerBg),
        heroBannerTitle = animateThemeColor(raw.heroBannerTitle),
        heroBannerBody = animateThemeColor(raw.heroBannerBody),
        timelineDotActive = animateThemeColor(raw.timelineDotActive),
        timelineLine = animateThemeColor(raw.timelineLine)
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
