package com.hkm.qraft.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
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
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.hkm.qraft.model.BarcodeFormatType
import com.hkm.qraft.qr.QrExporter
import com.hkm.qraft.ui.components.QraftSectionCard
import com.hkm.qraft.ui.components.QraftStatusBarScrim
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

data class ScannedCodeResult(
    val rawText: String,
    val zxingFormat: BarcodeFormat,
    val mappedBarcodeFormat: BarcodeFormatType?,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val formatLabel: String
        get() = mappedBarcodeFormat?.label ?: if (zxingFormat == BarcodeFormat.QR_CODE) "QR Code" else zxingFormat.name
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var cameraAvailable by remember { mutableStateOf(true) }
    var scannedResult by remember { mutableStateOf<ScannedCodeResult?>(null) }
    var isScanningGallery by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

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
                    scannedResult = result
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
                    analyzerExecutor.shutdown()
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { previewView ->
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
                            val analyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(analyzerExecutor, ZxingMultiFormatAnalyzer { decoded ->
                                        if (scannedResult == null) {
                                            previewView.post {
                                                scannedResult = decoded
                                            }
                                        }
                                    })
                                }

                            cameraProvider.unbindAll()
                            cameraRef = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                analyzer
                            )
                        }.onFailure {
                            cameraAvailable = false
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            // Dark vignette & animated monochrome viewfinder reticle
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val boxSize = size.minDimension * 0.66f
                    val left = (size.width - boxSize) / 2f
                    val top = (size.height - boxSize) * 0.44f
                    val right = left + boxSize
                    val bottom = top + boxSize
                    val leg = 28.dp.toPx()
                    val stroke = 3.5.dp.toPx()

                    // Corner viewfinder brackets in pure white
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
                }

                // Scrolling-style top translucent status header
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                    text = "Align QR or Barcode",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(Modifier.size(40.dp))
                }

                // Action controls floating pill above gesture bar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScannerRoundButton(
                        icon = if (torchEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        label = if (torchEnabled) "Torch On" else "Torch",
                        active = torchEnabled,
                        onClick = { torchEnabled = !torchEnabled }
                    )
                    ScannerRoundButton(
                        icon = Icons.Rounded.PhotoLibrary,
                        label = if (isScanningGallery) "Reading..." else "Gallery",
                        active = false,
                        onClick = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
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
        } else {
            // Permission / Headless fallback with full Tonser edge-to-edge scrolling header
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
                            text = "Decode QR codes and 1D/2D barcodes instantly",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
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
                                "Allow camera access to scan live QR codes and retail barcodes, or import any photo from your gallery."
                            } else {
                                "No hardware camera stream detected here. You can still scan any QR code or barcode image directly from your gallery."
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
                                    galleryPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                                Spacer(Modifier.width(5.dp))
                                Text("Gallery", color = colors.textPrimary, maxLines = 1, softWrap = false)
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

    val activeResult = scannedResult
    if (activeResult != null) {
        ModalBottomSheet(
            onDismissRequest = { scannedResult = null },
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
                            text = "Detected ${activeResult.formatLabel}",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Decoded cleanly offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.background,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activeResult.rawText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = if (activeResult.mappedBarcodeFormat != null) FontFamily.Monospace else FontFamily.Default
                        ),
                        color = colors.textPrimary,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(14.dp)
                    )
                }

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

                    val isUrlLike = activeResult.rawText.startsWith("http://", ignoreCase = true) ||
                        activeResult.rawText.startsWith("https://", ignoreCase = true)
                    if (isUrlLike) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(activeResult.rawText)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.OpenInBrowser, null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text("Open Link", color = colors.textPrimary)
                        }
                    }
                }

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
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remix in Studio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                color = if (active) Color.Black else Color.White
            )
        }
    }
}

private class ZxingMultiFormatAnalyzer(
    private val onDecoded: (ScannedCodeResult) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedMs = 0L
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.POSSIBLE_FORMATS to BarcodeFormat.entries.toList()
            )
        )
    }

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedMs < 140L) {
            image.close()
            return
        }
        lastAnalyzedMs = now

        try {
            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            val width = image.width
            val height = image.height
            val source = PlanarYUVLuminanceSource(
                data,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val result = runCatching {
                reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            }.getOrNull() ?: runCatching {
                reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
            }.getOrNull()

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

private fun decodeImageUriAnyFormat(context: android.content.Context, uri: Uri): ScannedCodeResult? {
    val bmp = com.hkm.qraft.qr.LogoCatalog.loadSampledUri(context, uri, 1600) ?: return null
    return try {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        val reader = MultiFormatReader()
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to BarcodeFormat.entries.toList()
        )
        val result = runCatching {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints)
        }.getOrNull() ?: runCatching {
            reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), hints)
        }.getOrNull() ?: return null

        ScannedCodeResult(
            rawText = result.text,
            zxingFormat = result.barcodeFormat,
            mappedBarcodeFormat = mapZxingToBarcodeFormat(result.barcodeFormat)
        )
    } finally {
        bmp.recycle()
    }
}
