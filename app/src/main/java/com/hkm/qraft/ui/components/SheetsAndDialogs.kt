package com.hkm.qraft.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hkm.qraft.data.UserPreferences
import com.hkm.qraft.model.*
import com.hkm.qraft.qr.BatchSequenceExpander
import com.hkm.qraft.qr.PrintSheetLayout
import com.hkm.qraft.qr.QrExporter
import com.hkm.qraft.ui.theme.AppMotion
import com.hkm.qraft.ui.theme.AppleGlassSurface
import com.hkm.qraft.ui.theme.AppleGlassVariant
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.applePressable
import com.hkm.qraft.ui.theme.rememberReduceMotion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQrTypeSheet(
    onDismiss: () -> Unit,
    onSelectType: (QrContentType) -> Unit,
    onImportQrImage: (() -> Unit)? = null,
    onSelectBarcode: ((BarcodeFormatType) -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null
) {
    val colors = QraftThemeTokens.colors
    QraftModalSheet(
        onDismissRequest = onDismiss,
        maxHeightFraction = 0.88f,
        hardCap = 740.dp,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) { dismissSheet ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create Code",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary
            )
            Text(
                text = "Pick a QR format, 1D/2D barcode, or scan & remix",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onOpenScanner != null || onImportQrImage != null) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (onOpenScanner != null) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = colors.espressoAccent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .applePressable {
                                            dismissSheet()
                                            onOpenScanner()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.QrCodeScanner,
                                            contentDescription = null,
                                            tint = colors.espressoAccentContent
                                        )
                                        Column {
                                            Text(
                                                "Live Scan",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = colors.espressoAccentContent
                                            )
                                            Text(
                                                "Camera",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.espressoAccentContent.copy(alpha = 0.78f)
                                            )
                                        }
                                    }
                                }
                            }
                            if (onImportQrImage != null) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = colors.heroBannerBg,
                                    border = BorderStroke(1.dp, colors.cardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .applePressable {
                                            dismissSheet()
                                            onImportQrImage()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.PhotoLibrary,
                                            contentDescription = null,
                                            tint = colors.textPrimary
                                        )
                                        Column {
                                            Text(
                                                "Import Image",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                "Remix QR",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    SectionLabel("QR Code Types")
                }
                item {
                    FlowGrid(items = QrContentType.entries, columns = 2) { type ->
                        val pastel = colors.pastelAt(type.pastelIndex)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.background,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .applePressable {
                                    dismissSheet()
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
                if (onSelectBarcode != null) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        SectionLabel("1D & 2D Barcodes")
                    }
                    item {
                        FlowGrid(items = BarcodeFormatType.entries, columns = 2) { fmt ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = colors.background,
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .applePressable {
                                        dismissSheet()
                                        onSelectBarcode(fmt)
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
                                            .background(colors.surfaceMuted),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (fmt.is2D) Icons.Rounded.GridView else Icons.Rounded.ViewWeek,
                                            contentDescription = null,
                                            tint = colors.textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fmt.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (fmt.is2D) "2D Matrix" else "1D Linear",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary,
                                            maxLines = 1
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
    onCopyPayload: () -> Unit,
    onExportPrintSheet: ((PrintSheetLayout, Boolean) -> Unit)? = null,
    codeKind: CodeKind = CodeKind.QR,
    barcodeSpec: BarcodeDesignSpec? = null,
    onBarcodeChange: ((BarcodeDesignSpec) -> Unit)? = null
) {
    val colors = QraftThemeTokens.colors
    var selectedFormat by remember(initialFormat) { mutableStateOf(initialFormat) }
    var showPrintSheetOptions by remember { mutableStateOf(false) }
    var printLayout by remember { mutableStateOf(PrintSheetLayout.STICKERS_3X4) }
    val activeWidthPx = if (codeKind == CodeKind.BARCODE) (barcodeSpec?.exportWidthPx ?: design.exportSizePx) else design.exportSizePx

    QraftModalSheet(
        onDismissRequest = onDismiss,
        maxHeightFraction = 0.90f,
        hardCap = 760.dp,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (codeKind == CodeKind.BARCODE && barcodeSpec != null) {
                    BarcodeArtworkPreview(
                        spec = barcodeSpec,
                        widthDp = 112.dp,
                        showCheckerboardForTransparent = true
                    )
                } else {
                    QrArtworkPreview(
                        data = encodedPayload,
                        design = design,
                        qrViewportSize = 72.dp,
                        showCheckerboardForTransparent = true
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { if (codeKind == CodeKind.BARCODE) "Custom Barcode" else "Custom QR Code" },
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (selectedFormat) {
                            ExportImageFormat.SVG -> "True Infinite Vector • ${activeWidthPx}px viewport"
                            ExportImageFormat.PDF -> "300 DPI Print Sheet • A4 (2480×3508)"
                            else -> when {
                                activeWidthPx >= 8192 -> "8K UHD (8192px) • Master Print Quality"
                                activeWidthPx >= 4096 -> "4K UHD (4096px) • Print Clarity"
                                else -> "${activeWidthPx}px High Clarity"
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }

            if (selectedFormat != ExportImageFormat.PDF) {
                SectionLabel("Output Resolution")
                QraftResolutionSelector(
                    selectedPx = activeWidthPx,
                    onSelect = { px ->
                        if (codeKind == CodeKind.BARCODE && barcodeSpec != null && onBarcodeChange != null) {
                            onBarcodeChange(barcodeSpec.copy(exportWidthPx = px))
                        } else {
                            onDesignChange(design.copy(exportSizePx = px))
                        }
                    }
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
                            .applePressable { selectedFormat = fmt }
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

            if (onExportPrintSheet != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.background,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .applePressable { showPrintSheetOptions = !showPrintSheetOptions },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.GridView, contentDescription = null, tint = colors.espressoAccent, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Printable A4 Label Sheet", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text("Tile multiple stickers or table cards at 300 DPI", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                }
                            }
                            Icon(
                                imageVector = if (showPrintSheetOptions) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                        }

                        AnimatedVisibility(
                            visible = showPrintSheetOptions,
                            enter = AppMotion.softExpandEnter,
                            exit = AppMotion.softExpandExit
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PrintSheetLayout.entries.forEach { layout ->
                                        SelectableChip(
                                            label = layout.label,
                                            selected = printLayout == layout,
                                            onClick = { printLayout = layout }
                                        )
                                    }
                                }
                                Text(
                                    text = printLayout.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    QraftPillButton(
                                        text = "Save A4 PNG",
                                        variant = QraftButtonVariant.TONAL,
                                        leadingIcon = Icons.Rounded.Image,
                                        enabled = !isExporting,
                                        height = 42.dp,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onExportPrintSheet(printLayout, false) }
                                    )
                                    QraftPillButton(
                                        text = "Save 300 DPI PDF",
                                        variant = QraftButtonVariant.PRIMARY,
                                        leadingIcon = Icons.Rounded.PictureAsPdf,
                                        enabled = !isExporting,
                                        height = 42.dp,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onExportPrintSheet(printLayout, true) }
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

@Composable
fun FullscreenScanDialog(
    title: String,
    encodedPayload: String,
    design: QrDesignSpec,
    onDismiss: () -> Unit,
    codeKind: CodeKind = CodeKind.QR,
    barcodeSpec: BarcodeDesignSpec? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val revealAnim = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val dragYAnim = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }

    fun dismissSmooth(velocityY: Float = 0f) {
        if (isClosing) return
        isClosing = true
        scope.launch {
            if (reduceMotion) {
                revealAnim.snapTo(0f)
            } else {
                revealAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = AppMotion.navigationSpring(),
                    initialVelocity = (-velocityY / with(density) { 400.dp.toPx() }).coerceIn(-5f, 0f)
                )
            }
            currentOnDismiss()
        }
    }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            revealAnim.animateTo(1f, AppMotion.navigationSpring())
        }
    }

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
        onDismissRequest = { dismissSmooth() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val colors = QraftThemeTokens.colors
        QraftDialogSystemBars(
            background = colors.background,
            platformDimAmount = 0f
        )

        QraftPredictiveBackHandler(
            enabled = !isClosing,
            onProgress = { backProgress = it },
            onBack = { dismissSmooth() }
        )

        val dismissThresholdPx = with(density) { 130.dp.toPx() }
        val dragDismissFrac = (dragYAnim.value / dismissThresholdPx).coerceIn(0f, 1f)
        val combinedAlpha = revealAnim.value.coerceIn(0f, 1f) *
            (1f - dragDismissFrac * 0.45f) *
            (1f - backProgress * 0.35f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = combinedAlpha }
                .background(colors.background.copy(alpha = 0.96f))
                .pointerInput(Unit) {
                    val tracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            val next = dragYAnim.value + dragAmount
                            val mapped = if (next < 0f) {
                                AppMotion.rubberBand(next, 640f, constant = 0.55f)
                            } else {
                                next
                            }
                            scope.launch { dragYAnim.snapTo(mapped) }
                        },
                        onDragEnd = {
                            val vy = tracker.calculateVelocity().y
                            val projected = dragYAnim.value + AppMotion.projectOffset(vy, 0.99f)
                            if (projected > dismissThresholdPx || vy > 1100f) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                dismissSmooth(vy)
                            } else {
                                scope.launch {
                                    dragYAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = AppMotion.navigationSpring(),
                                        initialVelocity = vy
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { dragYAnim.animateTo(0f, AppMotion.navigationSpring()) }
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissSmooth() }
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.graphicsLayer {
                    val p = revealAnim.value
                    val backCurved = 0.7f * backProgress +
                        0.3f * (1f - (1f - backProgress) * (1f - backProgress))
                    val s = (0.92f + 0.08f * p) * (1f - 0.06f * backCurved) * (1f - 0.06f * dragDismissFrac)
                    scaleX = s
                    scaleY = s
                    translationY = dragYAnim.value + backCurved * 28.dp.toPx()
                    transformOrigin = TransformOrigin.Center
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = title.ifBlank { "Ready to Scan" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary
                )

                if (codeKind == CodeKind.BARCODE && barcodeSpec != null) {
                    BarcodeArtworkPreview(
                        spec = barcodeSpec,
                        widthDp = 304.dp,
                        showCheckerboardForTransparent = false
                    )
                } else {
                    QrArtworkPreview(
                        data = encodedPayload,
                        design = design,
                        qrViewportSize = 276.dp,
                        showCheckerboardForTransparent = false
                    )
                }

                AppleGlassSurface(
                    shape = RoundedCornerShape(50),
                    cornerRadiusDp = 50.dp,
                    variant = AppleGlassVariant.Regular,
                    enableRefraction = true
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
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
                            text = "Brightness boosted • Swipe or tap to close",
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
    batchProgress: Pair<Int, Int>? = null,
    onDismiss: () -> Unit,
    onRunBatch: (
        lines: List<String>,
        saveToLibrary: Boolean,
        exportPngs: Boolean,
        exportPdfLayout: PrintSheetLayout?
    ) -> Unit
) {
    val colors = QraftThemeTokens.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var useSequenceMode by remember { mutableStateOf(false) }
    var rawLines by remember {
        mutableStateOf("https://qraft.studio/table-1\nhttps://qraft.studio/table-2\nhttps://qraft.studio/table-3")
    }
    var seqPrefix by remember { mutableStateOf("https://qraft.studio/table-") }
    var seqSuffix by remember { mutableStateOf("") }
    var seqStartText by remember { mutableStateOf("1") }
    var seqEndText by remember { mutableStateOf("12") }
    var seqPadDigits by remember { mutableIntStateOf(2) }

    var saveToLibrary by remember { mutableStateOf(true) }
    var exportPngs by remember { mutableStateOf(true) }
    var exportPdfSheet by remember { mutableStateOf(false) }
    var pdfLayout by remember { mutableStateOf(PrintSheetLayout.STICKERS_3X4) }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val content = QrExporter.readTextFromUri(context, uri)
                if (!content.isNullOrBlank()) {
                    val imported = BatchSequenceExpander.parseImportedLines(content)
                    if (imported.isNotEmpty()) {
                        rawLines = imported.joinToString("\n")
                        useSequenceMode = false
                    }
                }
            }
        }
    }

    val parsedLines = remember(
        useSequenceMode,
        rawLines,
        seqPrefix,
        seqSuffix,
        seqStartText,
        seqEndText,
        seqPadDigits
    ) {
        if (!useSequenceMode) {
            rawLines.lines().map { it.trim() }.filter { it.isNotEmpty() }.take(200)
        } else {
            val start = seqStartText.trim().toIntOrNull() ?: 1
            val end = seqEndText.trim().toIntOrNull() ?: start
            BatchSequenceExpander.expandSequence(
                prefix = seqPrefix.trim(),
                suffix = seqSuffix.trim(),
                start = start,
                end = end,
                zeroPad = seqPadDigits
            )
        }
    }

    QraftModalSheet(
        onDismissRequest = onDismiss,
        maxHeightFraction = 0.90f,
        hardCap = 760.dp,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Batch QR Generator Pro", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
            Text(
                "Generate multiple QR codes using your Studio style (${currentDesign.exportSizePx}px export ready).",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableChip(
                    label = "Lines / CSV",
                    selected = !useSequenceMode,
                    onClick = { useSequenceMode = false },
                    modifier = Modifier.weight(1f)
                )
                SelectableChip(
                    label = "Sequence Range",
                    selected = useSequenceMode,
                    onClick = { useSequenceMode = true },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!useSequenceMode) {
                QraftTextField(
                    value = rawLines,
                    onValueChange = { rawLines = it },
                    label = "Payloads (${parsedLines.size} items)",
                    placeholder = "https://example.com/1\nhttps://example.com/2",
                    singleLine = false,
                    minLines = 4,
                    maxLines = 7
                )
                QraftPillButton(
                    text = "Import .csv or .txt File",
                    variant = QraftButtonVariant.TONAL,
                    leadingIcon = Icons.Rounded.UploadFile,
                    height = 42.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        runCatching {
                            csvImportLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values"))
                        }
                    }
                )
            } else {
                QraftTextField(
                    value = seqPrefix,
                    onValueChange = { seqPrefix = it },
                    label = "Prefix before number",
                    placeholder = "https://qraft.studio/table-"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QraftTextField(
                        value = seqStartText,
                        onValueChange = { seqStartText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(5) },
                        label = "Start #",
                        placeholder = "1",
                        modifier = Modifier.weight(1f)
                    )
                    QraftTextField(
                        value = seqEndText,
                        onValueChange = { seqEndText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(5) },
                        label = "End #",
                        placeholder = "12",
                        modifier = Modifier.weight(1f)
                    )
                }
                QraftTextField(
                    value = seqSuffix,
                    onValueChange = { seqSuffix = it },
                    label = "Optional suffix",
                    placeholder = "?utm_source=print"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Zero-pad:", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    listOf(1, 2, 3, 4).forEach { pad ->
                        SelectableChip(
                            label = if (pad == 1) "None" else "${pad}d",
                            selected = seqPadDigits == pad,
                            onClick = { seqPadDigits = pad }
                        )
                    }
                }
                if (parsedLines.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "${parsedLines.size} generated items (preview):",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            parsedLines.take(3).forEach { sample ->
                                Text(sample, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

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
            ToggleRow(
                title = "Export Multi-Page A4 Print Sheet PDF",
                subtitle = "300 DPI printable sheet in Documents/Qraft",
                checked = exportPdfSheet,
                onCheckedChange = { exportPdfSheet = it }
            )
            AnimatedVisibility(
                visible = exportPdfSheet,
                enter = AppMotion.softExpandEnter,
                exit = AppMotion.softExpandExit
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrintSheetLayout.entries.forEach { layout ->
                        SelectableChip(
                            label = layout.label,
                            selected = pdfLayout == layout,
                            onClick = { pdfLayout = layout }
                        )
                    }
                }
            }

            if (isRunning && batchProgress != null) {
                val (done, total) = batchProgress
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { if (total > 0) done.toFloat() / total.toFloat() else 0f },
                        color = colors.espressoAccent,
                        trackColor = colors.surfaceMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        "Processing $done of $total QR codes…",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }

            QraftPillButton(
                text = if (isRunning) "Generating Batch…" else "Generate ${parsedLines.size} QR Codes",
                variant = QraftButtonVariant.PRIMARY,
                leadingIcon = Icons.Rounded.AutoAwesomeMotion,
                loading = isRunning,
                enabled = parsedLines.isNotEmpty() && !isRunning && (saveToLibrary || exportPngs || exportPdfSheet),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onRunBatch(
                        parsedLines,
                        saveToLibrary,
                        exportPngs,
                        if (exportPdfSheet) pdfLayout else null
                    )
                }
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
