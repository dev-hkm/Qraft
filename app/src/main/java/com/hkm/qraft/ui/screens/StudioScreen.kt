package com.hkm.qraft.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import java.util.Locale
import com.hkm.qraft.StudioDraftState
import com.hkm.qraft.data.PresetCatalog
import com.hkm.qraft.model.*
import com.hkm.qraft.ui.components.*
import com.hkm.qraft.ui.theme.AppleSprings
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick

enum class StudioTab(val label: String, val icon: ImageVector) {
    CONTENT("Content", Icons.Rounded.EditNote),
    PRESETS("Presets", Icons.Rounded.AutoAwesome),
    SHAPES("Shapes", Icons.Rounded.Interests),
    COLORS("Colors", Icons.Rounded.Palette),
    LOGO("Logo", Icons.Rounded.CenterFocusStrong),
    FRAME("Frame & Pro", Icons.Rounded.CropFree)
}

@Composable
fun StudioScreen(
    draft: StudioDraftState,
    customPresets: List<SavedStylePreset>,
    scannability: ScannabilityReport,
    isVerifying: Boolean,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onExtractBrandColors: ((Boolean) -> Unit)? = null,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTypeChange: (QrContentType) -> Unit,
    onFieldsChange: (QrPayloadFields) -> Unit,
    onDesignChange: (QrDesignSpec) -> Unit,
    onPickLogoPhoto: (Uri) -> Unit,
    onPickBackgroundPhoto: (Uri) -> Unit,
    onToggleFavorite: () -> Unit,
    onApplyPreset: (SavedStylePreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onRandomizeStyle: () -> Unit,
    onAutoFixScannability: () -> Unit,
    onSaveToLibrary: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenFullscreen: () -> Unit
) {
    val colors = QraftThemeTokens.colors
    var activeTab by remember { mutableStateOf(StudioTab.CONTENT) }
    var previewCompact by remember { mutableStateOf(false) }
    val animatedViewportSize by animateDpAsState(
        targetValue = if (previewCompact) 84.dp else 166.dp,
        animationSpec = AppleSprings.snappy(),
        label = "previewViewportSize"
    )
    var tiltTargetX by remember { mutableFloatStateOf(0f) }
    var tiltTargetY by remember { mutableFloatStateOf(0f) }
    val animTiltX by animateFloatAsState(
        targetValue = tiltTargetX,
        animationSpec = AppleSprings.TactileBounce,
        label = "previewTiltX"
    )
    val animTiltY by animateFloatAsState(
        targetValue = tiltTargetY,
        animationSpec = AppleSprings.TactileBounce,
        label = "previewTiltY"
    )
    val densityPx = LocalDensity.current.density

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Spacious Top Apple Bar with Undo/Redo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            QraftHeaderCircleButton(
                icon = Icons.Rounded.ArrowBackIosNew,
                contentDescription = "Back",
                size = 36.dp,
                onClick = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = draft.effectiveTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${draft.type.shortCode} • ${draft.design.exportSizePx}px",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            QraftHeaderCircleButton(
                icon = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = "Undo",
                size = 34.dp,
                enabled = canUndo,
                contentColor = if (canUndo) colors.textPrimary else colors.textTertiary,
                modifier = Modifier.alpha(if (canUndo) 1f else 0.45f),
                onClick = onUndo
            )

            QraftHeaderCircleButton(
                icon = Icons.AutoMirrored.Rounded.Redo,
                contentDescription = "Redo",
                size = 34.dp,
                enabled = canRedo,
                contentColor = if (canRedo) colors.textPrimary else colors.textTertiary,
                modifier = Modifier.alpha(if (canRedo) 1f else 0.45f),
                onClick = onRedo
            )

            QraftHeaderCircleButton(
                icon = Icons.Rounded.Casino,
                contentDescription = "Randomize style",
                size = 34.dp,
                contentColor = colors.espressoAccent,
                onClick = onRandomizeStyle
            )

            QraftHeaderCircleButton(
                icon = if (draft.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                contentDescription = "Favorite",
                size = 34.dp,
                contentColor = if (draft.isFavorite) colors.timelineDotActive else colors.textSecondary,
                onClick = onToggleFavorite
            )
        }

        // Sticky Live Preview Card (Tap QR for Fullscreen, Save & Export unified CTAs)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (previewCompact) Arrangement.Start else Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = { tiltTargetX = 0f; tiltTargetY = 0f },
                                    onDragCancel = { tiltTargetX = 0f; tiltTargetY = 0f }
                                ) { change, dragAmount ->
                                    change.consume()
                                    tiltTargetY = (tiltTargetY + dragAmount.x * 0.08f).coerceIn(-14f, 14f)
                                    tiltTargetX = (tiltTargetX - dragAmount.y * 0.08f).coerceIn(-14f, 14f)
                                }
                            }
                            .graphicsLayer {
                                rotationX = animTiltX
                                rotationY = animTiltY
                                cameraDistance = 14f * densityPx
                            }
                            .appleBounceClick(scalePressed = 0.97f) { onOpenFullscreen() }
                    ) {
                        QrArtworkPreview(
                            data = draft.encodedPayload,
                            design = draft.design,
                            qrViewportSize = animatedViewportSize,
                            showCheckerboardForTransparent = true
                        )
                        val glintStrength = ((abs(animTiltX) + abs(animTiltY)) / 24f).coerceIn(0f, 1f)
                        if (glintStrength > 0.02f) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.24f * glintStrength),
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.10f * glintStrength)
                                            ),
                                            start = Offset(0f, 0f),
                                            end = Offset(420f + animTiltY * 18f, 420f - animTiltX * 18f)
                                        )
                                    )
                            )
                        }
                    }

                    if (previewCompact) {
                        Spacer(Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${draft.design.pixelStyle.label} • ${draft.design.eyeFrameStyle.label}",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            ScannabilityStatusPill(
                                report = scannability,
                                isVerifying = isVerifying,
                                onAutoFix = onAutoFixScannability
                            )
                        }
                    }
                }

                // Controls + Save / Export bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!previewCompact) {
                        ScannabilityStatusPill(
                            report = scannability,
                            isVerifying = isVerifying,
                            onAutoFix = onAutoFixScannability,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallRoundAction(
                            icon = Icons.Rounded.Fullscreen,
                            label = "Scan",
                            onClick = onOpenFullscreen
                        )
                        SmallRoundAction(
                            icon = if (previewCompact) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                            label = if (previewCompact) "Expand" else "Compact",
                            onClick = { previewCompact = !previewCompact }
                        )
                    }
                }

                if (!previewCompact) {
                    QrTelemetryGaugeRow(
                        charCount = draft.encodedPayload.length,
                        matrixSize = scannability.matrixSize,
                        effectiveEcc = scannability.effectiveEcc,
                        contrastRatio = scannability.contrastRatio
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QraftPillButton(
                        text = if (draft.editingId == null) "Save QR" else "Update QR",
                        variant = QraftButtonVariant.TONAL,
                        leadingIcon = Icons.Rounded.BookmarkAdded,
                        height = 42.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onSaveToLibrary
                    )
                    QraftPillButton(
                        text = "Export 4K",
                        variant = QraftButtonVariant.PRIMARY,
                        leadingIcon = Icons.Rounded.IosShare,
                        height = 42.dp,
                        modifier = Modifier.weight(1.1f),
                        onClick = onOpenExport
                    )
                }

                if (!scannability.warningMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.warningBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = scannability.warningMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.warningFg,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = "Auto-Fix",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.espressoAccent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.surface)
                                    .appleBounceClick { onAutoFixScannability() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Apple Segmented Tab Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioTab.entries.forEach { tab ->
                val selected = tab == activeTab
                val bg by animateColorAsState(
                    targetValue = if (selected) colors.activePillBg else colors.surface,
                    animationSpec = AppleSprings.snappy(),
                    label = "tabBg"
                )
                val fg by animateColorAsState(
                    targetValue = if (selected) colors.activePillContent else colors.textPrimary,
                    animationSpec = AppleSprings.snappy(),
                    label = "tabFg"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .border(1.dp, if (selected) bg else colors.cardBorder, RoundedCornerShape(50))
                        .appleBounceClick { activeTab = tab }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Icon(tab.icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = fg
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Tab Body
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                (slideInHorizontally(tween(240)) { if (forward) it / 8 else -it / 8 } + fadeIn(tween(200)))
                    .togetherWith(slideOutHorizontally(tween(180)) { if (forward) -it / 8 else it / 8 } + fadeOut(tween(160)))
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = "studioTabBody"
        ) { tab ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 6.dp, bottom = 36.dp)
                    .navigationBarsPadding()
            ) {
                when (tab) {
                    StudioTab.CONTENT -> ContentEditorTab(
                        title = draft.title,
                        type = draft.type,
                        fields = draft.payload,
                        encodedPayload = draft.encodedPayload,
                        onTitleChange = onTitleChange,
                        onTypeChange = onTypeChange,
                        onFieldsChange = onFieldsChange
                    )

                    StudioTab.PRESETS -> PresetsEditorTab(
                        builtInPresets = PresetCatalog.builtInPresets,
                        customPresets = customPresets,
                        currentDesign = draft.design,
                        onApplyPreset = onApplyPreset,
                        onSaveCurrentAsPreset = onSaveCustomPreset,
                        onDeleteCustomPreset = onDeleteCustomPreset
                    )

                    StudioTab.SHAPES -> ShapesEditorTab(
                        design = draft.design,
                        onDesignChange = onDesignChange
                    )

                    StudioTab.COLORS -> ColorsEditorTab(
                        design = draft.design,
                        onDesignChange = onDesignChange,
                        onPickBackgroundPhoto = onPickBackgroundPhoto,
                        onExtractColorsFromPhoto = { onExtractBrandColors?.invoke(false) }
                    )

                    StudioTab.LOGO -> LogoEditorTab(
                        design = draft.design,
                        onDesignChange = onDesignChange,
                        onPickLogoPhoto = onPickLogoPhoto,
                        onExtractColorsFromLogo = { onExtractBrandColors?.invoke(true) }
                    )

                    StudioTab.FRAME -> FrameAndProTab(
                        design = draft.design,
                        onDesignChange = onDesignChange
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannabilityStatusPill(
    report: ScannabilityReport,
    isVerifying: Boolean,
    onAutoFix: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QraftThemeTokens.colors
    val bg = when {
        isVerifying -> colors.surfaceMuted
        report.isScannable -> colors.successBg
        else -> colors.warningBg
    }
    val fg = when {
        isVerifying -> colors.textSecondary
        report.isScannable -> colors.successFg
        else -> colors.warningFg
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .appleBounceClick(enabled = !report.isScannable) { onAutoFix() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        if (isVerifying) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.6.dp, color = fg)
        } else {
            Icon(
                imageVector = if (report.isScannable) Icons.Rounded.Verified else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = when {
                isVerifying -> "Checking…"
                report.isScannable -> "Scannable"
                else -> "Low Scan • Fix"
            },
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QrTelemetryGaugeRow(
    charCount: Int,
    matrixSize: Int,
    effectiveEcc: String,
    contrastRatio: Double
) {
    val colors = QraftThemeTokens.colors
    val targetDensity = (charCount / 220f).coerceIn(0.08f, 1f)
    val densityProgress by animateFloatAsState(
        targetValue = targetDensity,
        animationSpec = AppleSprings.Smooth,
        label = "densityBar"
    )
    val contrastColor = when {
        contrastRatio >= 4.5 -> colors.successFg
        contrastRatio >= 3.0 -> colors.warningFg
        else -> colors.dangerFg
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceMuted.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$charCount ch • ${matrixSize}×$matrixSize • $effectiveEcc",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(CircleShape)
                .background(colors.cardBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(densityProgress)
                    .clip(CircleShape)
                    .background(colors.espressoAccent)
            )
        }
        Text(
            text = "${String.format(Locale.US, "%.1f", contrastRatio)}:1",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contrastColor
        )
    }
}

@Composable
private fun SmallRoundAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = QraftThemeTokens.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceMuted)
            .appleBounceClick { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(icon, contentDescription = label, tint = colors.textPrimary, modifier = Modifier.size(15.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
    }
}
