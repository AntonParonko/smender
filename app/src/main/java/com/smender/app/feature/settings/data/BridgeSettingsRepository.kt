package com.smender.app.feature.settings.data

import android.content.Context
import com.smender.core.domain.feature.bridge.domain.model.DeliveryChannel
import com.smender.core.domain.feature.bridge.domain.model.OutboxPayloadType
import com.smender.core.domain.feature.settings.domain.model.TelegramSettings
import com.smender.core.domain.feature.settings.domain.model.WhatsAppSettings
import com.smender.core.domain.feature.settings.domain.repository.BridgeSettingsGateway

class BridgeSettingsRepository(
    context: Context
) : BridgeSettingsGateway {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getTelegramSettings(): TelegramSettings {
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

    override fun saveTelegramSettings(settings: TelegramSettings) {
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

    override fun resetTelegramSettings() {
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

    override fun getWhatsAppSettings(): WhatsAppSettings {
        return WhatsAppSettings(
            accessToken = prefs.getString(KEY_WHATSAPP_ACCESS_TOKEN, "").orEmpty(),
            phoneNumberId = prefs.getString(KEY_WHATSAPP_PHONE_NUMBER_ID, "").orEmpty(),
            toPhone = prefs.getString(KEY_WHATSAPP_TO_PHONE, "").orEmpty(),
            smsEnabled = prefs.getBoolean(KEY_WHATSAPP_SMS_ENABLED, true),
            callsEnabled = prefs.getBoolean(KEY_WHATSAPP_CALLS_ENABLED, true)
        )
    }

    override fun saveWhatsAppSettings(settings: WhatsAppSettings) {
        prefs.edit()
            .putString(KEY_WHATSAPP_ACCESS_TOKEN, settings.accessToken)
            .putString(KEY_WHATSAPP_PHONE_NUMBER_ID, settings.phoneNumberId)
            .putString(KEY_WHATSAPP_TO_PHONE, settings.toPhone)
            .putBoolean(KEY_WHATSAPP_SMS_ENABLED, settings.smsEnabled)
            .putBoolean(KEY_WHATSAPP_CALLS_ENABLED, settings.callsEnabled)
            .apply()
    }

    override fun resetWhatsAppSettings() {
        prefs.edit()
            .remove(KEY_WHATSAPP_ACCESS_TOKEN)
            .remove(KEY_WHATSAPP_PHONE_NUMBER_ID)
            .remove(KEY_WHATSAPP_TO_PHONE)
            .remove(KEY_WHATSAPP_SMS_ENABLED)
            .remove(KEY_WHATSAPP_CALLS_ENABLED)
            .apply()
    }

    override fun isChannelConfigured(channel: DeliveryChannel): Boolean {
        return when (channel) {
            DeliveryChannel.TELEGRAM -> getTelegramSettings().isConfigured
            DeliveryChannel.WHATSAPP -> getWhatsAppSettings().isConfigured
        }
    }

    override fun canSend(channel: DeliveryChannel, payloadType: OutboxPayloadType): Boolean {
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

    override fun channelsForPayload(payloadType: OutboxPayloadType): List<DeliveryChannel> {
        return DeliveryChannel.entries.filter { canSend(it, payloadType) }
    }

    override fun isAutostartConfirmed(): Boolean {
        return prefs.getBoolean(KEY_AUTOSTART_CONFIRMED, false)
    }

    override fun setAutostartConfirmed(isConfirmed: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOSTART_CONFIRMED, isConfirmed).apply()
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
