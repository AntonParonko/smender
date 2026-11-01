package com.smender.core.domain.feature.outbox.domain.usecase

import com.smender.core.domain.feature.bridge.domain.repository.OutboxGateway

class DeleteOutboxMessageUseCase(
    private val outboxGateway: OutboxGateway
) {
    operator fun invoke(id: Long) {
        outboxGateway.deleteById(id)
    }
}
