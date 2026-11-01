package com.smender.core.domain.feature.outbox.domain.usecase

import com.smender.core.domain.feature.bridge.domain.model.OutboxPayloadType
import com.smender.core.domain.feature.bridge.domain.model.OutboxStatus
import com.smender.core.domain.feature.bridge.domain.repository.OutboxGateway

class ClearOutboxMessagesUseCase(
    private val outboxGateway: OutboxGateway
) {
    operator fun invoke(payloadType: OutboxPayloadType, status: OutboxStatus) {
        when (status) {
            OutboxStatus.SENT -> outboxGateway.deleteSent(payloadType)
            OutboxStatus.PENDING -> outboxGateway.deleteQueue(payloadType)
            OutboxStatus.ERROR -> outboxGateway.deleteErrors(payloadType)
        }
    }
}
