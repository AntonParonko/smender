package com.smender.app.feature.bridge.data.delivery

import android.content.Context
import com.smender.app.R
import com.smender.app.feature.bridge.domain.model.DeliveryChannel
import com.smender.app.feature.bridge.domain.model.OutboxMessage
import com.smender.app.feature.bridge.domain.model.OutboxPayloadType

class MessageDeliveryDispatcher(
    private val context: Context,
    private val telegramClient: TelegramDeliveryClient,
    private val whatsappClient: WhatsAppDeliveryClient
) {

    suspend fun deliver(message: OutboxMessage): DeliveryResult {
        val text = when (message.payloadType) {
            OutboxPayloadType.SMS -> context.getString(
                R.string.delivery_sms_message_format,
                message.senderAddress,
                message.messageBody
            )
            OutboxPayloadType.CALL -> context.getString(
                R.string.delivery_call_message_format,
                message.messageBody
            )
        }
        return when (message.channel) {
            DeliveryChannel.TELEGRAM -> telegramClient.send(text)
            DeliveryChannel.WHATSAPP -> whatsappClient.send(text)
        }
    }
}
