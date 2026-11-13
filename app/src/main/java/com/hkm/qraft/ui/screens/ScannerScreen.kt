package com.hkm.qraft.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Size as AndroidSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.hkm.qraft.model.BarcodeFormatType
import com.hkm.qraft.model.QrContentType
import com.hkm.qraft.model.QrPayloadFields
import com.hkm.qraft.qr.QrCodeImporter
import com.hkm.qraft.qr.QrExporter
import com.hkm.qraft.ui.components.QraftModalSheet
import com.hkm.qraft.ui.components.QraftSectionCard
import com.hkm.qraft.ui.components.QraftStatusBarScrim
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class ScannedCodeResult(
    val rawText: String,
    val zxingFormat: BarcodeFormat,
    val mappedBarcodeFormat: BarcodeFormatType?,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val formatLabel: String
        get() = mappedBarcodeFormat?.label ?: if (zxingFormat == BarcodeFormat.QR_CODE) "QR Code" else zxingFormat.name
}

@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenInStudio: (rawText: String, barcodeFormat: BarcodeFormatType?) -> Unit,
    onSaveToLibrary: (rawText: String, barcodeFormat: BarcodeFormatType?) -> Unit,
    onCopyResult: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val colors = QraftThemeTokens.colors
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var useFrontCamera by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraAvailable by remember { mutableStateOf(true) }

    // Pro camera states: optical/digital zoom, tap-to-focus reticle, continuous batch history
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(5f) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var focusTapPoint by remember { mutableStateOf<Offset?>(null) }
    var continuousScanMode by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    val scanHistory = remember { mutableStateListOf<ScannedCodeResult>() }

    var scannedResult by remember { mutableStateOf<ScannedCodeResult?>(null) }
    var isScanningGallery by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var lastDecodedLockTime by remember { mutableLongStateOf(0L) }
    var lastDecodedLockText by remember { mutableStateOf("") }

    fun handleDetectedCode(result: ScannedCodeResult) {
        val now = System.currentTimeMillis()
        if (continuousScanMode && result.rawText == lastDecodedLockText && now - lastDecodedLockTime < 2200L) {
            return
        }
        lastDecodedLockTime = now
        lastDecodedLockText = result.rawText
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scanHistory.removeAll { it.rawText == result.rawText }
        scanHistory.add(0, result)
        if (scanHistory.size > 30) {
            scanHistory.removeAt(scanHistory.lastIndex)
        }
        if (continuousScanMode) {
            onSaveToLibrary(result.rawText, result.mappedBarcodeFormat)
        } else {
            scannedResult = result
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isScanningGallery = true
                scanError = null
                val result = withContext(Dispatchers.Default) {
                    decodeImageUriAnyFormat(context, uri)
                }
                isScanningGallery = false
                if (result != null) {
                    handleDetectedCode(result)
                    if (continuousScanMode) scannedResult = result
                } else {
                    scanError = "No QR code or barcode detected in that image."
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(torchEnabled, cameraRef) {
        runCatching {
            cameraRef?.cameraControl?.enableTorch(torchEnabled)
        }
    }

    LaunchedEffect(cameraRef, lifecycleOwner) {
        val cam = cameraRef ?: return@LaunchedEffect
        runCatching {
            val state = cam.cameraInfo.zoomState.value
            if (state != null) {
                minZoomRatio = state.minZoomRatio.coerceAtLeast(0.5f)
                maxZoomRatio = state.maxZoomRatio.coerceAtLeast(minZoomRatio)
                currentZoomRatio = state.zoomRatio.coerceIn(minZoomRatio, maxZoomRatio)
            }
            cam.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                if (zoomState != null) {
                    minZoomRatio = zoomState.minZoomRatio.coerceAtLeast(0.5f)
                    maxZoomRatio = zoomState.maxZoomRatio.coerceAtLeast(minZoomRatio)
                    currentZoomRatio = zoomState.zoomRatio.coerceIn(minZoomRatio, maxZoomRatio)
                }
            }
        }
    }

    // Auto-hide tap-to-focus reticle
    LaunchedEffect(focusTapPoint) {
        if (focusTapPoint != null) {
            delay(1300)
            focusTapPoint = null
        }
    }

    val focusPulse = remember { Animatable(1.4f) }
    LaunchedEffect(focusTapPoint) {
        if (focusTapPoint != null) {
            focusPulse.snapTo(1.45f)
            focusPulse.animateTo(1f, tween(220))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scannerLaser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (hasCameraPermission && cameraAvailable) Color.Black else colors.background)
    ) {
        if (hasCameraPermission && cameraAvailable) {
            val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
            DisposableEffect(Unit) {
                onDispose {
                    runCatching { analyzerExecutor.shutdown() }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(cameraRef, maxZoomRatio, minZoomRatio) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            val cam = cameraRef ?: return@detectTransformGestures
                            if (zoomChange != 1f) {
                                val next = (currentZoomRatio * zoomChange).coerceIn(minZoomRatio, maxZoomRatio)
                                currentZoomRatio = next
                                runCatching { cam.cameraControl.setZoomRatio(next) }
                            }
                        }
                    }
                    .pointerInput(cameraRef, previewViewRef) {
                        detectTapGestures { offset ->
                            focusTapPoint = offset
                            val pv = previewViewRef ?: return@detectTapGestures
                            val cam = cameraRef ?: return@detectTapGestures
                            runCatching {
                                val point = pv.meteringPointFactory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(
                                    point,
                                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                )
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                                cam.cameraControl.startFocusAndMetering(action)
                            }
                        }
                    }
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewViewRef = this
                        }
                    },
                    update = { previewView ->
                        previewViewRef = previewView
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            runCatching {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val selector = if (useFrontCamera) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                                val resSelector = ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                        ResolutionStrategy(
                                            AndroidSize(1280, 960),
                                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                        )
                                    )
                                    .build()
                                val analyzer = ImageAnalysis.Builder()
                                    .setResolutionSelector(resSelector)
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(analyzerExecutor, ZxingMultiFormatAnalyzer { decoded ->
                                            if (scannedResult == null && !showHistorySheet) {
                                                previewView.post {
                                                    handleDetectedCode(decoded)
                                                }
                                            }
                                        })
                                    }

                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    analyzer
                                )
                                cameraRef = cam
                            }.onFailure {
                                cameraAvailable = false
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )

                // Viewfinder overlay + tap-to-focus reticle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val boxSize = size.minDimension * 0.68f
                    val left = (size.width - boxSize) / 2f
                    val top = (size.height - boxSize) * 0.39f
                    val right = left + boxSize
                    val bottom = top + boxSize
                    val leg = 28.dp.toPx()
                    val stroke = 3.5.dp.toPx()

                    val path = Path().apply {
                        moveTo(left, top + leg); lineTo(left, top); lineTo(left + leg, top)
                        moveTo(right - leg, top); lineTo(right, top); lineTo(right, top + leg)
                        moveTo(left, bottom - leg); lineTo(left, bottom); lineTo(left + leg, bottom)
                        moveTo(right - leg, bottom); lineTo(right, bottom); lineTo(right, bottom - leg)
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    if (scannedResult == null) {
                        val y = top + boxSize * laserProgress
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White, Color.Transparent),
                                startX = left + 12f,
                                endX = right - 12f
                            ),
                            start = Offset(left + 12f, y),
                            end = Offset(right - 12f, y),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    focusTapPoint?.let { pt ->
                        val radius = 26.dp.toPx() * focusPulse.value
                        drawCircle(
                            color = Color.White.copy(alpha = 0.92f),
                            radius = radius,
                            center = pt,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.65f),
                            radius = 3.dp.toPx(),
                            center = pt
                        )
                    }
                }

                // Top status bar overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onBack != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.56f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier
                                .size(40.dp)
                                .appleBounceClick { onBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.size(40.dp))
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.56f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (continuousScanMode) "Batch Scan Active" else "Tap to Focus • Pinch to Zoom",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Scan history pill button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (scanHistory.isNotEmpty()) Color.White else Color.Black.copy(alpha = 0.56f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .height(40.dp)
                            .appleBounceClick { showHistorySheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = "Scan history",
                                tint = if (scanHistory.isNotEmpty()) Color.Black else Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            if (scanHistory.isNotEmpty()) {
                                Text(
                                    text = "${scanHistory.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Bottom HUD: Zoom Presets + Slider + Action Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 22.dp, start = 18.dp, end = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(visible = scanError != null, enter = fadeIn(), exit = fadeOut()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.dangerBg,
                            border = BorderStroke(1.dp, colors.dangerFg.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = scanError.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.dangerFg,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Zoom control bar (presets 1x / 2x / 3x / 5x + smooth slider)
                    if (maxZoomRatio > minZoomRatio + 0.1f) {
                        val zoomSteps = remember(minZoomRatio, maxZoomRatio) {
                            buildList {
                                if (minZoomRatio < 0.85f) add(minZoomRatio)
                                add(1f)
                                if (maxZoomRatio >= 2f) add(2f)
                                if (maxZoomRatio >= 3f) add(3f)
                                if (maxZoomRatio >= 5f) add(5f) else if (maxZoomRatio > 3.2f) add(maxZoomRatio)
                            }.distinct()
                        }
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.Black.copy(alpha = 0.58f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        zoomSteps.forEach { preset ->
                                            val selected = kotlin.math.abs(currentZoomRatio - preset) < 0.25f
                                            val label = if (preset < 1f) {
                                                "${((preset * 10).roundToInt() / 10f)}x"
                                            } else {
                                                "${preset.roundToInt()}x"
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (selected) Color.White else Color.White.copy(alpha = 0.14f),
                                                modifier = Modifier.appleBounceClick {
                                                    val target = preset.coerceIn(minZoomRatio, maxZoomRatio)
                                                    currentZoomRatio = target
                                                    runCatching { cameraRef?.cameraControl?.setZoomRatio(target) }
                                                }
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (selected) Color.Black else Color.White,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${((currentZoomRatio * 10f).roundToInt() / 10f)}x",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                Slider(
                                    value = currentZoomRatio.coerceIn(minZoomRatio, maxZoomRatio),
                                    onValueChange = { z ->
                                        currentZoomRatio = z
                                        runCatching { cameraRef?.cameraControl?.setZoomRatio(z) }
                                    },
                                    valueRange = minZoomRatio..maxZoomRatio,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }

                    // Bottom primary camera actions
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScannerRoundButton(
                            icon = if (torchEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                            label = if (torchEnabled) "On" else "Torch",
                            active = torchEnabled,
                            onClick = { torchEnabled = !torchEnabled }
                        )
                        ScannerRoundButton(
                            icon = Icons.Rounded.PhotoLibrary,
                            label = if (isScanningGallery) "Reading..." else "Gallery",
                            active = false,
                            onClick = {
                                runCatching {
                                    galleryPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            }
                        )
                        ScannerRoundButton(
                            icon = Icons.Rounded.BurstMode,
                            label = if (continuousScanMode) "Batch" else "Single",
                            active = continuousScanMode,
                            onClick = { continuousScanMode = !continuousScanMode }
                        )
                        ScannerRoundButton(
                            icon = Icons.Rounded.FlipCameraAndroid,
                            label = "Flip",
                            active = useFrontCamera,
                            onClick = {
                                useFrontCamera = !useFrontCamera
                                torchEnabled = false
                            }
                        )
                    }
                }
            }
        } else {
            // Permission / Headless fallback with Tonser edge-to-edge scrolling header
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onBack != null) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .size(40.dp)
                                .appleBounceClick { onBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scanner",
                            style = MaterialTheme.typography.displaySmall,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Decode QR codes, 8K exports & 1D/2D barcodes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    if (scanHistory.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.appleBounceClick { showHistorySheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.History, null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                Text("${scanHistory.size}", style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
                            }
                        }
                    }
                }

                QraftSectionCard(contentPadding = PaddingValues(20.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = colors.espressoAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = if (!hasCameraPermission) {
                                "Camera Permission Required"
                            } else {
                                "Camera Unavailable on This Device"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (!hasCameraPermission) {
                                "Allow camera access with pinch-zoom & focus, or scan any QR code, 8K framed export, or barcode directly from your gallery."
                            } else {
                                "No hardware camera stream detected here. Upload any QR code, 8K framed PNG, or retail barcode image from your gallery."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!hasCameraPermission) {
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.espressoAccent,
                                        contentColor = colors.espressoAccentContent
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Camera", maxLines = 1, softWrap = false)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        galleryPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                                Spacer(Modifier.width(5.dp))
                                Text(if (isScanningGallery) "Reading..." else "Gallery", color = colors.textPrimary, maxLines = 1, softWrap = false)
                            }
                        }
                        if (scanError != null) {
                            Text(
                                text = scanError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.dangerFg
                            )
                        }
                    }
                }
                Spacer(Modifier.height(110.dp))
            }
            QraftStatusBarScrim(modifier = Modifier.align(Alignment.TopCenter))
        }
    }

    // Tonser-style Result Sheet (with smart payload actions & Recreate in Studio CTA)
    val activeResult = scannedResult
    if (activeResult != null) {
        val parsedImport = remember(activeResult.rawText, activeResult.mappedBarcodeFormat) {
            if (activeResult.mappedBarcodeFormat == null) {
                QrCodeImporter.parsePayloadToImport(activeResult.rawText)
            } else null
        }

        QraftModalSheet(
            onDismissRequest = { scannedResult = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.successBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = colors.successFg
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (parsedImport != null) {
                                "Detected ${parsedImport.type.title} QR"
                            } else {
                                "Detected ${activeResult.formatLabel}"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = parsedImport?.suggestedTitle ?: "Decoded cleanly offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Structured summary preview when available
                val summaryLines = remember(parsedImport) {
                    parsedImport?.let { buildParsedSummary(it.type, it.fields) }.orEmpty()
                }
                if (summaryLines.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.background,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            summaryLines.forEach { (k, v) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "$k:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = v,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Raw decoded data block
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceMuted,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activeResult.rawText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = if (activeResult.mappedBarcodeFormat != null) FontFamily.Monospace else FontFamily.Default
                        ),
                        color = colors.textPrimary,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                // Quick utilities row: Copy + Contextual Native Intent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            QrExporter.copyPayloadToClipboard(context, activeResult.formatLabel, activeResult.rawText)
                            onCopyResult(activeResult.rawText)
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy", color = colors.textPrimary)
                    }

                    val contextualAction = remember(activeResult.rawText, parsedImport) {
                        resolveSmartAction(activeResult.rawText, parsedImport?.type, parsedImport?.fields)
                    }
                    if (contextualAction != null) {
                        OutlinedButton(
                            onClick = { contextualAction.onInvoke(context, onCopyResult) },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(contextualAction.icon, null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text(contextualAction.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Primary Save & Recreate / Customize in Studio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSaveToLibrary(activeResult.rawText, activeResult.mappedBarcodeFormat)
                            scannedResult = null
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("Save", color = colors.textPrimary)
                    }
                    Button(
                        onClick = {
                            onOpenInStudio(activeResult.rawText, activeResult.mappedBarcodeFormat)
                            scannedResult = null
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.espressoAccent,
                            contentColor = colors.espressoAccentContent
                        ),
                        modifier = Modifier.weight(1.45f)
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Recreate in Studio", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }

    // Recent Scans History Sheet (Tonser-style QraftModalSheet)
    if (showHistorySheet) {
        QraftModalSheet(
            onDismissRequest = { showHistorySheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Recent Scans", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                        Text(
                            text = if (scanHistory.isEmpty()) "Scanned QR codes & barcodes appear here" else "${scanHistory.size} captured this session",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    if (scanHistory.isNotEmpty()) {
                        TextButton(onClick = { scanHistory.clear() }) {
                            Text("Clear", color = colors.dangerFg)
                        }
                    }
                }

                if (scanHistory.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.background,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Point the camera at a QR or barcode, or import a photo from your gallery.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    scanHistory.forEach { entry ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.background,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .appleBounceClick {
                                    showHistorySheet = false
                                    scannedResult = entry
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    if (entry.mappedBarcodeFormat != null) Icons.Rounded.ViewWeek else Icons.Rounded.QrCode2,
                                    contentDescription = null,
                                    tint = colors.espressoAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.rawText,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = entry.formatLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textSecondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        showHistorySheet = false
                                        onOpenInStudio(entry.rawText, entry.mappedBarcodeFormat)
                                    }
                                ) {
                                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = "Recreate", tint = colors.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SmartScanAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onInvoke: (Context, onCopied: (String) -> Unit) -> Unit
)

private fun resolveSmartAction(
    rawText: String,
    type: QrContentType?,
    fields: QrPayloadFields?
): SmartScanAction? {
    if (type == QrContentType.WIFI && fields != null && fields.wifiPassword.isNotBlank()) {
        return SmartScanAction("Copy Wi-Fi Pass", Icons.Rounded.Key) { ctx, onCopied ->
            QrExporter.copyPayloadToClipboard(ctx, "Wi-Fi Password", fields.wifiPassword)
            onCopied(fields.wifiPassword)
        }
    }
    if (type == QrContentType.PHONE && fields != null && fields.phoneNumber.isNotBlank()) {
        return SmartScanAction("Call Number", Icons.Rounded.Call) { ctx, _ ->
            safeLaunchIntent(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(fields.phoneNumber)}")))
        }
    }
    if (type == QrContentType.EMAIL && fields != null && fields.emailTo.isNotBlank()) {
        return SmartScanAction("Send Email", Icons.Rounded.Email) { ctx, _ ->
            safeLaunchIntent(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(fields.emailTo)}")))
        }
    }
    if (type == QrContentType.SMS && fields != null && fields.smsNumber.isNotBlank()) {
        return SmartScanAction("Text SMS", Icons.Rounded.Sms) { ctx, _ ->
            safeLaunchIntent(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(fields.smsNumber)}")))
        }
    }
    if (type == QrContentType.GEO && fields != null) {
        return SmartScanAction("Open Map", Icons.Rounded.LocationOn) { ctx, _ ->
            val q = Uri.encode("${fields.latitude},${fields.longitude}")
            safeLaunchIntent(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("geo:${fields.latitude},${fields.longitude}?q=$q")))
        }
    }
    if (type == QrContentType.EVENT && fields != null) {
        return SmartScanAction("Add Calendar", Icons.Rounded.CalendarMonth) { ctx, _ ->
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, fields.eventTitle)
                if (fields.eventLocation.isNotBlank()) {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, fields.eventLocation)
                }
                if (fields.eventDescription.isNotBlank()) {
                    putExtra(CalendarContract.Events.DESCRIPTION, fields.eventDescription)
                }
                parseEventMillis(fields.eventStart)?.let { putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
                parseEventMillis(fields.eventEnd)?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
            }
            safeLaunchIntent(ctx, intent)
        }
    }
    val isUrlLike = rawText.startsWith("http://", ignoreCase = true) ||
        rawText.startsWith("https://", ignoreCase = true)
    if (isUrlLike) {
        return SmartScanAction("Open Link", Icons.Rounded.OpenInBrowser) { ctx, _ ->
            safeLaunchIntent(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(rawText)))
        }
    }
    return null
}

private fun safeLaunchIntent(context: Context, intent: Intent) {
    runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun parseEventMillis(raw: String): Long? {
    val clean = raw.trim()
    if (clean.isBlank()) return null
    for (pattern in listOf("yyyyMMdd'T'HHmmss", "yyyyMMdd'T'HHmm", "yyyy-MM-dd HH:mm", "yyyy-MM-dd")) {
        runCatching {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            return sdf.parse(clean.removeSuffix("Z"))?.time
        }
    }
    return null
}

private fun buildParsedSummary(type: QrContentType, f: QrPayloadFields): List<Pair<String, String>> = when (type) {
    QrContentType.WIFI -> listOfNotNull(
        f.wifiSsid.takeIf { it.isNotBlank() }?.let { "Network" to it },
        "Security" to f.wifiSecurity.label,
        f.wifiPassword.takeIf { it.isNotBlank() }?.let { "Password" to it }
    )
    QrContentType.VCARD -> listOfNotNull(
        listOf(f.firstName, f.lastName).filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() }?.let { "Name" to it },
        f.organization.takeIf { it.isNotBlank() }?.let { "Org" to it },
        f.phone.takeIf { it.isNotBlank() }?.let { "Phone" to it },
        f.email.takeIf { it.isNotBlank() }?.let { "Email" to it }
    )
    QrContentType.EVENT -> listOfNotNull(
        f.eventTitle.takeIf { it.isNotBlank() }?.let { "Event" to it },
        f.eventStart.takeIf { it.isNotBlank() }?.let { "Starts" to it },
        f.eventLocation.takeIf { it.isNotBlank() }?.let { "Where" to it }
    )
    QrContentType.PHONE -> listOfNotNull(f.phoneNumber.takeIf { it.isNotBlank() }?.let { "Phone" to it })
    QrContentType.EMAIL -> listOfNotNull(
        f.emailTo.takeIf { it.isNotBlank() }?.let { "To" to it },
        f.emailSubject.takeIf { it.isNotBlank() }?.let { "Subject" to it }
    )
    QrContentType.GEO -> listOf("Coordinates" to "${f.latitude}, ${f.longitude}")
    else -> emptyList()
}

@Composable
private fun ScannerRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) Color.White else Color.Black.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (active) 1f else 0.25f)),
        modifier = Modifier.appleBounceClick(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) Color.Black else Color.White,
                maxLines = 1
            )
        }
    }
}

private class ZxingMultiFormatAnalyzer(
    private val onDecoded: (ScannedCodeResult) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedMs = 0L
    private var frameCounter = 0
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.ALSO_INVERTED to true,
                DecodeHintType.CHARACTER_SET to "UTF-8",
                DecodeHintType.POSSIBLE_FORMATS to BarcodeFormat.entries.toList()
            )
        )
    }

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedMs < 110L) {
            image.close()
            return
        }
        lastAnalyzedMs = now
        frameCounter++

        try {
            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            val width = image.width
            val height = image.height
            if (width <= 0 || height <= 0) return

            // 1. Center ROI crop improves long-distance small QR lock
            val cropW = (width * 0.56f).toInt().coerceIn(120, width)
            val cropH = (height * 0.56f).toInt().coerceIn(120, height)
            val cropLeft = (width - cropW) / 2
            val cropTop = (height - cropH) / 2
            val centerSource = PlanarYUVLuminanceSource(
                data, width, height, cropLeft, cropTop, cropW, cropH, false
            )
            var result = decodeLuminanceSource(centerSource)

            // 2. Full frame fallback (good for wide 1D retail barcodes or offset QRs)
            if (result == null) {
                val fullSource = PlanarYUVLuminanceSource(
                    data, width, height, 0, 0, width, height, false
                )
                result = decodeLuminanceSource(fullSource)
                    ?: if (frameCounter % 2 == 0) decodeLuminanceSource(fullSource.invert()) else null
            }

            if (result != null && result.text.isNotBlank()) {
                onDecoded(
                    ScannedCodeResult(
                        rawText = result.text,
                        zxingFormat = result.barcodeFormat,
                        mappedBarcodeFormat = mapZxingToBarcodeFormat(result.barcodeFormat)
                    )
                )
            }
        } catch (_: Throwable) {
        } finally {
            reader.reset()
            image.close()
        }
    }

    private fun decodeLuminanceSource(source: com.google.zxing.LuminanceSource): com.google.zxing.Result? {
        return runCatching {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        }.getOrNull() ?: runCatching {
            reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
        }.getOrNull()
    }
}

private fun mapZxingToBarcodeFormat(format: BarcodeFormat): BarcodeFormatType? = when (format) {
    BarcodeFormat.CODE_128 -> BarcodeFormatType.CODE_128
    BarcodeFormat.EAN_13 -> BarcodeFormatType.EAN_13
    BarcodeFormat.EAN_8 -> BarcodeFormatType.EAN_8
    BarcodeFormat.UPC_A -> BarcodeFormatType.UPC_A
    BarcodeFormat.CODE_39 -> BarcodeFormatType.CODE_39
    BarcodeFormat.ITF -> BarcodeFormatType.ITF
    BarcodeFormat.CODABAR -> BarcodeFormatType.CODABAR
    BarcodeFormat.PDF_417 -> BarcodeFormatType.PDF_417
    BarcodeFormat.AZTEC -> BarcodeFormatType.AZTEC
    BarcodeFormat.DATA_MATRIX -> BarcodeFormatType.DATA_MATRIX
    else -> null
}

private fun decodeImageUriAnyFormat(context: Context, uri: Uri): ScannedCodeResult? {
    val decoded = QrCodeImporter.decodeFromUriAnyFormat(context, uri) ?: return null
    return ScannedCodeResult(
        rawText = decoded.text,
        zxingFormat = decoded.format,
        mappedBarcodeFormat = mapZxingToBarcodeFormat(decoded.format)
    )
}
