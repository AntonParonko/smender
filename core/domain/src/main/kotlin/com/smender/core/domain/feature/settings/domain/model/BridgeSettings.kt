package com.smender.core.domain.feature.settings.domain.model

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
