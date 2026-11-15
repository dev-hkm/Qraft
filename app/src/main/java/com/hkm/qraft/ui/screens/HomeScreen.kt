package com.hkm.qraft.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.qraft.model.CodeKind
import com.hkm.qraft.model.QrContentType
import com.hkm.qraft.model.QrCreationItem
import com.hkm.qraft.model.ThemeMode
import com.hkm.qraft.qr.QrPayloadEncoder
import com.hkm.qraft.ui.components.HeroQrArtworkIllustration
import com.hkm.qraft.ui.components.QraftHeaderCircleButton
import com.hkm.qraft.ui.components.QraftPillButton
import com.hkm.qraft.ui.components.QraftButtonVariant
import com.hkm.qraft.ui.components.iconForContentType
import com.hkm.qraft.ui.theme.AppMotion
import com.hkm.qraft.ui.theme.AppleGlassSurface
import com.hkm.qraft.ui.theme.AppleGlassVariant
import com.hkm.qraft.ui.theme.AppleOdometerNumber
import com.hkm.qraft.ui.theme.AppleProgressiveEdgeFrost
import com.hkm.qraft.ui.theme.EdgeFrostDirection
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleEntrance
import com.hkm.qraft.ui.theme.appleHazeSource
import com.hkm.qraft.ui.theme.applePressable
import com.hkm.qraft.ui.theme.heroSource
import com.hkm.qraft.ui.theme.rememberReduceMotion
import dev.chrisbanes.haze.rememberHazeState
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

private val AmbientCosineEasing = CubicBezierEasing(0.45f, 0.05f, 0.55f, 0.95f)

@Composable
fun HomeScreen(
    creatorName: String,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    items: List<QrCreationItem>,
    selectedCategory: QrContentType?,
    onSelectCategory: (QrContentType?) -> Unit,
    onCycleTheme: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenStudioNew: (QrContentType) -> Unit,
    onOpenStudioNewBarcode: () -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onOpenExisting: (QrCreationItem) -> Unit,
    onQuickFullscreen: (QrCreationItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenBatch: () -> Unit,
    onOpenCreatePicker: () -> Unit,
    onImportQr: () -> Unit = {}
) {
    val colors = QraftThemeTokens.colors
    val reduceMotion = rememberReduceMotion()
    val hazeState = rememberHazeState()
    val listState = rememberLazyListState()

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 36
        }
    }

    val activeItems = remember(items) { items.filterNot { it.isArchived } }
    val filteredItems = remember(activeItems, selectedCategory) {
        if (selectedCategory == null) activeItems else activeItems.filter { it.type == selectedCategory }
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
    ) {
        val navBottomPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .appleHazeSource(hazeState),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = navBottomPad + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Scroll-Linked Header ("Morning, Creator" + Odometer count + Glass utility pills)
            item(key = "home_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 6.dp)
                        .graphicsLayer {
                            if (!reduceMotion && listState.firstVisibleItemIndex == 0) {
                                val off = listState.firstVisibleItemScrollOffset.toFloat()
                                val frac = (off / 160f).coerceIn(0f, 1f)
                                alpha = 1f - frac * 0.45f
                                translationY = -off * 0.14f
                            }
                        }
                        .appleEntrance(0, reduceMotion = reduceMotion),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$formattedDate • ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            AppleOdometerNumber(
                                value = activeItems.size,
                                suffix = if (activeItems.size == 1) " code" else " codes",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QraftHeaderCircleButton(
                            icon = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Live Scanner",
                            size = 42.dp,
                            onClick = onOpenScanner,
                            modifier = Modifier.heroSource("hero_scanner", cornerRadiusDp = 21f)
                        )
                        QraftHeaderCircleButton(
                            icon = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            size = 42.dp,
                            onClick = onOpenSettings,
                            modifier = Modifier.heroSource("hero_settings", cornerRadiusDp = 21f)
                        )
                    }
                }
            }

            // 2. Responsive 7-Circle Category Strip (fluid spring without tacky bounce)
            item(key = "home_strip") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleEntrance(1, reduceMotion = reduceMotion),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    homeStripOptions.forEach { option ->
                        val selected = selectedCategory == option.type
                        val circleBg by animateColorAsState(
                            targetValue = if (selected) colors.activePillBg else colors.surface,
                            animationSpec = AppMotion.snappySpring(),
                            label = "stripBg"
                        )
                        val circleFg by animateColorAsState(
                            targetValue = if (selected) colors.activePillContent else colors.textPrimary,
                            animationSpec = AppMotion.snappySpring(),
                            label = "stripFg"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.045f else 1f,
                            animationSpec = AppMotion.fluidSpring(),
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
                                .applePressable(pressedScale = 0.94f) {
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

            // 3. Spotlight Hero Card with ambient cosine float + Hero Source Morph bounds
            item(key = "home_hero") {
                val floatY by if (reduceMotion) {
                    remember { mutableFloatStateOf(0f) }
                } else {
                    val infinite = rememberInfiniteTransition(label = "heroFloat")
                    infinite.animateFloat(
                        initialValue = -2.5f,
                        targetValue = 2.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2400, easing = AmbientCosineEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "heroFloatY"
                    )
                }
                val targetType = selectedCategory ?: QrContentType.URL

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = colors.heroBannerBg,
                    border = BorderStroke(1.dp, colors.cardBorder.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleEntrance(2, reduceMotion = reduceMotion)
                        .heroSource("hero_type_${targetType.name}", cornerRadiusDp = 24f)
                        .applePressable(pressedScale = 0.985f) { onOpenStudioNew(targetType) }
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
                                    "Craft QR & Barcodes"
                                } else {
                                    "Create ${selectedCategory.title}"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = 20.sp,
                                color = colors.heroBannerTitle
                            )
                            Text(
                                text = "Custom shapes, logos, optional frames, 8K UHD & vector SVG",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.heroBannerBody,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(colors.espressoAccent)
                                        .applePressable { onOpenStudioNew(targetType) }
                                        .padding(horizontal = 15.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Studio",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.espressoAccentContent,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 40.dp)
                                        .heroSource("hero_barcode", cornerRadiusDp = 50f)
                                        .clip(RoundedCornerShape(50))
                                        .background(colors.surface)
                                        .border(1.dp, colors.cardBorder, RoundedCornerShape(50))
                                        .applePressable { onOpenStudioNewBarcode() }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Barcode",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        HeroQrArtworkIllustration(
                            modifier = Modifier
                                .size(92.dp)
                                .graphicsLayer { translationY = floatY.dp.toPx() * 0.4f }
                        )
                    }
                }
            }

            // 4. Recents Header ("Recents" + Remix / Batch / See all)
            item(key = "home_recents_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleEntrance(3, reduceMotion = reduceMotion),
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
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.heroBannerBg)
                                .applePressable { onImportQr() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Remix",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.espressoAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.surfaceMuted)
                                .applePressable { onOpenBatch() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Batch",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .heroSource("hero_library", cornerRadiusDp = 12f)
                                .clip(RoundedCornerShape(10.dp))
                                .applePressable { onOpenLibrary() }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "See all",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.espressoAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 5. Timeline List Cards (Clean non-glass elevated cards + Hero Bounds Morph + Item flow compaction)
            if (filteredItems.isEmpty()) {
                item(key = "home_empty") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleEntrance(4, reduceMotion = reduceMotion)
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
                            QraftPillButton(
                                text = "Create Now",
                                variant = QraftButtonVariant.PRIMARY,
                                height = 44.dp,
                                onClick = { onOpenStudioNew(selectedCategory ?: QrContentType.URL) }
                            )
                        }
                    }
                }
            } else {
                val visibleItems = filteredItems.take(8)
                itemsIndexed(
                    items = visibleItems,
                    key = { _, item -> item.id }
                ) { index, item ->
                    Box(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = AppMotion.snappySpring(),
                                placementSpec = AppMotion.layoutSpring()
                            )
                            .appleEntrance(index + 4, reduceMotion = reduceMotion)
                    ) {
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

        // Progressive Frosted Top Scrim + Sticky Compact Chrome when scrolled
        AppleProgressiveEdgeFrost(
            hazeState = hazeState,
            direction = EdgeFrostDirection.Top,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = statusTop + 18.dp)
            ) {
                AnimatedVisibility(
                    visible = isScrolled,
                    enter = AppMotion.floatingBarEnter,
                    exit = AppMotion.floatingBarExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppleGlassSurface(
                            shape = RoundedCornerShape(50),
                            cornerRadiusDp = 50.dp,
                            variant = AppleGlassVariant.Regular,
                            hazeState = hazeState,
                            enableRefraction = true
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Qraft",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                AppleOdometerNumber(
                                    value = activeItems.size,
                                    suffix = " codes",
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            QraftHeaderCircleButton(
                                icon = Icons.Rounded.QrCodeScanner,
                                contentDescription = "Live Scanner",
                                size = 40.dp,
                                hazeState = hazeState,
                                onClick = onOpenScanner
                            )
                            QraftHeaderCircleButton(
                                icon = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                size = 40.dp,
                                hazeState = hazeState,
                                onClick = onOpenSettings
                            )
                        }
                    }
                }
            }
        }

        // 6. Apple Liquid Glass Floating '+' Action Disc (concentric 31.dp outer / 26.dp inner)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 22.dp, bottom = 22.dp)
                .heroSource("hero_create", cornerRadiusDp = 31f)
                .applePressable(pressedScale = 0.90f) { onOpenCreatePicker() },
            contentAlignment = Alignment.Center
        ) {
            AppleGlassSurface(
                modifier = Modifier.size(62.dp),
                shape = CircleShape,
                cornerRadiusDp = 31.dp,
                variant = AppleGlassVariant.Regular,
                hazeState = hazeState,
                enableRefraction = true,
                elevation = 12.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(colors.espressoAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Create New Code",
                        tint = colors.espressoAccentContent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
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
    val isBarcode = item.resolvedCodeKind() == CodeKind.BARCODE
    val barcodeSpec = if (isBarcode) item.resolvedBarcode() else null
    val subtitle = remember(item.type, item.payload, item.design, item.codeKind, item.barcode) {
        if (barcodeSpec != null) {
            "${barcodeSpec.format.label} • ${barcodeSpec.data}"
        } else {
            "${item.design.pixelStyle.label} • ${QrPayloadEncoder.summarySubtitle(item.type, item.payload)}"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Timeline Dotted Rail + Status Dot (clean spring transition)
        Box(
            modifier = Modifier
                .width(30.dp)
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

            val dotFilled = item.isFavorite
            val dotBg by animateColorAsState(
                targetValue = if (dotFilled) colors.timelineDotActive else colors.background,
                animationSpec = AppMotion.snappySpring(),
                label = "timelineDotBg"
            )
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    .applePressable(pressedScale = 0.85f) { onDotClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(dotBg)
                        .border(
                            width = 1.6.dp,
                            color = if (dotFilled) colors.timelineDotActive else colors.textTertiary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (dotFilled) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Favorite",
                            tint = colors.espressoAccentContent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Clean Elevated Card with Hero Source Morph bounds (20.dp outer -> 14.dp inner)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier
                .weight(1f)
                .heroSource("hero_card_${item.id}", cornerRadiusDp = 20f)
                .applePressable { onCardClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pastel.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBarcode) Icons.Rounded.ViewWeek else iconForContentType(item.type),
                        contentDescription = if (isBarcode) "Barcode" else item.type.title,
                        tint = pastel.content,
                        modifier = Modifier.size(22.dp)
                    )
                }

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

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .applePressable(pressedScale = 0.92f) { onQuickPresent() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isBarcode) Icons.Rounded.ViewWeek else Icons.Rounded.QrCode2,
                        contentDescription = "Quick Present",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = barcodeSpec?.format?.shortCode ?: item.type.shortCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}
