package com.hkm.qraft

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.qraft.data.PresetCatalog
import com.hkm.qraft.data.QraftRepository
import com.hkm.qraft.data.UserPreferences
import com.hkm.qraft.model.*
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
    val isDirty: Boolean = false
) {
    val encodedPayload: String
        get() = QrPayloadEncoder.encode(type, payload)

    val effectiveTitle: String
        get() = title.trim().ifBlank { QrPayloadEncoder.defaultTitle(type, payload) }
}

class QraftViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QraftRepository(application.applicationContext)

    val savedItems: StateFlow<List<QrCreationItem>> = repository.itemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PresetCatalog.starterItems())

    val customPresets: StateFlow<List<SavedStylePreset>> = repository.customPresetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<UserPreferences> = repository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

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
                draftState.encodedPayload to (draftState.design to prefs.autoVerifyScannability)
            }.collectLatest { (encoded, pair) ->
                val (design, autoVerify) = pair
                if (!autoVerify) {
                    _scannability.value = ScannabilityReport(
                        isScannable = true,
                        contrastRatio = QrEngine.contrastRatio(
                            design.darkBrush.primaryColor,
                            design.background.brush.primaryColor
                        )
                    )
                    return@collectLatest
                }
                verifyJob?.cancel()
                verifyJob = viewModelScope.launch {
                    delay(220)
                    _isVerifying.value = true
                    val report = withContext(Dispatchers.Default) {
                        QrEngine.verifyScannability(getApplication(), encoded, design)
                    }
                    _scannability.value = report
                    _isVerifying.value = false
                }
            }
        }
    }

    fun selectHomeCategory(type: QrContentType?) {
        _selectedHomeCategory.value = type
    }

    fun startNewQr(type: QrContentType = QrContentType.URL) {
        val defaultPayload = when (type) {
            QrContentType.URL -> QrPayloadFields(url = "https://")
            QrContentType.WIFI -> QrPayloadFields(wifiSsid = "", wifiSecurity = WifiSecurity.WPA)
            else -> QrPayloadFields()
        }
        val baseDesign = PresetCatalog.builtInPresets.first().design.copy(
            exportSizePx = preferences.value.defaultExportResolution.coerceIn(512, 4096)
        )
        _draft.value = StudioDraftState(
            editingId = null,
            title = "",
            type = type,
            payload = defaultPayload,
            design = baseDesign,
            isFavorite = false,
            isDirty = false
        )
    }

    fun openExistingQr(item: QrCreationItem) {
        _draft.value = StudioDraftState(
            editingId = item.id,
            title = item.title,
            type = item.type,
            payload = item.payload,
            design = item.design,
            isFavorite = item.isFavorite,
            isDirty = false
        )
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
        _draft.update { it.copy(title = title, isDirty = true) }
    }

    fun updateDraftType(type: QrContentType) {
        _draft.update { it.copy(type = type, isDirty = true) }
    }

    fun updateDraftPayload(payload: QrPayloadFields) {
        _draft.update { it.copy(payload = payload, isDirty = true) }
    }

    fun updateDraftDesign(design: QrDesignSpec) {
        _draft.update { it.copy(design = design, isDirty = true) }
    }

    fun toggleDraftFavorite() {
        _draft.update { it.copy(isFavorite = !it.isFavorite, isDirty = true) }
    }

    fun applyPreset(preset: SavedStylePreset) {
        _draft.update { it.copy(design = preset.design, isDirty = true) }
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

        val current = _draft.value.design
        _draft.update {
            it.copy(
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
        val current = _draft.value.design
        val fixedLogo = if (current.logo.sourceType != LogoSourceType.NONE && current.logo.sizeFraction > 0.23f) {
            current.logo.copy(sizeFraction = 0.21f, paddingStyle = LogoPaddingStyle.NATURAL, backingEnabled = true)
        } else {
            current.logo
        }
        _draft.update {
            it.copy(
                design = current.copy(
                    darkBrush = current.darkBrush.copy(primaryColor = 0xFF1E1D1B),
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
        _statusBanner.tryEmit("Boosted contrast and error correction")
    }

    fun saveDraftToLibrary(onSaved: ((QrCreationItem) -> Unit)? = null) {
        val current = _draft.value
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
                updatedAtMillis = now
            )
            repository.saveItem(item)
            _draft.update { it.copy(editingId = item.id, title = item.title, isDirty = false) }
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
            val result = QrExporter.saveToDevice(
                context = getApplication(),
                title = current.effectiveTitle,
                data = current.encodedPayload,
                design = current.design,
                format = format
            )
            _isExporting.value = false
            _statusBanner.tryEmit(result.message)
        }
    }

    fun shareExport(format: ExportImageFormat) {
        val current = _draft.value
        viewModelScope.launch {
            _isExporting.value = true
            val result = QrExporter.shareQr(
                context = getApplication(),
                title = current.effectiveTitle,
                data = current.encodedPayload,
                design = current.design,
                format = format
            )
            _isExporting.value = false
            if (!result.success) _statusBanner.tryEmit(result.message)
        }
    }

    fun copyPayload() {
        val current = _draft.value
        QrExporter.copyPayloadToClipboard(
            context = getApplication(),
            label = current.effectiveTitle,
            payload = current.encodedPayload
        )
        _statusBanner.tryEmit("Copied payload to clipboard")
    }

    fun copyExportImage() {
        val current = _draft.value
        viewModelScope.launch {
            _isExporting.value = true
            val result = QrExporter.copyImageToClipboard(
                context = getApplication(),
                title = current.effectiveTitle,
                data = current.encodedPayload,
                design = current.design
            )
            _isExporting.value = false
            _statusBanner.tryEmit(result.message)
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
        onComplete: () -> Unit
    ) {
        val design = _draft.value.design
        viewModelScope.launch {
            _isExporting.value = true
            var exportedCount = 0
            val batchItems = mutableListOf<QrCreationItem>()
            lines.forEachIndexed { idx, line ->
                val isUrl = line.startsWith("http://") || line.startsWith("https://") || line.contains(".") && !line.contains(" ")
                val type = if (isUrl) QrContentType.URL else QrContentType.TEXT
                val fields = if (isUrl) QrPayloadFields(url = line) else QrPayloadFields(text = line)
                val title = "Batch ${idx + 1} - ${QrPayloadEncoder.defaultTitle(type, fields)}"

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
                if (exportPngs) {
                    val res = QrExporter.saveToDevice(
                        context = getApplication(),
                        title = title,
                        data = QrPayloadEncoder.encode(type, fields),
                        design = design,
                        format = ExportImageFormat.PNG
                    )
                    if (res.success) exportedCount++
                }
            }
            if (batchItems.isNotEmpty()) {
                repository.saveItems(batchItems)
            }
            _isExporting.value = false
            val summary = buildString {
                append("Batch finished: ${lines.size} QR")
                if (exportPngs) append(" • $exportedCount ${design.exportSizePx}px PNGs saved")
            }
            _statusBanner.tryEmit(summary)
            onComplete()
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
}
