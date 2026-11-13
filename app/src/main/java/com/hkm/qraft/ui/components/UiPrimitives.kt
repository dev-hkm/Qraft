package com.hkm.qraft.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick

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
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor, CircleShape)
            .appleBounceClick(enabled = enabled, scalePressed = 0.90f, onClick = onClick),
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
    val container = when (variant) {
        QraftButtonVariant.PRIMARY -> colors.espressoAccent
        QraftButtonVariant.SECONDARY -> colors.surface
        QraftButtonVariant.TONAL -> colors.surfaceMuted
        QraftButtonVariant.DARK -> colors.activePillBg
        QraftButtonVariant.DANGER -> colors.dangerBg
    }
    val contentColor = when (variant) {
        QraftButtonVariant.PRIMARY -> colors.espressoAccentContent
        QraftButtonVariant.SECONDARY -> colors.textPrimary
        QraftButtonVariant.TONAL -> colors.textPrimary
        QraftButtonVariant.DARK -> colors.activePillContent
        QraftButtonVariant.DANGER -> colors.dangerFg
    }
    val border = when (variant) {
        QraftButtonVariant.SECONDARY -> BorderStroke(1.dp, colors.cardBorder)
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        border = border,
        modifier = modifier
            .defaultMinSize(minHeight = height)
            .height(height)
            .appleBounceClick(enabled = enabled && !loading, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
                Spacer(Modifier.width(8.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
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
    val clickMod = if (onClick != null) Modifier.appleBounceClick(onClick = onClick) else Modifier
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
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = colors.espressoAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .appleBounceClick(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
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

@Composable
fun QraftResolutionSelector(
    selectedPx: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QraftThemeTokens.colors
    val options = listOf(
        512 to "512",
        1024 to "1K",
        2048 to "2K",
        4096 to "4K",
        8192 to "8K"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (px, label) ->
            val selected = selectedPx == px
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) colors.activePillBg else colors.surface)
                    .border(
                        1.dp,
                        if (selected) colors.activePillBg else colors.cardBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .appleBounceClick { onSelect(px) }
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (selected) colors.activePillContent else colors.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "${px}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (selected) colors.activePillContent.copy(alpha = 0.78f) else colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
