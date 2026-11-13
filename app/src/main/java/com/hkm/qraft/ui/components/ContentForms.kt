package com.hkm.qraft.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.qraft.model.*
import com.hkm.qraft.qr.QrPayloadEncoder
import com.hkm.qraft.ui.theme.QraftThemeTokens
import com.hkm.qraft.ui.theme.appleBounceClick

fun iconForContentType(type: QrContentType): ImageVector = when (type) {
    QrContentType.URL -> Icons.Rounded.Language
    QrContentType.WIFI -> Icons.Rounded.Wifi
    QrContentType.VCARD -> Icons.Rounded.ContactPage
    QrContentType.TEXT -> Icons.Rounded.TextFields
    QrContentType.EMAIL -> Icons.Rounded.Email
    QrContentType.PHONE -> Icons.Rounded.Phone
    QrContentType.SMS -> Icons.Rounded.Sms
    QrContentType.GEO -> Icons.Rounded.LocationOn
    QrContentType.EVENT -> Icons.Rounded.Event
    QrContentType.SOCIAL -> Icons.Rounded.Share
    QrContentType.PAYMENT -> Icons.Rounded.Payments
    QrContentType.APP_LINK -> Icons.Rounded.Apps
}

@Composable
fun ContentEditorTab(
    title: String,
    type: QrContentType,
    fields: QrPayloadFields,
    encodedPayload: String,
    onTitleChange: (String) -> Unit,
    onTypeChange: (QrContentType) -> Unit,
    onFieldsChange: (QrPayloadFields) -> Unit
) {
    val colors = QraftThemeTokens.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // QR Type horizontal pill selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("QR Code Type")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QrContentType.entries.forEach { item ->
                    val selected = item == type
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) colors.activePillBg else colors.surface)
                            .border(
                                1.dp,
                                if (selected) colors.activePillBg else colors.cardBorder,
                                RoundedCornerShape(50)
                            )
                            .appleBounceClick { onTypeChange(item) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = iconForContentType(item),
                            contentDescription = null,
                            tint = if (selected) colors.activePillContent else colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) colors.activePillContent else colors.textPrimary
                        )
                    }
                }
            }
        }

        // Friendly title
        QraftTextField(
            value = title,
            onValueChange = onTitleChange,
            label = "Card Label (for your library)",
            placeholder = QrPayloadEncoder.defaultTitle(type, fields),
            leadingIcon = Icons.AutoMirrored.Rounded.Label
        )

        // Dynamic content fields
        AnimatedContent(
            targetState = type,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "typeForm"
        ) { activeType ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (activeType) {
                    QrContentType.URL -> {
                        QraftTextField(
                            value = fields.url,
                            onValueChange = { onFieldsChange(fields.copy(url = it)) },
                            label = "Website URL",
                            placeholder = "https://your-site.com",
                            leadingIcon = Icons.Rounded.Link,
                            keyboardType = KeyboardType.Uri
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf("https://", "https://www.", "http://").forEach { prefix ->
                                SmallAssistChip(prefix) {
                                    val clean = fields.url
                                        .removePrefix("https://www.")
                                        .removePrefix("https://")
                                        .removePrefix("http://")
                                    onFieldsChange(fields.copy(url = prefix + clean))
                                }
                            }
                        }
                    }

                    QrContentType.TEXT -> {
                        QraftTextField(
                            value = fields.text,
                            onValueChange = { onFieldsChange(fields.copy(text = it)) },
                            label = "Text / Note Content",
                            placeholder = "Write any message, coupon code, or note…",
                            singleLine = false,
                            minLines = 3,
                            maxLines = 6
                        )
                        Text(
                            text = "${fields.text.length} characters • UTF-8 supported",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }

                    QrContentType.WIFI -> {
                        QraftTextField(
                            value = fields.wifiSsid,
                            onValueChange = { onFieldsChange(fields.copy(wifiSsid = it)) },
                            label = "Network Name (SSID)",
                            placeholder = "Studio_Guest_5G",
                            leadingIcon = Icons.Rounded.Wifi
                        )
                        var showPass by remember { mutableStateOf(false) }
                        if (fields.wifiSecurity != WifiSecurity.NONE) {
                            QraftTextField(
                                value = fields.wifiPassword,
                                onValueChange = { onFieldsChange(fields.copy(wifiPassword = it)) },
                                label = "Wi-Fi Password",
                                placeholder = "Enter password",
                                leadingIcon = Icons.Rounded.Lock,
                                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPass = !showPass }) {
                                        Icon(
                                            if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = "Toggle password"
                                        )
                                    }
                                }
                            )
                        }
                        SectionLabel("Security Protocol")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WifiSecurity.entries.forEach { sec ->
                                SelectableChip(
                                    label = sec.label.substringBefore(" "),
                                    selected = fields.wifiSecurity == sec,
                                    onClick = { onFieldsChange(fields.copy(wifiSecurity = sec)) }
                                )
                            }
                        }
                        ToggleRow(
                            title = "Hidden Network",
                            subtitle = "Enable if SSID is not broadcast publicly",
                            checked = fields.wifiHidden,
                            onCheckedChange = { onFieldsChange(fields.copy(wifiHidden = it)) }
                        )
                    }

                    QrContentType.VCARD -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QraftTextField(
                                value = fields.firstName,
                                onValueChange = { onFieldsChange(fields.copy(firstName = it)) },
                                label = "First Name",
                                placeholder = "Alex",
                                modifier = Modifier.weight(1f)
                            )
                            QraftTextField(
                                value = fields.lastName,
                                onValueChange = { onFieldsChange(fields.copy(lastName = it)) },
                                label = "Last Name",
                                placeholder = "Nguyen",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QraftTextField(
                                value = fields.organization,
                                onValueChange = { onFieldsChange(fields.copy(organization = it)) },
                                label = "Company",
                                placeholder = "Qraft Studio",
                                modifier = Modifier.weight(1f)
                            )
                            QraftTextField(
                                value = fields.jobTitle,
                                onValueChange = { onFieldsChange(fields.copy(jobTitle = it)) },
                                label = "Role / Title",
                                placeholder = "Designer",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        QraftTextField(
                            value = fields.phone,
                            onValueChange = { onFieldsChange(fields.copy(phone = it)) },
                            label = "Mobile Phone",
                            placeholder = "+84 901 234 567",
                            leadingIcon = Icons.Rounded.Phone,
                            keyboardType = KeyboardType.Phone
                        )
                        QraftTextField(
                            value = fields.email,
                            onValueChange = { onFieldsChange(fields.copy(email = it)) },
                            label = "Email Address",
                            placeholder = "hello@studio.com",
                            leadingIcon = Icons.Rounded.Email,
                            keyboardType = KeyboardType.Email
                        )
                        QraftTextField(
                            value = fields.website,
                            onValueChange = { onFieldsChange(fields.copy(website = it)) },
                            label = "Website",
                            placeholder = "https://studio.com",
                            leadingIcon = Icons.Rounded.Language,
                            keyboardType = KeyboardType.Uri
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QraftTextField(
                                value = fields.street,
                                onValueChange = { onFieldsChange(fields.copy(street = it)) },
                                label = "Street",
                                placeholder = "123 Nguyen Hue",
                                modifier = Modifier.weight(1f)
                            )
                            QraftTextField(
                                value = fields.city,
                                onValueChange = { onFieldsChange(fields.copy(city = it)) },
                                label = "City",
                                placeholder = "Ho Chi Minh",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        QraftTextField(
                            value = fields.note,
                            onValueChange = { onFieldsChange(fields.copy(note = it)) },
                            label = "Note",
                            placeholder = "Optional card note"
                        )
                    }

                    QrContentType.EMAIL -> {
                        QraftTextField(
                            value = fields.emailTo,
                            onValueChange = { onFieldsChange(fields.copy(emailTo = it)) },
                            label = "Recipient Email",
                            placeholder = "contact@company.com",
                            leadingIcon = Icons.Rounded.Email,
                            keyboardType = KeyboardType.Email
                        )
                        QraftTextField(
                            value = fields.emailSubject,
                            onValueChange = { onFieldsChange(fields.copy(emailSubject = it)) },
                            label = "Subject Line",
                            placeholder = "Project Inquiry"
                        )
                        QraftTextField(
                            value = fields.emailBody,
                            onValueChange = { onFieldsChange(fields.copy(emailBody = it)) },
                            label = "Email Message",
                            placeholder = "Hi there, I scanned your QR code…",
                            singleLine = false,
                            minLines = 3,
                            maxLines = 5
                        )
                    }

                    QrContentType.PHONE -> {
                        QraftTextField(
                            value = fields.phoneNumber,
                            onValueChange = { onFieldsChange(fields.copy(phoneNumber = it)) },
                            label = "Phone Number",
                            placeholder = "+84 901 234 567",
                            leadingIcon = Icons.Rounded.Phone,
                            keyboardType = KeyboardType.Phone
                        )
                    }

                    QrContentType.SMS -> {
                        QraftTextField(
                            value = fields.smsNumber,
                            onValueChange = { onFieldsChange(fields.copy(smsNumber = it)) },
                            label = "Phone Number",
                            placeholder = "+84 901 234 567",
                            leadingIcon = Icons.Rounded.Sms,
                            keyboardType = KeyboardType.Phone
                        )
                        QraftTextField(
                            value = fields.smsMessage,
                            onValueChange = { onFieldsChange(fields.copy(smsMessage = it)) },
                            label = "Pre-filled Message",
                            placeholder = "Hey! Let's connect.",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4
                        )
                    }

                    QrContentType.GEO -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QraftTextField(
                                value = fields.latitude,
                                onValueChange = { onFieldsChange(fields.copy(latitude = it)) },
                                label = "Latitude",
                                placeholder = "10.7769",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                            QraftTextField(
                                value = fields.longitude,
                                onValueChange = { onFieldsChange(fields.copy(longitude = it)) },
                                label = "Longitude",
                                placeholder = "106.7009",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        QraftTextField(
                            value = fields.geoLabel,
                            onValueChange = { onFieldsChange(fields.copy(geoLabel = it)) },
                            label = "Place Label (Optional)",
                            placeholder = "Qraft Flagship Studio",
                            leadingIcon = Icons.Rounded.LocationOn
                        )
                    }

                    QrContentType.EVENT -> {
                        QraftTextField(
                            value = fields.eventTitle,
                            onValueChange = { onFieldsChange(fields.copy(eventTitle = it)) },
                            label = "Event Title",
                            placeholder = "Product Launch Party",
                            leadingIcon = Icons.Rounded.Event
                        )
                        QraftTextField(
                            value = fields.eventLocation,
                            onValueChange = { onFieldsChange(fields.copy(eventLocation = it)) },
                            label = "Location / Room",
                            placeholder = "Studio Hall A",
                            leadingIcon = Icons.Rounded.LocationOn
                        )
                        QraftTextField(
                            value = fields.eventStart,
                            onValueChange = { onFieldsChange(fields.copy(eventStart = it)) },
                            label = "Start (YYYYMMDDTHHMMSS)",
                            placeholder = "20261120T090000",
                            leadingIcon = Icons.Rounded.Schedule
                        )
                        QraftTextField(
                            value = fields.eventEnd,
                            onValueChange = { onFieldsChange(fields.copy(eventEnd = it)) },
                            label = "End (YYYYMMDDTHHMMSS)",
                            placeholder = "20261120T110000",
                            leadingIcon = Icons.Rounded.EventAvailable
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmallAssistChip("+1h Meeting") {
                                val fmt = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss", java.util.Locale.US)
                                val cal = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    add(java.util.Calendar.HOUR_OF_DAY, 1)
                                }
                                val start = fmt.format(cal.time)
                                cal.add(java.util.Calendar.HOUR_OF_DAY, 1)
                                val end = fmt.format(cal.time)
                                onFieldsChange(fields.copy(eventStart = start, eventEnd = end))
                            }
                            SmallAssistChip("Tomorrow 9 AM") {
                                val fmt = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss", java.util.Locale.US)
                                val cal = java.util.Calendar.getInstance().apply {
                                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                                    set(java.util.Calendar.HOUR_OF_DAY, 9)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                }
                                val start = fmt.format(cal.time)
                                cal.add(java.util.Calendar.HOUR_OF_DAY, 2)
                                val end = fmt.format(cal.time)
                                onFieldsChange(fields.copy(eventStart = start, eventEnd = end))
                            }
                            SmallAssistChip("All Day") {
                                val fmt = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                                val today = fmt.format(java.util.Date())
                                onFieldsChange(fields.copy(eventStart = "${today}T000000", eventEnd = "${today}T235900"))
                            }
                        }
                        QraftTextField(
                            value = fields.eventDescription,
                            onValueChange = { onFieldsChange(fields.copy(eventDescription = it)) },
                            label = "Description",
                            placeholder = "Agenda or notes…",
                            singleLine = false,
                            minLines = 2
                        )
                    }

                    QrContentType.SOCIAL -> {
                        SectionLabel("Platform")
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SocialPlatform.entries.forEach { platform ->
                                SelectableChip(
                                    label = platform.label,
                                    selected = fields.socialPlatform == platform,
                                    onClick = { onFieldsChange(fields.copy(socialPlatform = platform)) }
                                )
                            }
                        }
                        QraftTextField(
                            value = fields.socialHandle,
                            onValueChange = { onFieldsChange(fields.copy(socialHandle = it)) },
                            label = "${fields.socialPlatform.label} Username or Full URL",
                            placeholder = fields.socialPlatform.placeholder,
                            leadingIcon = Icons.Rounded.AlternateEmail
                        )
                    }

                    QrContentType.PAYMENT -> {
                        SectionLabel("Payment Network")
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaymentMethod.entries.forEach { method ->
                                SelectableChip(
                                    label = method.label,
                                    selected = fields.paymentMethod == method,
                                    onClick = { onFieldsChange(fields.copy(paymentMethod = method)) }
                                )
                            }
                        }
                        QraftTextField(
                            value = fields.paymentTarget,
                            onValueChange = { onFieldsChange(fields.copy(paymentTarget = it)) },
                            label = "Wallet / Handle / Payment URI",
                            placeholder = fields.paymentMethod.hint,
                            leadingIcon = Icons.Rounded.AccountBalanceWallet
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QraftTextField(
                                value = fields.paymentAmount,
                                onValueChange = { onFieldsChange(fields.copy(paymentAmount = it)) },
                                label = "Amount (Optional)",
                                placeholder = "25.00",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                            QraftTextField(
                                value = fields.paymentNote,
                                onValueChange = { onFieldsChange(fields.copy(paymentNote = it)) },
                                label = "Memo / Note",
                                placeholder = "Coffee tip",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    QrContentType.APP_LINK -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppPlatform.entries.forEach { plat ->
                                SelectableChip(
                                    label = plat.label,
                                    selected = fields.appPlatform == plat,
                                    onClick = { onFieldsChange(fields.copy(appPlatform = plat)) }
                                )
                            }
                        }
                        QraftTextField(
                            value = fields.appTarget,
                            onValueChange = { onFieldsChange(fields.copy(appTarget = it)) },
                            label = "App Package / Store ID / URI",
                            placeholder = fields.appPlatform.hint,
                            leadingIcon = Icons.Rounded.Apps
                        )
                    }
                }
            }
        }

        // Encoded Raw Payload preview card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceMuted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ENCODED PAYLOAD (${encodedPayload.length} bytes)",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = encodedPayload,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = colors.textPrimary,
                    maxLines = 4
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = QraftThemeTokens.colors.textPrimary,
        modifier = modifier
    )
}

@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QraftThemeTokens.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.activePillBg else colors.surface)
            .border(
                1.dp,
                if (selected) colors.activePillBg else colors.cardBorder,
                RoundedCornerShape(50)
            )
            .appleBounceClick { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) colors.activePillContent else colors.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun SmallAssistChip(label: String, onClick: () -> Unit) {
    val colors = QraftThemeTokens.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceMuted)
            .appleBounceClick { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable
fun QraftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else maxOf(minLines, 4),
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val colors = QraftThemeTokens.colors
    val safeMaxLines = if (singleLine) 1 else maxOf(maxLines, minLines, 1)
    val safeMinLines = minLines.coerceIn(1, safeMaxLines)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = colors.textTertiary) },
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = colors.textSecondary) }
        },
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = safeMinLines,
        maxLines = safeMaxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.espressoAccent,
            unfocusedBorderColor = colors.cardBorder,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedLabelColor = colors.espressoAccent,
            unfocusedLabelColor = colors.textSecondary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = QraftThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .appleBounceClick { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontSize = 15.sp, color = colors.textPrimary)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontSize = 12.5.sp, color = colors.textSecondary)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.espressoAccentContent,
                    checkedTrackColor = colors.espressoAccent,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.surfaceMuted
                )
            )
        }
    }
}
