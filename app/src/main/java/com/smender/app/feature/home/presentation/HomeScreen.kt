package com.smender.app.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smender.core.domain.feature.home.domain.model.DeliverySummaryUiState
import com.smender.core.domain.feature.home.domain.model.HomeUiState
import com.smender.core.domain.feature.home.domain.model.PermissionStatus
import com.smender.core.domain.feature.home.domain.model.PermissionType
import com.smender.core.domain.feature.home.domain.model.PermissionUiState
import coil.compose.AsyncImage
import com.smender.app.R
import com.smender.app.ui.theme.Background
import com.smender.app.ui.theme.Gray100
import com.smender.app.ui.theme.Green500
import com.smender.app.ui.theme.AppBarBackground
import com.smender.app.ui.theme.PermissionChipErrorBackground
import com.smender.app.ui.theme.PermissionChipErrorText
import com.smender.app.ui.theme.PermissionChipSuccessBackground
import com.smender.app.ui.theme.PermissionChipSuccessText
import com.smender.app.ui.theme.SurfaceCard
import com.smender.app.ui.theme.SurfaceSubtle
import com.smender.app.ui.theme.TextPrimary
import com.smender.app.ui.theme.TextSecondary
import com.smender.app.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPermissionClick: (PermissionUiState) -> Unit,
    onSmsSentClick: () -> Unit,
    onSmsQueueClick: () -> Unit,
    onSmsErrorClick: () -> Unit,
    onCallsSentClick: () -> Unit,
    onCallsQueueClick: () -> Unit,
    onCallsErrorClick: () -> Unit,
    onTelegramSettingsClick: () -> Unit,
    telegramChatId: String,
    telegramBotName: String,
    telegramBotAvatarUrl: String
) {
    val sortedPermissions = uiState.permissions.sortedBy { it.isGranted }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBarBackground
                ),
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_app_avatar_toolbar),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                DeliverySummaryCard(
                    title = stringResource(R.string.home_sms_title),
                    summary = uiState.smsSummary,
                    onSentClick = onSmsSentClick,
                    onQueueClick = onSmsQueueClick,
                    onErrorClick = onSmsErrorClick
                )
            }
            item {
                DeliverySummaryCard(
                    title = stringResource(R.string.home_calls_title),
                    summary = uiState.callsSummary,
                    onSentClick = onCallsSentClick,
                    onQueueClick = onCallsQueueClick,
                    onErrorClick = onCallsErrorClick
                )
            }
            item {
                TelegramStatusWidget(
                    isConfigured = uiState.telegramConfigured,
                    chatId = telegramChatId,
                    botName = telegramBotName,
                    botAvatarUrl = telegramBotAvatarUrl,
                    onSettingsClick = onTelegramSettingsClick
                )
            }
            item {
                val isAllPermissionsGranted = uiState.permissions.all { it.isGranted }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.home_permissions), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    val grantedPermissions = uiState.permissions.count { it.isGranted }
                    val totalPermissions = uiState.permissions.size
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isAllPermissionsGranted) {
                                    PermissionChipSuccessBackground
                                } else {
                                    PermissionChipErrorBackground
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$grantedPermissions/$totalPermissions",
                            color = if (grantedPermissions < totalPermissions) {
                                PermissionChipErrorText
                            } else {
                                PermissionChipSuccessText
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            items(sortedPermissions) { permission ->
                PermissionCard(permission = permission, onClick = { onPermissionClick(permission) })
            }
            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
private fun TelegramStatusWidget(
    isConfigured: Boolean,
    chatId: String,
    botName: String,
    botAvatarUrl: String,
    onSettingsClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val borderBrush = if (isConfigured) {
        Brush.horizontalGradient(
            colors = if (isDarkTheme) {
                listOf(colorResource(R.color.emerald_400), colorResource(R.color.emerald_500))
            } else {
                listOf(colorResource(R.color.emerald_200), colorResource(R.color.green_200))
            }
        )
    } else {
        Brush.horizontalGradient(
            colors = if (isDarkTheme) {
                listOf(colorResource(R.color.red_400), colorResource(R.color.red_500))
            } else {
                listOf(colorResource(R.color.red_300), colorResource(R.color.red_400))
            }
        )
    }
    val indicatorTransition = rememberInfiniteTransition(label = "telegram-status-indicator")
    val indicatorAlpha by indicatorTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "telegram-status-indicator-alpha"
    )
    val widgetContainerColor = when {
        isConfigured -> colorResource(R.color.telegram_widget_configured_container)
        else -> colorResource(R.color.telegram_widget_unconfigured_container)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = widgetContainerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(colorResource(R.color.green_500), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (botAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = botAvatarUrl,
                                contentDescription = stringResource(R.string.telegram_avatar_description),
                                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = botName.takeIf { it.isNotBlank() } ?: stringResource(R.string.telegram_default_bot_name),
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isConfigured) {
                                stringResource(R.string.telegram_bot_active)
                            } else {
                                stringResource(R.string.telegram_bot_setup_required)
                            },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(SurfaceSubtle, RoundedCornerShape(8.dp))
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.telegram_settings_description),
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.telegram_chat_id_label, maskChatId(chatId, stringResource(R.string.not_set))),
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 38.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = SurfaceCard.copy(alpha = 0.9f), shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(indicatorAlpha)
                        .background(
                            color = if (isConfigured) colorResource(R.color.green_400) else colorResource(R.color.red_500),
                            shape = RoundedCornerShape(10.dp)
                        )
                )
                Text(
                    text = if (isConfigured) stringResource(R.string.connected) else stringResource(R.string.not_connected),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun maskChatId(chatId: String, notSetLabel: String): String {
    if (chatId.isBlank()) return notSetLabel
    return if (chatId.length <= 4) "•••••$chatId" else "•••••${chatId.takeLast(4)}"
}

@Composable
private fun DeliverySummaryCard(
    title: String,
    summary: DeliverySummaryUiState,
    onSentClick: () -> Unit,
    onQueueClick: () -> Unit,
    onErrorClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.status_sent),
                    value = summary.sent,
                    color = Green500,
                    onClick = onSentClick
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.status_pending),
                    value = summary.pending,
                    color = TextSecondary,
                    onClick = onQueueClick
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.status_error),
                    value = summary.error,
                    color = colorResource(R.color.red_600),
                    onClick = onErrorClick
                )
            }
        }
    }
}

@Composable
private fun SummaryBox(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSubtle)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = value.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = label, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PermissionCard(
    permission: PermissionUiState,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(permissionIconBackground(permission.status), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = permissionLeadingIcon(permission.type),
                        contentDescription = null,
                        tint = permissionIconTint(permission.status),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    val subtitleText = when {
                        permission.status == PermissionStatus.GRANTED -> permission.subtitle
                        permission.type == PermissionType.INTERNET -> permission.subtitle
                        else -> stringResource(R.string.required_permission)
                    }
                    Text(
                        text = permission.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleText,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun permissionLeadingIcon(type: PermissionType): ImageVector {
    return when (type) {
        PermissionType.BATTERY_OPTIMIZATION -> Icons.Default.BatterySaver
        PermissionType.BACKGROUND_ACTIVITY -> Icons.Default.Autorenew
        PermissionType.SMS -> Icons.AutoMirrored.Filled.Message
        PermissionType.CALLS -> Icons.Default.Call
        PermissionType.INTERNET -> Icons.Default.Language
        PermissionType.AUTOSTART -> Icons.Default.PowerSettingsNew
    }
}

@Composable
private fun permissionIconBackground(status: PermissionStatus): Color {
    val deniedPastel = colorResource(R.color.red_500_alpha_16)
    return when (status) {
        PermissionStatus.GRANTED -> Green500.copy(alpha = 0.16f)
        PermissionStatus.DENIED, PermissionStatus.UNAVAILABLE -> deniedPastel
    }
}

@Composable
private fun permissionIconTint(status: PermissionStatus): Color {
    val deniedTint = colorResource(R.color.red_600)
    return when (status) {
        PermissionStatus.GRANTED -> Green500
        PermissionStatus.DENIED, PermissionStatus.UNAVAILABLE -> deniedTint
    }
}
