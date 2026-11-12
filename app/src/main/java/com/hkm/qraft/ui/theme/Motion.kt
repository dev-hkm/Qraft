package com.hkm.qraft.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppleSprings {
    val Snappy = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 580f
    )
    fun <T> snappy(): SpringSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = 580f
    )
    val TactileBounce = spring<Float>(
        dampingRatio = 0.60f,
        stiffness = 520f
    )
    val Smooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val Gentle = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 340f
    )
}

fun Modifier.appleBounceClick(
    enabled: Boolean = true,
    scalePressed: Float = 0.96f,
    haptic: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scalePressed else 1f,
        animationSpec = AppleSprings.TactileBounce,
        label = "applePressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (!enabled) 0.5f else if (pressed) 0.92f else 1f,
        animationSpec = AppleSprings.Snappy,
        label = "applePressAlpha"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled
        ) {
            if (haptic) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onClick()
        }
}

@Composable
fun StaggeredReveal(
    index: Int,
    key: Any? = Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha = remember(key) { Animatable(0f) }
    val offsetY = remember(key) { Animatable(22f) }

    LaunchedEffect(key, index) {
        delay((index.coerceAtMost(8) * 38L))
        launch {
            alpha.animateTo(1f, animationSpec = AppleSprings.Smooth)
        }
        launch {
            offsetY.animateTo(0f, animationSpec = AppleSprings.Gentle)
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        }
    ) {
        content()
    }
}
