package com.hkm.qraft.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.hkm.qraft.model.CodeKind
import com.hkm.qraft.model.QrContentType
import com.hkm.qraft.model.QrCreationItem
import com.hkm.qraft.qr.PrintSheetLayout
import com.hkm.qraft.qr.QrPayloadEncoder
import com.hkm.qraft.ui.components.QraftButtonVariant
import com.hkm.qraft.ui.components.QraftHeaderCircleButton
import com.hkm.qraft.ui.components.QraftModalSheet
import com.hkm.qraft.ui.components.QraftPillButton
import com.hkm.qraft.ui.components.QraftTextField
import com.hkm.qraft.ui.components.SelectableChip
import com.hkm.qraft.ui.components.iconForContentType
import com.hkm.qraft.ui.theme.AppMotion
import com.hkm.qraft.ui.theme.AppleGlassSurface
import com.hkm.qraft.ui.theme.AppleGlassVariant
import com.hkm.qraft.ui.theme.AppleOdometerNumber
import com.hkm.qraft.ui.theme.AppleProgressiveEdgeFrost
import com.hkm.qraft.ui.theme.EdgeFrostDirection
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleHazeSource
import com.hkm.qraft.ui.theme.applePressable
import com.hkm.qraft.ui.theme.heroSource
import dev.chrisbanes.haze.rememberHazeState

private enum class LibrarySort(val label: String) {
    UPDATED("Latest"),
    NAME("A–Z"),
    TYPE("Type"),
    FAVORITES("Pinned First")
}

@Composable
fun LibraryScreen(
    items: List<QrCreationItem>,
    onBack: () -> Unit,
    onOpenExisting: (QrCreationItem) -> Unit,
    onDuplicate: (QrCreationItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDelete: (String) -> Unit,
    onQuickFullscreen: (QrCreationItem) -> Unit,
    onOpenBatch: () -> Unit,
    onCreateNew: () -> Unit,
    onImportQr: () -> Unit = {},
    onOpenScanner: () -> Unit = onImportQr,
    onBulkFavorite: (Set<String>, Boolean) -> Unit = { ids, fav -> ids.forEach { _ -> }; Unit },
    onBulkDelete: (Set<String>) -> Unit = { ids -> ids.forEach(onDelete) },
    onExportPrintSheet: (List<QrCreationItem>, PrintSheetLayout, Boolean) -> Unit = { _, _, _ -> },
    onToggleArchive: (String) -> Unit = {},
    onBulkArchive: (Set<String>, Boolean) -> Unit = { _, _ -> },
    onRenameItem: (String, String) -> Unit = { _, _ -> },
    onOrganizeItem: (String, String?, List<String>) -> Unit = { _, _, _ -> }
) {
    val colors = QraftThemeTokens.colors
    val hazeState = rememberHazeState()
    val listState = rememberLazyListState()
    var search by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var onlyBarcodes by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var filterType by remember { mutableStateOf<QrContentType?>(null) }
    var sortBy by remember { mutableStateOf(LibrarySort.UPDATED) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showPrintDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var actionTargetItem by remember { mutableStateOf<QrCreationItem?>(null) }
    var renameTargetItem by remember { mutableStateOf<QrCreationItem?>(null) }
    var organizeTargetItem by remember { mutableStateOf<QrCreationItem?>(null) }

    val availableFolders = remember(items) {
        items.mapNotNull { it.resolvedFolder().takeIf(String::isNotBlank) }.distinct().sorted()
    }
    val archivedCount = remember(items) { items.count { it.isArchived } }
    val activeCount = remember(items) { items.size - archivedCount }

    val filtered = remember(items, search, onlyFavorites, onlyBarcodes, showArchived, selectedFolder, filterType, sortBy) {
        val base = items.filter { item ->
            val matchArchive = item.isArchived == showArchived
            val isBc = item.resolvedCodeKind() == CodeKind.BARCODE
            val matchFav = !onlyFavorites || item.isFavorite
            val matchKind = !onlyBarcodes || isBc
            val matchFolder = selectedFolder == null || item.resolvedFolder().equals(selectedFolder, ignoreCase = true)
            val matchType = filterType == null || (!isBc && item.type == filterType)
            val q = search.trim().lowercase()
            val bc = if (isBc) item.resolvedBarcode() else null
            val matchSearch = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.type.title.lowercase().contains(q) ||
                item.resolvedFolder().lowercase().contains(q) ||
                item.resolvedTags().any { it.lowercase().contains(q) } ||
                (bc != null && (bc.data.lowercase().contains(q) || bc.format.label.lowercase().contains(q))) ||
                QrPayloadEncoder.encode(item.type, item.payload).lowercase().contains(q)
            matchArchive && matchFav && matchKind && matchFolder && matchType && matchSearch
        }
        when (sortBy) {
            LibrarySort.UPDATED -> base.sortedByDescending { it.updatedAtMillis }
            LibrarySort.NAME -> base.sortedBy { it.title.lowercase() }
            LibrarySort.TYPE -> base.sortedBy { it.type.ordinal }
            LibrarySort.FAVORITES -> base.sortedWith(
                compareByDescending<QrCreationItem> { it.isFavorite }.thenByDescending { it.updatedAtMillis }
            )
        }
    }

    LaunchedEffect(items) {
        val validIds = items.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(validIds)
        if (selectedIds.isEmpty() && selectionMode && items.isEmpty()) {
            selectionMode = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val navBottomPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .appleHazeSource(hazeState),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = navBottomPad + if (selectionMode) 104.dp else 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Edge-to-edge scrolling header with tabular odometer count
            item(key = "lib_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QraftHeaderCircleButton(
                                icon = Icons.Rounded.ArrowBackIosNew,
                                contentDescription = "Back",
                                size = 40.dp,
                                onClick = onBack
                            )
                            Column {
                                Text(
                                    text = "Library",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                AppleOdometerNumber(
                                    value = filtered.size,
                                    suffix = " saved",
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QraftHeaderCircleButton(
                                icon = Icons.Rounded.QrCodeScanner,
                                contentDescription = "Live Scanner",
                                size = 40.dp,
                                onClick = onOpenScanner,
                                modifier = Modifier.heroSource("hero_lib_scanner", cornerRadiusDp = 20f)
                            )
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.surfaceMuted)
                                    .applePressable { onOpenBatch() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(Icons.Rounded.AutoAwesomeMotion, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                                    Text("Batch", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary, maxLines = 1)
                                }
                            }
                            QraftHeaderCircleButton(
                                icon = Icons.Rounded.Add,
                                contentDescription = "Create new code",
                                size = 40.dp,
                                containerColor = colors.espressoAccent,
                                contentColor = colors.espressoAccentContent,
                                borderColor = colors.espressoAccent,
                                onClick = onCreateNew
                            )
                        }
                    }

                    QraftTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = "Search library",
                        placeholder = "Search title, URL, barcode, Wi-Fi…",
                        leadingIcon = Icons.Rounded.Search,
                        trailingIcon = if (search.isNotEmpty()) {
                            {
                                IconButton(onClick = { search = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectableChip(
                            label = if (selectionMode) "Done (${selectedIds.size})" else "Select",
                            selected = selectionMode,
                            onClick = {
                                selectionMode = !selectionMode
                                if (!selectionMode) selectedIds = emptySet()
                            }
                        )
                        SelectableChip(
                            label = "All ($activeCount)",
                            selected = !showArchived && !onlyFavorites && !onlyBarcodes && filterType == null && selectedFolder == null,
                            onClick = {
                                showArchived = false
                                onlyFavorites = false
                                onlyBarcodes = false
                                selectedFolder = null
                                filterType = null
                            }
                        )
                        SelectableChip(
                            label = "Favorites",
                            selected = onlyFavorites,
                            onClick = { onlyFavorites = !onlyFavorites }
                        )
                        SelectableChip(
                            label = "Barcodes",
                            selected = onlyBarcodes,
                            onClick = {
                                onlyBarcodes = !onlyBarcodes
                                if (onlyBarcodes) filterType = null
                            }
                        )
                        SelectableChip(
                            label = if (archivedCount > 0) "Archived ($archivedCount)" else "Archived",
                            selected = showArchived,
                            onClick = {
                                showArchived = !showArchived
                                selectedIds = emptySet()
                            }
                        )
                        availableFolders.forEach { folderName ->
                            SelectableChip(
                                label = "Folder: $folderName",
                                selected = selectedFolder.equals(folderName, ignoreCase = true),
                                onClick = {
                                    selectedFolder = if (selectedFolder.equals(folderName, ignoreCase = true)) null else folderName
                                }
                            )
                        }
                        QrContentType.entries.forEach { t ->
                            SelectableChip(
                                label = t.shortCode,
                                selected = filterType == t,
                                onClick = {
                                    filterType = if (filterType == t) null else t
                                    if (filterType != null) onlyBarcodes = false
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort:",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                        LibrarySort.entries.forEach { sortOption ->
                            SelectableChip(
                                label = sortOption.label,
                                selected = sortBy == sortOption,
                                onClick = { sortBy = sortOption }
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    val emptyLibrary = items.isEmpty() && !showArchived
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                if (emptyLibrary) Icons.Rounded.QrCode2 else Icons.Rounded.SearchOff,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = if (emptyLibrary) "Your Library is Empty" else "No matching codes",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (emptyLibrary) {
                                    "Craft your first custom QR code or scan one from camera or gallery."
                                } else {
                                    "Try clearing filters or create a new custom QR or barcode."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                            if (emptyLibrary) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    QraftPillButton(
                                        text = "Create Code",
                                        variant = QraftButtonVariant.PRIMARY,
                                        leadingIcon = Icons.Rounded.Add,
                                        height = 42.dp,
                                        onClick = onCreateNew
                                    )
                                    QraftPillButton(
                                        text = "Import",
                                        variant = QraftButtonVariant.SECONDARY,
                                        leadingIcon = Icons.Rounded.PhotoLibrary,
                                        height = 42.dp,
                                        onClick = onImportQr
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    val isSelected = item.id in selectedIds
                    Box(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = AppMotion.snappySpring(),
                                placementSpec = AppMotion.layoutSpring()
                            )
                            .heroSource("hero_lib_${item.id}", cornerRadiusDp = 22f)
                    ) {
                        LibraryItemCard(
                            item = item,
                            selectionMode = selectionMode,
                            selected = isSelected,
                            onLongPress = {
                                selectionMode = true
                                selectedIds = selectedIds + item.id
                            },
                            onOpen = {
                                if (selectionMode) {
                                    selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                } else {
                                    onOpenExisting(item)
                                }
                            },
                            onFullscreen = { onQuickFullscreen(item) },
                            onToggleFavorite = { onToggleFavorite(item.id) },
                            onMoreActions = { actionTargetItem = item }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectionMode,
            enter = AppMotion.floatingBarEnter,
            exit = AppMotion.floatingBarExit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            AppleGlassSurface(
                shape = RoundedCornerShape(24.dp),
                cornerRadiusDp = 24.dp,
                variant = AppleGlassVariant.Regular,
                hazeState = hazeState,
                enableRefraction = true,
                elevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = {
                            val filteredIds = filtered.map { it.id }.toSet()
                            selectedIds = if (selectedIds.containsAll(filteredIds) && filteredIds.isNotEmpty()) {
                                emptySet()
                            } else {
                                filteredIds
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedIds.size == filtered.size && filtered.isNotEmpty()) "None" else "All",
                            color = colors.espressoAccent
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        enabled = selectedIds.isNotEmpty(),
                        onClick = {
                            val anyUnpinned = items.any { it.id in selectedIds && !it.isFavorite }
                            onBulkFavorite(selectedIds, anyUnpinned)
                        }
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = "Pin / Unpin", tint = if (selectedIds.isNotEmpty()) colors.timelineDotActive else colors.textTertiary)
                    }
                    IconButton(
                        enabled = selectedIds.isNotEmpty(),
                        onClick = {
                            onBulkArchive(selectedIds, !showArchived)
                            selectedIds = emptySet()
                            selectionMode = false
                        }
                    ) {
                        Icon(
                            imageVector = if (showArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = if (showArchived) "Restore from archive" else "Archive selected",
                            tint = if (selectedIds.isNotEmpty()) colors.textPrimary else colors.textTertiary
                        )
                    }
                    QraftPillButton(
                        text = "Print",
                        variant = QraftButtonVariant.TONAL,
                        leadingIcon = Icons.Rounded.Print,
                        enabled = selectedIds.isNotEmpty(),
                        height = 40.dp,
                        onClick = { showPrintDialog = true }
                    )
                    QraftPillButton(
                        text = "Delete (${selectedIds.size})",
                        variant = QraftButtonVariant.DANGER,
                        enabled = selectedIds.isNotEmpty(),
                        height = 40.dp,
                        onClick = { showDeleteConfirm = true }
                    )
                }
            }
        }

        AppleProgressiveEdgeFrost(
            hazeState = hazeState,
            direction = EdgeFrostDirection.Top,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 18.dp)
        ) {}
    }

    if (showDeleteConfirm) {
        QraftModalSheet(
            onDismissRequest = { showDeleteConfirm = false },
            maxHeightFraction = 0.55f,
            hardCap = 360.dp,
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ) { dismissSheet ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Delete ${selectedIds.size} QR codes?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "This permanently removes the selected items from your Library. Tip: you can Archive items instead to keep them hidden without losing them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QraftPillButton(
                        text = "Cancel",
                        variant = QraftButtonVariant.SECONDARY,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                        onClick = dismissSheet
                    )
                    QraftPillButton(
                        text = "Delete",
                        variant = QraftButtonVariant.DANGER,
                        leadingIcon = Icons.Rounded.DeleteOutline,
                        height = 46.dp,
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            onBulkDelete(selectedIds)
                            selectedIds = emptySet()
                            selectionMode = false
                            dismissSheet()
                        }
                    )
                }
            }
        }
    }

    if (showPrintDialog) {
        val chosenItems = remember(items, selectedIds) { items.filter { it.id in selectedIds } }
        var layout by remember { mutableStateOf(PrintSheetLayout.STICKERS_3X4) }
        QraftModalSheet(
            onDismissRequest = { showPrintDialog = false },
            maxHeightFraction = 0.78f,
            hardCap = 540.dp,
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
                    text = "Print ${chosenItems.size} QR Labels",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "Choose A4 printable sheet layout (300 DPI):",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                PrintSheetLayout.entries.forEach { opt ->
                    SelectableChip(
                        label = "${opt.label} (${opt.description})",
                        selected = layout == opt,
                        onClick = { layout = opt },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QraftPillButton(
                        text = "A4 PNG",
                        variant = QraftButtonVariant.SECONDARY,
                        leadingIcon = Icons.Rounded.Image,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onExportPrintSheet(chosenItems, layout, false)
                            selectionMode = false
                            selectedIds = emptySet()
                            dismissSheet()
                        }
                    )
                    QraftPillButton(
                        text = "Save PDF",
                        variant = QraftButtonVariant.PRIMARY,
                        leadingIcon = Icons.Rounded.PictureAsPdf,
                        height = 46.dp,
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            onExportPrintSheet(chosenItems, layout, true)
                            selectionMode = false
                            selectedIds = emptySet()
                            dismissSheet()
                        }
                    )
                }
            }
        }
    }

    actionTargetItem?.let { target ->
        LibraryActionSheet(
            item = target,
            onDismiss = { actionTargetItem = null },
            onEdit = {
                actionTargetItem = null
                onOpenExisting(target)
            },
            onRename = {
                actionTargetItem = null
                renameTargetItem = target
            },
            onOrganize = {
                actionTargetItem = null
                organizeTargetItem = target
            },
            onFullscreen = {
                actionTargetItem = null
                onQuickFullscreen(target)
            },
            onDuplicate = {
                actionTargetItem = null
                onDuplicate(target)
            },
            onToggleArchive = {
                actionTargetItem = null
                onToggleArchive(target.id)
            },
            onDelete = {
                actionTargetItem = null
                onDelete(target.id)
            }
        )
    }

    renameTargetItem?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.title) }
        QraftModalSheet(
            onDismissRequest = { renameTargetItem = null },
            maxHeightFraction = 0.60f,
            hardCap = 360.dp,
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ) { dismissSheet ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Rename Code", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                QraftTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = "Title",
                    placeholder = "Enter a memorable name",
                    leadingIcon = Icons.Rounded.DriveFileRenameOutline
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QraftPillButton(
                        text = "Cancel",
                        variant = QraftButtonVariant.SECONDARY,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                        onClick = dismissSheet
                    )
                    QraftPillButton(
                        text = "Save Name",
                        variant = QraftButtonVariant.PRIMARY,
                        leadingIcon = Icons.Rounded.Check,
                        height = 46.dp,
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            onRenameItem(target.id, newName)
                            dismissSheet()
                        }
                    )
                }
            }
        }
    }

    organizeTargetItem?.let { target ->
        var folderDraft by remember(target.id) { mutableStateOf(target.resolvedFolder()) }
        var tagsDraft by remember(target.id) { mutableStateOf(target.resolvedTags().joinToString(", ")) }
        val suggestedFolders = remember(availableFolders) {
            (listOf("Work", "Personal", "Business", "Events", "Wi-Fi") + availableFolders).distinct()
        }
        QraftModalSheet(
            onDismissRequest = { organizeTargetItem = null },
            maxHeightFraction = 0.78f,
            hardCap = 520.dp,
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ) { dismissSheet ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Folder & Tags", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    Text(
                        text = "Group \"${target.title}\" into a folder and searchable tags.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableChip(
                        label = "No Folder",
                        selected = folderDraft.isBlank(),
                        onClick = { folderDraft = "" }
                    )
                    suggestedFolders.forEach { presetFolder ->
                        SelectableChip(
                            label = presetFolder,
                            selected = folderDraft.equals(presetFolder, ignoreCase = true),
                            onClick = { folderDraft = presetFolder }
                        )
                    }
                }
                QraftTextField(
                    value = folderDraft,
                    onValueChange = { folderDraft = it },
                    label = "Folder Name",
                    placeholder = "e.g. Work, Menu, Invoices",
                    leadingIcon = Icons.Rounded.FolderOpen
                )
                QraftTextField(
                    value = tagsDraft,
                    onValueChange = { tagsDraft = it },
                    label = "Tags (comma separated)",
                    placeholder = "e.g. promo, v2, store-front",
                    leadingIcon = Icons.Rounded.Tag
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QraftPillButton(
                        text = "Cancel",
                        variant = QraftButtonVariant.SECONDARY,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                        onClick = dismissSheet
                    )
                    QraftPillButton(
                        text = "Apply",
                        variant = QraftButtonVariant.PRIMARY,
                        leadingIcon = Icons.Rounded.Check,
                        height = 46.dp,
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            val parsedTags = tagsDraft.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            onOrganizeItem(target.id, folderDraft.trim().takeIf { it.isNotEmpty() }, parsedTags)
                            dismissSheet()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryActionSheet(
    item: QrCreationItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onOrganize: () -> Unit,
    onFullscreen: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = QraftThemeTokens.colors
    QraftModalSheet(
        onDismissRequest = onDismiss,
        maxHeightFraction = 0.80f,
        hardCap = 580.dp,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) { dismissSheet ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildString {
                    append(if (item.resolvedCodeKind() == CodeKind.BARCODE) item.resolvedBarcode().format.label else item.type.title)
                    if (item.resolvedFolder().isNotEmpty()) append(" • Folder: ${item.resolvedFolder()}")
                    if (item.isArchived) append(" • Archived")
                }
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceMuted,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LibrarySheetActionRow(Icons.Rounded.Edit, "Edit in Studio") {
                        dismissSheet(); onEdit()
                    }
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.7f))
                    LibrarySheetActionRow(Icons.Rounded.DriveFileRenameOutline, "Rename") {
                        dismissSheet(); onRename()
                    }
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.7f))
                    LibrarySheetActionRow(Icons.Rounded.FolderOpen, "Folder & Tags") {
                        dismissSheet(); onOrganize()
                    }
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.7f))
                    LibrarySheetActionRow(Icons.Rounded.Fullscreen, "Fullscreen Scan Preview") {
                        dismissSheet(); onFullscreen()
                    }
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.7f))
                    LibrarySheetActionRow(Icons.Rounded.ContentCopy, "Duplicate") {
                        dismissSheet(); onDuplicate()
                    }
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.7f))
                    LibrarySheetActionRow(
                        icon = if (item.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        label = if (item.isArchived) "Restore to Active Library" else "Move to Archive"
                    ) {
                        dismissSheet(); onToggleArchive()
                    }
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.7f))
                    LibrarySheetActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = "Delete Permanently",
                        tint = colors.dangerFg
                    ) {
                        dismissSheet(); onDelete()
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySheetActionRow(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = QraftThemeTokens.colors.textPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .applePressable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 15.sp,
            color = tint
        )
    }
}

@Composable
private fun LibraryItemCard(
    item: QrCreationItem,
    selectionMode: Boolean,
    selected: Boolean,
    onLongPress: () -> Unit,
    onOpen: () -> Unit,
    onFullscreen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreActions: () -> Unit
) {
    val colors = QraftThemeTokens.colors
    val pastel = colors.pastelAt(item.type.pastelIndex)
    val isBarcode = item.resolvedCodeKind() == CodeKind.BARCODE
    val bc = if (isBarcode) item.resolvedBarcode() else null
    val folderLabel = item.resolvedFolder()
    val tagsList = item.resolvedTags()
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.espressoAccent else colors.cardBorder,
        animationSpec = AppMotion.snappySpring(),
        label = "libCardBorder"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .applePressable(
                onLongClick = onLongPress,
                onClick = onOpen
            )
    ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectionMode) {
                    Icon(
                        imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) colors.espressoAccent else colors.textTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(pastel.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBarcode) Icons.Rounded.ViewWeek else iconForContentType(item.type),
                        contentDescription = null,
                        tint = pastel.content,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (bc != null) "${bc.format.label} • ${bc.data}" else QrPayloadEncoder.summarySubtitle(item.type, item.payload),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                        Text(
                            text = if (bc != null) {
                                "${bc.format.shortCode} • ${bc.exportWidthPx}px"
                            } else {
                                "${item.design.pixelStyle.label} • ${item.design.eyeFrameStyle.label} • ${item.design.outerFrame.style.label}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (folderLabel.isNotEmpty() || tagsList.isNotEmpty() || item.isArchived) {
                            val badgeText = buildString {
                                if (item.isArchived) append("Archived")
                                if (folderLabel.isNotEmpty()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(folderLabel)
                                }
                                if (tagsList.isNotEmpty()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(tagsList.take(2).joinToString(" ") { "#$it" })
                                }
                            }
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) colors.timelineDotActive else colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onFullscreen,
                        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = if (isBarcode) Icons.Rounded.ViewWeek else Icons.Rounded.QrCode2,
                            contentDescription = "Fullscreen scan",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoreActions,
                        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "More actions",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
}
