package com.hkm.qraft.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.qraft.data.UserPreferences
import com.hkm.qraft.model.ExportImageFormat
import com.hkm.qraft.model.ThemeMode
import com.hkm.qraft.ui.components.QraftButtonVariant
import com.hkm.qraft.ui.components.QraftHeaderCircleButton
import com.hkm.qraft.ui.components.QraftModalSheet
import com.hkm.qraft.ui.components.QraftPillButton
import com.hkm.qraft.ui.components.QraftSegmentedPicker
import com.hkm.qraft.ui.components.QraftStatusBarScrim
import com.hkm.qraft.ui.components.QraftTextField
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    savedCount: Int,
    archivedCount: Int,
    customPresetCount: Int,
    onBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onCreatorNameChange: (String) -> Unit,
    onAutoVerifyChange: (Boolean) -> Unit,
    onDefaultFormatChange: (ExportImageFormat) -> Unit,
    onDefaultResolutionChange: (Int) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onStatusMessage: (String) -> Unit = {}
) {
    val colors = QraftThemeTokens.colors
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showCreatorSheet by remember { mutableStateOf(false) }
    var showFormatSheet by remember { mutableStateOf(false) }

    val resolutionValues = remember { listOf(1024, 2048, 4096, 8192) }
    val resolutionLabels = remember { listOf("1K", "2K", "4K", "8K") }
    val selectedResIdx = resolutionValues.indexOfFirst { it == preferences.defaultExportResolution }.let {
        if (it >= 0) it else 2
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Tonser-style scrolling header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                QraftHeaderCircleButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    size = 42.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Appearance, studio defaults & library backup",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 1. Appearance & Theme Mode (Tonser SettingsCard + SegmentedPicker)
            Column(modifier = Modifier.fillMaxWidth()) {
                QraftSettingsSectionHeader("Preferences & Appearance")
                Spacer(Modifier.height(8.dp))
                QraftSettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceMuted,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (preferences.themeMode) {
                                            ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                                        },
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Theme Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = when (preferences.themeMode) {
                                        ThemeMode.SYSTEM -> "Follow system settings"
                                        ThemeMode.LIGHT -> "Always monochrome light"
                                        ThemeMode.DARK -> "Always studio dark"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        val selectedThemeIdx = when (preferences.themeMode) {
                            ThemeMode.SYSTEM -> 0
                            ThemeMode.LIGHT -> 1
                            ThemeMode.DARK -> 2
                        }
                        QraftSegmentedPicker(
                            options = listOf("System", "Light", "Dark"),
                            selectedIndex = selectedThemeIdx,
                            onSelect = { idx ->
                                val mode = when (idx) {
                                    0 -> ThemeMode.SYSTEM
                                    1 -> ThemeMode.LIGHT
                                    else -> ThemeMode.DARK
                                }
                                onThemeChange(mode)
                            }
                        )
                    }

                    QraftSettingsDivider()

                    QraftSettingsRow(
                        icon = Icons.Rounded.Person,
                        title = "Creator Greeting Name",
                        subtitle = preferences.creatorName.ifBlank { "Creator" },
                        onClick = { showCreatorSheet = true }
                    )
                }
            }

            // 2. Studio & Export Preferences
            Column(modifier = Modifier.fillMaxWidth()) {
                QraftSettingsSectionHeader("Studio Preferences")
                Spacer(Modifier.height(8.dp))
                QraftSettingsCard {
                    QraftSettingsRow(
                        icon = Icons.Rounded.VerifiedUser,
                        title = "Live Scannability Check",
                        subtitle = "Auto-verify QR readability with ZXing while customizing",
                        trailing = {
                            Switch(
                                checked = preferences.autoVerifyScannability,
                                onCheckedChange = onAutoVerifyChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.espressoAccentContent,
                                    checkedTrackColor = colors.espressoAccent,
                                    uncheckedThumbColor = colors.textSecondary,
                                    uncheckedTrackColor = colors.surfaceMuted
                                )
                            )
                        },
                        onClick = { onAutoVerifyChange(!preferences.autoVerifyScannability) }
                    )

                    QraftSettingsDivider()

                    QraftSettingsRow(
                        icon = Icons.Rounded.Image,
                        title = "Default Export Format",
                        subtitle = preferences.defaultExportFormat.label,
                        onClick = { showFormatSheet = true }
                    )

                    QraftSettingsDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceMuted,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.HighQuality,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Default Export Resolution",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Up to Ultra 8K (${preferences.defaultExportResolution}px)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        QraftSegmentedPicker(
                            options = resolutionLabels,
                            selectedIndex = selectedResIdx,
                            onSelect = { idx ->
                                onDefaultResolutionChange(resolutionValues[idx])
                            }
                        )
                    }
                }
            }

            // 3. Storage & Backup
            Column(modifier = Modifier.fillMaxWidth()) {
                QraftSettingsSectionHeader("Storage & Backup")
                Spacer(Modifier.height(8.dp))
                QraftSettingsCard {
                    QraftSettingsRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Vault Overview",
                        subtitle = "$savedCount active • $archivedCount archived • $customPresetCount saved presets",
                        trailing = null,
                        onClick = {}
                    )

                    QraftSettingsDivider()

                    QraftSettingsRow(
                        icon = Icons.Rounded.CloudUpload,
                        title = "Export Library Backup",
                        subtitle = "Save all QR cards, barcodes & style presets (.qraft)",
                        onClick = onExportBackup
                    )

                    QraftSettingsDivider()

                    QraftSettingsRow(
                        icon = Icons.Rounded.CloudDownload,
                        title = "Restore from Backup",
                        subtitle = "Import .qraft or .json backup file cleanly",
                        onClick = onImportBackup
                    )
                }
            }

            // 4. About Qraft
            Column(modifier = Modifier.fillMaxWidth()) {
                QraftSettingsSectionHeader("About Qraft")
                Spacer(Modifier.height(8.dp))
                QraftSettingsCard {
                    QraftSettingsRow(
                        icon = Icons.Rounded.Shield,
                        title = "100% Offline & Free Forever",
                        subtitle = "Zero ads, no accounts, no paywalls, full local 8K rendering",
                        trailing = null,
                        onClick = {}
                    )

                    QraftSettingsDivider()

                    QraftSettingsRow(
                        icon = Icons.Rounded.Code,
                        title = "Open Source • MIT License",
                        subtitle = "Built by Khanh Minh (dev-hkm) • github.com/dev-hkm",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/dev-hkm"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                onStatusMessage("No browser app found to open GitHub link")
                            } catch (_: Exception) {
                                onStatusMessage("Unable to open link")
                            }
                        }
                    )

                    QraftSettingsDivider()

                    QraftSettingsRow(
                        icon = Icons.Rounded.QrCode2,
                        title = "Qraft QR Studio",
                        subtitle = "Version 6.0.0 • Play Store Release Build",
                        trailing = null,
                        onClick = {}
                    )
                }
            }
        }

        QraftStatusBarScrim(modifier = Modifier.align(Alignment.TopCenter))
    }

    if (showCreatorSheet) {
        var nameDraft by remember(preferences.creatorName) { mutableStateOf(preferences.creatorName) }
        QraftModalSheet(
            onDismissRequest = { showCreatorSheet = false },
            maxHeightFraction = 0.62f,
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
                Text("Creator Name", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                QraftTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = "Greeting Name",
                    placeholder = "Creator",
                    leadingIcon = Icons.Rounded.Person
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
                        text = "Save",
                        variant = QraftButtonVariant.PRIMARY,
                        leadingIcon = Icons.Rounded.Check,
                        height = 46.dp,
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            onCreatorNameChange(nameDraft.trim().ifBlank { "Creator" })
                            dismissSheet()
                        }
                    )
                }
            }
        }
    }

    if (showFormatSheet) {
        QraftModalSheet(
            onDismissRequest = { showFormatSheet = false },
            maxHeightFraction = 0.72f,
            hardCap = 520.dp,
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Default Export Format",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Pre-selected whenever you open the Export sheet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surfaceMuted,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ExportImageFormat.entries.forEachIndexed { index, fmt ->
                            val selected = preferences.defaultExportFormat == fmt
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .appleBounceClick {
                                        onDefaultFormatChange(fmt)
                                        dismissSheet()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fmt.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 15.sp,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = ".${fmt.extension} • ${fmt.mimeType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = colors.espressoAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (index < ExportImageFormat.entries.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = colors.cardBorder.copy(alpha = 0.7f),
                                    thickness = 0.6.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QraftSettingsSectionHeader(title: String) {
    val colors = QraftThemeTokens.colors
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp
        ),
        color = colors.textSecondary,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
private fun QraftSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = QraftThemeTokens.colors
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun QraftSettingsDivider() {
    val colors = QraftThemeTokens.colors
    HorizontalDivider(
        color = colors.cardBorder.copy(alpha = 0.65f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(start = 70.dp, end = 16.dp)
    )
}

@Composable
private fun QraftSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = {
        val colors = QraftThemeTokens.colors
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = colors.textSecondary.copy(alpha = 0.65f),
            modifier = Modifier.size(20.dp)
        )
    },
    onClick: () -> Unit
) {
    val colors = QraftThemeTokens.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowAlpha = if (isPressed) 0.72f else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .graphicsLayer { alpha = rowAlpha }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceMuted,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 15.sp,
                color = colors.textPrimary
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}
