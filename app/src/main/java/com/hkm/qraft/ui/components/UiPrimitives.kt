package com.hkm.qraft.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.qraft.ui.theme.AppMotion
import com.hkm.qraft.ui.theme.AppleAnimatedSwap
import com.hkm.qraft.ui.theme.AppleGlassSurface
import com.hkm.qraft.ui.theme.AppleGlassVariant
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleLiquidRefraction
import com.hkm.qraft.ui.theme.applePressable
import com.hkm.qraft.ui.theme.rememberReduceMotion
import dev.chrisbanes.haze.HazeState

enum class QraftButtonVariant {
    PRIMARY,
    SECONDARY,
    TONAL,
    DARK,
    DANGER
}

@Composable
fun QraftHeaderCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = QraftThemeTokens.colors.surface,
    contentColor: Color = QraftThemeTokens.colors.textPrimary,
    borderColor: Color = QraftThemeTokens.colors.cardBorder,
    hazeState: HazeState? = null,
    glassVariant: AppleGlassVariant? = null,
    enabled: Boolean = true
) {
    val visualSize = size.coerceAtLeast(40.dp)
    val useGlass = hazeState != null || glassVariant != null

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .applePressable(enabled = enabled, pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (useGlass) {
            AppleGlassSurface(
                modifier = Modifier.size(visualSize),
                shape = CircleShape,
                cornerRadiusDp = visualSize / 2,
                variant = glassVariant ?: AppleGlassVariant.Regular,
                hazeState = hazeState,
                enableRefraction = true,
                elevation = 4.dp,
                contentColor = contentColor
            ) {
                Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(visualSize)
                    .clip(CircleShape)
                    .background(containerColor)
                    .border(1.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun QraftPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: QraftButtonVariant = QraftButtonVariant.PRIMARY,
    leadingIcon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 48.dp
) {
    val colors = QraftThemeTokens.colors
    val targetContainer = when (variant) {
        QraftButtonVariant.PRIMARY -> colors.espressoAccent
        QraftButtonVariant.SECONDARY -> colors.surface
        QraftButtonVariant.TONAL -> colors.surfaceMuted
        QraftButtonVariant.DARK -> colors.activePillBg
        QraftButtonVariant.DANGER -> colors.dangerBg
    }
    val targetContent = when (variant) {
        QraftButtonVariant.PRIMARY -> colors.espressoAccentContent
        QraftButtonVariant.SECONDARY -> colors.textPrimary
        QraftButtonVariant.TONAL -> colors.textPrimary
        QraftButtonVariant.DARK -> colors.activePillContent
        QraftButtonVariant.DANGER -> colors.dangerFg
    }
    val container by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = AppMotion.snappySpring(),
        label = "pillBtnBg"
    )
    val contentColor by animateColorAsState(
        targetValue = targetContent,
        animationSpec = AppMotion.snappySpring(),
        label = "pillBtnFg"
    )
    val border = when (variant) {
        QraftButtonVariant.SECONDARY -> BorderStroke(1.dp, colors.cardBorder)
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        border = border,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .height(height)
            .applePressable(enabled = enabled && !loading, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading || leadingIcon != null) {
                AppleAnimatedSwap(targetState = loading) { isLoading ->
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = contentColor
                        )
                    } else if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QraftSectionCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = QraftThemeTokens.colors.surface,
    borderColor: Color = QraftThemeTokens.colors.cardBorder,
    cornerRadius: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickMod = if (onClick != null) Modifier.applePressable(onClick = onClick) else Modifier
    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth().then(clickMod)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun QraftSectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = QraftThemeTokens.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .applePressable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.espressoAccent
                )
            }
        }
    }
}

@Composable
fun QraftStatusBarScrim(
    modifier: Modifier = Modifier
) {
    val colors = QraftThemeTokens.colors
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusTop + 18.dp)
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to colors.background.copy(alpha = 0.96f),
                        0.68f to colors.background.copy(alpha = 0.68f),
                        1.0f to colors.background.copy(alpha = 0f)
                    )
                )
            )
    )
}

/**
 * Apple sliding liquid-pill resolution selector (512 / 1K / 2K / 4K / 8K).
 * Concentric radius discipline: 18.dp outer track - 4.dp inset = 14.dp active pill.
 */
@Composable
fun QraftResolutionSelector(
    selectedPx: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QraftThemeTokens.colors
    val reduceMotion = rememberReduceMotion()
    val options = listOf(
        512 to "512",
        1024 to "1K",
        2048 to "2K",
        4096 to "4K",
        8192 to "8K"
    )
    val selectedIdx = options.indexOfFirst { it.first == selectedPx }.coerceAtLeast(0)
    val outerPadding = 4.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceMuted)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
            .padding(outerPadding)
    ) {
        val segmentWidth = maxWidth / options.size
        val targetOffset = segmentWidth * selectedIdx
        val pillOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = AppMotion.layoutSpring(),
            label = "resPillOffset"
        )

        // Gliding concentric liquid pill
        Box(
            modifier = Modifier
                .offset(x = if (reduceMotion) targetOffset else pillOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.activePillBg)
                .appleLiquidRefraction(
                    cornerRadius = 14.dp,
                    refractionStrength = 4.5f,
                    specularStrength = 0.16f
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
        ) {
            options.forEach { (px, label) ->
                val selected = selectedPx == px
                val titleColor by animateColorAsState(
                    targetValue = if (selected) colors.activePillContent else colors.textPrimary,
                    animationSpec = AppMotion.snappySpring(),
                    label = "resTitle_$px"
                )
                val subColor by animateColorAsState(
                    targetValue = if (selected) colors.activePillContent.copy(alpha = 0.78f) else colors.textSecondary,
                    animationSpec = AppMotion.snappySpring(),
                    label = "resSub_$px"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .applePressable(pressedScale = 0.95f) {
                            if (!selected) onSelect(px)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            maxLines = 1
                        )
                        Text(
                            text = "$px",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = subColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Gliding Apple Segmented Control with concentric corners (`16.dp` outer - `4.dp` pad = `12.dp` pill),
 * spring-driven sliding lens (`AppMotion.layoutSpring()`), and `applePressable` tactile feedback.
 */
@Composable
fun AppleSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    icons: List<ImageVector?>? = null
) {
    if (options.isEmpty()) return
    val colors = QraftThemeTokens.colors
    val reduceMotion = rememberReduceMotion()
    val safeIndex = selectedIndex.coerceIn(0, options.lastIndex)
    val outerRadius = 16.dp
    val pad = 4.dp
    val innerRadius = outerRadius - pad

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .height(height)
            .clip(RoundedCornerShape(outerRadius))
            .background(colors.surfaceMuted)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(outerRadius))
            .padding(pad)
    ) {
        val segmentWidth = maxWidth / options.size
        val targetOffset = segmentWidth * safeIndex
        val pillOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = AppMotion.layoutSpring(),
            label = "segPillOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = if (reduceMotion) targetOffset else pillOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(innerRadius))
                .background(colors.activePillBg)
                .appleLiquidRefraction(
                    cornerRadius = innerRadius,
                    refractionStrength = 4.5f,
                    specularStrength = 0.16f
                )
        )

        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, label ->
                val selected = index == safeIndex
                val fgColor by animateColorAsState(
                    targetValue = if (selected) colors.activePillContent else colors.textSecondary,
                    animationSpec = AppMotion.snappySpring(),
                    label = "segFg_$index"
                )
                val icon = icons?.getOrNull(index)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(innerRadius))
                        .applePressable(pressedScale = 0.96f) {
                            if (!selected) onSelect(index)
                        }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = fgColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = fgColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QraftSegmentedPicker(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AppleSegmentedControl(
        options = options,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier
    )
}
