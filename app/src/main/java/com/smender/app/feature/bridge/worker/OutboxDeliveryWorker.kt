package com.smender.app.feature.bridge.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smender.app.feature.bridge.OutboxEvents
import com.smender.app.feature.bridge.data.delivery.DeliveryResult
import com.smender.app.feature.bridge.data.delivery.MessageDeliveryDispatcher
import com.smender.app.feature.bridge.data.delivery.TelegramDeliveryClient
import com.smender.app.feature.bridge.data.delivery.WhatsAppDeliveryClient
import com.smender.app.feature.bridge.data.local.OutboxDatabaseHelper
import com.smender.app.feature.bridge.data.repository.OutboxRepository
import com.smender.app.feature.settings.data.BridgeSettingsRepository

class OutboxDeliveryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = OutboxRepository(OutboxDatabaseHelper.getInstance(applicationContext))
        val dispatcher = MessageDeliveryDispatcher(
            context = applicationContext,
            telegramClient = TelegramDeliveryClient(applicationContext),
            whatsappClient = WhatsAppDeliveryClient(applicationContext)
        )
        val settingsRepository = BridgeSettingsRepository(applicationContext)

        val pending = repository.getPending(limit = 100)
        if (pending.isEmpty()) return Result.success()

        var shouldRetry = false
        pending.forEach { message ->
            if (!settingsRepository.canSend(message.channel, message.payloadType)) {
                return@forEach
            }

            when (val deliveryResult = dispatcher.deliver(message)) {
                DeliveryResult.Success -> {
                    repository.markSent(message.id)
                }

                is DeliveryResult.Failure -> {
                    if (deliveryResult.isRetryable) {
                        val nextAttempt = message.attempts + 1
                        repository.markAttemptFailure(
                            id = message.id,
                            error = deliveryResult.reason,
                            attemptsToAdd = 1,
                            maxAttempts = MAX_RETRYABLE_ATTEMPTS
                        )
                        if (nextAttempt < MAX_RETRYABLE_ATTEMPTS) {
                            shouldRetry = true
                        }
                    } else {
                        repository.markAttemptFailure(
                            id = message.id,
                            error = deliveryResult.reason,
                            attemptsToAdd = 1,
                            maxAttempts = 1
                        )
                    }
                }
            }
        }

        OutboxEvents.notifyUpdated(applicationContext)
        return if (shouldRetry) Result.retry() else Result.success()
    }

    companion object {
        private const val MAX_RETRYABLE_ATTEMPTS = 10
    }
}
