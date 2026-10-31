package com.smender.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.smender.app.feature.bridge.data.local.OutboxDatabaseHelper
import com.smender.app.feature.bridge.data.delivery.DeliveryResult
import com.smender.app.feature.bridge.data.delivery.TelegramDeliveryClient
import com.smender.app.feature.bridge.data.repository.OutboxRepository
import com.smender.app.feature.bridge.domain.model.DeliveryChannel
import com.smender.app.feature.bridge.domain.model.OutboxMessage
import com.smender.app.feature.bridge.domain.model.OutboxPayloadType
import com.smender.app.feature.bridge.OutboxEvents
import com.smender.app.feature.bridge.worker.DeliveryWorkScheduler
import com.smender.app.feature.home.data.repository.HomeRepositoryImpl
import com.smender.app.feature.home.data.system.AutostartSettingsNavigator
import com.smender.app.feature.home.domain.model.HomeUiState
import com.smender.app.feature.home.domain.model.PermissionType
import com.smender.app.feature.home.domain.model.PermissionUiState
import com.smender.app.feature.home.presentation.HomeScreen
import com.smender.app.feature.outbox.presentation.OutboxListScreen
import com.smender.app.feature.settings.data.BridgeSettingsRepository
import com.smender.app.feature.telegram.presentation.TelegramInstructionScreen
import com.smender.app.feature.telegram.presentation.TelegramSettingsScreen
import com.smender.app.feature.whatsapp.presentation.WhatsAppInstructionScreen
import com.smender.app.feature.whatsapp.presentation.WhatsAppSettingsScreen
import com.smender.app.ui.theme.SmenderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private lateinit var outboxRepository: OutboxRepository
    private lateinit var settingsRepository: BridgeSettingsRepository
    private lateinit var homeRepository: HomeRepositoryImpl
    private lateinit var connectivityManager: ConnectivityManager

    private var uiState: HomeUiState? by mutableStateOf(null)
    private var instructionDialog: PermissionType? by mutableStateOf(null)
    private var isNetworkCallbackRegistered = false
    private var isInternetConnectedOverride: Boolean = false
    private var currentScreen: AppScreen by mutableStateOf(AppScreen.HOME)

    private var smsSentMessages: List<OutboxMessage> by mutableStateOf(emptyList())
    private var smsQueueMessages: List<OutboxMessage> by mutableStateOf(emptyList())
    private var smsErrorMessages: List<OutboxMessage> by mutableStateOf(emptyList())
    private var callSentMessages: List<OutboxMessage> by mutableStateOf(emptyList())
    private var callQueueMessages: List<OutboxMessage> by mutableStateOf(emptyList())
    private var callErrorMessages: List<OutboxMessage> by mutableStateOf(emptyList())

    private var telegramSettings by mutableStateOf(
        BridgeSettingsRepository.TelegramSettings("", "", "", "", "", true, true)
    )
    private var savedTelegramSettings by mutableStateOf(
        BridgeSettingsRepository.TelegramSettings("", "", "", "", "", true, true)
    )
    private var isTelegramBotTokenMasked by mutableStateOf(false)
    private var isTelegramChatIdMasked by mutableStateOf(false)
    private var telegramInstructionReturnScreen: AppScreen by mutableStateOf(AppScreen.TELEGRAM_SETTINGS)
    private var whatsAppSettings by mutableStateOf(
        BridgeSettingsRepository.WhatsAppSettings("", "", "", true, true)
    )
    private var isOutboxReceiverRegistered = false
    private var isAwaitingAutostartConfirmation = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = recalculateInternetState()
        override fun onLost(network: Network) = recalculateInternetState()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            recalculateInternetState()
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshHomeState()
        }

    private val outboxUpdatedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == OutboxEvents.ACTION_OUTBOX_UPDATED) {
                runOnUiThread { refreshHomeState() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }

        settingsRepository = BridgeSettingsRepository(applicationContext)
        outboxRepository = OutboxRepository(OutboxDatabaseHelper.getInstance(applicationContext))
        homeRepository = HomeRepositoryImpl(
            context = applicationContext,
            outboxRepository = outboxRepository,
            settingsRepository = settingsRepository
        )
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        DeliveryWorkScheduler.ensurePeriodic(applicationContext)
        DeliveryWorkScheduler.enqueueInboundImmediate(applicationContext)
        refreshSettingsState()
        requestTelegramBotProfileRefresh()
        recalculateInternetState()

        setContent {
            SmenderTheme {
                BackHandler(
                    enabled = currentScreen != AppScreen.HOME &&
                        currentScreen != AppScreen.TELEGRAM_SETTINGS
                ) {
                    currentScreen = when (currentScreen) {
                        AppScreen.TELEGRAM_INSTRUCTION -> telegramInstructionReturnScreen
                        AppScreen.WHATSAPP_INSTRUCTION -> AppScreen.WHATSAPP_SETTINGS
                        else -> AppScreen.HOME
                    }
                }

                when (currentScreen) {
                    AppScreen.HOME -> {
                        uiState?.let {
                            HomeScreen(
                                uiState = it,
                                onPermissionClick = ::handlePermissionClick,
                                onSmsSentClick = ::openSmsSent,
                                onSmsQueueClick = ::openSmsQueue,
                                onSmsErrorClick = ::openSmsErrors,
                                onCallsSentClick = ::openCallsSent,
                                onCallsQueueClick = ::openCallsQueue,
                                onCallsErrorClick = ::openCallsErrors,
                                onTelegramSettingsClick = ::openTelegramSettings,
                                telegramChatId = telegramSettings.chatId,
                                telegramBotName = telegramSettings.botName,
                                telegramBotAvatarUrl = telegramSettings.botAvatarUrl
                            )
                        }
                    }

                    AppScreen.TELEGRAM_SETTINGS -> {
                        TelegramSettingsScreen(
                            settings = telegramSettings,
                            onTokenChanged = { telegramSettings = telegramSettings.copy(botToken = it) },
                            onChatIdChanged = { telegramSettings = telegramSettings.copy(chatId = it) },
                            onDeviceNameChanged = { telegramSettings = telegramSettings.copy(deviceName = it) },
                            onSmsEnabledChanged = { telegramSettings = telegramSettings.copy(smsEnabled = it) },
                            onCallsEnabledChanged = { telegramSettings = telegramSettings.copy(callsEnabled = it) },
                            onSaveClick = {
                                saveTelegramSettings()
                            },
                            onSaveAndExitClick = {
                                saveTelegramSettings()
                                currentScreen = AppScreen.HOME
                            },
                            onDiscardAndExitClick = ::discardTelegramDraftAndGoHome,
                            hasUnsavedChanges = hasTelegramUnsavedChanges(),
                            maskedBotToken = maskTelegramBotToken(savedTelegramSettings.botToken),
                            maskedChatId = maskTelegramChatId(savedTelegramSettings.chatId),
                            isBotTokenMasked = isTelegramBotTokenMasked,
                            isChatIdMasked = isTelegramChatIdMasked,
                            onEditBotTokenClick = { isTelegramBotTokenMasked = false },
                            onEditChatIdClick = { isTelegramChatIdMasked = false },
                            onInstructionsClick = {
                                telegramInstructionReturnScreen = AppScreen.TELEGRAM_SETTINGS
                                currentScreen = AppScreen.TELEGRAM_INSTRUCTION
                            },
                            onResetClick = {
                                settingsRepository.resetTelegramSettings()
                                val clearedSettings = settingsRepository.getTelegramSettings().withDefaultDeviceName()
                                savedTelegramSettings = clearedSettings
                                telegramSettings = clearedSettings
                                isTelegramBotTokenMasked = false
                                isTelegramChatIdMasked = false
                                refreshHomeState()
                                currentScreen = AppScreen.HOME
                            }
                        )
                    }

                    AppScreen.TELEGRAM_INSTRUCTION -> {
                        TelegramInstructionScreen(
                            settings = telegramSettings,
                            onBackClick = { currentScreen = telegramInstructionReturnScreen },
                            onTokenChanged = {
                                isTelegramBotTokenMasked = false
                                telegramSettings = telegramSettings.copy(botToken = it)
                            },
                            onChatIdChanged = {
                                isTelegramChatIdMasked = false
                                telegramSettings = telegramSettings.copy(chatId = it)
                            },
                            onOpenBotFatherClick = { openTelegramDeepLink("BotFather") },
                            onOpenUserInfoBotClick = { openTelegramDeepLink("userinfobot") },
                            onCopyTextClick = ::copyToClipboard,
                            onPasteTokenClick = { pasteFromClipboard { telegramSettings = telegramSettings.copy(botToken = it) } },
                            onPasteChatIdClick = { pasteFromClipboard { telegramSettings = telegramSettings.copy(chatId = it) } },
                            onSendTestMessageClick = ::sendTelegramInstructionTest,
                            onSaveAndFinishClick = {
                                saveTelegramSettings()
                                currentScreen = AppScreen.HOME
                            }
                        )
                    }

                    AppScreen.WHATSAPP_SETTINGS -> {
                        WhatsAppSettingsScreen(
                            settings = whatsAppSettings,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onAccessTokenChanged = { whatsAppSettings = whatsAppSettings.copy(accessToken = it) },
                            onPhoneNumberIdChanged = { whatsAppSettings = whatsAppSettings.copy(phoneNumberId = it) },
                            onToPhoneChanged = { whatsAppSettings = whatsAppSettings.copy(toPhone = it) },
                            onSmsEnabledChanged = { whatsAppSettings = whatsAppSettings.copy(smsEnabled = it) },
                            onCallsEnabledChanged = { whatsAppSettings = whatsAppSettings.copy(callsEnabled = it) },
                            onSaveClick = {
                                settingsRepository.saveWhatsAppSettings(whatsAppSettings)
                                refreshAllState()
                                currentScreen = AppScreen.HOME
                            },
                            onInstructionsClick = { currentScreen = AppScreen.WHATSAPP_INSTRUCTION },
                            onResetClick = {
                                settingsRepository.resetWhatsAppSettings()
                                refreshSettingsState()
                            }
                        )
                    }

                    AppScreen.WHATSAPP_INSTRUCTION -> {
                        WhatsAppInstructionScreen(onBackClick = { currentScreen = AppScreen.WHATSAPP_SETTINGS })
                    }

                    AppScreen.SMS_SENT -> {
                        OutboxListScreen(
                            title = stringResource(R.string.outbox_sms_sent_title),
                            messages = smsSentMessages,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onDeleteClick = ::deleteOutboxMessage,
                            onClearSectionClick = ::clearSmsSent,
                            clearButtonDescription = stringResource(R.string.outbox_clear_sms_sent_desc),
                            clearConfirmTitle = stringResource(R.string.outbox_clear_sms_sent_title),
                            clearConfirmMessage = stringResource(R.string.outbox_clear_sms_sent_message)
                        )
                    }

                    AppScreen.SMS_QUEUE -> {
                        OutboxListScreen(
                            title = stringResource(R.string.outbox_sms_queue_title),
                            messages = smsQueueMessages,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onDeleteClick = ::deleteOutboxMessage,
                            onClearSectionClick = ::clearSmsQueue,
                            clearButtonDescription = stringResource(R.string.outbox_clear_sms_queue_desc),
                            clearConfirmTitle = stringResource(R.string.outbox_clear_sms_queue_title),
                            clearConfirmMessage = stringResource(R.string.outbox_clear_sms_queue_message)
                        )
                    }

                    AppScreen.SMS_ERRORS -> {
                        OutboxListScreen(
                            title = stringResource(R.string.outbox_sms_errors_title),
                            messages = smsErrorMessages,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onDeleteClick = ::deleteOutboxMessage,
                            onClearSectionClick = ::clearSmsErrors,
                            clearButtonDescription = stringResource(R.string.outbox_clear_sms_errors_desc),
                            clearConfirmTitle = stringResource(R.string.outbox_clear_sms_errors_title),
                            clearConfirmMessage = stringResource(R.string.outbox_clear_sms_errors_message)
                        )
                    }

                    AppScreen.CALLS_SENT -> {
                        OutboxListScreen(
                            title = stringResource(R.string.outbox_calls_sent_title),
                            messages = callSentMessages,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onDeleteClick = ::deleteOutboxMessage,
                            onClearSectionClick = ::clearCallsSent,
                            clearButtonDescription = stringResource(R.string.outbox_clear_calls_sent_desc),
                            clearConfirmTitle = stringResource(R.string.outbox_clear_calls_sent_title),
                            clearConfirmMessage = stringResource(R.string.outbox_clear_calls_sent_message)
                        )
                    }

                    AppScreen.CALLS_QUEUE -> {
                        OutboxListScreen(
                            title = stringResource(R.string.outbox_calls_queue_title),
                            messages = callQueueMessages,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onDeleteClick = ::deleteOutboxMessage,
                            onClearSectionClick = ::clearCallsQueue,
                            clearButtonDescription = stringResource(R.string.outbox_clear_calls_queue_desc),
                            clearConfirmTitle = stringResource(R.string.outbox_clear_calls_queue_title),
                            clearConfirmMessage = stringResource(R.string.outbox_clear_calls_queue_message)
                        )
                    }

                    AppScreen.CALLS_ERRORS -> {
                        OutboxListScreen(
                            title = stringResource(R.string.outbox_calls_errors_title),
                            messages = callErrorMessages,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onDeleteClick = ::deleteOutboxMessage,
                            onClearSectionClick = ::clearCallsErrors,
                            clearButtonDescription = stringResource(R.string.outbox_clear_calls_errors_desc),
                            clearConfirmTitle = stringResource(R.string.outbox_clear_calls_errors_title),
                            clearConfirmMessage = stringResource(R.string.outbox_clear_calls_errors_message)
                        )
                    }
                }

                instructionDialog?.let { type ->
                    AlertDialog(
                        onDismissRequest = { instructionDialog = null },
                        title = { Text(text = instructionTitle(type)) },
                        text = { Text(text = instructionMessage(type)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    instructionDialog = null
                                    openSettingsByInstruction(type)
                                }
                            ) { Text(stringResource(R.string.open_settings)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { instructionDialog = null }) { Text(stringResource(R.string.cancel)) }
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerNetworkCallback()
        registerOutboxUpdatesReceiver()
    }

    override fun onResume() {
        super.onResume()
        if (isAwaitingAutostartConfirmation) {
            settingsRepository.setAutostartConfirmed(true)
            isAwaitingAutostartConfirmation = false
        }
        recalculateInternetState()
    }

    override fun onStop() {
        if (isOutboxReceiverRegistered) {
            unregisterReceiver(outboxUpdatedReceiver)
            isOutboxReceiverRegistered = false
        }
        if (isNetworkCallbackRegistered) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isNetworkCallbackRegistered = false
        }
        super.onStop()
    }

    private fun refreshAllState() {
        refreshSettingsState()
        refreshHomeState()
    }

    private fun refreshSettingsState() {
        val persistedSettings = settingsRepository.getTelegramSettings().withDefaultDeviceName()
        savedTelegramSettings = persistedSettings
        if (currentScreen != AppScreen.TELEGRAM_SETTINGS) {
            telegramSettings = persistedSettings
            isTelegramBotTokenMasked = persistedSettings.botToken.isNotBlank()
            isTelegramChatIdMasked = persistedSettings.chatId.isNotBlank()
        }
        whatsAppSettings = settingsRepository.getWhatsAppSettings()
    }

    private fun refreshHomeState() {
        uiState = homeRepository.getHomeUiState(isInternetConnectedOverride)
        smsSentMessages = outboxRepository.getSent(OutboxPayloadType.SMS)
        smsQueueMessages = outboxRepository.getQueue(OutboxPayloadType.SMS)
        smsErrorMessages = outboxRepository.getErrors(OutboxPayloadType.SMS)
        callSentMessages = outboxRepository.getSent(OutboxPayloadType.CALL)
        callQueueMessages = outboxRepository.getQueue(OutboxPayloadType.CALL)
        callErrorMessages = outboxRepository.getErrors(OutboxPayloadType.CALL)
    }

    private fun handlePermissionClick(permission: PermissionUiState) {
        when (permission.type) {
            PermissionType.SMS -> requestRuntimePermissions(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
            )

            PermissionType.CALLS -> requestRuntimePermissions(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_PHONE_STATE
            )

            PermissionType.BATTERY_OPTIMIZATION,
            PermissionType.BACKGROUND_ACTIVITY -> instructionDialog = permission.type

            PermissionType.INTERNET -> openInternetSettings()
            PermissionType.AUTOSTART -> openAutostartSettings()
        }
    }

    private fun requestRuntimePermissions(vararg permissions: String) {
        val missingPermissions = permissions.filterNot(::hasPermission).toTypedArray()
        if (missingPermissions.isEmpty()) {
            openAppDetailsSettings()
            return
        }
        requestPermissionsLauncher.launch(missingPermissions)
    }

    private fun openTelegramSettings() {
        telegramSettings = savedTelegramSettings
        isTelegramBotTokenMasked = savedTelegramSettings.botToken.isNotBlank()
        isTelegramChatIdMasked = savedTelegramSettings.chatId.isNotBlank()
        currentScreen = if (savedTelegramSettings.isConfigured) {
            AppScreen.TELEGRAM_SETTINGS
        } else {
            telegramInstructionReturnScreen = AppScreen.HOME
            AppScreen.TELEGRAM_INSTRUCTION
        }
    }

    private fun openSmsSent() {
        smsSentMessages = outboxRepository.getSent(OutboxPayloadType.SMS)
        currentScreen = AppScreen.SMS_SENT
    }

    private fun openSmsQueue() {
        smsQueueMessages = outboxRepository.getQueue(OutboxPayloadType.SMS)
        currentScreen = AppScreen.SMS_QUEUE
    }

    private fun openCallsSent() {
        callSentMessages = outboxRepository.getSent(OutboxPayloadType.CALL)
        currentScreen = AppScreen.CALLS_SENT
    }

    private fun openCallsQueue() {
        callQueueMessages = outboxRepository.getQueue(OutboxPayloadType.CALL)
        currentScreen = AppScreen.CALLS_QUEUE
    }

    private fun openSmsErrors() {
        smsErrorMessages = outboxRepository.getErrors(OutboxPayloadType.SMS)
        currentScreen = AppScreen.SMS_ERRORS
    }

    private fun openCallsErrors() {
        callErrorMessages = outboxRepository.getErrors(OutboxPayloadType.CALL)
        currentScreen = AppScreen.CALLS_ERRORS
    }

    private fun saveTelegramSettings() {
        val tokenChanged = telegramSettings.botToken != savedTelegramSettings.botToken
        val normalized = telegramSettings
            .withDefaultDeviceName()
            .let { current ->
                if (tokenChanged) current.copy(botName = "", botAvatarUrl = "") else current
            }
        settingsRepository.saveTelegramSettings(normalized)
        savedTelegramSettings = normalized
        telegramSettings = normalized
        isTelegramBotTokenMasked = normalized.botToken.isNotBlank()
        isTelegramChatIdMasked = normalized.chatId.isNotBlank()
        refreshHomeState()
        requestTelegramBotProfileRefresh()
    }

    private fun openTelegramDeepLink(username: String) {
        val normalized = username.removePrefix("@")
        val candidates = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$normalized")).apply {
                setPackage("org.telegram.messenger")
            },
            Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$normalized")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$normalized")).apply {
                setPackage("org.telegram.messenger")
            },
            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$normalized"))
        )
        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                continue
            } catch (_: SecurityException) {
                continue
            }
        }
        Toast.makeText(this, getString(R.string.toast_telegram_not_found), Toast.LENGTH_SHORT).show()
    }

    private fun copyToClipboard(value: String) {
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.clipboard_label_telegram_instruction), value))
        Toast.makeText(this, getString(R.string.toast_copy_success, value), Toast.LENGTH_SHORT).show()
    }

    private fun pasteFromClipboard(onPasted: (String) -> Unit) {
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return
        val clip = clipboard.primaryClip
        val text = clip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
        if (text.isBlank()) {
            Toast.makeText(this, getString(R.string.toast_clipboard_empty), Toast.LENGTH_SHORT).show()
            return
        }
        onPasted(text)
        Toast.makeText(this, getString(R.string.toast_pasted_from_clipboard), Toast.LENGTH_SHORT).show()
    }

    private fun sendTelegramInstructionTest() {
        val botToken = telegramSettings.botToken.trim()
        val chatId = telegramSettings.chatId.trim()
        if (botToken.isBlank() || chatId.isBlank()) {
            Toast.makeText(this, getString(R.string.toast_fill_token_chat_id), Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            when (
                val result = TelegramDeliveryClient(applicationContext).sendWithCredentials(
                    botToken = botToken,
                    chatId = chatId,
                    deviceName = telegramSettings.deviceName,
                    text = getString(R.string.telegram_test_message)
                )
            ) {
                DeliveryResult.Success -> {
                    Toast.makeText(this@MainActivity, getString(R.string.toast_test_message_sent), Toast.LENGTH_SHORT).show()
                }

                is DeliveryResult.Failure -> {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.toast_send_error, result.reason),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun discardTelegramDraftAndGoHome() {
        telegramSettings = savedTelegramSettings
        isTelegramBotTokenMasked = savedTelegramSettings.botToken.isNotBlank()
        isTelegramChatIdMasked = savedTelegramSettings.chatId.isNotBlank()
        currentScreen = AppScreen.HOME
    }

    private fun hasTelegramUnsavedChanges(): Boolean {
        return telegramSettings.withDefaultDeviceName() != savedTelegramSettings.withDefaultDeviceName()
    }

    private fun hasTelegramUnsavedChangesExceptBotMeta(): Boolean {
        return telegramSettings
            .withDefaultDeviceName()
            .copy(botName = "", botAvatarUrl = "") != savedTelegramSettings
            .withDefaultDeviceName()
            .copy(botName = "", botAvatarUrl = "")
    }

    private fun clearSmsErrors() {
        outboxRepository.deleteErrors(OutboxPayloadType.SMS)
        refreshHomeState()
    }

    private fun clearSmsSent() {
        outboxRepository.deleteSent(OutboxPayloadType.SMS)
        refreshHomeState()
    }

    private fun clearSmsQueue() {
        outboxRepository.deleteQueue(OutboxPayloadType.SMS)
        refreshHomeState()
    }

    private fun clearCallsSent() {
        outboxRepository.deleteSent(OutboxPayloadType.CALL)
        refreshHomeState()
    }

    private fun clearCallsQueue() {
        outboxRepository.deleteQueue(OutboxPayloadType.CALL)
        refreshHomeState()
    }

    private fun clearCallsErrors() {
        outboxRepository.deleteErrors(OutboxPayloadType.CALL)
        refreshHomeState()
    }

    private fun deleteOutboxMessage(id: Long) {
        outboxRepository.deleteById(id)
        refreshHomeState()
    }

    private fun openBatteryOptimizationScreen() {
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager?.isIgnoringBatteryOptimizations(packageName) == true) {
            openAppDetailsSettings()
            return
        }
        val requestIntent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        if (requestIntent.resolveActivity(packageManager) != null) {
            startActivity(requestIntent)
        } else {
            openAppDetailsSettings()
        }
    }

    private fun openAppDetailsSettings() {
        val settingsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(settingsIntent)
    }

    private fun openInternetSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        startActivity(intent)
    }

    private fun openAutostartSettings() {
        val intents = AutostartSettingsNavigator.allAutostartIntentsWithFallback(this)
        for (intent in intents) {
            try {
                if (intent.resolveActivity(packageManager) == null) continue
                startActivity(intent)
                isAwaitingAutostartConfirmation = true
                return
            } catch (_: ActivityNotFoundException) {
                continue
            } catch (_: SecurityException) {
                continue
            }
        }
        openAppDetailsSettings()
    }

    private fun openSettingsByInstruction(type: PermissionType) {
        when (type) {
            PermissionType.BATTERY_OPTIMIZATION -> openBatteryOptimizationScreen()
            PermissionType.BACKGROUND_ACTIVITY -> openAppDetailsSettings()
            else -> Unit
        }
    }

    private fun instructionTitle(type: PermissionType): String {
        return when (type) {
            PermissionType.BATTERY_OPTIMIZATION -> getString(R.string.instruction_battery_title)
            PermissionType.BACKGROUND_ACTIVITY -> getString(R.string.instruction_background_title)
            else -> getString(R.string.instruction_settings_title)
        }
    }

    private fun instructionMessage(type: PermissionType): String {
        return when (type) {
            PermissionType.BATTERY_OPTIMIZATION -> getString(R.string.instruction_battery_message)
            PermissionType.BACKGROUND_ACTIVITY -> getString(R.string.instruction_background_message)
            else -> ""
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun registerNetworkCallback() {
        if (isNetworkCallbackRegistered) return
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        isNetworkCallbackRegistered = true
    }

    private fun registerOutboxUpdatesReceiver() {
        if (isOutboxReceiverRegistered) return
        val filter = IntentFilter(OutboxEvents.ACTION_OUTBOX_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(outboxUpdatedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(outboxUpdatedReceiver, filter)
        }
        isOutboxReceiverRegistered = true
    }

    private fun recalculateInternetState() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        isInternetConnectedOverride = capabilities?.let(::isValidatedInternet) == true
        runOnUiThread { refreshHomeState() }
    }

    private fun isValidatedInternet(capabilities: NetworkCapabilities): Boolean {
        val hasTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        return hasTransport &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun BridgeSettingsRepository.TelegramSettings.withDefaultDeviceName():
        BridgeSettingsRepository.TelegramSettings {
        return if (deviceName.isBlank()) {
            copy(deviceName = systemDeviceName())
        } else {
            this
        }
    }

    private fun systemDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        if (manufacturer.isBlank()) return model.ifBlank { getString(R.string.default_device_name) }
        if (model.isBlank()) return manufacturer
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    private fun requestTelegramBotProfileRefresh() {
        val botToken = savedTelegramSettings.botToken.trim()
        if (botToken.isBlank()) return

        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                fetchTelegramBotProfile(botToken)
            } ?: return@launch

            val updated = savedTelegramSettings.copy(
                botName = profile.firstName.ifBlank { savedTelegramSettings.botName },
                botAvatarUrl = profile.avatarUrl.ifBlank { savedTelegramSettings.botAvatarUrl }
            )
            if (updated == savedTelegramSettings) return@launch

            settingsRepository.saveTelegramSettings(updated)
            savedTelegramSettings = updated
            if (currentScreen != AppScreen.TELEGRAM_SETTINGS || !hasTelegramUnsavedChangesExceptBotMeta()) {
                telegramSettings = updated
            }
            refreshHomeState()
        }
    }

    private fun fetchTelegramBotProfile(botToken: String): TelegramBotProfile? {
        val meUrl = URL("https://api.telegram.org/bot$botToken/getMe")
        val meConnection = (meUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        return try {
            if (meConnection.responseCode !in 200..299) return null
            val meBody = meConnection.inputStream.bufferedReader().use { it.readText() }
            val meJson = JSONObject(meBody)
            val meResult = meJson.optJSONObject("result") ?: return null
            val userId = meResult.optLong("id", 0L)
            val firstName = meResult.optString("first_name").orEmpty().trim()
            val avatarUrl = if (userId > 0L) {
                fetchTelegramBotAvatarUrl(botToken, userId)
            } else {
                ""
            }
            TelegramBotProfile(
                firstName = firstName,
                avatarUrl = avatarUrl
            )
        } catch (_: IOException) {
            null
        } catch (_: JSONException) {
            null
        } finally {
            meConnection.disconnect()
        }
    }

    private fun fetchTelegramBotAvatarUrl(botToken: String, userId: Long): String {
        val photosUrl = URL("https://api.telegram.org/bot$botToken/getUserProfilePhotos?user_id=$userId&limit=1")
        val photosConnection = (photosUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        return try {
            if (photosConnection.responseCode !in 200..299) return ""
            val photosBody = photosConnection.inputStream.bufferedReader().use { it.readText() }
            val photosJson = JSONObject(photosBody)
            val photosResult = photosJson.optJSONObject("result") ?: return ""
            val photos = photosResult.optJSONArray("photos") ?: return ""
            if (photos.length() == 0) return ""
            val sizes = photos.optJSONArray(0) ?: return ""
            if (sizes.length() == 0) return ""
            val largestSize = sizes.optJSONObject(sizes.length() - 1) ?: return ""
            val fileId = largestSize.optString("file_id").orEmpty()
            if (fileId.isBlank()) return ""

            fetchTelegramFileUrl(botToken, fileId)
        } catch (_: IOException) {
            ""
        } catch (_: JSONException) {
            ""
        } finally {
            photosConnection.disconnect()
        }
    }

    private fun fetchTelegramFileUrl(botToken: String, fileId: String): String {
        val fileUrl = URL("https://api.telegram.org/bot$botToken/getFile?file_id=$fileId")
        val fileConnection = (fileUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        return try {
            if (fileConnection.responseCode !in 200..299) return ""
            val fileBody = fileConnection.inputStream.bufferedReader().use { it.readText() }
            val fileJson = JSONObject(fileBody)
            val fileResult = fileJson.optJSONObject("result") ?: return ""
            val filePath = fileResult.optString("file_path").orEmpty()
            if (filePath.isBlank()) "" else "https://api.telegram.org/file/bot$botToken/$filePath"
        } catch (_: IOException) {
            ""
        } catch (_: JSONException) {
            ""
        } finally {
            fileConnection.disconnect()
        }
    }

    private fun maskTelegramBotToken(value: String): String {
        if (value.isBlank()) return ""
        if (value.length <= 6) return value
        return "${value.take(3)}••••${value.takeLast(3)}"
    }

    private fun maskTelegramChatId(value: String): String {
        if (value.isBlank()) return ""
        return if (value.length <= 4) "•••••$value" else "•••••${value.takeLast(4)}"
    }

    private enum class AppScreen {
        HOME,
        TELEGRAM_SETTINGS,
        TELEGRAM_INSTRUCTION,
        WHATSAPP_SETTINGS,
        WHATSAPP_INSTRUCTION,
        SMS_SENT,
        SMS_QUEUE,
        SMS_ERRORS,
        CALLS_SENT,
        CALLS_QUEUE,
        CALLS_ERRORS
    }

    private data class TelegramBotProfile(
        val firstName: String,
        val avatarUrl: String
    )
}
