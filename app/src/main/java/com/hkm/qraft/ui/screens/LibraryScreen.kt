package com.hkm.qraft.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.qraft.model.QrContentType
import com.hkm.qraft.model.QrCreationItem
import com.hkm.qraft.qr.PrintSheetLayout
import com.hkm.qraft.qr.QrPayloadEncoder
import com.hkm.qraft.ui.components.QraftButtonVariant
import com.hkm.qraft.ui.components.QraftHeaderCircleButton
import com.hkm.qraft.ui.components.QraftPillButton
import com.hkm.qraft.ui.components.QraftTextField
import com.hkm.qraft.ui.components.SelectableChip
import com.hkm.qraft.ui.components.iconForContentType
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick

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
    onBulkFavorite: (Set<String>, Boolean) -> Unit = { ids, fav -> ids.forEach { _ -> }; Unit },
    onBulkDelete: (Set<String>) -> Unit = { ids -> ids.forEach(onDelete) },
    onExportPrintSheet: (List<QrCreationItem>, PrintSheetLayout, Boolean) -> Unit = { _, _, _ -> }
) {
    val colors = QraftThemeTokens.colors
    var search by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf<QrContentType?>(null) }
    var sortBy by remember { mutableStateOf(LibrarySort.UPDATED) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showPrintDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val filtered = remember(items, search, onlyFavorites, filterType, sortBy) {
        val base = items.filter { item ->
            val matchFav = !onlyFavorites || item.isFavorite
            val matchType = filterType == null || item.type == filterType
            val q = search.trim().lowercase()
            val matchSearch = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.type.title.lowercase().contains(q) ||
                QrPayloadEncoder.encode(item.type, item.payload).lowercase().contains(q)
            matchFav && matchType && matchSearch
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

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QraftHeaderCircleButton(
                            icon = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "Back",
                            size = 40.dp,
                            onClick = onBack
                        )
                        Text(
                            text = "QR Library",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QraftHeaderCircleButton(
                            icon = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Import & Remix QR",
                            size = 40.dp,
                            onClick = onImportQr
                        )
                        FilledTonalButton(
                            onClick = onOpenBatch,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colors.surfaceMuted,
                                contentColor = colors.textPrimary
                            )
                        ) {
                            Icon(Icons.Rounded.AutoAwesomeMotion, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Batch", maxLines = 1)
                        }
                        QraftHeaderCircleButton(
                            icon = Icons.Rounded.Add,
                            contentDescription = "Create new QR",
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
                    placeholder = "Search title, URL, Wi-Fi SSID…",
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
                        label = "All (${items.size})",
                        selected = !onlyFavorites && filterType == null,
                        onClick = {
                            onlyFavorites = false
                            filterType = null
                        }
                    )
                    SelectableChip(
                        label = "Favorites",
                        selected = onlyFavorites,
                        onClick = { onlyFavorites = !onlyFavorites }
                    )
                    QrContentType.entries.forEach { t ->
                        SelectableChip(
                            label = t.shortCode,
                            selected = filterType == t,
                            onClick = { filterType = if (filterType == t) null else t }
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
        },
        bottomBar = {
            if (selectionMode) {
                Surface(
                    color = colors.surface,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.SearchOff, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(36.dp))
                            Text("No matching QR codes", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("Try clearing filters or create a new custom QR.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    val isSelected = item.id in selectedIds
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
                        onDuplicate = { onDuplicate(item) },
                        onDelete = { onDelete(item.id) }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.surface,
            title = { Text("Delete ${selectedIds.size} QR codes?", color = colors.textPrimary) },
            text = { Text("This permanently removes the selected items from your Library.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBulkDelete(selectedIds)
                        selectedIds = emptySet()
                        selectionMode = false
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = colors.dangerFg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (showPrintDialog) {
        val chosenItems = remember(items, selectedIds) { items.filter { it.id in selectedIds } }
        var layout by remember { mutableStateOf(PrintSheetLayout.STICKERS_3X4) }
        AlertDialog(
            onDismissRequest = { showPrintDialog = false },
            containerColor = colors.surface,
            title = { Text("Print ${chosenItems.size} QR Labels", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose A4 printable sheet layout (300 DPI):", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    PrintSheetLayout.entries.forEach { opt ->
                        SelectableChip(
                            label = "${opt.label} (${opt.description})",
                            selected = layout == opt,
                            onClick = { layout = opt },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onExportPrintSheet(chosenItems, layout, false)
                            showPrintDialog = false
                            selectionMode = false
                            selectedIds = emptySet()
                        }
                    ) {
                        Text("A4 PNG", color = colors.textPrimary)
                    }
                    TextButton(
                        onClick = {
                            onExportPrintSheet(chosenItems, layout, true)
                            showPrintDialog = false
                            selectionMode = false
                            selectedIds = emptySet()
                        }
                    ) {
                        Text("Save PDF", color = colors.espressoAccent, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrintDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryItemCard(
    item: QrCreationItem,
    selectionMode: Boolean,
    selected: Boolean,
    onLongPress: () -> Unit,
    onOpen: () -> Unit,
    onFullscreen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = QraftThemeTokens.colors
    val pastel = colors.pastelAt(item.type.pastelIndex)
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.espressoAccent else colors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectionMode) {
                Icon(
                    imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) colors.espressoAccent else colors.textTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(pastel.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForContentType(item.type),
                    contentDescription = null,
                    tint = pastel.content,
                    modifier = Modifier.size(22.dp)
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
                    text = QrPayloadEncoder.summarySubtitle(item.type, item.payload),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.design.pixelStyle.label} • ${item.design.eyeFrameStyle.label} eyes • ${item.design.outerFrame.style.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) colors.timelineDotActive else colors.textTertiary
                )
            }

            IconButton(onClick = onFullscreen) {
                Icon(
                    imageVector = Icons.Rounded.QrCode2,
                    contentDescription = "Fullscreen scan",
                    tint = colors.textPrimary
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More actions", tint = colors.textSecondary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit in Studio") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = {
                            showMenu = false
                            onOpen()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = colors.dangerFg) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = colors.dangerFg) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
