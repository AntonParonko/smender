package com.smender.app.feature.telegram.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smender.app.R
import com.smender.app.feature.settings.data.BridgeSettingsRepository
import com.smender.app.ui.components.AnchoredConfirmationMenu
import com.smender.app.ui.theme.AppBarBackground
import com.smender.app.ui.theme.Background
import com.smender.app.ui.theme.BorderSubtle
import com.smender.app.ui.theme.SurfaceCard
import com.smender.app.ui.theme.TextPrimary
import com.smender.app.ui.theme.TextSecondary
import com.smender.app.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramSettingsScreen(
    settings: BridgeSettingsRepository.TelegramSettings,
    onTokenChanged: (String) -> Unit,
    onChatIdChanged: (String) -> Unit,
    onDeviceNameChanged: (String) -> Unit,
    onSmsEnabledChanged: (Boolean) -> Unit,
    onCallsEnabledChanged: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onSaveAndExitClick: () -> Unit,
    onDiscardAndExitClick: () -> Unit,
    hasUnsavedChanges: Boolean,
    maskedBotToken: String,
    maskedChatId: String,
    isBotTokenMasked: Boolean,
    isChatIdMasked: Boolean,
    onEditBotTokenClick: () -> Unit,
    onEditChatIdClick: () -> Unit,
    onInstructionsClick: () -> Unit,
    onResetClick: () -> Unit
) {
    var showExitConfirmation by remember { mutableStateOf(false) }

    fun requestBack() {
        if (hasUnsavedChanges) {
            showExitConfirmation = true
        } else {
            onDiscardAndExitClick()
        }
    }

    BackHandler(enabled = true) {
        requestBack()
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.telegram_settings_title), color = White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBarBackground),
                navigationIcon = {
                    Box {
                        IconButton(onClick = { requestBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = White)
                        }
                        AnchoredConfirmationMenu(
                            expanded = showExitConfirmation,
                            title = stringResource(R.string.telegram_save_changes_title),
                            message = stringResource(R.string.telegram_unsaved_changes_message),
                            confirmText = stringResource(R.string.save),
                            dismissText = stringResource(R.string.telegram_dont_save),
                            onDismissRequest = { showExitConfirmation = false },
                            onDismissClick = onDiscardAndExitClick,
                            onConfirm = onSaveAndExitClick
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TelegramSectionTitle(stringResource(R.string.telegram_bot_parameters))

                TelegramCredentialCard(
                    title = stringResource(R.string.telegram_bot_token),
                    titleIcon = Icons.Default.Key,
                    titleIconTint = colorResource(R.color.blue_600),
                    value = if (isBotTokenMasked) maskedBotToken else settings.botToken,
                    onValueChange = onTokenChanged,
                    readOnly = isBotTokenMasked,
                    placeholder = stringResource(R.string.telegram_enter_bot_token),
                    primaryActionIcon = if (isBotTokenMasked) Icons.Default.Edit else null,
                    onPrimaryActionClick = {
                        if (isBotTokenMasked) {
                            onEditBotTokenClick()
                        }
                    }
                )

                TelegramCredentialCard(
                    title = stringResource(R.string.telegram_chat_id),
                    titleIcon = Icons.Default.Person,
                    titleIconTint = colorResource(R.color.purple_600),
                    value = if (isChatIdMasked) maskedChatId else settings.chatId,
                    onValueChange = onChatIdChanged,
                    readOnly = isChatIdMasked,
                    placeholder = stringResource(R.string.telegram_enter_chat_id),
                    primaryActionIcon = if (isChatIdMasked) Icons.Default.Edit else null,
                    onPrimaryActionClick = if (isChatIdMasked) onEditChatIdClick else null
                )

                TelegramCredentialCard(
                    title = stringResource(R.string.telegram_device_name),
                    titleIcon = Icons.Default.Person,
                    titleIconTint = colorResource(R.color.sky_500),
                    value = settings.deviceName,
                    onValueChange = onDeviceNameChanged,
                    readOnly = false,
                    placeholder = stringResource(R.string.telegram_device_name_placeholder),
                    primaryActionIcon = null,
                    onPrimaryActionClick = null
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TelegramActionButton(
                        text = stringResource(R.string.telegram_save_settings),
                        icon = Icons.AutoMirrored.Filled.Send,
                        background = colorResource(R.color.green_600),
                        onClick = onSaveClick
                    )
                    TelegramActionButton(
                        text = stringResource(R.string.telegram_configure_instructions),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        background = colorResource(R.color.purple_600),
                        onClick = onInstructionsClick
                    )
                    TelegramActionButton(
                        text = stringResource(R.string.telegram_reset_bot_settings),
                        icon = Icons.Default.Replay,
                        background = colorResource(R.color.red_600),
                        onClick = onResetClick
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TelegramSectionTitle(stringResource(R.string.telegram_additional_settings))

                TelegramSwitchCard(
                    title = stringResource(R.string.telegram_sms_notifications),
                    subtitle = stringResource(R.string.telegram_sms_forwarding),
                    icon = Icons.Default.Sms,
                    iconBackground = colorResource(R.color.blue_100),
                    iconTint = colorResource(R.color.blue_600),
                    checked = settings.smsEnabled,
                    onCheckedChange = onSmsEnabledChanged
                )

                TelegramSwitchCard(
                    title = stringResource(R.string.telegram_calls_notifications),
                    subtitle = stringResource(R.string.telegram_calls_forwarding),
                    icon = Icons.Default.Call,
                    iconBackground = colorResource(R.color.green_100_alt),
                    iconTint = colorResource(R.color.green_600),
                    checked = settings.callsEnabled,
                    onCheckedChange = onCallsEnabledChanged
                )
            }
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramInstructionScreen(
    settings: BridgeSettingsRepository.TelegramSettings,
    onBackClick: () -> Unit,
    onTokenChanged: (String) -> Unit,
    onChatIdChanged: (String) -> Unit,
    onOpenBotFatherClick: () -> Unit,
    onOpenUserInfoBotClick: () -> Unit,
    onCopyTextClick: (String) -> Unit,
    onPasteTokenClick: () -> Unit,
    onPasteChatIdClick: () -> Unit,
    onSendTestMessageClick: () -> Unit,
    onSaveAndFinishClick: () -> Unit
) {
    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.telegram_instruction_title), color = White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBarBackground),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = White)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelegramInstructionInfoCard(
                text = stringResource(R.string.telegram_instruction_intro)
            )

            TelegramInstructionStepCard(
                step = 1,
                title = stringResource(R.string.telegram_open_botfather_title),
                subtitle = stringResource(R.string.telegram_open_botfather_subtitle),
                badgeColor = colorResource(R.color.blue_600)
            ) {
                TelegramActionButton(
                    text = stringResource(R.string.telegram_open_botfather_button),
                    icon = Icons.AutoMirrored.Filled.Send,
                    background = colorResource(R.color.blue_600),
                    onClick = onOpenBotFatherClick
                )
            }

            TelegramInstructionStepCard(
                step = 2,
                title = stringResource(R.string.telegram_create_bot_title),
                subtitle = stringResource(R.string.telegram_create_bot_subtitle),
                badgeColor = colorResource(R.color.violet_600)
            ) {
                TelegramCommandRow(command = "/newbot", onCopyClick = { onCopyTextClick("/newbot") })
            }

            TelegramInstructionStepCard(
                step = 3,
                title = stringResource(R.string.telegram_get_token_title),
                subtitle = stringResource(R.string.telegram_get_token_subtitle),
                badgeColor = colorResource(R.color.green_600)
            ) {
                TelegramCredentialCard(
                    title = stringResource(R.string.telegram_bot_token),
                    titleIcon = Icons.Default.Key,
                    titleIconTint = colorResource(R.color.blue_600),
                    value = settings.botToken,
                    onValueChange = onTokenChanged,
                    readOnly = false,
                    placeholder = stringResource(R.string.telegram_bot_token_example),
                    primaryActionIcon = Icons.Default.ContentPaste,
                    onPrimaryActionClick = onPasteTokenClick
                )
            }

            TelegramInstructionStepCard(
                step = 4,
                title = stringResource(R.string.telegram_open_userinfobot_title),
                subtitle = stringResource(R.string.telegram_open_userinfobot_subtitle),
                badgeColor = colorResource(R.color.orange_600)
            ) {
                TelegramActionButton(
                    text = stringResource(R.string.telegram_open_userinfobot_button),
                    icon = Icons.AutoMirrored.Filled.Send,
                    background = colorResource(R.color.blue_600),
                    onClick = onOpenUserInfoBotClick
                )
            }

            TelegramInstructionStepCard(
                step = 5,
                title = stringResource(R.string.telegram_get_chat_id_title),
                subtitle = stringResource(R.string.telegram_get_chat_id_subtitle),
                badgeColor = colorResource(R.color.rose_600)
            ) {
                TelegramCommandRow(command = "/start", onCopyClick = { onCopyTextClick("/start") })
            }

            TelegramInstructionStepCard(
                step = 6,
                title = stringResource(R.string.telegram_enter_chat_id_title),
                subtitle = stringResource(R.string.telegram_enter_chat_id_subtitle),
                badgeColor = colorResource(R.color.red_600)
            ) {
                TelegramCredentialCard(
                    title = stringResource(R.string.telegram_chat_id),
                    titleIcon = Icons.Default.Person,
                    titleIconTint = colorResource(R.color.purple_600),
                    value = settings.chatId,
                    onValueChange = onChatIdChanged,
                    readOnly = false,
                    placeholder = stringResource(R.string.telegram_chat_id_example),
                    primaryActionIcon = Icons.Default.ContentPaste,
                    onPrimaryActionClick = onPasteChatIdClick
                )
            }

            TelegramInstructionStepCard(
                step = 7,
                title = stringResource(R.string.telegram_check_connection_title),
                subtitle = stringResource(R.string.telegram_check_connection_subtitle),
                badgeColor = colorResource(R.color.indigo_600)
            ) {
                TelegramActionButton(
                    text = stringResource(R.string.telegram_send_test_message),
                    icon = Icons.AutoMirrored.Filled.Send,
                    background = colorResource(R.color.blue_600),
                    onClick = onSendTestMessageClick
                )
            }

            TelegramInstructionStepCard(
                step = 8,
                title = stringResource(R.string.telegram_confirm_receive_title),
                subtitle = stringResource(R.string.telegram_confirm_receive_subtitle),
                badgeColor = colorResource(R.color.teal_500)
            ) {
                TelegramActionButton(
                    text = stringResource(R.string.telegram_yes_message_received),
                    icon = Icons.AutoMirrored.Filled.Send,
                    background = colorResource(R.color.green_600),
                    onClick = onSaveAndFinishClick
                )
            }
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}
