package com.hkm.qraft

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hkm.qraft.model.ThemeMode
import com.hkm.qraft.qr.QrPayloadEncoder
import com.hkm.qraft.ui.components.*
import com.hkm.qraft.ui.screens.HomeScreen
import com.hkm.qraft.ui.screens.LibraryScreen
import com.hkm.qraft.ui.screens.StudioScreen
import com.hkm.qraft.ui.theme.QraftTheme
import com.hkm.qraft.ui.theme.QraftThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private object Routes {
    const val HOME = "home"
    const val STUDIO = "studio"
    const val LIBRARY = "library"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: QraftViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(effectiveDark) {
                val barStyle = if (effectiveDark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }

            QraftTheme(themeMode = prefs.themeMode) {
                QraftAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun QraftAppRoot(viewModel: QraftViewModel) {
    val navController = rememberNavController()
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val selectedHomeCategory by viewModel.selectedHomeCategory.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val scannability by viewModel.scannability.collectAsStateWithLifecycle()
    val isVerifying by viewModel.isVerifying.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()

    var showCreatePicker by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showBatchSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var fullscreenItem by remember { mutableStateOf<Triple<String, String, com.hkm.qraft.model.QrDesignSpec>?>(null) }

    var bannerMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.statusBanner.collectLatest { msg ->
            bannerMessage = msg
            delay(2600)
            if (bannerMessage == msg) bannerMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = {
                slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(260))
            },
            exitTransition = {
                slideOutHorizontally(tween(260)) { -it / 6 } + fadeOut(tween(200))
            },
            popEnterTransition = {
                slideInHorizontally(tween(280)) { -it / 6 } + fadeIn(tween(240))
            },
            popExitTransition = {
                slideOutHorizontally(tween(280)) { it / 4 } + fadeOut(tween(200))
            }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    creatorName = prefs.creatorName,
                    themeMode = prefs.themeMode,
                    items = savedItems,
                    selectedCategory = selectedHomeCategory,
                    onSelectCategory = viewModel::selectHomeCategory,
                    onCycleTheme = viewModel::cycleThemeMode,
                    onOpenSettings = { showSettingsSheet = true },
                    onOpenStudioNew = { type ->
                        viewModel.startNewQr(type)
                        navController.navigate(Routes.STUDIO)
                    },
                    onOpenExisting = { item ->
                        viewModel.openExistingQr(item)
                        navController.navigate(Routes.STUDIO)
                    },
                    onQuickFullscreen = { item ->
                        fullscreenItem = Triple(
                            item.title,
                            QrPayloadEncoder.encode(item.type, item.payload),
                            item.design
                        )
                    },
                    onToggleFavorite = viewModel::toggleItemFavorite,
                    onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                    onOpenBatch = { showBatchSheet = true },
                    onOpenCreatePicker = { showCreatePicker = true }
                )
            }

            composable(Routes.LIBRARY) {
                LibraryScreen(
                    items = savedItems,
                    onBack = { navController.popBackStack() },
                    onOpenExisting = { item ->
                        viewModel.openExistingQr(item)
                        navController.navigate(Routes.STUDIO)
                    },
                    onDuplicate = viewModel::duplicateQr,
                    onToggleFavorite = viewModel::toggleItemFavorite,
                    onDelete = viewModel::deleteItem,
                    onQuickFullscreen = { item ->
                        fullscreenItem = Triple(
                            item.title,
                            QrPayloadEncoder.encode(item.type, item.payload),
                            item.design
                        )
                    },
                    onOpenBatch = { showBatchSheet = true },
                    onCreateNew = { showCreatePicker = true }
                )
            }

            composable(Routes.STUDIO) {
                StudioScreen(
                    draft = draft,
                    customPresets = customPresets,
                    scannability = scannability,
                    isVerifying = isVerifying,
                    onBack = { navController.popBackStack() },
                    onTitleChange = viewModel::updateDraftTitle,
                    onTypeChange = viewModel::updateDraftType,
                    onFieldsChange = viewModel::updateDraftPayload,
                    onDesignChange = viewModel::updateDraftDesign,
                    onToggleFavorite = viewModel::toggleDraftFavorite,
                    onApplyPreset = viewModel::applyPreset,
                    onSaveCustomPreset = viewModel::saveCurrentAsPreset,
                    onDeleteCustomPreset = viewModel::deleteCustomPreset,
                    onRandomizeStyle = viewModel::randomizeStyle,
                    onAutoFixScannability = viewModel::autoFixScannability,
                    onSaveToLibrary = { viewModel.saveDraftToLibrary() },
                    onOpenExport = { showExportSheet = true },
                    onOpenFullscreen = {
                        fullscreenItem = Triple(
                            draft.effectiveTitle,
                            draft.encodedPayload,
                            draft.design
                        )
                    }
                )
            }
        }

        // Apple-style floating status toast pill
        AnimatedVisibility(
            visible = bannerMessage != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            val colors = QraftThemeTokens.colors
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.activePillBg,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = colors.timelineDotActive,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = bannerMessage.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.activePillContent
                    )
                }
            }
        }
    }

    if (showCreatePicker) {
        CreateQrTypeSheet(
            onDismiss = { showCreatePicker = false },
            onSelectType = { type ->
                viewModel.startNewQr(type)
                navController.navigate(Routes.STUDIO)
            }
        )
    }

    if (showExportSheet) {
        ExportQrSheet(
            title = draft.effectiveTitle,
            encodedPayload = draft.encodedPayload,
            design = draft.design,
            initialFormat = prefs.defaultExportFormat,
            isExporting = isExporting,
            onDismiss = { showExportSheet = false },
            onSave = viewModel::saveExport,
            onShare = viewModel::shareExport,
            onCopyPayload = viewModel::copyPayload
        )
    }

    if (showBatchSheet) {
        BatchGeneratorSheet(
            currentDesign = draft.design,
            isRunning = isExporting,
            onDismiss = { showBatchSheet = false },
            onRunBatch = { lines, saveToLib, exportPngs ->
                viewModel.runBatchGenerate(lines, saveToLib, exportPngs) {
                    showBatchSheet = false
                }
            }
        )
    }

    if (showSettingsSheet) {
        SettingsModalSheet(
            preferences = prefs,
            onDismiss = { showSettingsSheet = false },
            onThemeChange = viewModel::setThemeMode,
            onCreatorNameChange = viewModel::setCreatorName,
            onAutoVerifyChange = viewModel::setAutoVerify,
            onDefaultFormatChange = viewModel::setDefaultExportFormat
        )
    }

    fullscreenItem?.let { (title, payload, design) ->
        FullscreenScanDialog(
            title = title,
            encodedPayload = payload,
            design = design,
            onDismiss = { fullscreenItem = null }
        )
    }
}
