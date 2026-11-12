package com.hkm.qraft.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Spacious Top Apple Bar (keeps title readable even on 360dp phones)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QraftHeaderCircleButton(
                icon = Icons.Rounded.ArrowBackIosNew,
                contentDescription = "Back",
                onClick = onBack
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.effectiveTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Qraft Studio • ${draft.type.title} • ${draft.design.exportSizePx}px",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            QraftHeaderCircleButton(
                icon = Icons.Rounded.Casino,
                contentDescription = "Randomize style",
                contentColor = colors.espressoAccent,
                onClick = onRandomizeStyle
            )

            QraftHeaderCircleButton(
                icon = if (draft.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                contentDescription = "Favorite",
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
                        modifier = Modifier.appleBounceClick(scalePressed = 0.97f) { onOpenFullscreen() }
                    ) {
                        QrArtworkPreview(
                            data = draft.encodedPayload,
                            design = draft.design,
                            qrViewportSize = animatedViewportSize,
                            showCheckerboardForTransparent = true
                        )
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
                        onPickBackgroundPhoto = onPickBackgroundPhoto
                    )

                    StudioTab.LOGO -> LogoEditorTab(
                        design = draft.design,
                        onDesignChange = onDesignChange,
                        onPickLogoPhoto = onPickLogoPhoto
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
                report.isScannable -> "Scannable ✓ • ${report.matrixSize}×${report.matrixSize} • ECC ${report.effectiveEcc}"
                else -> "Low Scan Margin • Tap Fix"
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
