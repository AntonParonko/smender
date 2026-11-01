package com.smender.core.domain.feature.outbox.domain.usecase

import com.smender.core.domain.feature.bridge.domain.model.OutboxMessage
import com.smender.core.domain.feature.bridge.domain.model.OutboxPayloadType
import com.smender.core.domain.feature.bridge.domain.repository.OutboxGateway

class GetOutboxMessagesUseCase(
    private val outboxGateway: OutboxGateway
) {
    fun sent(payloadType: OutboxPayloadType): List<OutboxMessage> = outboxGateway.getSent(payloadType)
    fun queue(payloadType: OutboxPayloadType): List<OutboxMessage> = outboxGateway.getQueue(payloadType)
    fun errors(payloadType: OutboxPayloadType): List<OutboxMessage> = outboxGateway.getErrors(payloadType)
}
