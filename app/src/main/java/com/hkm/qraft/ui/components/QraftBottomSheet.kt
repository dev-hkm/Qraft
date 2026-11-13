package com.hkm.qraft.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.hkm.qraft.ui.theme.QraftThemeTokens
import kotlinx.coroutines.flow.Flow

internal val QraftParallaxEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** Single, snappy Tonser-grade motion used by every Qraft bottom sheet. */
data class QraftSheetMotionProfile(
    val scrimAlpha: Float,
    val scrimDurationMillis: Int,
    val enterDurationMillis: Int,
    val enterFadeDurationMillis: Int,
    val exitDurationMillis: Int,
    val exitFadeDurationMillis: Int,
    val platformDimAmount: Float?
) {
    companion object {
        val Focused = QraftSheetMotionProfile(
            scrimAlpha = 0.34f,
            scrimDurationMillis = 140,
            enterDurationMillis = 190,
            enterFadeDurationMillis = 140,
            exitDurationMillis = 150,
            exitFadeDurationMillis = 100,
            platformDimAmount = 0f
        )
    }
}

@Composable
fun QraftSheetDragHandle(
    modifier: Modifier = Modifier,
    color: Color = QraftThemeTokens.colors.textSecondary.copy(alpha = 0.42f)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Tonser-derived modal bottom sheet. Opens cleanly to full natural height up to [maxHeightFraction],
 * eliminating Material 3 ModalBottomSheet partial-expansion clipping and drag/scroll conflicts.
 */
@Composable
fun QraftModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeightFraction: Float = 0.90f,
    hardCap: Dp = 780.dp,
    containerColor: Color = QraftThemeTokens.colors.surface,
    contentColor: Color = QraftThemeTokens.colors.textPrimary,
    motion: QraftSheetMotionProfile = QraftSheetMotionProfile.Focused,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle: @Composable (() -> Unit)? = { QraftSheetDragHandle() },
    content: @Composable ColumnScope.(dismissWithAnimation: () -> Unit) -> Unit
) {
    val sheetVisibility = remember { MutableTransitionState(false) }
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    var dismissalRequested by remember { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        sheetVisibility.targetState = true
    }

    LaunchedEffect(
        sheetVisibility.currentState,
        sheetVisibility.targetState,
        sheetVisibility.isIdle,
        dismissalRequested
    ) {
        if (dismissalRequested && sheetVisibility.isIdle && !sheetVisibility.currentState) {
            currentOnDismiss()
        }
    }

    val dismissWithAnimation: () -> Unit = {
        if (!dismissalRequested) {
            dismissalRequested = true
            sheetVisibility.targetState = false
        }
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (sheetVisibility.targetState) {
            motion.scrimAlpha * (1f - backProgress * 0.45f)
        } else 0f,
        animationSpec = tween(
            durationMillis = if (sheetVisibility.targetState) motion.scrimDurationMillis else motion.exitDurationMillis,
            easing = if (sheetVisibility.targetState) LinearOutSlowInEasing else FastOutLinearInEasing
        ),
        label = "qraft_sheet_scrim_alpha"
    )

    Dialog(
        onDismissRequest = dismissWithAnimation,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false
        )
    ) {
        QraftDialogSystemBars(
            background = containerColor,
            platformDimAmount = motion.platformDimAmount
        )
        QraftPredictiveBackHandler(
            enabled = !dismissalRequested,
            onProgress = { progress ->
                if (!dismissalRequested) {
                    backProgress = progress.coerceIn(0f, 1f)
                }
            },
            onBack = {
                backProgress = 0f
                dismissWithAnimation()
            }
        )
        val imePadding = qraftModalImePadding()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha }
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismissWithAnimation
                    )
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val isWideWindow = maxWidth >= 600.dp
                val systemBarPadding = if (isWideWindow) Modifier.safeDrawingPadding() else Modifier.statusBarsPadding()
                val sheetMaxHeight = minOf(
                    maxHeight * maxHeightFraction.coerceIn(0.1f, 1f),
                    hardCap,
                    maxHeight
                )
                val sheetWidth = if (isWideWindow) {
                    (maxWidth - 48.dp).coerceAtLeast(1.dp).coerceAtMost(640.dp)
                } else {
                    maxWidth
                }
                val sheetShape = if (isWideWindow) RoundedCornerShape(28.dp) else shape

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(imePadding)
                        .then(systemBarPadding),
                    contentAlignment = if (isWideWindow) Alignment.Center else Alignment.BottomCenter
                ) {
                    AnimatedVisibility(
                        visibleState = sheetVisibility,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(motion.enterDurationMillis, easing = QraftParallaxEasing)
                        ) + fadeIn(tween(motion.enterFadeDurationMillis), initialAlpha = 0f),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(motion.exitDurationMillis, easing = FastOutLinearInEasing)
                        ) + fadeOut(tween(motion.exitFadeDurationMillis), targetAlpha = 0f),
                        modifier = Modifier
                            .width(sheetWidth)
                            .heightIn(max = sheetMaxHeight)
                    ) {
                        Surface(
                            modifier = modifier
                                .fillMaxWidth()
                                .heightIn(max = sheetMaxHeight)
                                .graphicsLayer {
                                    if (backProgress > 0f) {
                                        translationY = 72.dp.toPx() * backProgress
                                        val s = 1f - 0.04f * backProgress
                                        scaleX = s
                                        scaleY = s
                                    }
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { /* consume inside sheet */ }
                                ),
                            shape = sheetShape,
                            color = containerColor,
                            contentColor = contentColor,
                            tonalElevation = 0.dp,
                            shadowElevation = 18.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isWideWindow) Modifier else Modifier.navigationBarsPadding())
                            ) {
                                if (dragHandle != null) {
                                    dragHandle()
                                }
                                content(dismissWithAnimation)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QraftPredictiveBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
    onProgress: (Float) -> Unit = {}
) {
    val currentOnBack = rememberUpdatedState(onBack)
    val currentOnProgress = rememberUpdatedState(onProgress)

    PredictiveBackHandler(enabled = enabled) { progress: Flow<androidx.activity.BackEventCompat> ->
        try {
            progress.collect { event ->
                val raw = event.progress
                val curved = 1f - (1f - raw) * (1f - raw)
                currentOnProgress.value(curved)
            }
            currentOnProgress.value(1f)
            currentOnBack.value()
        } finally {
            currentOnProgress.value(0f)
        }
    }
}

@Composable
private fun QraftDialogSystemBars(
    background: Color = QraftThemeTokens.colors.surface,
    platformDimAmount: Float? = null
) {
    val view = LocalView.current
    val darkIcons = background.luminance() > 0.179f
    val configuredWindow = remember(view) { mutableStateOf<Window?>(null) }
    DisposableEffect(view, darkIcons, platformDimAmount) {
        val observer = view.viewTreeObserver
        fun applyAppearance() {
            val provider = generateSequence(view.parent) { it.parent }
                .filterIsInstance<DialogWindowProvider>().firstOrNull() ?: return
            val window = provider.window
            if (window !== configuredWindow.value) {
                @Suppress("DEPRECATION")
                if (window.statusBarColor != AndroidColor.TRANSPARENT) {
                    window.statusBarColor = AndroidColor.TRANSPARENT
                }
                @Suppress("DEPRECATION")
                if (window.navigationBarColor != AndroidColor.TRANSPARENT) {
                    window.navigationBarColor = AndroidColor.TRANSPARENT
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
                configuredWindow.value = window
            }
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = darkIcons
                if (Build.VERSION.SDK_INT >= 26) {
                    isAppearanceLightNavigationBars = darkIcons
                }
            }
            if (platformDimAmount != null) {
                val attributes = window.attributes
                val desiredDimAmount = platformDimAmount.coerceIn(0f, 1f)
                if (kotlin.math.abs(attributes.dimAmount - desiredDimAmount) > 0.001f) {
                    window.setDimAmount(desiredDimAmount)
                }
                val hasDimBehind = attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0
                if (desiredDimAmount <= 0f && hasDimBehind) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                } else if (desiredDimAmount > 0f && !hasDimBehind) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                }
            }
        }
        val focus = ViewTreeObserver.OnWindowFocusChangeListener { if (it) applyAppearance() }
        val attach = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { applyAppearance() }
            override fun onViewDetachedFromWindow(v: View) {}
        }
        val preDraw = ViewTreeObserver.OnPreDrawListener {
            val provider = generateSequence(view.parent) { it.parent }
                .filterIsInstance<DialogWindowProvider>().firstOrNull()
            provider?.let {
                val controller = androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
                if (it.window !== configuredWindow.value ||
                    controller.isAppearanceLightStatusBars != darkIcons ||
                    (Build.VERSION.SDK_INT >= 26 && controller.isAppearanceLightNavigationBars != darkIcons)
                ) {
                    applyAppearance()
                }
            }
            true
        }
        observer.addOnPreDrawListener(preDraw)
        observer.addOnWindowFocusChangeListener(focus)
        view.addOnAttachStateChangeListener(attach)
        applyAppearance()
        onDispose {
            if (observer.isAlive) {
                observer.removeOnPreDrawListener(preDraw)
                observer.removeOnWindowFocusChangeListener(focus)
            }
            view.removeOnAttachStateChangeListener(attach)
        }
    }
}

@Composable
private fun qraftModalImePadding(): Modifier {
    val view = LocalView.current
    val window = remember(view) { findHostingWindow(view) }
    val useComposeInsets = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    DisposableEffect(window, useComposeInsets) {
        val previousSoftInputMode = window?.attributes?.softInputMode
        if (window != null && previousSoftInputMode != null) {
            @Suppress("DEPRECATION")
            val adjustMask = WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
            @Suppress("DEPRECATION")
            val adjustMode = if (useComposeInsets) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
            window.setSoftInputMode((previousSoftInputMode and adjustMask.inv()) or adjustMode)
        }
        onDispose {
            if (window != null && previousSoftInputMode != null) {
                window.setSoftInputMode(previousSoftInputMode)
            }
        }
    }

    return if (useComposeInsets) Modifier.imePadding() else Modifier
}

private fun findHostingWindow(view: View): Window? {
    val dialogWindow = generateSequence(view.parent) { it.parent }
        .filterIsInstance<DialogWindowProvider>()
        .firstOrNull()
        ?.window
    if (dialogWindow != null) return dialogWindow

    var context: Context? = view.context
    while (context != null) {
        if (context is Activity) return context.window
        context = (context as? ContextWrapper)?.baseContext
    }
    return null
}
