package com.hkm.qraft

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.qraft.data.PresetCatalog
import com.hkm.qraft.data.QraftRepository
import com.hkm.qraft.data.UserPreferences
import com.hkm.qraft.model.*
import com.hkm.qraft.qr.BarcodeEngine
import com.hkm.qraft.qr.LogoCatalog
import com.hkm.qraft.qr.QrEngine
import com.hkm.qraft.qr.QrExporter
import com.hkm.qraft.qr.QrPayloadEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

data class StudioDraftState(
    val editingId: String? = null,
    val title: String = "",
    val type: QrContentType = QrContentType.URL,
    val payload: QrPayloadFields = QrPayloadFields(),
    val design: QrDesignSpec = PresetCatalog.builtInPresets.first().design,
    val isFavorite: Boolean = false,
    val isDirty: Boolean = false,
    val codeKind: CodeKind = CodeKind.QR,
    val barcode: BarcodeDesignSpec = BarcodeDesignSpec()
) {
    val encodedPayload: String
        get() = if (codeKind == CodeKind.BARCODE) {
            barcode.data.ifBlank { barcode.format.defaultSample }
        } else {
            QrPayloadEncoder.encode(type, payload)
        }

    val effectiveTitle: String
        get() = title.trim().ifBlank {
            if (codeKind == CodeKind.BARCODE) {
                "${barcode.format.label} • ${barcode.data.take(16).ifBlank { "Barcode" }}"
            } else {
                QrPayloadEncoder.defaultTitle(type, payload)
            }
        }
}

class QraftViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QraftRepository(application.applicationContext)

    val savedItems: StateFlow<List<QrCreationItem>> = repository.itemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PresetCatalog.starterItems())

    val customPresets: StateFlow<List<SavedStylePreset>> = repository.customPresetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _preferencesLoaded = MutableStateFlow(false)
    val preferencesLoaded: StateFlow<Boolean> = _preferencesLoaded.asStateFlow()

    val preferences: StateFlow<UserPreferences> = repository.preferencesFlow
        .onEach { _preferencesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _selectedHomeCategory = MutableStateFlow<QrContentType?>(null)
    val selectedHomeCategory: StateFlow<QrContentType?> = _selectedHomeCategory.asStateFlow()

    private val _draft = MutableStateFlow(StudioDraftState())
    val draft: StateFlow<StudioDraftState> = _draft.asStateFlow()

    private val _scannability = MutableStateFlow(ScannabilityReport(isScannable = true))
    val scannability: StateFlow<ScannabilityReport> = _scannability.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _statusBanner = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val statusBanner: SharedFlow<String> = _statusBanner.asSharedFlow()

    private var verifyJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
        viewModelScope.launch {
            combine(_draft, preferences) { draftState, prefs ->
                draftState to prefs.autoVerifyScannability
            }.collectLatest { (draftState, autoVerify) ->
                if (!autoVerify) {
                    val fg = if (draftState.codeKind == CodeKind.BARCODE) {
                        draftState.barcode.foregroundColor
                    } else {
                        draftState.design.darkBrush.primaryColor
                    }
                    val bg = if (draftState.codeKind == CodeKind.BARCODE) {
                        draftState.barcode.backgroundColor
                    } else {
                        draftState.design.background.brush.primaryColor
                    }
                    _scannability.value = ScannabilityReport(
                        isScannable = true,
                        contrastRatio = QrEngine.contrastRatio(fg, bg)
                    )
                    return@collectLatest
                }
                verifyJob?.cancel()
                verifyJob = viewModelScope.launch {
                    delay(200)
                    _isVerifying.value = true
                    try {
                        val report = withContext(Dispatchers.Default) {
                            runCatching {
                                if (draftState.codeKind == CodeKind.BARCODE) {
                                    BarcodeEngine.verifyBarcodeScannable(draftState.barcode)
                                } else {
                                    QrEngine.verifyScannability(getApplication(), draftState.encodedPayload, draftState.design)
                                }
                            }.getOrElse { err ->
                                ScannabilityReport(
                                    isScannable = false,
                                    warningMessage = err.localizedMessage ?: "Verification error"
                                )
                            }
                        }
                        _scannability.value = report
                    } finally {
                        _isVerifying.value = false
                    }
                }
            }
        }
    }

    fun selectHomeCategory(type: QrContentType?) {
        _selectedHomeCategory.value = type
    }

    private val undoStack = ArrayDeque<StudioDraftState>()
    private val redoStack = ArrayDeque<StudioDraftState>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    private val _batchProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val batchProgress: StateFlow<Pair<Int, Int>?> = _batchProgress.asStateFlow()
    private var lastHistoryKind: String? = null
    private var lastHistoryTimeMs: Long = 0L

    private fun syncUndoFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        lastHistoryKind = null
        lastHistoryTimeMs = 0L
        syncUndoFlags()
    }

    private fun updateDraftWithHistory(
        commitKind: String,
        coalesceWindowMs: Long = 0L,
        transform: (StudioDraftState) -> StudioDraftState
    ) {
        val prev = _draft.value
        val next = transform(prev)
        if (next == prev) return
        val now = System.currentTimeMillis()
        val shouldPush = undoStack.isEmpty() ||
            coalesceWindowMs <= 0L ||
            commitKind != lastHistoryKind ||
            (now - lastHistoryTimeMs) > coalesceWindowMs
        if (shouldPush) {
            if (undoStack.size >= 30) {
                undoStack.removeFirst()
            }
            undoStack.addLast(prev)
            lastHistoryKind = commitKind
            lastHistoryTimeMs = now
        }
        redoStack.clear()
        _draft.value = next
        syncUndoFlags()
    }

    private fun isContinuousDesignTweak(a: QrDesignSpec, b: QrDesignSpec): Boolean {
        val discreteUnchanged = a.pixelStyle == b.pixelStyle &&
            a.eyeFrameStyle == b.eyeFrameStyle &&
            a.eyeBallStyle == b.eyeBallStyle &&
            a.lightPixelStyle == b.lightPixelStyle &&
            a.lightPixelColor == b.lightPixelColor &&
            a.codeSilhouette == b.codeSilhouette &&
            a.fourthEyeEnabled == b.fourthEyeEnabled &&
            a.darkBrush == b.darkBrush &&
            a.customEyeFrameColors == b.customEyeFrameColors &&
            a.eyeFrameBrush == b.eyeFrameBrush &&
            a.customEyeBallColors == b.customEyeBallColors &&
            a.eyeBallBrush == b.eyeBallBrush &&
            a.background.mode == b.background.mode &&
            a.background.brush == b.background.brush &&
            a.background.photoUri == b.background.photoUri &&
            a.background.photoOverlayColor == b.background.photoOverlayColor &&
            a.logo.sourceType == b.logo.sourceType &&
            a.logo.iconKey == b.logo.iconKey &&
            a.logo.emoji == b.logo.emoji &&
            a.logo.monogramText == b.logo.monogramText &&
            a.logo.galleryUri == b.logo.galleryUri &&
            a.logo.clipShape == b.logo.clipShape &&
            a.logo.paddingStyle == b.logo.paddingStyle &&
            a.logo.backingEnabled == b.logo.backingEnabled &&
            a.logo.backingColor == b.logo.backingColor &&
            a.logo.borderEnabled == b.logo.borderEnabled &&
            a.outerFrame.style == b.outerFrame.style &&
            a.outerFrame.label == b.outerFrame.label &&
            a.outerFrame.subtitle == b.outerFrame.subtitle &&
            a.outerFrame.frameFillColor == b.outerFrame.frameFillColor &&
            a.outerFrame.accentColor == b.outerFrame.accentColor &&
            a.outerFrame.accentSecondaryColor == b.outerFrame.accentSecondaryColor &&
            a.outerFrame.textColor == b.outerFrame.textColor &&
            a.ecc == b.ecc &&
            a.exportSizePx == b.exportSizePx &&
            a.matrixShadowEnabled == b.matrixShadowEnabled
        return discreteUnchanged
    }

    fun undoDesignEdit() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeLast()
        if (redoStack.size >= 30) redoStack.removeFirst()
        redoStack.addLast(_draft.value)
        lastHistoryKind = null
        _draft.value = previous
        syncUndoFlags()
    }

    fun redoDesignEdit() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        if (undoStack.size >= 30) undoStack.removeFirst()
        undoStack.addLast(_draft.value)
        lastHistoryKind = null
        _draft.value = next
        syncUndoFlags()
    }

    fun startNewQr(type: QrContentType = QrContentType.URL) {
        val defaultPayload = when (type) {
            QrContentType.URL -> QrPayloadFields(url = "https://")
            QrContentType.WIFI -> QrPayloadFields(wifiSsid = "", wifiSecurity = WifiSecurity.WPA)
            else -> QrPayloadFields()
        }
        val res = preferences.value.defaultExportResolution.coerceIn(512, 8192)
        val baseDesign = PresetCatalog.builtInPresets.first().design.copy(exportSizePx = res)
        clearHistory()
        _draft.value = StudioDraftState(
            editingId = null,
            title = "",
            type = type,
            payload = defaultPayload,
            design = baseDesign,
            isFavorite = false,
            isDirty = false,
            codeKind = CodeKind.QR,
            barcode = BarcodeDesignSpec(exportWidthPx = res)
        )
    }

    fun startNewBarcode(format: BarcodeFormatType = BarcodeFormatType.CODE_128) {
        val res = preferences.value.defaultExportResolution.coerceIn(512, 8192)
        clearHistory()
        _draft.value = StudioDraftState(
            editingId = null,
            title = "",
            type = QrContentType.TEXT,
            payload = QrPayloadFields(text = format.defaultSample),
            design = PresetCatalog.builtInPresets.first().design.copy(exportSizePx = res),
            isFavorite = false,
            isDirty = false,
            codeKind = CodeKind.BARCODE,
            barcode = BarcodeDesignSpec(
                format = format,
                data = format.defaultSample,
                exportWidthPx = res
            )
        )
    }

    fun openExistingQr(item: QrCreationItem) {
        clearHistory()
        _draft.value = StudioDraftState(
            editingId = item.id,
            title = item.title,
            type = item.type,
            payload = item.payload,
            design = item.design,
            isFavorite = item.isFavorite,
            isDirty = false,
            codeKind = item.resolvedCodeKind(),
            barcode = item.resolvedBarcode()
        )
    }

    fun setDraftCodeKind(kind: CodeKind) {
        updateDraftWithHistory("code_kind", 0L) { current ->
            if (current.codeKind == kind) current
            else current.copy(codeKind = kind, isDirty = true)
        }
    }

    fun updateDraftBarcode(spec: BarcodeDesignSpec) {
        updateDraftWithHistory("barcode_edit", 500L) { current ->
            current.copy(
                barcode = spec,
                design = current.design.copy(exportSizePx = spec.exportWidthPx),
                isDirty = true
            )
        }
    }

    fun openScannedInStudio(rawText: String, detectedBarcodeFormat: BarcodeFormatType? = null) {
        val clean = rawText.trim()
        if (clean.isEmpty()) return
        val res = preferences.value.defaultExportResolution.coerceIn(512, 8192)
        if (detectedBarcodeFormat != null) {
            clearHistory()
            _draft.value = StudioDraftState(
                editingId = null,
                title = "Scanned ${detectedBarcodeFormat.label}",
                type = QrContentType.TEXT,
                payload = QrPayloadFields(text = clean),
                design = PresetCatalog.builtInPresets.first().design.copy(exportSizePx = res),
                isDirty = true,
                codeKind = CodeKind.BARCODE,
                barcode = BarcodeDesignSpec(
                    format = detectedBarcodeFormat,
                    data = clean,
                    exportWidthPx = res
                )
            )
            _statusBanner.tryEmit("Loaded scanned ${detectedBarcodeFormat.label} into Studio")
        } else {
            importSharedPayload(clean)
        }
    }

    fun saveScannedToLibrary(rawText: String, detectedBarcodeFormat: BarcodeFormatType? = null) {
        val clean = rawText.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val res = preferences.value.defaultExportResolution.coerceIn(512, 8192)
            val item = if (detectedBarcodeFormat != null) {
                QrCreationItem(
                    title = "Scanned ${detectedBarcodeFormat.label}",
                    type = QrContentType.TEXT,
                    payload = QrPayloadFields(text = clean),
                    design = PresetCatalog.builtInPresets.first().design.copy(exportSizePx = res),
                    isVerifiedScannable = true,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    codeKind = CodeKind.BARCODE,
                    barcode = BarcodeDesignSpec(
                        format = detectedBarcodeFormat,
                        data = clean,
                        exportWidthPx = res
                    )
                )
            } else {
                val parsed = com.hkm.qraft.qr.QrCodeImporter.parsePayloadToImport(clean)
                QrCreationItem(
                    title = parsed.suggestedTitle,
                    type = parsed.type,
                    payload = parsed.fields,
                    design = PresetCatalog.builtInPresets.first().design.copy(exportSizePx = res),
                    isVerifiedScannable = true,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    codeKind = CodeKind.QR
                )
            }
            repository.saveItem(item)
            _statusBanner.tryEmit("Saved scanned code \"${item.title}\"")
        }
    }

    fun importQrFromUri(uri: Uri, onLoaded: () -> Unit) {
        viewModelScope.launch {
            val decoded = withContext(Dispatchers.Default) {
                com.hkm.qraft.qr.QrCodeImporter.decodeFromUri(getApplication(), uri)
            }
            if (decoded == null) {
                _statusBanner.tryEmit("No readable QR code detected in that image")
                return@launch
            }
            val baseDesign = PresetCatalog.builtInPresets.first().design.copy(
                exportSizePx = preferences.value.defaultExportResolution.coerceIn(512, 8192)
            )
            clearHistory()
            _draft.value = StudioDraftState(
                editingId = null,
                title = decoded.suggestedTitle,
                type = decoded.type,
                payload = decoded.fields,
                design = baseDesign,
                isFavorite = false,
                isDirty = true,
                codeKind = CodeKind.QR
            )
            _statusBanner.tryEmit("Imported ${decoded.type.title} QR • Ready to remix")
            onLoaded()
        }
    }

    fun importSharedPayload(rawText: String, onLoaded: () -> Unit = {}) {
        val clean = rawText.trim()
        if (clean.isEmpty()) return
        val decoded = com.hkm.qraft.qr.QrCodeImporter.parsePayloadToImport(clean)
        val baseDesign = PresetCatalog.builtInPresets.first().design.copy(
            exportSizePx = preferences.value.defaultExportResolution.coerceIn(512, 8192)
        )
        clearHistory()
        _draft.value = StudioDraftState(
            editingId = null,
            title = decoded.suggestedTitle,
            type = decoded.type,
            payload = decoded.fields,
            design = baseDesign,
            isFavorite = false,
            isDirty = true,
            codeKind = CodeKind.QR
        )
        _statusBanner.tryEmit("Loaded ${decoded.type.title} content into Studio")
        onLoaded()
    }

    fun extractAndApplyBrandPalette(fromLogo: Boolean) {
        val currentDesign = _draft.value.design
        val uriString = if (fromLogo) currentDesign.logo.galleryUri else currentDesign.background.photoUri
        if (uriString.isNullOrBlank()) {
            _statusBanner.tryEmit(if (fromLogo) "Pick a logo image first" else "Pick a background photo first")
            return
        }
        viewModelScope.launch {
            val extracted = withContext(Dispatchers.IO) {
                com.hkm.qraft.qr.PaletteExtractor.extractFromUri(getApplication(), Uri.parse(uriString))
            }
            if (extracted == null) {
                _statusBanner.tryEmit("Could not read image colors")
                return@launch
            }
            updateDraftWithHistory("palette_extract", 0L) { state ->
                val d = state.design
                val updatedBg = when {
                    !fromLogo && d.background.mode == BackgroundMode.PHOTO -> {
                        d.background.copy(
                            photoOverlayColor = extracted.lightCanvasArgb,
                            photoOverlayAlpha = maxOf(d.background.photoOverlayAlpha, 0.72f)
                        )
                    }
                    d.background.mode == BackgroundMode.TRANSPARENT -> d.background
                    else -> d.background.copy(
                        mode = BackgroundMode.SOLID,
                        brush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = extracted.lightCanvasArgb)
                    )
                }
                state.copy(
                    design = d.copy(
                        darkBrush = BrushStyleConfig(
                            mode = BrushMode.LINEAR_GRADIENT,
                            primaryColor = extracted.primaryDarkArgb,
                            secondaryColor = extracted.secondaryAccentArgb,
                            direction = GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT
                        ),
                        customEyeFrameColors = false,
                        customEyeBallColors = true,
                        eyeBallBrush = BrushStyleConfig(
                            mode = BrushMode.SOLID,
                            primaryColor = extracted.frameAccentArgb
                        ),
                        background = updatedBg,
                        outerFrame = d.outerFrame.copy(accentColor = extracted.frameAccentArgb)
                    ),
                    isDirty = true
                )
            }
            _statusBanner.tryEmit(
                "Extracted brand colors (${String.format(java.util.Locale.US, "%.1f", extracted.contrastOnCanvas)}:1 contrast)"
            )
        }
    }

    fun duplicateQr(item: QrCreationItem) {
        viewModelScope.launch {
            val copy = item.copy(
                id = UUID.randomUUID().toString(),
                title = "${item.title} (Copy)",
                createdAtMillis = System.currentTimeMillis(),
                updatedAtMillis = System.currentTimeMillis()
            )
            repository.saveItem(copy)
            _statusBanner.tryEmit("Duplicated \"${item.title}\"")
        }
    }

    fun updateDraftTitle(title: String) {
        updateDraftWithHistory("title", 650L) { it.copy(title = title, isDirty = true) }
    }

    fun updateDraftType(type: QrContentType) {
        updateDraftWithHistory("type", 0L) { it.copy(type = type, isDirty = true) }
    }

    fun updateDraftPayload(payload: QrPayloadFields) {
        updateDraftWithHistory("payload", 650L) { it.copy(payload = payload, isDirty = true) }
    }

    fun updateDraftDesign(design: QrDesignSpec) {
        val current = _draft.value.design
        val continuous = isContinuousDesignTweak(current, design)
        updateDraftWithHistory(
            commitKind = if (continuous) "design_slider" else "design_discrete",
            coalesceWindowMs = if (continuous) 450L else 0L
        ) {
            it.copy(design = design, isDirty = true)
        }
    }

    fun toggleDraftFavorite() {
        updateDraftWithHistory("favorite", 0L) { it.copy(isFavorite = !it.isFavorite, isDirty = true) }
    }

    fun applyPreset(preset: SavedStylePreset) {
        updateDraftWithHistory("preset", 0L) {
            it.copy(design = preset.design.copy(exportSizePx = it.design.exportSizePx), isDirty = true)
        }
        _statusBanner.tryEmit("Applied \"${preset.name}\"")
    }

    fun randomizeStyle() {
        val palettes = listOf(
            Triple(0xFF5C2E0B, 0xFFF07C2E, 0xFFFFFFFF),
            Triple(0xFF1F4E26, 0xFF72A329, 0xFFFAFCF7),
            Triple(0xFF6B217D, 0xFFD9539F, 0xFFFEF9FD),
            Triple(0xFF154E9C, 0xFF1AA3C9, 0xFFF7FBFF),
            Triple(0xFFC22D51, 0xFFF3873B, 0xFFFFFFFF),
            Triple(0xFF201E1C, 0xFF6C655E, 0xFFFAF7F4),
            Triple(0xFFF6C677, 0xFFE9853B, 0xFF191614)
        )
        val (c1, c2, bg) = palettes[Random.nextInt(palettes.size)]
        val pixels = QrPixelStyle.entries
        val frames = QrEyeFrameStyle.entries
        val balls = QrEyeBallStyle.entries
        val mode = if (Random.nextBoolean()) BrushMode.LINEAR_GRADIENT else BrushMode.SOLID

        updateDraftWithHistory("randomize", 0L) { state ->
            val current = state.design
            state.copy(
                design = current.copy(
                    pixelStyle = pixels[Random.nextInt(pixels.size)],
                    eyeFrameStyle = frames[Random.nextInt(frames.size)],
                    eyeBallStyle = balls[Random.nextInt(balls.size)],
                    darkBrush = current.darkBrush.copy(
                        mode = mode,
                        primaryColor = c1,
                        secondaryColor = c2
                    ),
                    customEyeFrameColors = Random.nextBoolean(),
                    eyeFrameBrush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = c1),
                    customEyeBallColors = true,
                    eyeBallBrush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = c2),
                    background = current.background.copy(
                        mode = BackgroundMode.SOLID,
                        brush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = bg)
                    )
                ),
                isDirty = true
            )
        }
    }

    fun autoFixScannability() {
        updateDraftWithHistory("autofix", 0L) { state ->
            if (state.codeKind == CodeKind.BARCODE) {
                val fixed = state.barcode.copy(
                    foregroundColor = 0xFF111113,
                    backgroundColor = 0xFFFFFFFF,
                    transparentBg = false,
                    roundedBars = false,
                    quietMarginFraction = state.barcode.quietMarginFraction.coerceAtLeast(0.08f)
                )
                return@updateDraftWithHistory state.copy(barcode = fixed, isDirty = true)
            }
            val current = state.design
            val fixedLogo = if (current.logo.sourceType != LogoSourceType.NONE && current.logo.sizeFraction > 0.23f) {
                current.logo.copy(sizeFraction = 0.21f, paddingStyle = LogoPaddingStyle.NATURAL, backingEnabled = true)
            } else {
                current.logo
            }
            state.copy(
                design = current.copy(
                    darkBrush = current.darkBrush.copy(primaryColor = 0xFF111113),
                    background = current.background.copy(
                        mode = BackgroundMode.SOLID,
                        brush = BrushStyleConfig(mode = BrushMode.SOLID, primaryColor = 0xFFFFFFFF)
                    ),
                    logo = fixedLogo,
                    ecc = QrEccSetting.HIGH,
                    quietZonePadding = current.quietZonePadding.coerceAtLeast(0.10f)
                ),
                isDirty = true
            )
        }
        _statusBanner.tryEmit("Boosted contrast & scan reliability")
    }

    fun swapDarkGradientColors() {
        updateDraftWithHistory("swap_grad", 0L) { state ->
            val b = state.design.darkBrush
            state.copy(
                design = state.design.copy(
                    darkBrush = b.copy(primaryColor = b.secondaryColor, secondaryColor = b.primaryColor)
                ),
                isDirty = true
            )
        }
    }

    fun invertInkAndBackground() {
        updateDraftWithHistory("invert_ink_bg", 0L) { state ->
            if (state.codeKind == CodeKind.BARCODE) {
                val bc = state.barcode
                return@updateDraftWithHistory state.copy(
                    barcode = bc.copy(
                        foregroundColor = bc.backgroundColor,
                        backgroundColor = bc.foregroundColor,
                        transparentBg = false
                    ),
                    isDirty = true
                )
            }
            val d = state.design
            val fg = d.darkBrush.primaryColor
            val bg = d.background.brush.primaryColor
            state.copy(
                design = d.copy(
                    darkBrush = d.darkBrush.copy(primaryColor = bg, mode = BrushMode.SOLID),
                    background = d.background.copy(
                        mode = BackgroundMode.SOLID,
                        brush = d.background.brush.copy(primaryColor = fg, mode = BrushMode.SOLID)
                    )
                ),
                isDirty = true
            )
        }
    }

    fun saveDraftToLibrary(onSaved: ((QrCreationItem) -> Unit)? = null) {
        val current = _draft.value
        if (current.codeKind == CodeKind.BARCODE) {
            val check = BarcodeEngine.normalizeAndValidate(current.barcode.format, current.barcode.data)
            if (!check.isValid) {
                _statusBanner.tryEmit(check.warningOrError ?: "Invalid barcode input")
                return
            }
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val item = QrCreationItem(
                    id = current.editingId ?: UUID.randomUUID().toString(),
                    title = current.effectiveTitle,
                    type = QrContentType.TEXT,
                    payload = QrPayloadFields(text = check.normalizedData),
                    design = current.design,
                    isFavorite = current.isFavorite,
                    isVerifiedScannable = _scannability.value.isScannable,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    codeKind = CodeKind.BARCODE,
                    barcode = current.barcode.copy(data = check.normalizedData)
                )
                repository.saveItem(item)
                _draft.update {
                    it.copy(
                        editingId = item.id,
                        isDirty = it.title != current.title || it.design != current.design
                    )
                }
                _statusBanner.tryEmit("Saved \"${item.title}\"")
                onSaved?.invoke(item)
            }
            return
        }
        val validationError = QrPayloadEncoder.validate(current.type, current.payload)
        if (validationError != null) {
            _statusBanner.tryEmit(validationError)
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val item = QrCreationItem(
                id = current.editingId ?: UUID.randomUUID().toString(),
                title = current.effectiveTitle,
                type = current.type,
                payload = current.payload,
                design = current.design,
                isFavorite = current.isFavorite,
                isVerifiedScannable = _scannability.value.isScannable,
                createdAtMillis = now,
                updatedAtMillis = now,
                codeKind = CodeKind.QR
            )
            repository.saveItem(item)
            _draft.update {
                it.copy(
                    editingId = item.id,
                    isDirty = it.title != current.title || it.design != current.design
                )
            }
            _statusBanner.tryEmit("Saved \"${item.title}\"")
            onSaved?.invoke(item)
        }
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val d = _draft.value.design
            val preset = SavedStylePreset(
                id = UUID.randomUUID().toString(),
                name = name,
                subtitle = "${d.pixelStyle.label} • ${d.eyeFrameStyle.label} eyes",
                design = d,
                isBuiltIn = false
            )
            repository.saveCustomPreset(preset)
            _statusBanner.tryEmit("Saved custom preset \"$name\"")
        }
    }

    fun deleteCustomPreset(id: String) {
        viewModelScope.launch {
            repository.deleteCustomPreset(id)
        }
    }

    fun toggleItemFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteItem(id)
            _statusBanner.tryEmit("Removed QR card")
        }
    }

    fun saveExport(format: ExportImageFormat) {
        val current = _draft.value
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val result = if (current.codeKind == CodeKind.BARCODE) {
                    QrExporter.saveBarcodeToDevice(
                        context = getApplication(),
                        title = current.effectiveTitle,
                        spec = current.barcode,
                        format = format
                    )
                } else {
                    QrExporter.saveToDevice(
                        context = getApplication(),
                        title = current.effectiveTitle,
                        data = current.encodedPayload,
                        design = current.design,
                        format = format
                    )
                }
                _statusBanner.tryEmit(result.message)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun shareExport(format: ExportImageFormat) {
        val current = _draft.value
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val result = if (current.codeKind == CodeKind.BARCODE) {
                    QrExporter.shareBarcode(
                        context = getApplication(),
                        title = current.effectiveTitle,
                        spec = current.barcode,
                        format = format
                    )
                } else {
                    QrExporter.shareQr(
                        context = getApplication(),
                        title = current.effectiveTitle,
                        data = current.encodedPayload,
                        design = current.design,
                        format = format
                    )
                }
                if (!result.success) _statusBanner.tryEmit(result.message)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun copyPayload() {
        val current = _draft.value
        val payload = if (current.codeKind == CodeKind.BARCODE) current.barcode.data else current.encodedPayload
        QrExporter.copyPayloadToClipboard(
            context = getApplication(),
            label = current.effectiveTitle,
            payload = payload
        )
        _statusBanner.tryEmit("Copied payload to clipboard")
    }

    fun copyRawText(text: String) {
        QrExporter.copyPayloadToClipboard(
            context = getApplication(),
            label = "Scanned Code",
            payload = text
        )
        _statusBanner.tryEmit("Copied to clipboard")
    }

    fun copyExportImage() {
        val current = _draft.value
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val result = if (current.codeKind == CodeKind.BARCODE) {
                    QrExporter.copyBarcodeToClipboard(
                        context = getApplication(),
                        title = current.effectiveTitle,
                        spec = current.barcode
                    )
                } else {
                    QrExporter.copyImageToClipboard(
                        context = getApplication(),
                        title = current.effectiveTitle,
                        data = current.encodedPayload,
                        design = current.design
                    )
                }
                _statusBanner.tryEmit(result.message)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun persistAndApplyLogoUri(sourceUri: Uri) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                LogoCatalog.persistPickedMedia(getApplication(), sourceUri, "logo")
            } ?: sourceUri.toString()
            val currentDesign = _draft.value.design
            updateDraftDesign(
                currentDesign.copy(
                    logo = currentDesign.logo.copy(
                        sourceType = LogoSourceType.GALLERY,
                        galleryUri = saved
                    )
                )
            )
            _statusBanner.tryEmit("Logo image imported cleanly")
        }
    }

    fun persistAndApplyBackgroundPhotoUri(sourceUri: Uri) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                LogoCatalog.persistPickedMedia(getApplication(), sourceUri, "bg")
            } ?: sourceUri.toString()
            val currentDesign = _draft.value.design
            updateDraftDesign(
                currentDesign.copy(
                    background = currentDesign.background.copy(
                        mode = BackgroundMode.PHOTO,
                        photoUri = saved
                    )
                )
            )
            _statusBanner.tryEmit("Background photo imported cleanly")
        }
    }

    fun runBatchGenerate(
        lines: List<String>,
        saveToLibrary: Boolean,
        exportPngs: Boolean,
        exportPdfSheetLayout: com.hkm.qraft.qr.PrintSheetLayout? = null,
        onComplete: () -> Unit
    ) {
        val design = _draft.value.design
        viewModelScope.launch {
            _isExporting.value = true
            _batchProgress.value = 0 to lines.size
            try {
                var exportedCount = 0
                val batchItems = mutableListOf<QrCreationItem>()
                val sheetItems = mutableListOf<com.hkm.qraft.qr.PrintSheetItem>()
                lines.forEachIndexed { idx, line ->
                    val isUrl = line.startsWith("http://") || line.startsWith("https://") ||
                        (line.contains(".") && !line.contains(" "))
                    val type = if (isUrl) QrContentType.URL else QrContentType.TEXT
                    val fields = if (isUrl) QrPayloadFields(url = line) else QrPayloadFields(text = line)
                    val shortLabel = QrPayloadEncoder.defaultTitle(type, fields)
                    val title = "Batch ${idx + 1} - $shortLabel"
                    val encoded = QrPayloadEncoder.encode(type, fields)

                    if (saveToLibrary) {
                        batchItems += QrCreationItem(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            type = type,
                            payload = fields,
                            design = design,
                            isVerifiedScannable = true
                        )
                    }
                    if (exportPdfSheetLayout != null) {
                        sheetItems += com.hkm.qraft.qr.PrintSheetItem(
                            title = shortLabel.take(24),
                            payload = encoded,
                            design = design
                        )
                    }
                    if (exportPngs) {
                        val res = QrExporter.saveToDevice(
                            context = getApplication(),
                            title = title,
                            data = encoded,
                            design = design,
                            format = ExportImageFormat.PNG
                        )
                        if (res.success) exportedCount++
                    }
                    _batchProgress.value = (idx + 1) to lines.size
                }
                if (batchItems.isNotEmpty()) {
                    repository.saveItems(batchItems)
                }
                var sheetSaved = false
                if (exportPdfSheetLayout != null && sheetItems.isNotEmpty()) {
                    val res = QrExporter.savePrintSheetPdf(
                        context = getApplication(),
                        sheetTitle = "Qraft Batch Labels",
                        items = sheetItems,
                        layout = exportPdfSheetLayout
                    )
                    sheetSaved = res.success
                }
                val summary = buildString {
                    append("Batch finished: ${lines.size} QR")
                    if (exportPngs) append(" • $exportedCount PNGs saved")
                    if (sheetSaved && exportPdfSheetLayout != null) append(" • ${exportPdfSheetLayout.label} PDF saved")
                }
                _statusBanner.tryEmit(summary)
                onComplete()
            } finally {
                _batchProgress.value = null
                _isExporting.value = false
            }
        }
    }

    fun exportSinglePrintSheet(
        title: String,
        data: String,
        design: QrDesignSpec,
        layout: com.hkm.qraft.qr.PrintSheetLayout,
        asPdf: Boolean
    ) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val tiled = com.hkm.qraft.qr.PrintSheetEngine.tileSingleQr(title, data, design, layout)
                val result = if (asPdf) {
                    QrExporter.savePrintSheetPdf(getApplication(), title, tiled, layout)
                } else {
                    QrExporter.savePrintSheetPng(getApplication(), title, tiled, layout)
                }
                _statusBanner.tryEmit(result.message)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportLibraryPrintSheet(
        items: List<QrCreationItem>,
        layout: com.hkm.qraft.qr.PrintSheetLayout,
        asPdf: Boolean
    ) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val sheetItems = items.map { item ->
                    com.hkm.qraft.qr.PrintSheetItem(
                        title = item.title,
                        payload = QrPayloadEncoder.encode(item.type, item.payload),
                        design = item.design
                    )
                }
                val result = if (asPdf) {
                    QrExporter.savePrintSheetPdf(getApplication(), "Qraft Library Sheet", sheetItems, layout)
                } else {
                    QrExporter.savePrintSheetPng(getApplication(), "Qraft Library Sheet", sheetItems, layout)
                }
                _statusBanner.tryEmit(result.message)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun bulkSetFavorite(ids: Set<String>, favorite: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setFavoriteForItems(ids, favorite)
            _statusBanner.tryEmit(
                if (favorite) "Pinned ${ids.size} QR codes" else "Unpinned ${ids.size} QR codes"
            )
        }
    }

    fun toggleItemArchived(id: String) {
        viewModelScope.launch {
            val target = savedItems.value.firstOrNull { it.id == id }
            repository.toggleArchived(id)
            _statusBanner.tryEmit(
                if (target?.isArchived == true) "Restored from Archive" else "Moved to Archive"
            )
        }
    }

    fun bulkSetArchived(ids: Set<String>, archived: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setArchivedForItems(ids, archived)
            _statusBanner.tryEmit(
                if (archived) "Archived ${ids.size} items" else "Restored ${ids.size} items"
            )
        }
    }

    fun renameItem(id: String, newTitle: String) {
        val cleaned = newTitle.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            repository.renameItem(id, cleaned)
            _statusBanner.tryEmit("Renamed to \"$cleaned\"")
        }
    }

    fun updateItemOrganization(id: String, folder: String?, tags: List<String>) {
        viewModelScope.launch {
            repository.updateItemOrganization(id, folder, tags)
            _statusBanner.tryEmit("Updated folder & tags")
        }
    }

    fun bulkDeleteItems(ids: Set<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteItems(ids)
            _statusBanner.tryEmit("Deleted ${ids.size} QR codes")
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val items = savedItems.value
            val presets = customPresets.value
            val json = withContext(Dispatchers.Default) {
                com.hkm.qraft.data.QraftBackupManager.serialize(items, presets)
            }
            val ok = QrExporter.writeTextToUri(getApplication(), uri, json)
            _statusBanner.tryEmit(
                if (ok) "Exported ${items.size} QR codes & ${presets.size} presets (.qraft)"
                else "Could not export backup file"
            )
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val raw = QrExporter.readTextFromUri(getApplication(), uri)
            val parsed = withContext(Dispatchers.Default) {
                raw?.let { com.hkm.qraft.data.QraftBackupManager.deserialize(it) }
            }
            if (parsed == null) {
                _statusBanner.tryEmit("Invalid or unreadable .qraft backup file")
                return@launch
            }
            if (parsed.items.isNotEmpty()) {
                repository.saveItems(parsed.items)
            }
            if (parsed.customPresets.isNotEmpty()) {
                repository.saveCustomPresets(parsed.customPresets)
            }
            _statusBanner.tryEmit(
                "Restored ${parsed.items.size} QR codes & ${parsed.customPresets.size} presets"
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun cycleThemeMode() {
        val next = when (preferences.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
        _statusBanner.tryEmit("Theme: ${next.label}")
    }

    fun setCreatorName(name: String) {
        viewModelScope.launch { repository.setCreatorName(name) }
    }

    fun setAutoVerify(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoVerify(enabled) }
    }

    fun setDefaultExportFormat(format: ExportImageFormat) {
        viewModelScope.launch { repository.setDefaultExportFormat(format) }
    }

    fun setDefaultExportResolution(resolution: Int) {
        viewModelScope.launch { repository.setDefaultExportResolution(resolution) }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch { repository.setOnboardingCompleted(completed) }
    }
}
