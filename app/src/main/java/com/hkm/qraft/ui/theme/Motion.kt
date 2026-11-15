package com.hkm.qraft.ui.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * Canonical Apple-grade motion tokens, perceptual springs, physics helpers,
 * hero-bounds registry, and micro-interaction primitives for Qraft.
 */
object AppMotion {
    // ── Easing Curves ────────────────────────────────────────────────────────
    val AppleSheet = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val AppleSilk = CubicBezierEasing(0.28f, 0.11f, 0.14f, 1f)
    val AppleEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val AppleEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    // ── Perceptual Springs ───────────────────────────────────────────────────
    fun <T> navigationSpring(): SpringSpec<T> = spring(dampingRatio = 0.90f, stiffness = 240f)
    fun <T> fluidSpring(): SpringSpec<T> = spring(dampingRatio = 0.88f, stiffness = 260f)
    fun <T> layoutSpring(): SpringSpec<T> = spring(dampingRatio = 0.88f, stiffness = 270f)
    fun <T> gentleSpring(): SpringSpec<T> = spring(dampingRatio = 0.92f, stiffness = 210f)
    fun <T> snappySpring(): SpringSpec<T> = spring(dampingRatio = 0.80f, stiffness = 360f)
    fun pressDownSpring(): SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 420f)
    fun pressReleaseSpring(): SpringSpec<Float> = spring(dampingRatio = 0.68f, stiffness = 300f)
    fun <T> softPopSpring(): SpringSpec<T> = spring(dampingRatio = 0.72f, stiffness = 380f)

    // ── Physics Projections ──────────────────────────────────────────────────
    /** UIScrollView exponential deceleration projection for release velocity handoff. */
    fun projectOffset(velocityPxPerSec: Float, decelerationRate: Float = 0.99f): Float {
        val clampedRate = decelerationRate.coerceIn(0.90f, 0.999f)
        return (velocityPxPerSec / 1000f) * clampedRate / (1f - clampedRate)
    }

    /** iOS UIScrollView asymptotic rubber-band resistance past boundary bounds. */
    fun rubberBand(overshoot: Float, dimension: Float, constant: Float = 0.55f): Float {
        if (overshoot == 0f) return 0f
        val safeDim = dimension.coerceAtLeast(1f)
        val absOvershoot = overshoot.absoluteValue
        val resistance = 1f - 1f / ((absOvershoot * constant / safeDim) + 1f)
        return resistance * safeDim * overshoot.sign
    }

    // ── Transform Presets ────────────────────────────────────────────────────
    fun tabTransform(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) return softCrossDissolve(160)
        return (fadeIn(tween(240, easing = AppleEaseOut)) +
            scaleIn(initialScale = 0.975f, animationSpec = tween(260, easing = AppleSilk))) togetherWith
            (fadeOut(tween(150, easing = AppleEaseOut)) +
                scaleOut(targetScale = 0.985f, animationSpec = tween(150, easing = AppleEaseOut)))
    }

    fun sectionTransform(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) return softCrossDissolve(160)
        return (fadeIn(tween(220, easing = AppleEaseOut)) +
            slideInVertically(tween(260, easing = AppleSilk)) { (it * 0.045f).toInt() }) togetherWith
            (fadeOut(tween(140, easing = AppleEaseOut)) +
                slideOutVertically(tween(140, easing = AppleEaseOut)) { -(it * 0.03f).toInt() })
    }

    fun softCrossDissolve(durationMs: Int = 220): ContentTransform =
        fadeIn(tween(durationMs, easing = AppleSilk)) togetherWith
            fadeOut(tween((durationMs * 0.75f).toInt().coerceAtLeast(80), easing = AppleEaseOut))

    val softExpandEnter: EnterTransition =
        fadeIn(tween(240, easing = AppleEaseOut)) +
            expandVertically(
                animationSpec = spring(dampingRatio = 0.90f, stiffness = 270f),
                expandFrom = Alignment.Top
            )

    val softExpandExit: ExitTransition =
        fadeOut(tween(160, easing = AppleEaseOut)) +
            shrinkVertically(
                animationSpec = spring(dampingRatio = 0.92f, stiffness = 310f),
                shrinkTowards = Alignment.Top
            )

    val floatingBarEnter: EnterTransition =
        fadeIn(tween(220, easing = AppleEaseOut)) +
            slideInVertically(spring(dampingRatio = 0.88f, stiffness = 280f)) { it / 2 } +
            scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.88f, stiffness = 280f))

    val floatingBarExit: ExitTransition =
        fadeOut(tween(160, easing = AppleEaseOut)) +
            slideOutVertically(tween(180, easing = AppleEaseOut)) { it / 2 } +
            scaleOut(targetScale = 0.94f, animationSpec = tween(180, easing = AppleEaseOut))

    val toastEnter: EnterTransition =
        fadeIn(tween(240, easing = AppleEaseOut)) +
            slideInVertically(spring(dampingRatio = 0.82f, stiffness = 320f)) { -it } +
            scaleIn(
                initialScale = 0.88f,
                transformOrigin = TransformOrigin(0.5f, 0f),
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f)
            )

    val toastExit: ExitTransition =
        fadeOut(tween(170, easing = AppleEaseOut)) +
            slideOutVertically(tween(200, easing = AppleSilk)) { -it } +
            scaleOut(
                targetScale = 0.92f,
                transformOrigin = TransformOrigin(0.5f, 0f),
                animationSpec = tween(200, easing = AppleEaseOut)
            )
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

@Composable
fun rememberReduceTransparency(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Secure.getInt(
                context.contentResolver,
                "high_text_contrast_enabled",
                0
            ) == 1
        }.getOrDefault(false)
    }
}

// ── Hero Shared-Bounds Spatial Morph Registry ────────────────────────────────

data class HeroSourceRect(
    val rect: Rect,
    val cornerRadiusDp: Float = 20f
)

@Stable
class HeroBoundsRegistry {
    private val sources = mutableStateMapOf<Any, HeroSourceRect>()

    fun register(key: Any, rect: Rect, cornerRadiusDp: Float = 20f) {
        if (!rect.isEmpty) {
            sources[key] = HeroSourceRect(rect, cornerRadiusDp)
        }
    }

    fun get(key: Any?): HeroSourceRect? = key?.let { sources[it] }
}

val LocalHeroBounds = compositionLocalOf { HeroBoundsRegistry() }

fun Modifier.heroSource(key: Any, cornerRadiusDp: Float = 20f): Modifier = composed {
    val registry = LocalHeroBounds.current
    onGloballyPositioned { coords ->
        if (coords.isAttached) {
            registry.register(key, coords.boundsInRoot(), cornerRadiusDp)
        }
    }
}

// ── Physical Pointer-Down Compression & Haptics ──────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.applePressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    hapticOnClick: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }
    val reduceMotion = rememberReduceMotion()

    val pressModifier = if (enabled && !reduceMotion) {
        Modifier.pointerInput(pressedScale) {
            awaitPointerEventScope {
                while (true) {
                    val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                    val anyPressed = downEvent.changes.any { it.pressed && !it.previousPressed }
                    if (!anyPressed) continue
                    scope.launch {
                        scaleAnim.animateTo(pressedScale, AppMotion.pressDownSpring())
                    }
                    while (true) {
                        val moveOrUp = awaitPointerEvent(PointerEventPass.Initial)
                        val stillPressed = moveOrUp.changes.any { it.pressed }
                        if (!stillPressed) {
                            scope.launch {
                                scaleAnim.animateTo(1f, AppMotion.pressReleaseSpring())
                            }
                            break
                        }
                    }
                }
            }
        }
    } else {
        Modifier
    }

    this
        .graphicsLayer {
            val s = scaleAnim.value
            scaleX = s
            scaleY = s
        }
        .then(pressModifier)
        .combinedClickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onLongClick = onLongClick?.let { longAction ->
                {
                    if (hapticOnClick) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    longAction()
                }
            },
            onClick = {
                if (hapticOnClick) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
}

/** Smooth staggered entrance wave strictly via compositor transforms (`graphicsLayer`). */
fun Modifier.appleEntrance(
    index: Int,
    stepDelayMs: Int = 42,
    translateYDp: Float = 12f,
    reduceMotion: Boolean = false
): Modifier = composed {
    if (reduceMotion) return@composed this

    var shown by remember { mutableStateOf(false) }
    val clampedStep = index.coerceIn(0, 5)
    // Subtle non-uniform organic cadence on first few cards
    val delayMs = (clampedStep * stepDelayMs) + (clampedStep * clampedStep * 3)

    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        shown = true
    }

    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = AppMotion.fluidSpring(),
        label = "appleEntrance"
    )

    graphicsLayer {
        val p = progress.coerceIn(0f, 1.04f)
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * translateYDp.dp.toPx()
        val s = 0.985f + (0.015f * p)
        scaleX = s
        scaleY = s
    }
}

// ── Odometer Numeric Roll ────────────────────────────────────────────────────

@Composable
fun AppleOdometerNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    prefix: String = "",
    suffix: String = ""
) {
    val reduceMotion = rememberReduceMotion()
    val formatted = value.toString()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix.isNotEmpty()) {
            Text(text = prefix, style = style.copy(fontFeatureSettings = "tnum"))
        }
        formatted.forEachIndexed { idx, ch ->
            Box(modifier = Modifier.clipToBounds()) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        if (reduceMotion) {
                            AppMotion.softCrossDissolve(120)
                        } else {
                            val dir = if (targetState > initialState) 1 else -1
                            (fadeIn(tween(180, easing = AppleMotionConst.EaseOut)) +
                                slideInVertically(
                                    animationSpec = spring<IntOffset>(dampingRatio = 0.86f, stiffness = 320f)
                                ) { h -> dir * (h * 0.55f).toInt() }) togetherWith
                                (fadeOut(tween(130, easing = AppleMotionConst.EaseOut)) +
                                    slideOutVertically(
                                        animationSpec = tween(150, easing = AppleMotionConst.EaseOut)
                                    ) { h -> -dir * (h * 0.55f).toInt() })
                        }
                    },
                    label = "odometerDigit_$idx"
                ) { digit ->
                    Text(
                        text = digit.toString(),
                        style = style.copy(fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
        if (suffix.isNotEmpty()) {
            Text(
                text = suffix,
                style = style.copy(fontFeatureSettings = "tnum"),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

private object AppleMotionConst {
    val EaseOut = AppMotion.AppleEaseOut
}

// ── SF-Style Symbolic & Content Swap ─────────────────────────────────────────

@Composable
fun <T> AppleAnimatedSwap(
    targetState: T,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") blurRadiusDp: Float = 2.5f,
    content: @Composable (T) -> Unit
) {
    val reduceMotion = rememberReduceMotion()
    val initialState = remember { targetState }
    var hasTransitioned by remember { mutableStateOf(false) }
    if (targetState != initialState) {
        hasTransitioned = true
    }

    if (!hasTransitioned || reduceMotion) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            content(targetState)
        }
        return
    }

    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(200, easing = AppMotion.AppleSilk)) +
                scaleIn(
                    initialScale = 0.86f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 340f)
                )) togetherWith
                (fadeOut(tween(130, easing = AppMotion.AppleEaseOut)) +
                    scaleOut(
                        targetScale = 0.86f,
                        animationSpec = tween(130, easing = AppMotion.AppleEaseOut)
                    ))
        },
        label = "appleAnimatedSwap"
    ) { state ->
        Box(contentAlignment = Alignment.Center) {
            content(state)
        }
    }
}
