package com.smender.core.domain.feature.settings.domain.repository

import com.smender.core.domain.feature.bridge.domain.model.DeliveryChannel
import com.smender.core.domain.feature.bridge.domain.model.OutboxPayloadType
import com.smender.core.domain.feature.settings.domain.model.TelegramSettings
import com.smender.core.domain.feature.settings.domain.model.WhatsAppSettings

interface BridgeSettingsGateway {
    fun getTelegramSettings(): TelegramSettings
    fun saveTelegramSettings(settings: TelegramSettings)
    fun resetTelegramSettings()

    fun getWhatsAppSettings(): WhatsAppSettings
    fun saveWhatsAppSettings(settings: WhatsAppSettings)
    fun resetWhatsAppSettings()

    fun isChannelConfigured(channel: DeliveryChannel): Boolean
    fun canSend(channel: DeliveryChannel, payloadType: OutboxPayloadType): Boolean
    fun channelsForPayload(payloadType: OutboxPayloadType): List<DeliveryChannel>

    fun isAutostartConfirmed(): Boolean
    fun setAutostartConfirmed(isConfirmed: Boolean)
}
