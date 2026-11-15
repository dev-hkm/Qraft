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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.hkm.qraft.ui.theme.AppMotion
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.rememberReduceMotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal val QraftParallaxEasing: CubicBezierEasing = AppMotion.AppleSheet

/** Single Apple-grade motion profile used by every Qraft bottom sheet. */
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
            scrimAlpha = 0.40f,
            scrimDurationMillis = 240,
            enterDurationMillis = 360,
            enterFadeDurationMillis = 200,
            exitDurationMillis = 280,
            exitFadeDurationMillis = 160,
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
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Interactive Apple-grade modal bottom sheet driven by ONE normalized progress scalar:
 * - Opens via [AppMotion.navigationSpring] (critically calm, zero wobble on tap open).
 * - Supports 1:1 vertical drag-to-dismiss from both the drag handle and nested-scroll top overscroll.
 * - Applies [AppMotion.rubberBand] resistance when pulled upward past rest and projects flick velocity
 *   via [AppMotion.projectOffset] with direct velocity handoff (`initialVelocity`).
 * - Supports Android 14+ finger-coupled [QraftPredictiveBackHandler].
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
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)

    // Unified scalar: 0f = offscreen/dismissed, 1f = resting open
    val openProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    var rawDragPx by remember { mutableFloatStateOf(0f) }
    var sheetHeightPx by remember { mutableFloatStateOf(with(density) { 420.dp.toPx() }) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    fun triggerDismiss(releaseVelocityY: Float = 0f) {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            if (reduceMotion) {
                openProgress.snapTo(0f)
            } else {
                val safeHeight = sheetHeightPx.coerceAtLeast(360f)
                val normalizedVel = (-releaseVelocityY / safeHeight).coerceIn(-6f, 0f)
                openProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = AppMotion.navigationSpring(),
                    initialVelocity = normalizedVel
                )
            }
            currentOnDismiss()
        }
    }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            openProgress.animateTo(1f, AppMotion.navigationSpring())
        }
    }

    fun settleAfterDrag(velocityY: Float) {
        if (isDismissing) return
        val safeHeight = sheetHeightPx.coerceAtLeast(360f)
        val projected = rawDragPx + AppMotion.projectOffset(velocityY, decelerationRate = 0.99f)
        val shouldDismiss = (projected > safeHeight * 0.34f && rawDragPx > 12f) ||
            (velocityY > 1050f && rawDragPx > 24f)

        if (shouldDismiss) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            rawDragPx = 0f
            triggerDismiss(velocityY)
        } else {
            rawDragPx = 0f
            scope.launch {
                val velNorm = (-velocityY / safeHeight).coerceIn(-4f, 4f)
                openProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = AppMotion.navigationSpring(),
                    initialVelocity = velNorm
                )
            }
        }
    }

    // Nested scroll connection so dragging down at scroll top smoothly peels the sheet
    val nestedSheetScroll = remember {
        var nestedAccumulator = 0f
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && rawDragPx > 0f && available.y < 0f) {
                    val prev = rawDragPx
                    rawDragPx = (rawDragPx + available.y).coerceAtLeast(0f)
                    val safeH = sheetHeightPx.coerceAtLeast(360f)
                    scope.launch { openProgress.snapTo((1f - rawDragPx / safeH).coerceIn(0f, 1.15f)) }
                    return Offset(0f, rawDragPx - prev)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    nestedAccumulator = (rawDragPx + available.y).coerceAtLeast(0f)
                    rawDragPx = nestedAccumulator
                    val safeH = sheetHeightPx.coerceAtLeast(360f)
                    scope.launch { openProgress.snapTo((1f - rawDragPx / safeH).coerceIn(0f, 1.15f)) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (rawDragPx > 0f) {
                    settleAfterDrag(available.y)
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (rawDragPx > 0f) {
                    settleAfterDrag(available.y)
                }
                nestedAccumulator = 0f
                return Velocity.Zero
            }
        }
    }

    val dismissAction: () -> Unit = { triggerDismiss(0f) }

    Dialog(
        onDismissRequest = dismissAction,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        QraftDialogSystemBars(
            background = containerColor,
            platformDimAmount = motion.platformDimAmount
        )

        QraftPredictiveBackHandler(
            enabled = !isDismissing,
            onProgress = { p -> backProgress = p },
            onBack = dismissAction
        )

        val backdropInteraction = remember { MutableInteractionSource() }
        val colors = QraftThemeTokens.colors

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val dynamicCap = ((maxHeight - statusTopPadding).coerceAtLeast(240.dp) * maxHeightFraction).coerceAtMost(hardCap)
            val fallbackHeightPx = with(density) { (dynamicCap * 0.85f).toPx() }

            // One normalized scalar driving scrim, translation, scale, and predictive peel
            val progressScalar = openProgress.value
            val dimFraction = progressScalar.coerceIn(0f, 1f) * (1f - 0.35f * backProgress)

            // Scrim fade strictly coupled to openProgress + backProgress across full edge-to-edge height
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = dimFraction }
                    .background(Color.Black.copy(alpha = motion.scrimAlpha))
                    .clickable(
                        interactionSource = backdropInteraction,
                        indication = null,
                        onClick = dismissAction
                    )
            )

            // Upward rubber-band anchor bleed so over-pulling never exposes a sliver behind system nav bar
            if (progressScalar > 1.005f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(containerColor)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .then(qraftModalImePadding()),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(max = dynamicCap)
                        .onSizeChanged { sz ->
                            if (sz.height > 0) sheetHeightPx = sz.height.toFloat()
                        }
                        .graphicsLayer {
                            val h = (sheetHeightPx.takeIf { it > 0f } ?: fallbackHeightPx)
                            val backCurved = 0.7f * backProgress +
                                0.3f * (1f - (1f - backProgress) * (1f - backProgress))
                            val slideFromOpen = if (progressScalar <= 1f) {
                                (1f - progressScalar) * (h + 36.dp.toPx())
                            } else {
                                // Upward rubber-band when pulled above rest
                                val overPx = (progressScalar - 1f) * h
                                -AppMotion.rubberBand(overPx, h, constant = 0.55f)
                            }
                            translationY = slideFromOpen + backCurved * 28.dp.toPx()
                            val s = (0.96f + 0.04f * progressScalar.coerceIn(0f, 1f)) *
                                (1f - 0.055f * backCurved)
                            scaleX = s
                            scaleY = s
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            alpha = progressScalar.coerceIn(0f, 1f).let { p ->
                                if (p < 0.15f) p / 0.15f else 1f
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .nestedScroll(nestedSheetScroll),
                    shape = shape,
                    color = containerColor,
                    contentColor = contentColor,
                    border = null,
                    shadowElevation = 18.dp,
                    tonalElevation = 0.dp
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        ) {
                            if (dragHandle != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            val tracker = VelocityTracker()
                                            detectVerticalDragGestures(
                                                onDragStart = {
                                                    tracker.resetTracking()
                                                    rawDragPx = 0f
                                                },
                                                onVerticalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    tracker.addPosition(change.uptimeMillis, change.position)
                                                    rawDragPx += dragAmount
                                                    val safeH = sheetHeightPx.coerceAtLeast(360f)
                                                    val targetScalar = if (rawDragPx >= 0f) {
                                                        (1f - rawDragPx / safeH).coerceIn(0f, 1f)
                                                    } else {
                                                        // Upward overshoot rubber-band
                                                        1f + (-rawDragPx / safeH).coerceIn(0f, 0.45f)
                                                    }
                                                    scope.launch { openProgress.snapTo(targetScalar) }
                                                },
                                                onDragEnd = {
                                                    val vy = tracker.calculateVelocity().y
                                                    settleAfterDrag(vy)
                                                },
                                                onDragCancel = {
                                                    settleAfterDrag(0f)
                                                }
                                            )
                                        }
                                ) {
                                    dragHandle()
                                }
                            }
                            content(dismissAction)
                        }
                        // Crisp top-only specular hairline matching the sheet's rounded top edge without cutting the bottom nav edge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(1.dp)
                                .background(colors.cardBorder.copy(alpha = 0.55f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QraftPredictiveBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
    onProgress: (Float) -> Unit = {}
) {
    val currentOnBack = rememberUpdatedState(onBack)
    val currentOnProgress = rememberUpdatedState(onProgress)

    PredictiveBackHandler(enabled = enabled) { progress: Flow<androidx.activity.BackEventCompat> ->
        try {
            progress.collect { event ->
                val raw = event.progress.coerceIn(0f, 1f)
                val curved = 0.7f * raw + 0.3f * (1f - (1f - raw) * (1f - raw))
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
internal fun QraftDialogSystemBars(
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
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                window.setBackgroundDrawableResource(android.R.color.transparent)
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
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
