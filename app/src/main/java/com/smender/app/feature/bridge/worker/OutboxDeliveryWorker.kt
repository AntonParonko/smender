package com.smender.app.feature.bridge.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smender.app.core.di.AppContainer
import com.smender.app.feature.bridge.OutboxEvents
import com.smender.app.feature.bridge.data.delivery.DeliveryResult

class OutboxDeliveryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContainer = AppContainer.from(applicationContext)
        val repository = appContainer.outboxRepository
        val dispatcher = appContainer.messageDeliveryDispatcher
        val settingsRepository = appContainer.bridgeSettingsRepository

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
