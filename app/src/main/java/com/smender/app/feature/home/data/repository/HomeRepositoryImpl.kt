package com.smender.app.feature.home.data.repository

import android.Manifest
import android.app.ActivityManager
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.smender.app.R
import com.smender.core.domain.feature.bridge.domain.model.OutboxPayloadType
import com.smender.core.domain.feature.bridge.domain.model.OutboxStatus
import com.smender.core.domain.feature.bridge.domain.repository.OutboxGateway
import com.smender.app.feature.home.data.system.AutostartSettingsNavigator
import com.smender.core.domain.feature.home.domain.model.DeliverySummaryUiState
import com.smender.core.domain.feature.home.domain.model.HomeUiState
import com.smender.core.domain.feature.home.domain.model.PermissionType
import com.smender.core.domain.feature.home.domain.model.PermissionStatus
import com.smender.core.domain.feature.home.domain.model.PermissionUiState
import com.smender.core.domain.feature.home.domain.repository.HomeRepository
import com.smender.core.domain.feature.settings.domain.repository.BridgeSettingsGateway

class HomeRepositoryImpl(
    private val context: Context,
    private val outboxRepository: OutboxGateway,
    private val settingsRepository: BridgeSettingsGateway
) : HomeRepository {
    private val requiredPermissionSubtitle = context.getString(R.string.required_permission)

    override fun getHomeUiState(isInternetConnectedOverride: Boolean?): HomeUiState {
        val telegramConfigured = settingsRepository.getTelegramSettings().isConfigured
        val whatsappConfigured = settingsRepository.getWhatsAppSettings().isConfigured

        return HomeUiState(
            smsSummary = deliverySummary(OutboxPayloadType.SMS),
            callsSummary = deliverySummary(OutboxPayloadType.CALL),
            telegramConfigured = telegramConfigured,
            whatsappConfigured = whatsappConfigured,
            permissions = buildPermissionStates(isInternetConnectedOverride)
        )
    }

    private fun deliverySummary(payloadType: OutboxPayloadType): DeliverySummaryUiState {
        return DeliverySummaryUiState(
            sent = outboxRepository.countByStatus(payloadType, OutboxStatus.SENT),
            pending = outboxRepository.countByStatus(payloadType, OutboxStatus.PENDING),
            error = outboxRepository.countByStatus(payloadType, OutboxStatus.ERROR)
        )
    }

    private fun buildPermissionStates(isInternetConnectedOverride: Boolean?): List<PermissionUiState> {
        return listOf(
            batteryOptimizationPermission(),
            backgroundActivityPermission(),
            smsPermission(),
            callsPermission(),
            internetPermission(isInternetConnectedOverride),
            autoStartPermission()
        )
    }

    private fun batteryOptimizationPermission(): PermissionUiState {
        val powerManager = context.getSystemService(PowerManager::class.java)
        val isIgnoringOptimization =
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        return PermissionUiState(
            type = PermissionType.BATTERY_OPTIMIZATION,
            title = context.getString(R.string.instruction_battery_title),
            subtitle = if (isIgnoringOptimization) {
                context.getString(R.string.permission_battery_disabled_background)
            } else {
                requiredPermissionSubtitle
            },
            status = if (isIgnoringOptimization) PermissionStatus.GRANTED else PermissionStatus.DENIED
        )
    }

    private fun backgroundActivityPermission(): PermissionUiState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return PermissionUiState(
                type = PermissionType.BACKGROUND_ACTIVITY,
                title = context.getString(R.string.instruction_background_title),
                subtitle = requiredPermissionSubtitle,
                status = PermissionStatus.UNAVAILABLE
            )
        }

        val activityManager = context.getSystemService(ActivityManager::class.java)
        val isRestricted = activityManager?.isBackgroundRestricted ?: true

        return PermissionUiState(
            type = PermissionType.BACKGROUND_ACTIVITY,
            title = context.getString(R.string.instruction_background_title),
            subtitle = if (isRestricted) {
                requiredPermissionSubtitle
            } else {
                context.getString(R.string.permission_background_allowed)
            },
            status = if (isRestricted) PermissionStatus.DENIED else PermissionStatus.GRANTED
        )
    }

    private fun smsPermission(): PermissionUiState {
        val hasReadSms = hasPermission(Manifest.permission.READ_SMS)
        val hasReceiveSms = hasPermission(Manifest.permission.RECEIVE_SMS)
        val isGranted = hasReadSms && hasReceiveSms

        return PermissionUiState(
            type = PermissionType.SMS,
            title = context.getString(R.string.permission_sms_messages),
            subtitle = if (isGranted) {
                context.getString(R.string.permission_sms_allowed)
            } else {
                requiredPermissionSubtitle
            },
            status = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
        )
    }

    private fun callsPermission(): PermissionUiState {
        val hasCallLog = hasPermission(Manifest.permission.READ_CALL_LOG)
        val hasPhoneState = hasPermission(Manifest.permission.READ_PHONE_STATE)
        val isGranted = hasCallLog && hasPhoneState

        return PermissionUiState(
            type = PermissionType.CALLS,
            title = context.getString(R.string.permission_phone_calls),
            subtitle = if (isGranted) {
                context.getString(R.string.permission_calls_allowed)
            } else {
                requiredPermissionSubtitle
            },
            status = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
        )
    }

    private fun internetPermission(isInternetConnectedOverride: Boolean?): PermissionUiState {
        val isConnected = isInternetConnectedOverride ?: isInternetConnected()

        return PermissionUiState(
            type = PermissionType.INTERNET,
            title = context.getString(R.string.permission_internet),
            subtitle = if (isConnected) {
                context.getString(R.string.permission_internet_connected)
            } else {
                context.getString(R.string.permission_internet_disconnected)
            },
            status = if (isConnected) PermissionStatus.GRANTED else PermissionStatus.DENIED
        )
    }

    private fun autoStartPermission(): PermissionUiState {
        val hasAutostartScreen = AutostartSettingsNavigator.hasAutostartSettingsScreen(context)
        val isConfirmed = settingsRepository.isAutostartConfirmed()
        return PermissionUiState(
            type = PermissionType.AUTOSTART,
            title = context.getString(R.string.permission_autostart),
            subtitle = if (!hasAutostartScreen) {
                context.getString(R.string.permission_autostart_unavailable)
            } else if (isConfirmed) {
                context.getString(R.string.permission_autostart_confirmed)
            } else {
                requiredPermissionSubtitle
            },
            status = if (!hasAutostartScreen) {
                PermissionStatus.UNAVAILABLE
            } else if (isConfirmed) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.DENIED
            }
        )
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isInternetConnected(): Boolean {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
