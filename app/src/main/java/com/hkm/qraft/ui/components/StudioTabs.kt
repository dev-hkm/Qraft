package com.hkm.qraft.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.qraft.model.*
import com.hkm.qraft.qr.CustomQrShapes
import com.hkm.qraft.qr.LogoCatalog
import com.hkm.qraft.qr.QrEngine
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun PresetsEditorTab(
    builtInPresets: List<SavedStylePreset>,
    customPresets: List<SavedStylePreset>,
    currentDesign: QrDesignSpec,
    onApplyPreset: (SavedStylePreset) -> Unit,
    onSaveCurrentAsPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit
) {
    val colors = QraftThemeTokens.colors
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("Designer Themes")
            OutlinedButton(
                onClick = { showSaveDialog = true },
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, colors.espressoAccent),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.espressoAccent)
                Spacer(Modifier.width(6.dp))
                Text("Save Look", color = colors.espressoAccent, style = MaterialTheme.typography.labelMedium)
            }
        }

        if (customPresets.isNotEmpty()) {
            SectionLabel("Your Saved Styles")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                customPresets.forEach { preset ->
                    PresetRowCard(
                        preset = preset,
                        isSelected = preset.design == currentDesign,
                        onSelect = { onApplyPreset(preset) },
                        onDelete = { onDeleteCustomPreset(preset.id) }
                    )
                }
            }
        }

        SectionLabel("Curated Presets")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            builtInPresets.forEach { preset ->
                PresetRowCard(
                    preset = preset,
                    isSelected = preset.design == currentDesign,
                    onSelect = { onApplyPreset(preset) },
                    onDelete = null
                )
            }
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = colors.surface,
            title = { Text("Save Custom Style", color = colors.textPrimary) },
            text = {
                QraftTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Preset Name",
                    placeholder = "e.g. Brand Coral Badge"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCurrentAsPreset(name.trim().ifBlank { "My Custom Look" })
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.espressoAccent)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

private object MiniQrThumbnailCache {
    private val cache = android.util.LruCache<String, android.graphics.Bitmap>(32)
    fun get(key: String): android.graphics.Bitmap? = cache.get(key)
    fun put(key: String, bitmap: android.graphics.Bitmap) {
        if (cache.get(key) == null) {
            cache.put(key, bitmap)
        }
    }
}

@Composable
private fun PresetRowCard(
    preset: SavedStylePreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = QraftThemeTokens.colors
    val context = LocalContext.current
    val thumbnail by produceState<android.graphics.Bitmap?>(
        initialValue = MiniQrThumbnailCache.get(preset.id),
        key1 = preset.id
    ) {
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                val safeDesign = if (preset.design.background.mode == BackgroundMode.TRANSPARENT) {
                    preset.design.copy(
                        background = preset.design.background.copy(
                            mode = BackgroundMode.SOLID,
                            brush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFFFFFFFF)
                        )
                    )
                } else {
                    preset.design
                }
                QrEngine.renderQrCoreBitmap(
                    context = context,
                    data = "https://qraft.studio",
                    design = safeDesign.copy(quietZonePadding = 0.06f),
                    sizePx = 136
                ).also { MiniQrThumbnailCache.put(preset.id, it) }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) colors.espressoAccent else colors.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .appleBounceClick { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(preset.design.background.brush.primaryColor))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = thumbnail
                if (bmp != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "${preset.name} live QR preview",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Canvas(modifier = Modifier.size(30.dp)) {
                        val brush = QrEngine.buildComposeBrush(preset.design.darkBrush, size.minDimension)
                        drawCircle(brush = brush, radius = size.minDimension * 0.24f, center = Offset(size.width * 0.30f, size.height * 0.30f))
                        drawCircle(brush = brush, radius = size.minDimension * 0.24f, center = Offset(size.width * 0.72f, size.height * 0.30f))
                        drawCircle(brush = brush, radius = size.minDimension * 0.24f, center = Offset(size.width * 0.30f, size.height * 0.72f))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(
                    preset.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete preset", tint = colors.textTertiary)
                }
            } else if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.espressoAccent)
            }
        }
    }
}

@Composable
fun ShapesEditorTab(
    design: QrDesignSpec,
    onDesignChange: (QrDesignSpec) -> Unit
) {
    val colors = QraftThemeTokens.colors

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // 1. Data Pixels
        SectionLabel("Data Modules (12 Shapes)")
        FlowGrid(items = QrPixelStyle.entries, columns = 4) { style ->
            val selected = design.pixelStyle == style
            ShapePreviewTile(
                label = style.label,
                selected = selected,
                onClick = { onDesignChange(design.copy(pixelStyle = style)) }
            ) {
                Canvas(modifier = Modifier.size(26.dp)) {
                    val modifierShape = CustomQrShapes.pixelShape(style, 0.38f, 0.9f)
                    val path = Path()
                    modifierShape.run { path.path(size.minDimension, io.github.alexzhirkevich.qrose.Neighbors()) }
                    drawPath(
                        path = path,
                        color = if (selected) colors.activePillContent else colors.textPrimary
                    )
                }
            }
        }

        if (design.pixelStyle in setOf(QrPixelStyle.ROUNDED, QrPixelStyle.LIQUID)) {
            LabeledSlider(
                label = "Module Corner Radius",
                valueText = "${(design.pixelCornerRoundness * 200).roundToInt()}%",
                value = design.pixelCornerRoundness,
                valueRange = 0.05f..0.50f,
                onValueChange = { onDesignChange(design.copy(pixelCornerRoundness = it)) }
            )
        }

        LabeledSlider(
            label = "Module Density / Scale",
            valueText = "${(design.pixelSizeFactor * 100).roundToInt()}%",
            value = design.pixelSizeFactor,
            valueRange = 0.55f..1.00f,
            onValueChange = { onDesignChange(design.copy(pixelSizeFactor = it)) }
        )

        // 2. Eye Frame
        SectionLabel("Finder Eye Frames (8 Shapes)")
        FlowGrid(items = QrEyeFrameStyle.entries, columns = 4) { style ->
            val selected = design.eyeFrameStyle == style
            ShapePreviewTile(
                label = style.label,
                selected = selected,
                onClick = { onDesignChange(design.copy(eyeFrameStyle = style)) }
            ) {
                Canvas(modifier = Modifier.size(26.dp)) {
                    val shape = CustomQrShapes.frameShape(style, 0.32f)
                    val p = Path()
                    p.fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                    shape.run { p.path(size.minDimension, io.github.alexzhirkevich.qrose.Neighbors()) }
                    drawPath(
                        path = p,
                        color = if (selected) colors.activePillContent else colors.textPrimary
                    )
                }
            }
        }

        if (design.eyeFrameStyle in setOf(
                QrEyeFrameStyle.ROUNDED,
                QrEyeFrameStyle.LEAF_TL_BR,
                QrEyeFrameStyle.LEAF_TR_BL,
                QrEyeFrameStyle.THREE_ROUND
            )
        ) {
            LabeledSlider(
                label = "Eye Frame Roundness",
                valueText = "${(design.eyeFrameRoundness * 200).roundToInt()}%",
                value = design.eyeFrameRoundness,
                valueRange = 0.05f..0.50f,
                onValueChange = { onDesignChange(design.copy(eyeFrameRoundness = it)) }
            )
        }

        // 3. Eye Ball
        SectionLabel("Finder Eye Center Balls (8 Shapes)")
        FlowGrid(items = QrEyeBallStyle.entries, columns = 4) { style ->
            val selected = design.eyeBallStyle == style
            ShapePreviewTile(
                label = style.label,
                selected = selected,
                onClick = { onDesignChange(design.copy(eyeBallStyle = style)) }
            ) {
                Canvas(modifier = Modifier.size(22.dp)) {
                    val shape = CustomQrShapes.ballShape(style, 0.35f)
                    val p = Path()
                    shape.run { p.path(size.minDimension, io.github.alexzhirkevich.qrose.Neighbors()) }
                    drawPath(
                        path = p,
                        color = if (selected) colors.activePillContent else colors.espressoAccent
                    )
                }
            }
        }

        // 4. Overall QR Silhouette & Light Dots & 4th Eye
        SectionLabel("QR Matrix Silhouette")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            QrCodeSilhouette.entries.forEach { sil ->
                SelectableChip(
                    label = sil.label,
                    selected = design.codeSilhouette == sil,
                    onClick = { onDesignChange(design.copy(codeSilhouette = sil)) }
                )
            }
        }

        SectionLabel("Subtle Light-Module Pattern")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            QrLightPixelStyle.entries.forEach { lp ->
                SelectableChip(
                    label = lp.label,
                    selected = design.lightPixelStyle == lp,
                    onClick = { onDesignChange(design.copy(lightPixelStyle = lp)) }
                )
            }
        }

        ToggleRow(
            title = "4th Corner Alignment Eye",
            subtitle = "Adds a decorative stylized eye at bottom-right",
            checked = design.fourthEyeEnabled,
            onCheckedChange = { onDesignChange(design.copy(fourthEyeEnabled = it)) }
        )
    }
}

private enum class ColorTarget(val label: String) {
    PIXELS("Data Pixels"),
    EYE_FRAME("Eye Frames"),
    EYE_BALL("Eye Balls"),
    BACKGROUND("Background"),
    LIGHT_DOTS("Light Grid")
}

@Composable
fun ColorsEditorTab(
    design: QrDesignSpec,
    onDesignChange: (QrDesignSpec) -> Unit,
    onPickBackgroundPhoto: ((Uri) -> Unit)? = null,
    onExtractColorsFromPhoto: (() -> Unit)? = null
) {
    val colors = QraftThemeTokens.colors
    var target by remember { mutableStateOf(ColorTarget.PIXELS) }
    var colorDialogConfig by remember { mutableStateOf<Triple<String, Long, (Long) -> Unit>?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (onPickBackgroundPhoto != null) {
                onPickBackgroundPhoto(uri)
            } else {
                onDesignChange(
                    design.copy(
                        background = design.background.copy(
                            mode = BackgroundMode.PHOTO,
                            photoUri = uri.toString()
                        )
                    )
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Target chips
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorTarget.entries.forEach { t ->
                SelectableChip(
                    label = t.label,
                    selected = target == t,
                    onClick = { target = t }
                )
            }
        }

        when (target) {
            ColorTarget.PIXELS -> {
                BrushEditorCard(
                    title = "Data Pixel Color & Gradient",
                    brush = design.darkBrush,
                    onBrushChange = { onDesignChange(design.copy(darkBrush = it)) },
                    onPickColor = { label, current, cb ->
                        colorDialogConfig = Triple(label, current, cb)
                    }
                )
            }

            ColorTarget.EYE_FRAME -> {
                ToggleRow(
                    title = "Custom Eye Frame Color",
                    subtitle = "Off = matches data pixel color/gradient",
                    checked = design.customEyeFrameColors,
                    onCheckedChange = { onDesignChange(design.copy(customEyeFrameColors = it)) }
                )
                if (design.customEyeFrameColors) {
                    BrushEditorCard(
                        title = "Eye Frame Fill",
                        brush = design.eyeFrameBrush,
                        onBrushChange = { onDesignChange(design.copy(eyeFrameBrush = it)) },
                        onPickColor = { label, current, cb ->
                            colorDialogConfig = Triple(label, current, cb)
                        }
                    )
                }
            }

            ColorTarget.EYE_BALL -> {
                ToggleRow(
                    title = "Custom Eye Ball Color",
                    subtitle = "Highlight the 3 corner centers independently",
                    checked = design.customEyeBallColors,
                    onCheckedChange = { onDesignChange(design.copy(customEyeBallColors = it)) }
                )
                if (design.customEyeBallColors) {
                    BrushEditorCard(
                        title = "Eye Ball Fill",
                        brush = design.eyeBallBrush,
                        onBrushChange = { onDesignChange(design.copy(eyeBallBrush = it)) },
                        onPickColor = { label, current, cb ->
                            colorDialogConfig = Triple(label, current, cb)
                        }
                    )
                }
            }

            ColorTarget.BACKGROUND -> {
                SectionLabel("Canvas Background Mode")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackgroundMode.entries.forEach { mode ->
                        SelectableChip(
                            label = mode.label,
                            selected = design.background.mode == mode,
                            onClick = {
                                val updatedBrush = if (mode == BackgroundMode.GRADIENT && design.background.brush.mode == BrushMode.SOLID) {
                                    design.background.brush.copy(mode = BrushMode.LINEAR_GRADIENT)
                                } else if (mode == BackgroundMode.SOLID) {
                                    design.background.brush.copy(mode = BrushMode.SOLID)
                                } else {
                                    design.background.brush
                                }
                                onDesignChange(
                                    design.copy(
                                        background = design.background.copy(
                                            mode = mode,
                                            brush = updatedBrush
                                        )
                                    )
                                )
                            }
                        )
                    }
                }

                when (design.background.mode) {
                    BackgroundMode.SOLID -> {
                        ColorSwatchRow(
                            label = "Canvas Color",
                            argb = design.background.brush.primaryColor,
                            onClick = {
                                colorDialogConfig = Triple("Canvas Background", design.background.brush.primaryColor) { newC ->
                                    onDesignChange(
                                        design.copy(
                                            background = design.background.copy(
                                                brush = design.background.brush.copy(primaryColor = newC)
                                            )
                                        )
                                    )
                                }
                            }
                        )
                    }

                    BackgroundMode.GRADIENT -> {
                        BrushEditorCard(
                            title = "Background Gradient",
                            brush = design.background.brush.let {
                                if (it.mode == BrushMode.SOLID) it.copy(mode = BrushMode.LINEAR_GRADIENT) else it
                            },
                            allowSolidOption = false,
                            onBrushChange = { newBrush ->
                                onDesignChange(design.copy(background = design.background.copy(brush = newBrush)))
                            },
                            onPickColor = { label, current, cb ->
                                colorDialogConfig = Triple(label, current, cb)
                            }
                        )
                    }

                    BackgroundMode.TRANSPARENT -> {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.surfaceMuted,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Checkerboard preview indicates full alpha transparency. Export as PNG or SVG to keep transparent canvas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    BackgroundMode.PHOTO -> {
                        Button(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.espressoAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (design.background.photoUri == null) "Choose Background Photo" else "Change Background Photo")
                        }
                        if (!design.background.photoUri.isNullOrBlank() && onExtractColorsFromPhoto != null) {
                            FilledTonalButton(
                                onClick = onExtractColorsFromPhoto,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = colors.surfaceMuted,
                                    contentColor = colors.textPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Extract Brand Colors from Photo")
                            }
                        }
                        ColorSwatchRow(
                            label = "Scrim Tint Color",
                            argb = design.background.photoOverlayColor,
                            onClick = {
                                colorDialogConfig = Triple("Photo Scrim Color", design.background.photoOverlayColor) { c ->
                                    onDesignChange(design.copy(background = design.background.copy(photoOverlayColor = c)))
                                }
                            }
                        )
                        LabeledSlider(
                            label = "Scrim Opacity (Improves Scan Speed)",
                            valueText = "${(design.background.photoOverlayAlpha * 100).roundToInt()}%",
                            value = design.background.photoOverlayAlpha,
                            valueRange = 0f..0.95f,
                            onValueChange = {
                                onDesignChange(design.copy(background = design.background.copy(photoOverlayAlpha = it)))
                            }
                        )
                    }
                }
            }

            ColorTarget.LIGHT_DOTS -> {
                if (design.lightPixelStyle == QrLightPixelStyle.NONE) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Light dots are currently hidden in Shapes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                            TextButton(onClick = { onDesignChange(design.copy(lightPixelStyle = QrLightPixelStyle.DOT)) }) {
                                Text("Enable Micro Dots", color = colors.espressoAccent)
                            }
                        }
                    }
                } else {
                    ColorSwatchRow(
                        label = "Light Module Tint",
                        argb = design.lightPixelColor,
                        onClick = {
                            colorDialogConfig = Triple("Light Module Color", design.lightPixelColor) { c ->
                                onDesignChange(design.copy(lightPixelColor = c))
                            }
                        }
                    )
                }
            }
        }
    }

    colorDialogConfig?.let { (label, initial, onConfirm) ->
        ColorPickerModalSheet(
            title = label,
            initialArgb = initial,
            allowAlpha = target == ColorTarget.LIGHT_DOTS,
            onDismiss = { colorDialogConfig = null },
            onColorSelected = onConfirm
        )
    }
}

@Composable
private fun BrushEditorCard(
    title: String,
    brush: BrushStyleConfig,
    allowSolidOption: Boolean = true,
    onBrushChange: (BrushStyleConfig) -> Unit,
    onPickColor: (String, Long, (Long) -> Unit) -> Unit
) {
    val colors = QraftThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrushMode.entries
                    .filter { allowSolidOption || it != BrushMode.SOLID }
                    .forEach { mode ->
                        SelectableChip(
                            label = mode.label,
                            selected = brush.mode == mode,
                            onClick = { onBrushChange(brush.copy(mode = mode)) }
                        )
                    }
            }

            ColorSwatchRow(
                label = if (brush.mode == BrushMode.SOLID) "Color" else "Start Color",
                argb = brush.primaryColor,
                onClick = {
                    onPickColor("Primary Color", brush.primaryColor) { newC ->
                        onBrushChange(brush.copy(primaryColor = newC))
                    }
                }
            )

            if (brush.mode != BrushMode.SOLID) {
                ColorSwatchRow(
                    label = "End Color",
                    argb = brush.secondaryColor,
                    onClick = {
                        onPickColor("Secondary Color", brush.secondaryColor) { newC ->
                            onBrushChange(brush.copy(secondaryColor = newC))
                        }
                    }
                )

                val hasMid = brush.tertiaryColor != null
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3rd Mid-Stop Color", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    Switch(
                        checked = hasMid,
                        onCheckedChange = { enabled ->
                            onBrushChange(brush.copy(tertiaryColor = if (enabled) 0xFFBC59AF else null))
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.espressoAccent)
                    )
                }
                val midColor = brush.tertiaryColor
                if (midColor != null) {
                    ColorSwatchRow(
                        label = "Mid Color",
                        argb = midColor,
                        onClick = {
                            onPickColor("Mid Gradient Color", midColor) { newC ->
                                onBrushChange(brush.copy(tertiaryColor = newC))
                            }
                        }
                    )
                }

                if (brush.mode == BrushMode.LINEAR_GRADIENT) {
                    Text("Gradient Direction", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradientDirection.entries.forEach { dir ->
                            SelectableChip(
                                label = dir.label,
                                selected = brush.direction == dir,
                                onClick = {
                                    onBrushChange(brush.copy(direction = dir, angleDegrees = dir.angleDeg))
                                }
                            )
                        }
                    }
                    if (brush.direction == GradientDirection.CUSTOM_ANGLE) {
                        LabeledSlider(
                            label = "Gradient Angle",
                            valueText = "${brush.angleDegrees.roundToInt()}°",
                            value = brush.angleDegrees,
                            valueRange = 0f..360f,
                            onValueChange = { onBrushChange(brush.copy(angleDegrees = it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogoEditorTab(
    design: QrDesignSpec,
    onDesignChange: (QrDesignSpec) -> Unit,
    onPickLogoPhoto: ((Uri) -> Unit)? = null,
    onExtractColorsFromLogo: (() -> Unit)? = null
) {
    val colors = QraftThemeTokens.colors
    val logo = design.logo
    var colorDialog by remember { mutableStateOf<Triple<String, Long, (Long) -> Unit>?>(null) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (onPickLogoPhoto != null) {
                onPickLogoPhoto(uri)
            } else {
                onDesignChange(
                    design.copy(
                        logo = logo.copy(
                            sourceType = LogoSourceType.GALLERY,
                            galleryUri = uri.toString()
                        )
                    )
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionLabel("Center Logo Source")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogoSourceType.entries.forEach { src ->
                SelectableChip(
                    label = src.label,
                    selected = logo.sourceType == src,
                    onClick = {
                        if (src == LogoSourceType.GALLERY && logo.galleryUri == null) {
                            logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            onDesignChange(design.copy(logo = logo.copy(sourceType = src)))
                        }
                    }
                )
            }
        }

        when (logo.sourceType) {
            LogoSourceType.NONE -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceMuted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose Photo, Icon, Emoji, or a Monogram Badge above to embed a scannable center logo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            LogoSourceType.GALLERY -> {
                Button(
                    onClick = {
                        logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.espressoAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (logo.galleryUri == null) "Pick Image from Gallery" else "Replace Logo Image")
                }
                if (!logo.galleryUri.isNullOrBlank() && onExtractColorsFromLogo != null) {
                    FilledTonalButton(
                        onClick = onExtractColorsFromLogo,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.surfaceMuted,
                            contentColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Extract Brand Colors from Logo")
                    }
                }
            }

            LogoSourceType.ICON -> {
                FlowGrid(items = LogoCatalog.icons, columns = 4) { choice ->
                    val selected = logo.iconKey == choice.key
                    ShapePreviewTile(
                        label = choice.label,
                        selected = selected,
                        onClick = { onDesignChange(design.copy(logo = logo.copy(iconKey = choice.key))) }
                    ) {
                        Icon(
                            imageVector = choice.icon,
                            contentDescription = choice.label,
                            tint = if (selected) colors.activePillContent else Color(logo.iconTintColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                ColorSwatchRow(
                    label = "Icon Color",
                    argb = logo.iconTintColor,
                    onClick = {
                        colorDialog = Triple("Logo Icon Color", logo.iconTintColor) { c ->
                            onDesignChange(design.copy(logo = logo.copy(iconTintColor = c)))
                        }
                    }
                )
            }

            LogoSourceType.EMOJI -> {
                QraftTextField(
                    value = logo.emoji,
                    onValueChange = { onDesignChange(design.copy(logo = logo.copy(emoji = it.take(4)))) },
                    label = "Custom Emoji",
                    placeholder = "⚡"
                )
                FlowGrid(items = LogoCatalog.popularEmojis, columns = 6) { em ->
                    val selected = logo.emoji == em
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) colors.activePillBg else colors.surface)
                            .border(1.dp, if (selected) colors.activePillBg else colors.cardBorder, RoundedCornerShape(14.dp))
                            .appleBounceClick { onDesignChange(design.copy(logo = logo.copy(emoji = em))) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(em, fontSize = 22.sp)
                    }
                }
            }

            LogoSourceType.MONOGRAM -> {
                QraftTextField(
                    value = logo.monogramText,
                    onValueChange = { onDesignChange(design.copy(logo = logo.copy(monogramText = it.uppercase().take(4)))) },
                    label = "Badge Text (1–4 chars)",
                    placeholder = "QR"
                )
                ColorSwatchRow(
                    label = "Badge Background",
                    argb = logo.monogramBgColor,
                    onClick = {
                        colorDialog = Triple("Badge Background", logo.monogramBgColor) { c ->
                            onDesignChange(design.copy(logo = logo.copy(monogramBgColor = c)))
                        }
                    }
                )
                ColorSwatchRow(
                    label = "Badge Text Color",
                    argb = logo.monogramTextColor,
                    onClick = {
                        colorDialog = Triple("Badge Text Color", logo.monogramTextColor) { c ->
                            onDesignChange(design.copy(logo = logo.copy(monogramTextColor = c)))
                        }
                    }
                )
            }
        }

        if (logo.sourceType != LogoSourceType.NONE) {
            LabeledSlider(
                label = "Logo Size",
                valueText = "${(logo.sizeFraction * 100).roundToInt()}%",
                value = logo.sizeFraction,
                valueRange = 0.12f..0.34f,
                onValueChange = { onDesignChange(design.copy(logo = logo.copy(sizeFraction = it))) }
            )

            SectionLabel("Logo Mask Shape")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogoClipShape.entries.forEach { clip ->
                    SelectableChip(
                        label = clip.label,
                        selected = logo.clipShape == clip,
                        onClick = { onDesignChange(design.copy(logo = logo.copy(clipShape = clip))) }
                    )
                }
            }

            SectionLabel("Cutout Clearing Mode")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LogoPaddingStyle.entries.forEach { mode ->
                    SelectableChip(
                        label = mode.label,
                        selected = logo.paddingStyle == mode,
                        onClick = { onDesignChange(design.copy(logo = logo.copy(paddingStyle = mode))) }
                    )
                }
            }

            if (logo.paddingStyle != LogoPaddingStyle.NONE) {
                LabeledSlider(
                    label = "Cutout Halo Margin",
                    valueText = "${(logo.paddingFraction * 100).roundToInt()}%",
                    value = logo.paddingFraction,
                    valueRange = 0f..0.30f,
                    onValueChange = { onDesignChange(design.copy(logo = logo.copy(paddingFraction = it))) }
                )
            }

            if (logo.sourceType != LogoSourceType.MONOGRAM) {
                ToggleRow(
                    title = "Solid Backing Plate",
                    subtitle = "Adds a crisp pad behind the logo for instant recognition",
                    checked = logo.backingEnabled,
                    onCheckedChange = { onDesignChange(design.copy(logo = logo.copy(backingEnabled = it))) }
                )
                if (logo.backingEnabled) {
                    ColorSwatchRow(
                        label = "Backing Plate Color",
                        argb = logo.backingColor,
                        onClick = {
                            colorDialog = Triple("Backing Color", logo.backingColor) { c ->
                                onDesignChange(design.copy(logo = logo.copy(backingColor = c)))
                            }
                        }
                    )
                }
            }

            ToggleRow(
                title = "Logo Border Ring",
                checked = logo.borderEnabled,
                onCheckedChange = { onDesignChange(design.copy(logo = logo.copy(borderEnabled = it))) }
            )
            if (logo.borderEnabled) {
                ColorSwatchRow(
                    label = "Border Ring Color",
                    argb = logo.borderColor,
                    onClick = {
                        colorDialog = Triple("Logo Border Color", logo.borderColor) { c ->
                            onDesignChange(design.copy(logo = logo.copy(borderColor = c)))
                        }
                    }
                )
            }
        }
    }

    colorDialog?.let { (label, initial, onConfirm) ->
        ColorPickerModalSheet(
            title = label,
            initialArgb = initial,
            onDismiss = { colorDialog = null },
            onColorSelected = onConfirm
        )
    }
}

@Composable
fun FrameAndProTab(
    design: QrDesignSpec,
    onDesignChange: (QrDesignSpec) -> Unit
) {
    val frame = design.outerFrame
    var colorDialog by remember { mutableStateOf<Triple<String, Long, (Long) -> Unit>?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionLabel("Outer Card & Sticker Frame")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OuterFrameStyle.entries.forEach { style ->
                SelectableChip(
                    label = style.label,
                    selected = frame.style == style,
                    onClick = { onDesignChange(design.copy(outerFrame = frame.copy(style = style))) }
                )
            }
        }

        if (frame.style != OuterFrameStyle.NONE) {
            val hasLabel = frame.style in setOf(
                OuterFrameStyle.BOTTOM_SCAN_BANNER,
                OuterFrameStyle.TOP_BADGE_CARD,
                OuterFrameStyle.SPEECH_BUBBLE,
                OuterFrameStyle.POLAROID_STAMP,
                OuterFrameStyle.DOUBLE_PILL,
                OuterFrameStyle.WARM_POSTER,
                OuterFrameStyle.GLASS_CARD,
                OuterFrameStyle.CORNER_BRACKET
            )
            if (hasLabel) {
                QraftTextField(
                    value = frame.label,
                    onValueChange = { onDesignChange(design.copy(outerFrame = frame.copy(label = it.take(22)))) },
                    label = "Callout Label",
                    placeholder = "SCAN ME",
                    leadingIcon = Icons.Rounded.Campaign
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SCAN ME", "JOIN WI-FI", "OPEN LINK", "ADD CONTACT", "PAY HERE", "MENU").forEach { quick ->
                        SelectableChip(
                            label = quick,
                            selected = frame.label == quick,
                            onClick = { onDesignChange(design.copy(outerFrame = frame.copy(label = quick))) }
                        )
                    }
                }
                if (frame.style == OuterFrameStyle.DOUBLE_PILL) {
                    QraftTextField(
                        value = frame.subtitle,
                        onValueChange = { onDesignChange(design.copy(outerFrame = frame.copy(subtitle = it.take(26)))) },
                        label = "Bottom Pill Text",
                        placeholder = "TAP CAMERA TO OPEN"
                    )
                }
            }

            ColorSwatchRow(
                label = "Card Frame Fill",
                argb = frame.frameFillColor,
                onClick = {
                    colorDialog = Triple("Frame Card Fill", frame.frameFillColor) { c ->
                        onDesignChange(design.copy(outerFrame = frame.copy(frameFillColor = c)))
                    }
                }
            )
            ColorSwatchRow(
                label = "Banner / Accent Color",
                argb = frame.accentColor,
                onClick = {
                    colorDialog = Triple("Frame Accent Color", frame.accentColor) { c ->
                        onDesignChange(design.copy(outerFrame = frame.copy(accentColor = c)))
                    }
                }
            )
            if (frame.style == OuterFrameStyle.GRADIENT_BORDER || frame.style == OuterFrameStyle.WARM_POSTER) {
                ColorSwatchRow(
                    label = "Secondary Accent",
                    argb = frame.accentSecondaryColor,
                    onClick = {
                        colorDialog = Triple("Secondary Frame Accent", frame.accentSecondaryColor) { c ->
                            onDesignChange(design.copy(outerFrame = frame.copy(accentSecondaryColor = c)))
                        }
                    }
                )
            }
            if (hasLabel) {
                ColorSwatchRow(
                    label = "Label Text Color",
                    argb = frame.textColor,
                    onClick = {
                        colorDialog = Triple("Frame Label Color", frame.textColor) { c ->
                            onDesignChange(design.copy(outerFrame = frame.copy(textColor = c)))
                        }
                    }
                )
            }

            LabeledSlider(
                label = "Frame Corner Radius",
                valueText = "${frame.cornerRadiusDp.roundToInt()} dp",
                value = frame.cornerRadiusDp,
                valueRange = 8f..38f,
                onValueChange = { onDesignChange(design.copy(outerFrame = frame.copy(cornerRadiusDp = it))) }
            )

            if (frame.style != OuterFrameStyle.GRADIENT_BORDER) {
                LabeledSlider(
                    label = "Card Border Outline",
                    valueText = "${frame.borderThicknessDp.roundToInt()} dp",
                    value = frame.borderThicknessDp,
                    valueRange = 0f..8f,
                    onValueChange = { onDesignChange(design.copy(outerFrame = frame.copy(borderThicknessDp = it))) }
                )
            }
        }

        HorizontalDivider(color = QraftThemeTokens.colors.cardBorder)

        SectionLabel("Error Correction (Redundancy)")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QrEccSetting.entries.forEach { ecc ->
                SelectableChip(
                    label = if (ecc == QrEccSetting.AUTO) "Auto (Smart)" else "${ecc.code} (${ecc.ratioText})",
                    selected = design.ecc == ecc,
                    onClick = { onDesignChange(design.copy(ecc = ecc)) }
                )
            }
        }

        LabeledSlider(
            label = "Quiet Zone Margin",
            valueText = "${(design.quietZonePadding * 100).roundToInt()}%",
            value = design.quietZonePadding,
            valueRange = 0f..0.24f,
            onValueChange = { onDesignChange(design.copy(quietZonePadding = it)) }
        )

        ToggleRow(
            title = "Soft QR Elevation Shadow",
            subtitle = "Adds subtle depth behind the matrix inside card frames",
            checked = design.matrixShadowEnabled,
            onCheckedChange = { onDesignChange(design.copy(matrixShadowEnabled = it)) }
        )

        SectionLabel("Raster Export Resolution")
        QraftResolutionSelector(
            selectedPx = design.exportSizePx,
            onSelect = { px -> onDesignChange(design.copy(exportSizePx = px)) }
        )
    }

    colorDialog?.let { (label, initial, onConfirm) ->
        ColorPickerModalSheet(
            title = label,
            initialArgb = initial,
            onDismiss = { colorDialog = null },
            onColorSelected = onConfirm
        )
    }
}

@Composable
fun ColorSwatchRow(
    label: String,
    argb: Long,
    onClick: () -> Unit
) {
    val colors = QraftThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .appleBounceClick { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = String.format("#%06X", argb and 0xFFFFFFL),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(1.5.dp, colors.cardBorder, CircleShape)
                )
            }
        }
    }
}

@Composable
fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val colors = QraftThemeTokens.colors
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = colors.espressoAccent, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = colors.espressoAccent,
                activeTrackColor = colors.espressoAccent,
                inactiveTrackColor = colors.surfaceMuted
            )
        )
    }
}

@Composable
private fun ShapePreviewTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit
) {
    val colors = QraftThemeTokens.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.activePillBg else colors.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.activePillBg else colors.cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .appleBounceClick { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        preview()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.activePillContent else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun <T> FlowGrid(
    items: List<T>,
    columns: Int,
    itemContent: @Composable (T) -> Unit
) {
    val rows = items.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        itemContent(item)
                    }
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
