package com.hkm.qraft.ui.theme

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

enum class AppleGlassVariant {
    /** High-legibility adaptive frosted chrome for toolbars, docks, floating bars, and badges. */
    Regular,

    /** High-transparency optical glass strictly for controls overlaid on dark camera preview or rich media. */
    Clear
}

enum class EdgeFrostDirection {
    Top,
    Bottom
}

/** Tracks whether an ancestor is already rendering an Apple Glass surface to avoid Glass-on-Glass stacking. */
val LocalInsideGlass = compositionLocalOf { false }

private const val LIQUID_GLASS_LENS_AGSL = """
    uniform shader content;
    uniform float2 size;
    uniform float cornerRadius;
    uniform float refractionStrength;
    uniform float specularStrength;
    uniform float4 tintColor;

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
    }

    half4 main(float2 fragCoord) {
        float2 halfSize = size * 0.5;
        float2 p = fragCoord - halfSize;
        float radius = clamp(cornerRadius, 1.0, min(halfSize.x, halfSize.y));
        float dist = sdRoundedBox(p, halfSize, radius);

        float bezelThickness = min(min(halfSize.x, halfSize.y), radius * 0.72 + 12.0);
        float bezelFactor = smoothstep(-bezelThickness, 0.0, dist);
        float edgeMask = 1.0 - smoothstep(-1.0, 1.0, dist);

        float2 n = normalize(p + float2(0.0001));
        float offsetMag = bezelFactor * bezelFactor * refractionStrength;
        float2 sampleCoord = clamp(fragCoord - n * offsetMag, float2(0.0), size - float2(1.0));

        float2 disp = n * offsetMag * 0.075;
        half r = content.eval(clamp(sampleCoord + disp, float2(0.0), size - float2(1.0))).r;
        half g = content.eval(sampleCoord).g;
        half b = content.eval(clamp(sampleCoord - disp, float2(0.0), size - float2(1.0))).b;
        half a = content.eval(fragCoord).a;

        half3 refracted = half3(r, g, b);
        refracted = mix(refracted, half3(tintColor.rgb), half(tintColor.a * 0.14));

        float rim = smoothstep(-4.0, -0.4, dist) * (1.0 - smoothstep(-0.4, 1.0, dist));
        float fresnel = clamp(dot(n, normalize(float2(-0.65, -0.75))) * 0.5 + 0.5, 0.0, 1.0);
        half specular = half(rim * mix(0.28, 1.0, fresnel) * specularStrength);
        refracted += half3(specular);

        return half4(refracted, a) * half(edgeMask);
    }
"""

private fun isRobolectricRuntime(): Boolean =
    Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

/**
 * Tier 1 AGSL Liquid Glass Fresnel + Edge Refraction shader for Android 13+ hero capsules & chrome.
 * Safely no-ops on older API levels, preview mode, and Robolectric headless runtime.
 */
fun Modifier.appleLiquidRefraction(
    cornerRadius: Dp = 24.dp,
    refractionStrength: Float = 8f,
    specularStrength: Float = 0.24f,
    tintColor: Color = Color.White.copy(alpha = 0.10f),
    enabled: Boolean = true
): Modifier = composed {
    val reduceMotion = rememberReduceMotion()
    val reduceTransparency = rememberReduceTransparency()
    val inspection = LocalInspectionMode.current

    if (!enabled ||
        reduceMotion ||
        reduceTransparency ||
        inspection ||
        isRobolectricRuntime() ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
        return@composed this
    }

    val density = LocalDensity.current
    val shader = remember {
        runCatching { RuntimeShader(LIQUID_GLASS_LENS_AGSL) }.getOrNull()
    } ?: return@composed this

    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    val cornerPx = with(density) { cornerRadius.toPx() }

    this
        .onSizeChanged { sizePx = it }
        .graphicsLayer {
            if (sizePx.width <= 0 || sizePx.height <= 0) return@graphicsLayer
            runCatching {
                applyLiquidLensUniforms(
                    shader = shader,
                    width = sizePx.width.toFloat(),
                    height = sizePx.height.toFloat(),
                    cornerPx = cornerPx,
                    refractionStrength = refractionStrength,
                    specularStrength = specularStrength,
                    tintColor = tintColor
                )
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
                    .asComposeRenderEffect()
            }
        }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun applyLiquidLensUniforms(
    shader: RuntimeShader,
    width: Float,
    height: Float,
    cornerPx: Float,
    refractionStrength: Float,
    specularStrength: Float,
    tintColor: Color
) {
    shader.setFloatUniform("size", width, height)
    shader.setFloatUniform("cornerRadius", cornerPx)
    shader.setFloatUniform("refractionStrength", refractionStrength)
    shader.setFloatUniform("specularStrength", specularStrength)
    shader.setFloatUniform("tintColor", tintColor.red, tintColor.green, tintColor.blue, tintColor.alpha)
}

/** Null-safe Haze backdrop source marker so screens stay uncluttered when Haze isn't active. */
fun Modifier.appleHazeSource(hazeState: HazeState?): Modifier =
    if (hazeState != null) this.hazeSource(state = hazeState) else this

/**
 * Adaptive 3-tier Apple Glass surface tuned for Qraft's monochrome palette:
 * - Tier 1 (API 33+): AGSL bevel specular refraction on floating hero pills (`enableRefraction`).
 * - Tier 2: Live Haze 2.x multi-layer backdrop sampling (`hazeState`).
 * - Tier 3 (API 24+ & fallback): Adaptive luminance fill + 1px diagonal Fresnel specular catchlight.
 * Automatically steps up to high-contrast opaque mode when Reduce Transparency is active, and
 * guards against Glass-on-Glass nested blur stacking via `LocalInsideGlass`.
 */
@Composable
fun AppleGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    cornerRadiusDp: Dp = 22.dp,
    variant: AppleGlassVariant = AppleGlassVariant.Regular,
    hazeState: HazeState? = null,
    enableRefraction: Boolean = false,
    elevation: Dp = 6.dp,
    contentColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val c = QraftThemeTokens.colors
    val reduceTransparency = rememberReduceTransparency()
    val alreadyInsideGlass = LocalInsideGlass.current
    val useClear = variant == AppleGlassVariant.Clear
    val canApplyBackdropBlur = hazeState != null && !reduceTransparency && !alreadyInsideGlass && !isRobolectricRuntime()

    val fallbackBg = when {
        reduceTransparency -> if (useClear) Color(0xF2121215) else c.surface
        useClear -> Color(0xFF0A0A0B).copy(alpha = 0.28f)
        c.isDark -> Color(0xFF17171B).copy(alpha = 0.78f)
        else -> Color(0xFFFFFFFF).copy(alpha = 0.84f)
    }

    val blurTint = when {
        useClear -> Color(0xFF0A0A0B).copy(alpha = 0.24f)
        c.isDark -> Color(0xFF151518).copy(alpha = 0.68f)
        else -> Color(0xFFFFFFFF).copy(alpha = 0.74f)
    }

    val fresnelRim = if (reduceTransparency) {
        Brush.linearGradient(
            colors = listOf(c.cardBorder, c.cardBorder.copy(alpha = 0.6f), c.cardBorder)
        )
    } else if (useClear || c.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.44f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.22f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.96f),
                c.cardBorder.copy(alpha = 0.65f),
                Color.Black.copy(alpha = 0.08f)
            )
        )
    }

    val topSheenBrush = remember(c.isDark, useClear, reduceTransparency) {
        if (reduceTransparency) {
            Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent))
        } else {
            val topAlpha = when {
                useClear -> 0.16f
                c.isDark -> 0.07f
                else -> 0.36f
            }
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = topAlpha),
                0.35f to Color.Transparent,
                1f to Color.Transparent
            )
        }
    }

    val resolvedContentColor = contentColor ?: if (useClear) Color.White else c.textPrimary
    val shadowAmbient = if (c.isDark || useClear) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.16f)

    val glassModifier = if (canApplyBackdropBlur) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = blurTint.copy(alpha = 1f),
                tint = HazeTint(blurTint),
                blurRadius = if (useClear) 16.dp else 22.dp,
                noiseFactor = 0.03f
            )
        )
    } else {
        Modifier.background(fallbackBg)
    }

    val refractionMod = if (enableRefraction && !alreadyInsideGlass) {
        Modifier.appleLiquidRefraction(
            cornerRadius = cornerRadiusDp,
            refractionStrength = 6.5f,
            specularStrength = if (useClear || c.isDark) 0.22f else 0.14f,
            tintColor = blurTint
        )
    } else {
        Modifier
    }

    CompositionLocalProvider(
        LocalInsideGlass provides true,
        LocalContentColor provides resolvedContentColor
    ) {
        Box(
            modifier = modifier
                .shadow(
                    elevation = if (reduceTransparency) 2.dp else elevation,
                    shape = shape,
                    ambientColor = shadowAmbient,
                    spotColor = shadowAmbient
                )
                .clip(shape)
                .then(glassModifier)
                .background(topSheenBrush)
                .border(BorderStroke(1.dp, fresnelRim), shape)
                .then(refractionMod),
            content = content
        )
    }
}

/**
 * Progressive edge frost overlay for edge-to-edge scrolling headers and bottom action docks.
 * Smoothly ramps blur radius and background opacity along the edge without hard cutoff lines.
 */
@Composable
fun AppleProgressiveEdgeFrost(
    hazeState: HazeState?,
    direction: EdgeFrostDirection,
    modifier: Modifier = Modifier,
    tintColor: Color = QraftThemeTokens.colors.background,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val reduceTransparency = rememberReduceTransparency()
    val useLiveBlur = hazeState != null && !reduceTransparency && !isRobolectricRuntime()

    val hazeProgressiveMod = if (useLiveBlur) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = tintColor,
                tint = HazeTint(tintColor.copy(alpha = 0.48f)),
                blurRadius = 20.dp,
                noiseFactor = 0.02f
            )
        ) {
            progressive = if (direction == EdgeFrostDirection.Top) {
                HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
            } else {
                HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f)
            }
        }
    } else {
        Modifier
    }

    val scrimColors = if (direction == EdgeFrostDirection.Top) {
        listOf(
            tintColor.copy(alpha = if (reduceTransparency) 0.98f else if (useLiveBlur) 0.72f else 0.94f),
            tintColor.copy(alpha = if (reduceTransparency) 0.92f else if (useLiveBlur) 0.42f else 0.72f),
            Color.Transparent
        )
    } else {
        listOf(
            Color.Transparent,
            tintColor.copy(alpha = if (reduceTransparency) 0.92f else if (useLiveBlur) 0.44f else 0.76f),
            tintColor.copy(alpha = if (reduceTransparency) 0.98f else if (useLiveBlur) 0.80f else 0.95f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(hazeProgressiveMod)
            .background(Brush.verticalGradient(colors = scrimColors)),
        content = content
    )
}

/**
 * Clean non-glass elevated container for content cards where `AppleGlassSurface` is NOT placed
 * over media/scrolling chrome — prevents glass abuse on primary lists & editors while keeping
 * concentric border crispness.
 */
@Composable
fun AppleCleanSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    containerColor: Color = QraftThemeTokens.colors.surface,
    borderColor: Color = QraftThemeTokens.colors.cardBorder,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        content = content
    )
}
