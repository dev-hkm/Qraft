package com.hkm.qraft.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.qraft.qr.QrEngine
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick
import kotlin.math.roundToInt

object ColorSwatches {
    val curated: List<Long> = listOf(
        0xFF1E1D1B, 0xFF5C2E0B, 0xFFE76F25, 0xFFF07C2E,
        0xFFD9385A, 0xFFBC59AF, 0xFF6A237E, 0xFF1456A0,
        0xFF1898C4, 0xFF2F5E1B, 0xFF6E9725, 0xFFE88A3E,
        0xFFFFFFFF, 0xFFFAF7F4, 0xFFFCD8BE, 0xFFE4F0C8,
        0xFFF5DCF1, 0xFFDCEEFF, 0xFF191614, 0xFF282320
    )
    val recent = mutableStateListOf<Long>()

    fun recordRecent(argb: Long) {
        recent.remove(argb)
        recent.add(0, argb)
        if (recent.size > 6) {
            recent.removeAt(recent.lastIndex)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerModalSheet(
    title: String,
    initialArgb: Long,
    allowAlpha: Boolean = false,
    contrastAgainstArgb: Long? = null,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    val colors = QraftThemeTokens.colors
    val initialColor = remember(initialArgb) { Color(initialArgb) }
    val hsvInit = remember(initialArgb) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), arr)
        Triple(arr[0], arr[1], arr[2])
    }

    var hue by remember { mutableFloatStateOf(hsvInit.first) }
    var sat by remember { mutableFloatStateOf(hsvInit.second) }
    var value by remember { mutableFloatStateOf(hsvInit.third) }
    var alpha by remember { mutableFloatStateOf(if (allowAlpha) initialColor.alpha else 1f) }

    val currentColor = remember(hue, sat, value, alpha) {
        val rgb = android.graphics.Color.HSVToColor(
            (alpha * 255).roundToInt().coerceIn(0, 255),
            floatArrayOf(hue, sat, value)
        )
        rgb.toLong() and 0xFFFFFFFFL
    }

    fun formatHex(argb: Long): String = if (allowAlpha) {
        String.format("#%08X", argb)
    } else {
        String.format("#%06X", argb and 0xFFFFFFL)
    }

    var isEditingHex by remember { mutableStateOf(false) }
    var hexText by remember { mutableStateOf(formatHex(currentColor)) }
    LaunchedEffect(currentColor, isEditingHex) {
        if (!isEditingHex) {
            hexText = formatHex(currentColor)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.textTertiary.copy(alpha = 0.45f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Tap swatches or drag anywhere in the spectrum",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(currentColor))
                        .border(1.5.dp, colors.cardBorder, CircleShape)
                )
            }

            // Saturation / Brightness box
            val pureHueColor = remember(hue) {
                Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            sat = (pos.x / size.width).coerceIn(0f, 1f)
                            value = 1f - (pos.y / size.height).coerceIn(0f, 1f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            sat = (change.position.x / size.width).coerceIn(0f, 1f)
                            value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.White, pureHueColor)
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black)
                        )
                    )
                    val cx = sat * size.width
                    val cy = (1f - value) * size.height
                    drawCircle(
                        color = Color.White,
                        radius = 12.dp.toPx(),
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color(currentColor),
                        radius = 8.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }

            // Hue rainbow bar
            val rainbow = remember {
                listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            hue = ((pos.x / size.width).coerceIn(0f, 1f)) * 360f
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            hue = ((change.position.x / size.width).coerceIn(0f, 1f)) * 360f
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(brush = Brush.horizontalGradient(rainbow))
                    val x = (hue / 360f) * size.width
                    drawCircle(
                        color = Color.White,
                        radius = 11.dp.toPx(),
                        center = Offset(x, size.height / 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }
            }

            if (allowAlpha) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Opacity", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                        Text("${(alpha * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
                    }
                    Slider(
                        value = alpha,
                        onValueChange = { alpha = it },
                        colors = SliderDefaults.colors(
                            thumbColor = colors.espressoAccent,
                            activeTrackColor = colors.espressoAccent
                        )
                    )
                }
            }

            // Live Contrast Ratio Indicator + Quick swatches
            val vsLight = QrEngine.contrastRatio(currentColor, 0xFFFFFFFFL)
            val vsDark = QrEngine.contrastRatio(currentColor, 0xFF1E1D1BL)
            val targetContrast = contrastAgainstArgb?.let { QrEngine.contrastRatio(currentColor, it) }
            val contrastSummary = if (targetContrast != null) {
                "QR Contrast: ${"%.1f".format(targetContrast)}:1 • " +
                    if (targetContrast >= 4.5) "Excellent" else if (targetContrast >= 3.0) "Good" else "Low Margin"
            } else {
                "Contrast vs White: ${"%.1f".format(vsLight)}:1 • vs Dark: ${"%.1f".format(vsDark)}:1"
            }
            val contrastOk = targetContrast?.let { it >= 3.0 } ?: (vsLight >= 3.0 || vsDark >= 3.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (contrastOk) colors.successBg else colors.warningBg)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = contrastSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (contrastOk) colors.successFg else colors.warningFg,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (ColorSwatches.recent.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    ColorSwatches.recent.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(swatch))
                                .border(1.dp, colors.cardBorder, CircleShape)
                                .appleBounceClick {
                                    val arr = FloatArray(3)
                                    android.graphics.Color.colorToHSV(swatch.toInt(), arr)
                                    hue = arr[0]
                                    sat = arr[1]
                                    value = arr[2]
                                    isEditingHex = false
                                }
                        )
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ColorSwatches.curated) { swatch ->
                    val isSelected = (swatch and 0xFFFFFFL) == (currentColor and 0xFFFFFFL)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(swatch))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) colors.espressoAccent else colors.cardBorder,
                                shape = CircleShape
                            )
                            .appleBounceClick {
                                val arr = FloatArray(3)
                                android.graphics.Color.colorToHSV(swatch.toInt(), arr)
                                hue = arr[0]
                                sat = arr[1]
                                value = arr[2]
                                isEditingHex = false
                            }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input.uppercase().take(9)
                        val clean = input.trim().removePrefix("#")
                        if (clean.length == 6 || clean.length == 8) {
                            runCatching {
                                val parsed = clean.toLong(16)
                                val argb = if (clean.length == 6) (0xFF000000L or parsed) else parsed
                                val c = Color(argb)
                                val arr = FloatArray(3)
                                android.graphics.Color.colorToHSV(c.toArgb(), arr)
                                hue = arr[0]
                                sat = arr[1]
                                value = arr[2]
                                if (allowAlpha) alpha = c.alpha
                            }
                        }
                    },
                    label = { Text("Hex Code") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            isEditingHex = focusState.isFocused
                            if (!focusState.isFocused) {
                                hexText = formatHex(currentColor)
                            }
                        }
                )

                QraftPillButton(
                    text = "Apply",
                    variant = QraftButtonVariant.PRIMARY,
                    onClick = {
                        ColorSwatches.recordRecent(currentColor)
                        onColorSelected(currentColor)
                        onDismiss()
                    }
                )
            }
        }
    }
}
