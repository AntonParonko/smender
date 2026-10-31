package com.smender.app.feature.whatsapp.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smender.app.R
import com.smender.app.ui.theme.AppBarBackground
import com.smender.app.feature.settings.data.BridgeSettingsRepository
import com.smender.app.ui.theme.Background
import com.smender.app.ui.theme.SurfaceCard
import com.smender.app.ui.theme.TextPrimary
import com.smender.app.ui.theme.TextSecondary
import com.smender.app.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppSettingsScreen(
    settings: BridgeSettingsRepository.WhatsAppSettings,
    onBackClick: () -> Unit,
    onAccessTokenChanged: (String) -> Unit,
    onPhoneNumberIdChanged: (String) -> Unit,
    onToPhoneChanged: (String) -> Unit,
    onSmsEnabledChanged: (Boolean) -> Unit,
    onCallsEnabledChanged: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onInstructionsClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.whatsapp_settings_title), color = White) },
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
                .padding(12.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.whatsapp_api_parameters), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    OutlinedTextField(
                        value = settings.accessToken,
                        onValueChange = onAccessTokenChanged,
                        label = { Text(stringResource(R.string.whatsapp_access_token)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = settings.phoneNumberId,
                        onValueChange = onPhoneNumberIdChanged,
                        label = { Text(stringResource(R.string.whatsapp_phone_number_id)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = settings.toPhone,
                        onValueChange = onToPhoneChanged,
                        label = { Text(stringResource(R.string.whatsapp_recipient_number)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = onSaveClick, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.telegram_save_settings)) }
                    TextButton(onClick = onInstructionsClick, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.telegram_configure_instructions))
                    }
                    TextButton(onClick = onResetClick, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.whatsapp_reset_settings))
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.telegram_additional_settings), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    SettingSwitchRow(stringResource(R.string.telegram_sms_notifications), settings.smsEnabled, onSmsEnabledChanged)
                    SettingSwitchRow(stringResource(R.string.telegram_calls_notifications), settings.callsEnabled, onCallsEnabledChanged)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppInstructionScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.whatsapp_instruction_title), color = White) },
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
                .padding(12.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.whatsapp_prepare_intro),
                color = TextSecondary
            )
            StepCard(1, stringResource(R.string.whatsapp_step_1_title), stringResource(R.string.whatsapp_step_1_subtitle))
            StepCard(2, stringResource(R.string.whatsapp_step_2_title), stringResource(R.string.whatsapp_step_2_subtitle))
            StepCard(3, stringResource(R.string.whatsapp_step_3_title), stringResource(R.string.whatsapp_step_3_subtitle))
            StepCard(4, stringResource(R.string.whatsapp_step_4_title), stringResource(R.string.whatsapp_step_4_subtitle))
        }
    }
}

@Composable
private fun StepCard(index: Int, title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$index. $title", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
