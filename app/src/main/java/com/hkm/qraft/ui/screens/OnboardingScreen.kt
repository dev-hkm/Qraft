package com.hkm.qraft.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.hkm.qraft.data.PresetCatalog
import com.hkm.qraft.model.BrushMode
import com.hkm.qraft.model.BrushStyleConfig
import com.hkm.qraft.model.OuterFrameConfig
import com.hkm.qraft.model.OuterFrameStyle
import com.hkm.qraft.model.QrDesignSpec
import com.hkm.qraft.model.QrEyeBallStyle
import com.hkm.qraft.model.QrEyeFrameStyle
import com.hkm.qraft.model.QrPixelStyle
import com.hkm.qraft.ui.components.QraftButtonVariant
import com.hkm.qraft.ui.components.QraftPillButton
import com.hkm.qraft.ui.components.QrArtworkPreview
import com.hkm.qraft.ui.theme.AppMotion
import com.hkm.qraft.ui.theme.AppleGlassSurface
import com.hkm.qraft.ui.theme.AppleGlassVariant
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleHazeSource
import com.hkm.qraft.ui.theme.applePressable
import com.hkm.qraft.ui.theme.rememberReduceMotion
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class OnboardingPageSpec(
    val eyebrow: String,
    val title: String,
    val subtitle: String
)

private val ONBOARDING_PAGES = listOf(
    OnboardingPageSpec(
        eyebrow = "100% FREE FOREVER",
        title = "Craft QR & Barcodes.\nZero Paywalls.",
        subtitle = "No subscriptions, no watermarks, no ads, and zero cloud tracking. Every feature is unlocked and rendered privately on your phone."
    ),
    OnboardingPageSpec(
        eyebrow = "LIVE STUDIO ENGINE",
        title = "Design Beyond\nBlack Squares.",
        subtitle = "Tap a style below to feel it live — customize dots, finder eyes, gradients, logos, and print stamps with real-time scannability checks."
    ),
    OnboardingPageSpec(
        eyebrow = "PRO OUTPUT & SCANNER",
        title = "8K Print, Vectors &\nPrecision Scanner.",
        subtitle = "Export up to 8K PNG, crisp SVG & PDF print sheets, or scan blurry and distant QR codes up to 8K straight back into the Studio."
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QraftThemeTokens.colors
    val reduceMotion = rememberReduceMotion()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()

    val pagerState = rememberPagerState(initialPage = 0) { ONBOARDING_PAGES.size }
    var selectedShowcaseIdx by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState) {
        var initialized = false
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect {
                if (initialized) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                initialized = true
            }
    }

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch {
            if (reduceMotion) {
                pagerState.scrollToPage(pagerState.currentPage - 1)
            } else {
                pagerState.animateScrollToPage(
                    page = pagerState.currentPage - 1,
                    animationSpec = AppMotion.fluidSpring()
                )
            }
        }
    }

    val showcasePresets = remember {
        PresetCatalog.builtInPresets.take(3).ifEmpty {
            listOf(
                com.hkm.qraft.model.SavedStylePreset(
                    id = "fallback_clean",
                    name = "Swiss Mono",
                    subtitle = "Clean",
                    category = com.hkm.qraft.model.PresetCategory.MONOCHROME,
                    design = QrDesignSpec()
                )
            )
        }
    }

    val breatheTransition = rememberInfiniteTransition(label = "onboarding_ambient")
    val ambientPhase by breatheTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_phase"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        color = colors.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Soft monochrome ambient light field backing the Liquid Glass cards
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .appleHazeSource(hazeState)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val shiftX = if (reduceMotion) 0f else (ambientPhase - 0.5f) * size.width * 0.12f
                    val primaryOrb = if (colors.isDark) Color.White.copy(alpha = 0.055f) else Color.Black.copy(alpha = 0.045f)
                    val accentOrb = if (colors.isDark) Color(0xFF4F46E5).copy(alpha = 0.10f) else Color(0xFF27272A).copy(alpha = 0.05f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryOrb, Color.Transparent),
                            center = Offset(size.width * 0.25f + shiftX, size.height * 0.24f),
                            radius = size.minDimension * 0.65f
                        ),
                        radius = size.minDimension * 0.65f,
                        center = Offset(size.width * 0.25f + shiftX, size.height * 0.24f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentOrb, Color.Transparent),
                            center = Offset(size.width * 0.80f - shiftX, size.height * 0.62f),
                            radius = size.minDimension * 0.58f
                        ),
                        radius = size.minDimension * 0.58f,
                        center = Offset(size.width * 0.80f - shiftX, size.height * 0.62f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top chrome: Brand identity + "Skip" button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppleGlassSurface(
                            shape = CircleShape,
                            cornerRadiusDp = 20.dp,
                            hazeState = hazeState,
                            enableRefraction = true,
                            elevation = 4.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCode2,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Qraft",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Free QR & Barcode Studio",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = pagerState.currentPage < ONBOARDING_PAGES.lastIndex,
                        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f),
                        exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.92f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.surfaceMuted,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .height(40.dp)
                                .semantics { role = Role.Button }
                                .applePressable(pressedScale = 0.93f) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onFinish()
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Skip",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Main horizontal pager with scroll-coupled parallax & kinetic scale
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { pageIndex ->
                    val pageSpec = ONBOARDING_PAGES[pageIndex]
                    val pageOffset = ((pagerState.currentPage - pageIndex) +
                        pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                    val absOffset = pageOffset.absoluteValue

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Visual Hero Stage
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 380.dp)
                                .graphicsLayer {
                                    if (!reduceMotion) {
                                        val s = lerp(1f, 0.88f, absOffset)
                                        scaleX = s
                                        scaleY = s
                                        translationX = pageOffset * -42.dp.toPx()
                                        alpha = lerp(1f, 0.45f, absOffset)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (pageIndex) {
                                0 -> OnboardingHeroFreeForever(
                                    hazeState = hazeState,
                                    breathePhase = if (reduceMotion) 0.5f else ambientPhase
                                )
                                1 -> OnboardingHeroLiveStudio(
                                    hazeState = hazeState,
                                    presets = showcasePresets,
                                    selectedIndex = selectedShowcaseIdx,
                                    onSelect = { idx ->
                                        selectedShowcaseIdx = idx
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                                else -> OnboardingHeroProTools(hazeState = hazeState)
                            }
                        }

                        Spacer(Modifier.height(26.dp))

                        // Text copy block with subtler secondary parallax velocity
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 380.dp)
                                .graphicsLayer {
                                    if (!reduceMotion) {
                                        translationX = pageOffset * 28.dp.toPx()
                                        alpha = lerp(1f, 0.25f, absOffset)
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.surfaceMuted,
                                border = BorderStroke(1.dp, colors.cardBorder)
                            ) {
                                Text(
                                    text = pageSpec.eyebrow,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Text(
                                text = pageSpec.title,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 28.sp,
                                    lineHeight = 34.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = pageSpec.subtitle,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bottom Liquid Glass Navigation Dock (gliding capsule dots + primary CTA)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 6.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppleGlassSurface(
                        shape = RoundedCornerShape(28.dp),
                        cornerRadiusDp = 28.dp,
                        hazeState = hazeState,
                        enableRefraction = true,
                        elevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Gliding Capsule Page Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                repeat(ONBOARDING_PAGES.size) { index ->
                                    val isCurrent = pagerState.currentPage == index
                                    val dotWidth by animateDpAsState(
                                        targetValue = if (isCurrent) 24.dp else 7.dp,
                                        animationSpec = AppMotion.snappySpring(),
                                        label = "dot_width_$index"
                                    )
                                    val dotColor by animateColorAsState(
                                        targetValue = if (isCurrent) colors.textPrimary else colors.textTertiary.copy(alpha = 0.45f),
                                        animationSpec = tween(220),
                                        label = "dot_color_$index"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .height(7.dp)
                                            .width(dotWidth)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                            .applePressable(pressedScale = 0.90f) {
                                                scope.launch {
                                                    if (reduceMotion) {
                                                        pagerState.scrollToPage(index)
                                                    } else {
                                                        pagerState.animateScrollToPage(index, animationSpec = AppMotion.fluidSpring())
                                                    }
                                                }
                                            }
                                    )
                                }
                            }

                            val isLastPage = pagerState.currentPage == ONBOARDING_PAGES.lastIndex
                            AnimatedContent(
                                targetState = isLastPage,
                                transitionSpec = {
                                    (fadeIn(tween(180)) + scaleIn(initialScale = 0.92f)) togetherWith
                                        (fadeOut(tween(140)) + scaleOut(targetScale = 0.94f))
                                },
                                label = "onboarding_cta"
                            ) { finalSlide ->
                                QraftPillButton(
                                    text = if (finalSlide) "Start Crafting" else "Continue",
                                    variant = QraftButtonVariant.PRIMARY,
                                    leadingIcon = if (finalSlide) Icons.Rounded.AutoAwesome else Icons.AutoMirrored.Rounded.ArrowForward,
                                    height = 46.dp,
                                    modifier = Modifier.width(if (finalSlide) 174.dp else 144.dp),
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (finalSlide) {
                                            onFinish()
                                        } else {
                                            scope.launch {
                                                val next = (pagerState.currentPage + 1).coerceAtMost(ONBOARDING_PAGES.lastIndex)
                                                if (reduceMotion) {
                                                    pagerState.scrollToPage(next)
                                                } else {
                                                    pagerState.animateScrollToPage(next, animationSpec = AppMotion.fluidSpring())
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeroFreeForever(
    hazeState: dev.chrisbanes.haze.HazeState,
    breathePhase: Float
) {
    val colors = QraftThemeTokens.colors
    val heroDesign = remember {
        QrDesignSpec(
            pixelStyle = QrPixelStyle.ROUNDED,
            pixelCornerRoundness = 0.48f,
            pixelSizeFactor = 0.92f,
            eyeFrameStyle = QrEyeFrameStyle.SQUIRCLE,
            eyeBallStyle = QrEyeBallStyle.CIRCLE,
            darkBrush = BrushStyleConfig(BrushMode.SOLID, 0xFF111113, 0xFF27272A),
            outerFrame = OuterFrameConfig(
                style = OuterFrameStyle.BOTTOM_SCAN_BANNER,
                label = "FREE FOREVER",
                frameFillColor = 0xFFFFFFFF,
                accentColor = 0xFF111113,
                textColor = 0xFFFFFFFF
            )
        )
    }

    AppleGlassSurface(
        shape = RoundedCornerShape(30.dp),
        cornerRadiusDp = 30.dp,
        hazeState = hazeState,
        enableRefraction = true,
        elevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    val s = lerp(0.985f, 1.015f, breathePhase)
                    scaleX = s
                    scaleY = s
                }
            ) {
                QrArtworkPreview(
                    data = "https://github.com/dev-hkm/Qraft",
                    design = heroDesign,
                    qrViewportSize = 166.dp,
                    showCheckerboardForTransparent = false
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                OnboardingMiniBadge(icon = Icons.Rounded.LockOpen, label = "Zero Paywalls")
                OnboardingMiniBadge(icon = Icons.Rounded.Shield, label = "100% Offline")
            }
        }
    }
}

@Composable
private fun OnboardingHeroLiveStudio(
    hazeState: dev.chrisbanes.haze.HazeState,
    presets: List<com.hkm.qraft.model.SavedStylePreset>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val colors = QraftThemeTokens.colors
    val activePreset = presets[selectedIndex.coerceIn(0, presets.lastIndex)]

    AppleGlassSurface(
        shape = RoundedCornerShape(30.dp),
        cornerRadiusDp = 30.dp,
        hazeState = hazeState,
        enableRefraction = true,
        elevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QrArtworkPreview(
                data = "https://qraft.studio",
                design = activePreset.design,
                qrViewportSize = 164.dp,
                showCheckerboardForTransparent = false
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEachIndexed { idx, preset ->
                    val selected = idx == selectedIndex
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) colors.activePillBg else colors.surfaceMuted,
                        border = BorderStroke(1.dp, if (selected) colors.activePillBg else colors.cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .semantics { role = Role.Button }
                            .applePressable(pressedScale = 0.94f) { onSelect(idx) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = preset.name.substringBefore(" "),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) colors.activePillContent else colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeroProTools(
    hazeState: dev.chrisbanes.haze.HazeState
) {
    AppleGlassSurface(
        shape = RoundedCornerShape(30.dp),
        cornerRadiusDp = 30.dp,
        hazeState = hazeState,
        enableRefraction = true,
        elevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OnboardingFeatureRow(
                icon = Icons.Rounded.HighQuality,
                title = "8K PNG, SVG Vector & PDF",
                subtitle = "Pixel-perfect digital export or multi-grid print labels ready in one tap."
            )
            OnboardingFeatureRow(
                icon = Icons.Rounded.CenterFocusStrong,
                title = "Precision Zoom Scanner & Remix",
                subtitle = "Scan distant codes or 8K gallery images and remix them straight in Studio."
            )
            OnboardingFeatureRow(
                icon = Icons.Rounded.Palette,
                title = "Local Vault & Portable Backups",
                subtitle = "Save, favorite, archive, and export .qraft backups without ever creating an account."
            )
        }
    }
}

@Composable
private fun OnboardingMiniBadge(
    icon: ImageVector,
    label: String
) {
    val colors = QraftThemeTokens.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceMuted)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val colors = QraftThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceMuted)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}
