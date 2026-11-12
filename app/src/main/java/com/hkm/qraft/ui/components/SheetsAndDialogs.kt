package com.hkm.qraft.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hkm.qraft.data.UserPreferences
import com.hkm.qraft.model.*
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQrTypeSheet(
    onDismiss: () -> Unit,
    onSelectType: (QrContentType) -> Unit
) {
    val colors = QraftThemeTokens.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.textTertiary.copy(alpha = 0.45f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Create New QR Code",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary
            )
            Text(
                text = "Pick a payload format to open Qraft Studio",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 460.dp)
            ) {
                item {
                    FlowGrid(items = QrContentType.entries, columns = 2) { type ->
                        val pastel = colors.pastelAt(type.pastelIndex)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.background,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .appleBounceClick {
                                    onDismiss()
                                    onSelectType(type)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(pastel.container),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconForContentType(type),
                                        contentDescription = null,
                                        tint = pastel.content,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = type.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportQrSheet(
    title: String,
    encodedPayload: String,
    design: QrDesignSpec,
    initialFormat: ExportImageFormat,
    isExporting: Boolean,
    onDesignChange: (QrDesignSpec) -> Unit,
    onDismiss: () -> Unit,
    onSave: (ExportImageFormat) -> Unit,
    onShare: (ExportImageFormat) -> Unit,
    onCopyImage: () -> Unit,
    onCopyPayload: () -> Unit
) {
    val colors = QraftThemeTokens.colors
    var selectedFormat by remember(initialFormat) { mutableStateOf(initialFormat) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.textTertiary.copy(alpha = 0.45f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QrArtworkPreview(
                    data = encodedPayload,
                    design = design,
                    qrViewportSize = 72.dp,
                    showCheckerboardForTransparent = true
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { "Custom QR Code" },
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (selectedFormat) {
                            ExportImageFormat.SVG -> "True Infinite Vector • ${design.exportSizePx}px viewport"
                            ExportImageFormat.PDF -> "300 DPI Print Sheet • A4 (2480×3508)"
                            else -> if (design.exportSizePx >= 4096) "4K UHD (${design.exportSizePx}px) • Print Clarity" else "${design.exportSizePx}px High Clarity"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }

            if (selectedFormat != ExportImageFormat.PDF) {
                SectionLabel("Output Resolution")
                QraftResolutionSelector(
                    selectedPx = design.exportSizePx,
                    onSelect = { px -> onDesignChange(design.copy(exportSizePx = px)) }
                )
            }

            SectionLabel("Export Format")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportImageFormat.entries.forEach { fmt ->
                    val selected = fmt == selectedFormat
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) colors.surfaceMuted else colors.background,
                        border = BorderStroke(
                            if (selected) 1.5.dp else 1.dp,
                            if (selected) colors.espressoAccent else colors.cardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleBounceClick { selectedFormat = fmt }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = when (fmt) {
                                        ExportImageFormat.PNG,
                                        ExportImageFormat.JPEG,
                                        ExportImageFormat.WEBP -> Icons.Rounded.Image
                                        ExportImageFormat.SVG -> Icons.Rounded.Draw
                                        ExportImageFormat.PDF -> Icons.Rounded.PictureAsPdf
                                    },
                                    contentDescription = null,
                                    tint = if (selected) colors.espressoAccent else colors.textSecondary
                                )
                                Text(
                                    text = fmt.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary
                                )
                            }
                            if (selected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.espressoAccent
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QraftPillButton(
                    text = "Share",
                    variant = QraftButtonVariant.SECONDARY,
                    leadingIcon = Icons.Rounded.Share,
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f),
                    onClick = { onShare(selectedFormat) }
                )

                QraftPillButton(
                    text = "Save ${selectedFormat.extension.uppercase()}",
                    variant = QraftButtonVariant.PRIMARY,
                    leadingIcon = Icons.Rounded.Download,
                    loading = isExporting,
                    modifier = Modifier.weight(1.35f),
                    onClick = { onSave(selectedFormat) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCopyImage,
                    enabled = !isExporting
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, tint = colors.espressoAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy QR Image", color = colors.espressoAccent, fontWeight = FontWeight.SemiBold)
                }

                TextButton(onClick = onCopyPayload) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Raw Text", color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun FullscreenScanDialog(
    title: String,
    encodedPayload: String,
    design: QrDesignSpec,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window
        val previousBrightness = window?.attributes?.screenBrightness ?: -1f
        if (window != null) {
            val lp = window.attributes
            lp.screenBrightness = 1.0f
            window.attributes = lp
        }
        onDispose {
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = previousBrightness
                window.attributes = lp
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colors = QraftThemeTokens.colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.98f))
                .appleBounceClick(scalePressed = 1f, haptic = false) { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = title.ifBlank { "Ready to Scan" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary
                )

                QrArtworkPreview(
                    data = encodedPayload,
                    design = design,
                    qrViewportSize = 276.dp,
                    showCheckerboardForTransparent = false
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.surfaceMuted
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.BrightnessHigh,
                            contentDescription = null,
                            tint = colors.espressoAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Screen brightness boosted • Tap anywhere to close",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchGeneratorSheet(
    currentDesign: QrDesignSpec,
    isRunning: Boolean,
    onDismiss: () -> Unit,
    onRunBatch: (lines: List<String>, saveToLibrary: Boolean, exportPngs: Boolean) -> Unit
) {
    val colors = QraftThemeTokens.colors
    var rawLines by remember {
        mutableStateOf("https://qraft.studio/table-1\nhttps://qraft.studio/table-2\nhttps://qraft.studio/table-3")
    }
    var saveToLibrary by remember { mutableStateOf(true) }
    var exportPngs by remember { mutableStateOf(true) }
    val parsedLines = remember(rawLines) {
        rawLines.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.textTertiary.copy(alpha = 0.45f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Batch QR Generator", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
            Text(
                "Generate multiple QR codes at once using your current Studio style. Enter one URL or text per line.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            QraftTextField(
                value = rawLines,
                onValueChange = { rawLines = it },
                label = "Payloads (${parsedLines.size} items)",
                placeholder = "https://example.com/1\nhttps://example.com/2",
                singleLine = false,
                minLines = 4,
                maxLines = 7
            )

            ToggleRow(
                title = "Save to Qraft Library",
                checked = saveToLibrary,
                onCheckedChange = { saveToLibrary = it }
            )
            ToggleRow(
                title = "Export PNGs to Pictures/Qraft",
                checked = exportPngs,
                onCheckedChange = { exportPngs = it }
            )

            Button(
                onClick = { onRunBatch(parsedLines, saveToLibrary, exportPngs) },
                enabled = parsedLines.isNotEmpty() && !isRunning && (saveToLibrary || exportPngs),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.espressoAccent,
                    contentColor = colors.espressoAccentContent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = colors.espressoAccentContent,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Generating…")
                } else {
                    Icon(Icons.Rounded.AutoAwesomeMotion, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate ${parsedLines.size} QR Codes", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModalSheet(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onCreatorNameChange: (String) -> Unit,
    onAutoVerifyChange: (Boolean) -> Unit,
    onDefaultFormatChange: (ExportImageFormat) -> Unit,
    onDefaultResolutionChange: (Int) -> Unit
) {
    val colors = QraftThemeTokens.colors
    var nameDraft by remember(preferences.creatorName) { mutableStateOf(preferences.creatorName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.textTertiary.copy(alpha = 0.45f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Studio Preferences", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)

            SectionLabel("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    SelectableChip(
                        label = mode.label,
                        selected = preferences.themeMode == mode,
                        onClick = { onThemeChange(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            QraftTextField(
                value = nameDraft,
                onValueChange = {
                    nameDraft = it
                    onCreatorNameChange(it)
                },
                label = "Greeting Name",
                placeholder = "Creator",
                leadingIcon = Icons.Rounded.Person
            )

            ToggleRow(
                title = "Live Scannability Verification",
                subtitle = "Automatically test-decodes your custom QR with ZXing while designing",
                checked = preferences.autoVerifyScannability,
                onCheckedChange = onAutoVerifyChange
            )

            SectionLabel("Default Export Format")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    ExportImageFormat.PNG,
                    ExportImageFormat.JPEG,
                    ExportImageFormat.SVG,
                    ExportImageFormat.PDF
                ).forEach { fmt ->
                    SelectableChip(
                        label = fmt.extension.uppercase(),
                        selected = preferences.defaultExportFormat == fmt,
                        onClick = { onDefaultFormatChange(fmt) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionLabel("Default Export Resolution")
            QraftResolutionSelector(
                selectedPx = preferences.defaultExportResolution,
                onSelect = onDefaultResolutionChange
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
