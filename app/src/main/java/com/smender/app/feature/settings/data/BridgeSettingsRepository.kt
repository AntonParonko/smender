package com.smender.app.feature.settings.data

import android.content.Context
import com.smender.app.feature.bridge.domain.model.DeliveryChannel
import com.smender.app.feature.bridge.domain.model.OutboxPayloadType

class BridgeSettingsRepository(
    context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTelegramSettings(): TelegramSettings {
        return TelegramSettings(
            botToken = prefs.getString(KEY_TELEGRAM_BOT_TOKEN, "").orEmpty(),
            chatId = prefs.getString(KEY_TELEGRAM_CHAT_ID, "").orEmpty(),
            botName = prefs.getString(KEY_TELEGRAM_BOT_NAME, "").orEmpty(),
            botAvatarUrl = prefs.getString(KEY_TELEGRAM_BOT_AVATAR_URL, "").orEmpty(),
            deviceName = prefs.getString(KEY_TELEGRAM_DEVICE_NAME, "").orEmpty(),
            smsEnabled = prefs.getBoolean(KEY_TELEGRAM_SMS_ENABLED, true),
            callsEnabled = prefs.getBoolean(KEY_TELEGRAM_CALLS_ENABLED, true)
        )
    }

    fun saveTelegramSettings(settings: TelegramSettings) {
        prefs.edit()
            .putString(KEY_TELEGRAM_BOT_TOKEN, settings.botToken)
            .putString(KEY_TELEGRAM_CHAT_ID, settings.chatId)
            .putString(KEY_TELEGRAM_BOT_NAME, settings.botName)
            .putString(KEY_TELEGRAM_BOT_AVATAR_URL, settings.botAvatarUrl)
            .putString(KEY_TELEGRAM_DEVICE_NAME, settings.deviceName)
            .putBoolean(KEY_TELEGRAM_SMS_ENABLED, settings.smsEnabled)
            .putBoolean(KEY_TELEGRAM_CALLS_ENABLED, settings.callsEnabled)
            .apply()
    }

    fun resetTelegramSettings() {
        prefs.edit()
            .remove(KEY_TELEGRAM_BOT_TOKEN)
            .remove(KEY_TELEGRAM_CHAT_ID)
            .remove(KEY_TELEGRAM_BOT_NAME)
            .remove(KEY_TELEGRAM_BOT_AVATAR_URL)
            .remove(KEY_TELEGRAM_DEVICE_NAME)
            .remove(KEY_TELEGRAM_SMS_ENABLED)
            .remove(KEY_TELEGRAM_CALLS_ENABLED)
            .apply()
    }

    fun getWhatsAppSettings(): WhatsAppSettings {
        return WhatsAppSettings(
            accessToken = prefs.getString(KEY_WHATSAPP_ACCESS_TOKEN, "").orEmpty(),
            phoneNumberId = prefs.getString(KEY_WHATSAPP_PHONE_NUMBER_ID, "").orEmpty(),
            toPhone = prefs.getString(KEY_WHATSAPP_TO_PHONE, "").orEmpty(),
            smsEnabled = prefs.getBoolean(KEY_WHATSAPP_SMS_ENABLED, true),
            callsEnabled = prefs.getBoolean(KEY_WHATSAPP_CALLS_ENABLED, true)
        )
    }

    fun saveWhatsAppSettings(settings: WhatsAppSettings) {
        prefs.edit()
            .putString(KEY_WHATSAPP_ACCESS_TOKEN, settings.accessToken)
            .putString(KEY_WHATSAPP_PHONE_NUMBER_ID, settings.phoneNumberId)
            .putString(KEY_WHATSAPP_TO_PHONE, settings.toPhone)
            .putBoolean(KEY_WHATSAPP_SMS_ENABLED, settings.smsEnabled)
            .putBoolean(KEY_WHATSAPP_CALLS_ENABLED, settings.callsEnabled)
            .apply()
    }

    fun resetWhatsAppSettings() {
        prefs.edit()
            .remove(KEY_WHATSAPP_ACCESS_TOKEN)
            .remove(KEY_WHATSAPP_PHONE_NUMBER_ID)
            .remove(KEY_WHATSAPP_TO_PHONE)
            .remove(KEY_WHATSAPP_SMS_ENABLED)
            .remove(KEY_WHATSAPP_CALLS_ENABLED)
            .apply()
    }

    fun isChannelConfigured(channel: DeliveryChannel): Boolean {
        return when (channel) {
            DeliveryChannel.TELEGRAM -> getTelegramSettings().isConfigured
            DeliveryChannel.WHATSAPP -> getWhatsAppSettings().isConfigured
        }
    }

    fun canSend(channel: DeliveryChannel, payloadType: OutboxPayloadType): Boolean {
        return when (channel) {
            DeliveryChannel.TELEGRAM -> {
                val settings = getTelegramSettings()
                settings.isConfigured && when (payloadType) {
                    OutboxPayloadType.SMS -> settings.smsEnabled
                    OutboxPayloadType.CALL -> settings.callsEnabled
                }
            }

            DeliveryChannel.WHATSAPP -> {
                val settings = getWhatsAppSettings()
                settings.isConfigured && when (payloadType) {
                    OutboxPayloadType.SMS -> settings.smsEnabled
                    OutboxPayloadType.CALL -> settings.callsEnabled
                }
            }
        }
    }

    fun channelsForPayload(payloadType: OutboxPayloadType): List<DeliveryChannel> {
        return DeliveryChannel.entries.filter { canSend(it, payloadType) }
    }

    fun isAutostartConfirmed(): Boolean {
        return prefs.getBoolean(KEY_AUTOSTART_CONFIRMED, false)
    }

    fun setAutostartConfirmed(isConfirmed: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOSTART_CONFIRMED, isConfirmed).apply()
    }

    private fun isChannelEnabledForPayload(
        channel: DeliveryChannel,
        payloadType: OutboxPayloadType
    ): Boolean {
        return when (channel) {
            DeliveryChannel.TELEGRAM -> {
                val settings = getTelegramSettings()
                when (payloadType) {
                    OutboxPayloadType.SMS -> settings.smsEnabled
                    OutboxPayloadType.CALL -> settings.callsEnabled
                }
            }

            DeliveryChannel.WHATSAPP -> {
                val settings = getWhatsAppSettings()
                when (payloadType) {
                    OutboxPayloadType.SMS -> settings.smsEnabled
                    OutboxPayloadType.CALL -> settings.callsEnabled
                }
            }
        }
    }

    data class TelegramSettings(
        val botToken: String,
        val chatId: String,
        val botName: String,
        val botAvatarUrl: String,
        val deviceName: String,
        val smsEnabled: Boolean,
        val callsEnabled: Boolean
    ) {
        val isConfigured: Boolean get() = botToken.isNotBlank() && chatId.isNotBlank()
    }

    data class WhatsAppSettings(
        val accessToken: String,
        val phoneNumberId: String,
        val toPhone: String,
        val smsEnabled: Boolean,
        val callsEnabled: Boolean
    ) {
        val isConfigured: Boolean get() =
            accessToken.isNotBlank() && phoneNumberId.isNotBlank() && toPhone.isNotBlank()
    }

    companion object {
        private const val PREFS_NAME = "bridge_settings"

        private const val KEY_TELEGRAM_BOT_TOKEN = "telegram_bot_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val KEY_TELEGRAM_BOT_NAME = "telegram_bot_name"
        private const val KEY_TELEGRAM_BOT_AVATAR_URL = "telegram_bot_avatar_url"
        private const val KEY_TELEGRAM_DEVICE_NAME = "telegram_device_name"
        private const val KEY_TELEGRAM_SMS_ENABLED = "telegram_sms_enabled"
        private const val KEY_TELEGRAM_CALLS_ENABLED = "telegram_calls_enabled"

        private const val KEY_WHATSAPP_ACCESS_TOKEN = "whatsapp_access_token"
        private const val KEY_WHATSAPP_PHONE_NUMBER_ID = "whatsapp_phone_number_id"
        private const val KEY_WHATSAPP_TO_PHONE = "whatsapp_to_phone"
        private const val KEY_WHATSAPP_SMS_ENABLED = "whatsapp_sms_enabled"
        private const val KEY_WHATSAPP_CALLS_ENABLED = "whatsapp_calls_enabled"
        private const val KEY_AUTOSTART_CONFIRMED = "autostart_confirmed"
    }
}
