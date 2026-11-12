package com.hkm.qraft.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.qraft.model.QrContentType
import com.hkm.qraft.model.QrCreationItem
import com.hkm.qraft.model.ThemeMode
import com.hkm.qraft.qr.QrPayloadEncoder
import com.hkm.qraft.ui.components.HeroQrArtworkIllustration
import com.hkm.qraft.ui.components.iconForContentType
import com.hkm.qraft.ui.theme.AppleSprings
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.StaggeredReveal
import com.hkm.qraft.ui.theme.appleBounceClick
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class StripFilterOption(
    val type: QrContentType?,
    val label: String
)

private val homeStripOptions = listOf(
    StripFilterOption(null, "ALL"),
    StripFilterOption(QrContentType.URL, "WEB"),
    StripFilterOption(QrContentType.WIFI, "WIFI"),
    StripFilterOption(QrContentType.VCARD, "CARD"),
    StripFilterOption(QrContentType.TEXT, "TXT"),
    StripFilterOption(QrContentType.PAYMENT, "PAY"),
    StripFilterOption(QrContentType.SOCIAL, "SOC")
)

@Composable
fun HomeScreen(
    creatorName: String,
    themeMode: ThemeMode,
    items: List<QrCreationItem>,
    selectedCategory: QrContentType?,
    onSelectCategory: (QrContentType?) -> Unit,
    onCycleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStudioNew: (QrContentType) -> Unit,
    onOpenExisting: (QrCreationItem) -> Unit,
    onQuickFullscreen: (QrCreationItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenBatch: () -> Unit,
    onOpenCreatePicker: () -> Unit,
    onImportQr: () -> Unit = {}
) {
    val colors = QraftThemeTokens.colors
    val filteredItems = remember(items, selectedCategory) {
        if (selectedCategory == null) items else items.filter { it.type == selectedCategory }
    }

    val greeting = remember(creatorName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val prefix = when {
            hour < 12 -> "Morning"
            hour < 18 -> "Afternoon"
            else -> "Evening"
        }
        "$prefix, $creatorName"
    }

    val formattedDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 14.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Header ("Morning, Creator" + date + theme/profile buttons)
            item {
                StaggeredReveal(index = 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.displaySmall,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$formattedDate • ${items.size} QR codes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Light / Dark / System quick toggle pill
                            Surface(
                                shape = CircleShape,
                                color = colors.surface,
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier
                                    .size(44.dp)
                                    .appleBounceClick { onCycleTheme() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    AnimatedContent(
                                        targetState = themeMode,
                                        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                                        label = "themeIcon"
                                    ) { mode ->
                                        Icon(
                                            imageVector = when (mode) {
                                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                                                ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                            },
                                            contentDescription = "Switch theme",
                                            tint = colors.espressoAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Avatar / Studio Settings Circle (matching top-right circle in screenshot)
                            Surface(
                                shape = CircleShape,
                                color = colors.heroBannerBg,
                                border = BorderStroke(1.5.dp, colors.cardBorder),
                                modifier = Modifier
                                    .size(48.dp)
                                    .appleBounceClick { onOpenSettings() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCode2,
                                        contentDescription = "Studio Preferences",
                                        tint = colors.espressoAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Responsive 7-Circle Category Strip (mirrors Mon 7 .. Sun 13 evenly with zero edge clipping)
            item {
                StaggeredReveal(index = 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        homeStripOptions.forEach { option ->
                            val selected = selectedCategory == option.type
                            val circleBg by animateColorAsState(
                                targetValue = if (selected) colors.activePillBg else colors.surface,
                                animationSpec = AppleSprings.snappy(),
                                label = "stripBg"
                            )
                            val circleFg by animateColorAsState(
                                targetValue = if (selected) colors.activePillContent else colors.textPrimary,
                                animationSpec = AppleSprings.snappy(),
                                label = "stripFg"
                            )
                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.06f else 1f,
                                animationSpec = AppleSprings.TactileBounce,
                                label = "stripScale"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .appleBounceClick {
                                        onSelectCategory(
                                            if (selected && option.type != null) null else option.type
                                        )
                                    }
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) colors.textPrimary else colors.textSecondary,
                                    maxLines = 1
                                )
                                Box(
                                    modifier = Modifier
                                        .sizeIn(minWidth = 38.dp, minHeight = 38.dp, maxWidth = 46.dp, maxHeight = 46.dp)
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(circleBg)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) colors.activePillBg else colors.cardBorder,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = option.type?.let { iconForContentType(it) } ?: Icons.Rounded.GridView,
                                        contentDescription = option.label,
                                        tint = circleFg,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Peach Spotlight Hero Card (mirrors "Set the reminder" card!)
            item {
                StaggeredReveal(index = 2) {
                    val infinite = rememberInfiniteTransition(label = "heroFloat")
                    val floatY by infinite.animateFloat(
                        initialValue = -3f,
                        targetValue = 3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "heroFloatY"
                    )
                    val targetType = selectedCategory ?: QrContentType.URL

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.heroBannerBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleBounceClick(scalePressed = 0.985f) { onOpenStudioNew(targetType) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (selectedCategory == null) {
                                        "Craft custom QR"
                                    } else {
                                        "Create ${selectedCategory.title}"
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontSize = 20.sp,
                                    color = colors.heroBannerTitle
                                )
                                Text(
                                    text = "Custom shapes, gradients, logos, frames & vector SVG export",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.heroBannerBody,
                                    lineHeight = 18.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(colors.espressoAccent)
                                        .appleBounceClick { onOpenStudioNew(targetType) }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Open Studio",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.espressoAccentContent
                                    )
                                }
                            }

                            HeroQrArtworkIllustration(
                                modifier = Modifier
                                    .size(104.dp)
                                    .graphicsLayer { translationY = floatY }
                            )
                        }
                    }
                }
            }

            // 4. Section Header ("Recent creations" + "Batch" + "See all")
            item {
                StaggeredReveal(index = 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCategory == null) "Recents" else selectedCategory.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Remix",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.espressoAccent,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.heroBannerBg)
                                    .appleBounceClick { onImportQr() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                            Text(
                                text = "Batch",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textSecondary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.surfaceMuted)
                                    .appleBounceClick { onOpenBatch() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                            Text(
                                text = "See all",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.espressoAccent,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .appleBounceClick { onOpenLibrary() }
                                    .padding(start = 2.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            // 5. Timeline List Cards (mirrors the Daily routine cards!)
            if (filteredItems.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = selectedCategory?.let { iconForContentType(it) } ?: Icons.Rounded.QrCode2,
                                contentDescription = null,
                                tint = colors.espressoAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No ${selectedCategory?.title ?: "QR"} cards yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Tap + or Create below to design your first custom code.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                            Button(
                                onClick = { onOpenStudioNew(selectedCategory ?: QrContentType.URL) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.espressoAccent)
                            ) {
                                Text("Create Now")
                            }
                        }
                    }
                }
            } else {
                val visibleItems = filteredItems.take(8)
                itemsIndexed(
                    items = visibleItems,
                    key = { _, item -> item.id }
                ) { index, item ->
                    StaggeredReveal(index = index + 4, key = item.id) {
                        QrTimelineRowCard(
                            item = item,
                            isFirst = index == 0,
                            isLast = index == visibleItems.lastIndex,
                            onCardClick = { onOpenExisting(item) },
                            onDotClick = { onToggleFavorite(item.id) },
                            onQuickPresent = { onQuickFullscreen(item) }
                        )
                    }
                }
            }
        }

        // 6. Deep Espresso Circular '+' FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 22.dp, bottom = 22.dp)
                .size(62.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = colors.espressoAccent,
                    spotColor = colors.espressoAccent
                )
                .clip(CircleShape)
                .background(colors.espressoAccent)
                .appleBounceClick(scalePressed = 0.91f) { onOpenCreatePicker() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Create New QR Code",
                tint = colors.espressoAccentContent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun QrTimelineRowCard(
    item: QrCreationItem,
    isFirst: Boolean,
    isLast: Boolean,
    onCardClick: () -> Unit,
    onDotClick: () -> Unit,
    onQuickPresent: () -> Unit
) {
    val colors = QraftThemeTokens.colors
    val pastel = colors.pastelAt(item.type.pastelIndex)
    val subtitle = remember(item.type, item.payload, item.design) {
        "${item.design.pixelStyle.label} • ${QrPayloadEncoder.summarySubtitle(item.type, item.payload)}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Timeline Dotted Rail + Status Dot (exact match to screenshot!)
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            val lineColor = colors.timelineLine
            Canvas(modifier = Modifier.fillMaxSize()) {
                val x = size.width / 2f
                val topY = if (isFirst) size.height / 2f else 0f
                val bottomY = if (isLast) size.height / 2f else size.height
                drawLine(
                    color = lineColor,
                    start = Offset(x, topY),
                    end = Offset(x, bottomY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }

            // Orange filled checkmark circle for favorites/verified, clean outline otherwise
            val dotFilled = item.isFavorite
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (dotFilled) colors.timelineDotActive else colors.background)
                    .border(
                        width = 1.6.dp,
                        color = if (dotFilled) colors.timelineDotActive else colors.textTertiary,
                        shape = CircleShape
                    )
                    .appleBounceClick { onDotClick() },
                contentAlignment = Alignment.Center
            ) {
                if (dotFilled) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Favorite",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // White Elevated Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier
                .weight(1f)
                .appleBounceClick { onCardClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pastel Icon Container
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pastel.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForContentType(item.type),
                        contentDescription = item.type.title,
                        tint = pastel.content,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Title + Subtitle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right Metadata + Quick Scan Present (mirrors clock + '5 min' column!)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .appleBounceClick { onQuickPresent() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = "Quick Present",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = item.type.shortCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}
