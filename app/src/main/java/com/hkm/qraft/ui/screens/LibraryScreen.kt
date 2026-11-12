package com.hkm.qraft.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.hkm.qraft.qr.QrPayloadEncoder
import com.hkm.qraft.ui.components.QraftTextField
import com.hkm.qraft.ui.components.SelectableChip
import com.hkm.qraft.ui.components.iconForContentType
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick

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
    onCreateNew: () -> Unit
) {
    val colors = QraftThemeTokens.colors
    var search by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf<QrContentType?>(null) }

    val filtered = remember(items, search, onlyFavorites, filterType) {
        items.filter { item ->
            val matchFav = !onlyFavorites || item.isFavorite
            val matchType = filterType == null || item.type == filterType
            val q = search.trim().lowercase()
            val matchSearch = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.type.title.lowercase().contains(q) ||
                QrPayloadEncoder.encode(item.type, item.payload).lowercase().contains(q)
            matchFav && matchType && matchSearch
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .size(40.dp)
                                .appleBounceClick { onBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text("QR Library", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onOpenBatch,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colors.surfaceMuted,
                                contentColor = colors.textPrimary
                            )
                        ) {
                            Icon(Icons.Rounded.AutoAwesomeMotion, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Batch")
                        }
                        Button(
                            onClick = onCreateNew,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.espressoAccent)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
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
                    LibraryItemCard(
                        item = item,
                        onOpen = { onOpenExisting(item) },
                        onFullscreen = { onQuickFullscreen(item) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onDuplicate = { onDuplicate(item) },
                        onDelete = { onDelete(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    item: QrCreationItem,
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
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .appleBounceClick { onOpen() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
