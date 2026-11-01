package com.smender.app.feature.bridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.smender.app.core.di.AppContainer
import com.smender.app.feature.bridge.OutboxEvents
import com.smender.core.domain.feature.bridge.domain.model.OutboxPayloadType
import com.smender.app.feature.bridge.worker.DeliveryWorkScheduler
import com.smender.app.feature.bridge.worker.InboundSyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsIncomingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isEmpty()) return@launch

                val senderAddress = messages.firstOrNull()?.originatingAddress.orEmpty()
                if (isUnknownSender(senderAddress)) return@launch
                val messageBody = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
                if (messageBody.isBlank()) return@launch

                val smsTimestamp = messages.firstOrNull()?.timestampMillis ?: 0L
                val receivedAt = smsTimestamp.takeIf { it > 0L } ?: System.currentTimeMillis()
                val watermarkAt = maxOf(receivedAt, System.currentTimeMillis())
                InboundSyncState.markSmsAtLeast(context, watermarkAt)
                val appContainer = AppContainer.from(context)
                val repository = appContainer.outboxRepository
                val settingsRepository = appContainer.bridgeSettingsRepository
                val channels = settingsRepository.channelsForPayload(OutboxPayloadType.SMS)
                repository.enqueue(
                    senderAddress = senderAddress,
                    messageBody = messageBody,
                    receivedAtEpochMillis = receivedAt,
                    payloadType = OutboxPayloadType.SMS,
                    channels = channels,
                    sourceSim = null
                )
                if (channels.isNotEmpty()) {
                    DeliveryWorkScheduler.enqueueImmediate(context)
                    OutboxEvents.notifyUpdated(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isUnknownSender(value: String): Boolean {
        if (value.isBlank()) return true
        val normalized = value.trim().lowercase()
        return normalized == "unknown" ||
            normalized == "private number" ||
            normalized == "anonymous" ||
            normalized == "unavailable" ||
            normalized.contains("неизвест")
    }
}
