package com.hkm.qraft

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.content.IntentCompat
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hkm.qraft.model.BarcodeDesignSpec
import com.hkm.qraft.model.BarcodeFormatType
import com.hkm.qraft.model.CodeKind
import com.hkm.qraft.model.QrDesignSpec
import com.hkm.qraft.model.ThemeMode
import com.hkm.qraft.qr.QrPayloadEncoder
import com.hkm.qraft.ui.components.*
import com.hkm.qraft.ui.screens.HomeScreen
import com.hkm.qraft.ui.screens.LibraryScreen
import com.hkm.qraft.ui.screens.OnboardingScreen
import com.hkm.qraft.ui.screens.ScannerScreen
import com.hkm.qraft.ui.screens.SettingsScreen
import com.hkm.qraft.ui.screens.StudioScreen
import com.hkm.qraft.ui.theme.AppMotion
import com.hkm.qraft.ui.theme.AppleAnimatedSwap
import com.hkm.qraft.ui.theme.AppleGlassSurface
import com.hkm.qraft.ui.theme.AppleGlassVariant
import com.hkm.qraft.ui.theme.HeroBoundsRegistry
import com.hkm.qraft.ui.theme.HeroSourceRect
import com.hkm.qraft.ui.theme.LocalHeroBounds
import com.hkm.qraft.ui.theme.QraftTheme
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.rememberReduceMotion
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private sealed interface QraftDest {
    val stateKey: String
    val heroKey: Any?

    data object Home : QraftDest {
        override val stateKey: String = "home"
        override val heroKey: Any? = null
    }

    data class Studio(override val heroKey: Any? = null) : QraftDest {
        override val stateKey: String = "studio"
    }

    data class Library(override val heroKey: Any? = null) : QraftDest {
        override val stateKey: String = "library"
    }

    data class Scanner(override val heroKey: Any? = null) : QraftDest {
        override val stateKey: String = "scanner"
    }

    data class Settings(override val heroKey: Any? = null) : QraftDest {
        override val stateKey: String = "settings"
    }
}

data class FullscreenPreviewState(
    val title: String,
    val encodedPayload: String,
    val design: QrDesignSpec,
    val codeKind: CodeKind = CodeKind.QR,
    val barcode: BarcodeDesignSpec? = null
)

sealed interface PendingSharedInput {
    data class Text(val text: String) : PendingSharedInput
    data class ImageUri(val uri: Uri) : PendingSharedInput
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialShared = extractSharedInput(intent)
        setContent {
            var pendingSharedInput by remember { mutableStateOf(initialShared) }
            val viewModel: QraftViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
            }

            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent ->
                    extractSharedInput(newIntent)?.let { pendingSharedInput = it }
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }

            QraftTheme(themeMode = prefs.themeMode) {
                QraftRoot(
                    viewModel = viewModel,
                    pendingSharedInput = pendingSharedInput,
                    onConsumeSharedInput = { pendingSharedInput = null }
                )
            }
        }
    }

    private fun extractSharedInput(intent: Intent?): PendingSharedInput? {
        if (intent == null) return null
        val streamUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
                if (streamUri != null && (intent.type?.startsWith("image/") == true || extraText == null)) {
                    PendingSharedInput.ImageUri(streamUri)
                } else {
                    extraText?.let(PendingSharedInput::Text)
                }
            }
            Intent.ACTION_VIEW -> intent.dataString?.takeIf { it.isNotBlank() }?.let(PendingSharedInput::Text)
            else -> {
                val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
                when {
                    streamUri != null && intent.type?.startsWith("image/") == true -> PendingSharedInput.ImageUri(streamUri)
                    extraText != null -> PendingSharedInput.Text(extraText)
                    else -> null
                }
            }
        }
    }
}

@Composable
fun QraftRoot(
    viewModel: QraftViewModel,
    pendingSharedInput: PendingSharedInput? = null,
    onConsumeSharedInput: () -> Unit = {}
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val preferencesLoaded by viewModel.preferencesLoaded.collectAsStateWithLifecycle()
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val scannability by viewModel.scannability.collectAsStateWithLifecycle()
    val isVerifying by viewModel.isVerifying.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val batchProgress by viewModel.batchProgress.collectAsStateWithLifecycle()
    val selectedHomeCategory by viewModel.selectedHomeCategory.collectAsStateWithLifecycle()
    val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    val heroRegistry = remember { HeroBoundsRegistry() }
    val saveableHolder = rememberSaveableStateHolder()
    val reduceMotion = rememberReduceMotion()
    val scope = rememberCoroutineScope()

    val backStack = remember { mutableStateListOf<QraftDest>(QraftDest.Home) }
    val transitionReveal = remember { Animatable(1f) }
    val backPeelProgress = remember { Animatable(0f) }
    var isAnimatingTransition by remember { mutableStateOf(false) }
    var transitionJob by remember { mutableStateOf<Job?>(null) }

    // Predictive Back gesture state (finger-coupled quadratic softening + peel continuity)
    var backGestureActive by remember { mutableStateOf(false) }
    var backTouchY by remember { mutableFloatStateOf(0.5f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    fun pushDest(dest: QraftDest, popPreviousTop: Boolean = false) {
        if (backStack.any { it.stateKey == dest.stateKey } && !popPreviousTop) return
        transitionJob?.cancel()
        backGestureActive = false
        transitionJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            isAnimatingTransition = true
            try {
                backPeelProgress.snapTo(0f)
                if (reduceMotion) {
                    if (popPreviousTop && backStack.size > 1) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                    backStack.add(dest)
                    transitionReveal.snapTo(1f)
                } else {
                    transitionReveal.snapTo(0f)
                    if (popPreviousTop && backStack.size > 1) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                    backStack.add(dest)
                    transitionReveal.animateTo(1f, AppMotion.navigationSpring())
                }
            } finally {
                isAnimatingTransition = false
            }
        }
    }

    fun popDest() {
        if (backStack.size <= 1) return
        transitionJob?.cancel()
        backGestureActive = false
        transitionJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            isAnimatingTransition = true
            try {
                backPeelProgress.snapTo(0f)
                if (!reduceMotion && transitionReveal.value > 0.01f) {
                    transitionReveal.animateTo(0f, AppMotion.navigationSpring())
                } else {
                    transitionReveal.snapTo(0f)
                }
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
                transitionReveal.snapTo(1f)
            } finally {
                isAnimatingTransition = false
            }
        }
    }

    var showCreatePicker by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showBatchSheet by remember { mutableStateOf(false) }
    var fullscreenItem by remember { mutableStateOf<FullscreenPreviewState?>(null) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    val qrImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importQrFromUri(uri) {
                pushDest(QraftDest.Studio())
            }
        }
    }
    val launchQrImport: () -> Unit = {
        runCatching {
            qrImportLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackupToUri(uri)
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackupFromUri(uri)
    }

    var replayOnboarding by remember { mutableStateOf(false) }
    val showOnboarding = preferencesLoaded && (!prefs.hasCompletedOnboarding || replayOnboarding)

    LaunchedEffect(pendingSharedInput, showOnboarding, preferencesLoaded) {
        if (preferencesLoaded && !showOnboarding && pendingSharedInput != null) {
            when (pendingSharedInput) {
                is PendingSharedInput.Text -> viewModel.importSharedPayload(pendingSharedInput.text) {
                    pushDest(QraftDest.Studio())
                }
                is PendingSharedInput.ImageUri -> viewModel.importQrFromUri(pendingSharedInput.uri) {
                    pushDest(QraftDest.Studio())
                }
            }
            onConsumeSharedInput()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.statusBanner.collectLatest { msg ->
            bannerMessage = msg
            delay(2600)
            if (bannerMessage == msg) bannerMessage = null
        }
    }

    PredictiveBackHandler(enabled = backStack.size > 1 && !isAnimatingTransition) { progress: Flow<BackEventCompat> ->
        backGestureActive = true
        try {
            progress.collect { event ->
                val raw = event.progress.coerceIn(0f, 1f)
                val curved = 0.7f * raw + 0.3f * (1f - (1f - raw) * (1f - raw))
                backPeelProgress.snapTo(curved)
                backSwipeEdge = event.swipeEdge
                backTouchY = if (event.touchY > 0f) event.touchY else 0.5f
            }
            // Committed back gesture: finish smoothly outward from current peel scalar without reversing direction
            isAnimatingTransition = true
            if (!reduceMotion) {
                backPeelProgress.animateTo(1f, AppMotion.snappySpring())
            } else {
                backPeelProgress.snapTo(1f)
            }
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
            backPeelProgress.snapTo(0f)
            transitionReveal.snapTo(1f)
            backGestureActive = false
            isAnimatingTransition = false
        } catch (t: Throwable) {
            scope.launch {
                if (!reduceMotion) {
                    backPeelProgress.animateTo(0f, AppMotion.navigationSpring())
                } else {
                    backPeelProgress.snapTo(0f)
                }
                backGestureActive = false
            }
            if (t !is kotlinx.coroutines.CancellationException) throw t
        }
    }

    @Composable
    fun RenderRoute(dest: QraftDest) {
        saveableHolder.SaveableStateProvider(dest.stateKey) {
            when (dest) {
                QraftDest.Home -> HomeScreen(
                    creatorName = prefs.creatorName,
                    themeMode = prefs.themeMode,
                    items = savedItems,
                    selectedCategory = selectedHomeCategory,
                    onSelectCategory = viewModel::selectHomeCategory,
                    onCycleTheme = viewModel::cycleThemeMode,
                    onOpenSettings = { pushDest(QraftDest.Settings("hero_settings")) },
                    onOpenStudioNew = { type ->
                        viewModel.startNewQr(type)
                        pushDest(QraftDest.Studio("hero_type_${type.name}"))
                    },
                    onOpenStudioNewBarcode = {
                        viewModel.startNewBarcode(BarcodeFormatType.CODE_128)
                        pushDest(QraftDest.Studio("hero_barcode"))
                    },
                    onOpenScanner = { pushDest(QraftDest.Scanner("hero_scanner")) },
                    onOpenExisting = { item ->
                        viewModel.openExistingQr(item)
                        pushDest(QraftDest.Studio("hero_card_${item.id}"))
                    },
                    onQuickFullscreen = { item ->
                        fullscreenItem = FullscreenPreviewState(
                            title = item.title,
                            encodedPayload = QrPayloadEncoder.encode(item.type, item.payload),
                            design = item.design,
                            codeKind = item.resolvedCodeKind(),
                            barcode = item.resolvedBarcode()
                        )
                    },
                    onToggleFavorite = viewModel::toggleItemFavorite,
                    onOpenLibrary = { pushDest(QraftDest.Library("hero_library")) },
                    onOpenBatch = { showBatchSheet = true },
                    onOpenCreatePicker = { showCreatePicker = true },
                    onImportQr = launchQrImport
                )

                is QraftDest.Library -> LibraryScreen(
                    items = savedItems,
                    onBack = { popDest() },
                    onOpenExisting = { item ->
                        viewModel.openExistingQr(item)
                        pushDest(QraftDest.Studio("hero_lib_${item.id}"))
                    },
                    onDuplicate = viewModel::duplicateQr,
                    onToggleFavorite = viewModel::toggleItemFavorite,
                    onDelete = viewModel::deleteItem,
                    onQuickFullscreen = { item ->
                        fullscreenItem = FullscreenPreviewState(
                            title = item.title,
                            encodedPayload = QrPayloadEncoder.encode(item.type, item.payload),
                            design = item.design,
                            codeKind = item.resolvedCodeKind(),
                            barcode = item.resolvedBarcode()
                        )
                    },
                    onOpenBatch = { showBatchSheet = true },
                    onCreateNew = { showCreatePicker = true },
                    onImportQr = launchQrImport,
                    onOpenScanner = { pushDest(QraftDest.Scanner("hero_lib_scanner")) },
                    onBulkFavorite = viewModel::bulkSetFavorite,
                    onBulkDelete = viewModel::bulkDeleteItems,
                    onExportPrintSheet = viewModel::exportLibraryPrintSheet,
                    onToggleArchive = viewModel::toggleItemArchived,
                    onBulkArchive = viewModel::bulkSetArchived,
                    onRenameItem = viewModel::renameItem,
                    onOrganizeItem = viewModel::updateItemOrganization
                )

                is QraftDest.Studio -> StudioScreen(
                    draft = draft,
                    customPresets = customPresets,
                    scannability = scannability,
                    isVerifying = isVerifying,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = viewModel::undoDesignEdit,
                    onRedo = viewModel::redoDesignEdit,
                    onExtractBrandColors = viewModel::extractAndApplyBrandPalette,
                    onBack = { popDest() },
                    onTitleChange = viewModel::updateDraftTitle,
                    onCodeKindChange = viewModel::setDraftCodeKind,
                    onTypeChange = viewModel::updateDraftType,
                    onFieldsChange = viewModel::updateDraftPayload,
                    onDesignChange = viewModel::updateDraftDesign,
                    onBarcodeChange = viewModel::updateDraftBarcode,
                    onPickLogoPhoto = viewModel::persistAndApplyLogoUri,
                    onPickBackgroundPhoto = viewModel::persistAndApplyBackgroundPhotoUri,
                    onToggleFavorite = viewModel::toggleDraftFavorite,
                    onApplyPreset = viewModel::applyPreset,
                    onSaveCustomPreset = viewModel::saveCurrentAsPreset,
                    onDeleteCustomPreset = viewModel::deleteCustomPreset,
                    onRandomizeStyle = viewModel::randomizeStyle,
                    onAutoFixScannability = viewModel::autoFixScannability,
                    onSaveToLibrary = { viewModel.saveDraftToLibrary() },
                    onOpenExport = { showExportSheet = true },
                    onOpenFullscreen = {
                        fullscreenItem = FullscreenPreviewState(
                            title = draft.effectiveTitle,
                            encodedPayload = draft.encodedPayload,
                            design = draft.design,
                            codeKind = draft.codeKind,
                            barcode = draft.barcode
                        )
                    }
                )

                is QraftDest.Scanner -> ScannerScreen(
                    onBack = { popDest() },
                    onOpenInStudio = { rawText, barcodeFormat ->
                        viewModel.openScannedInStudio(rawText, barcodeFormat)
                        pushDest(QraftDest.Studio(), popPreviousTop = true)
                    },
                    onSaveToLibrary = viewModel::saveScannedToLibrary,
                    onCopyResult = viewModel::copyRawText
                )

                is QraftDest.Settings -> SettingsScreen(
                    preferences = prefs,
                    savedCount = savedItems.count { !it.isArchived },
                    archivedCount = savedItems.count { it.isArchived },
                    customPresetCount = customPresets.size,
                    onBack = { popDest() },
                    onThemeChange = viewModel::setThemeMode,
                    onCreatorNameChange = viewModel::setCreatorName,
                    onAutoVerifyChange = viewModel::setAutoVerify,
                    onDefaultFormatChange = viewModel::setDefaultExportFormat,
                    onDefaultResolutionChange = viewModel::setDefaultExportResolution,
                    onExportBackup = {
                        runCatching { backupExportLauncher.launch("qraft_backup_v7.qraft") }
                    },
                    onImportBackup = {
                        runCatching { backupImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    },
                    onReplayOnboarding = { replayOnboarding = true }
                )
            }
        }
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    CompositionLocalProvider(LocalHeroBounds provides heroRegistry) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
        ) {
            val peelCurved = backPeelProgress.value.coerceIn(0f, 1f)
            val effectiveReveal = if (backGestureActive) {
                (1f - peelCurved).coerceIn(0f, 1f)
            } else {
                transitionReveal.value.coerceIn(0f, 1f)
            }
            val hasActiveOverlay = backStack.size > 1

            // Keep every back-stack entry in one persistent key(dest.stateKey) slot so pushing/popping
            // never unmounts or remounts the underlying screen (prevents flicker & entrance replay).
            backStack.forEachIndexed { index, dest ->
                val isTop = index == backStack.lastIndex
                val isUnder = index == backStack.lastIndex - 1
                val isHiddenDeep = index < backStack.lastIndex - 1

                key(dest.stateKey) {
                    val frozenHeroSource = remember(dest.stateKey, dest.heroKey) {
                        heroRegistry.get(dest.heroKey)
                    }
                    val cornerRadiusDp = when {
                        !isTop || !hasActiveOverlay -> 0.dp
                        backGestureActive -> lerp(0f, 30f, peelCurved).dp
                        frozenHeroSource != null -> lerp(frozenHeroSource.cornerRadiusDp, 0f, effectiveReveal).dp
                        else -> lerp(26f, 0f, effectiveReveal).dp
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(index.toFloat())
                            .pointerInput(dest.stateKey, isTop) {
                                if (!isTop) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                                            ev.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            }
                            .graphicsLayer {
                                val w = containerSize.width.toFloat().coerceAtLeast(1f)
                                val h = containerSize.height.toFloat().coerceAtLeast(1f)
                                if (cornerRadiusDp > 0.5.dp) {
                                    shape = RoundedCornerShape(cornerRadiusDp)
                                    clip = true
                                } else {
                                    clip = false
                                }
                                when {
                                    isHiddenDeep -> {
                                        alpha = 0f
                                    }
                                    isUnder -> {
                                        if (!reduceMotion) {
                                            val s = lerp(1.0f, 0.945f, effectiveReveal)
                                            scaleX = s
                                            scaleY = s
                                            alpha = lerp(1.0f, 0.90f, effectiveReveal)
                                            transformOrigin = TransformOrigin.Center
                                        }
                                    }
                                    isTop && hasActiveOverlay -> {
                                        if (backGestureActive) {
                                            val c = peelCurved
                                            val s = lerp(1.0f, 0.84f, c)
                                            scaleX = s
                                            scaleY = s
                                            val dir = if (backSwipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                                            translationX = dir * w * 0.28f * c
                                            val yNorm = (backTouchY / h).coerceIn(0f, 1f) - 0.5f
                                            translationY = yNorm * 32.dp.toPx() * c
                                            rotationZ = dir * yNorm * 2.4f * c
                                            alpha = (1f - ((c - 0.55f) / 0.45f).coerceIn(0f, 1f))
                                            transformOrigin = TransformOrigin.Center
                                        } else if (!reduceMotion && frozenHeroSource != null) {
                                            val rect: Rect = frozenHeroSource.rect
                                            val startScale = maxOf(rect.width / w, rect.height / h).coerceIn(0.62f, 0.95f)
                                            val s = lerp(startScale, 1f, effectiveReveal)
                                            scaleX = s
                                            scaleY = s
                                            val heroCx = rect.center.x - w / 2f
                                            val heroCy = rect.center.y - h / 2f
                                            translationX = lerp(heroCx, 0f, effectiveReveal)
                                            translationY = lerp(heroCy, 0f, effectiveReveal)
                                            alpha = (effectiveReveal / 0.25f).coerceIn(0f, 1f)
                                            transformOrigin = TransformOrigin.Center
                                        } else if (!reduceMotion) {
                                            translationX = (1f - effectiveReveal) * (w * 0.26f)
                                            val s = lerp(0.965f, 1f, effectiveReveal)
                                            scaleX = s
                                            scaleY = s
                                            alpha = (effectiveReveal / 0.30f).coerceIn(0f, 1f)
                                            transformOrigin = TransformOrigin.Center
                                        }
                                    }
                                }
                            }
                    ) {
                        RenderRoute(dest)
                    }
                }
            }

            // Optical depth scrim cleanly separating under-layer and top-layer via zIndex
            if (hasActiveOverlay && !reduceMotion) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(backStack.lastIndex - 0.5f)
                        .graphicsLayer { alpha = effectiveReveal * 0.24f }
                        .background(Color.Black)
                )
            }

            if (!preferencesLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(90f)
                        .background(QraftThemeTokens.colors.background)
                )
            }

            AnimatedVisibility(
                visible = showOnboarding,
                enter = when {
                    !replayOnboarding -> EnterTransition.None
                    reduceMotion -> fadeIn(tween(120))
                    else -> fadeIn(tween(240))
                },
                exit = if (reduceMotion) fadeOut(tween(120)) else (fadeOut(tween(260)) + scaleOut(targetScale = 1.04f, animationSpec = tween(260))),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(95f)
            ) {
                OnboardingScreen(
                    onFinish = {
                        replayOnboarding = false
                        viewModel.setOnboardingCompleted(true)
                    }
                )
            }

            // Dynamic-Island Apple Glass Status Toast
            AnimatedVisibility(
                visible = bannerMessage != null,
                enter = AppMotion.toastEnter,
                exit = AppMotion.toastExit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(100f)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 20.dp, end = 20.dp)
            ) {
                val colors = QraftThemeTokens.colors
                AppleGlassSurface(
                    shape = RoundedCornerShape(50),
                    cornerRadiusDp = 50.dp,
                    variant = AppleGlassVariant.Regular,
                    enableRefraction = true,
                    elevation = 10.dp
                ) {
                    AppleAnimatedSwap(targetState = bannerMessage.orEmpty()) { messageText ->
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = colors.espressoAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = messageText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreatePicker) {
        CreateQrTypeSheet(
            onDismiss = { showCreatePicker = false },
            onSelectType = { type ->
                viewModel.startNewQr(type)
                pushDest(QraftDest.Studio("hero_create"))
            },
            onSelectBarcode = { format ->
                viewModel.startNewBarcode(format)
                pushDest(QraftDest.Studio("hero_create"))
            },
            onImportQrImage = launchQrImport,
            onOpenScanner = { pushDest(QraftDest.Scanner("hero_scanner")) }
        )
    }

    if (showExportSheet) {
        ExportQrSheet(
            title = draft.effectiveTitle,
            encodedPayload = draft.encodedPayload,
            design = draft.design,
            initialFormat = prefs.defaultExportFormat,
            isExporting = isExporting,
            onDesignChange = viewModel::updateDraftDesign,
            onDismiss = { showExportSheet = false },
            onSave = viewModel::saveExport,
            onShare = viewModel::shareExport,
            onCopyImage = viewModel::copyExportImage,
            onCopyPayload = viewModel::copyPayload,
            onExportPrintSheet = { layout, asPdf ->
                viewModel.exportSinglePrintSheet(
                    title = draft.effectiveTitle,
                    data = draft.encodedPayload,
                    design = draft.design,
                    layout = layout,
                    asPdf = asPdf
                )
            },
            codeKind = draft.codeKind,
            barcodeSpec = draft.barcode,
            onBarcodeChange = viewModel::updateDraftBarcode
        )
    }

    if (showBatchSheet) {
        BatchGeneratorSheet(
            currentDesign = draft.design,
            isRunning = isExporting,
            batchProgress = batchProgress,
            onDismiss = { showBatchSheet = false },
            onRunBatch = { lines, saveToLib, exportPngs, exportPdfLayout ->
                viewModel.runBatchGenerate(lines, saveToLib, exportPngs, exportPdfLayout) {
                    showBatchSheet = false
                }
            }
        )
    }

    fullscreenItem?.let { preview ->
        FullscreenScanDialog(
            title = preview.title,
            encodedPayload = preview.encodedPayload,
            design = preview.design,
            onDismiss = { fullscreenItem = null },
            codeKind = preview.codeKind,
            barcodeSpec = preview.barcode
        )
    }
}
