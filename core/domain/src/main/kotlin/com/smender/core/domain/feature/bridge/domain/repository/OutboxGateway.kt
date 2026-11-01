package com.smender.core.domain.feature.bridge.domain.repository

import com.smender.core.domain.feature.bridge.domain.model.DeliveryChannel
import com.smender.core.domain.feature.bridge.domain.model.OutboxMessage
import com.smender.core.domain.feature.bridge.domain.model.OutboxPayloadType
import com.smender.core.domain.feature.bridge.domain.model.OutboxStatus

interface OutboxGateway {
    fun enqueue(
        senderAddress: String,
        messageBody: String,
        receivedAtEpochMillis: Long,
        payloadType: OutboxPayloadType,
        channels: List<DeliveryChannel>,
        sourceSim: String? = null
    )

    fun getPending(limit: Int = 50): List<OutboxMessage>
    fun getSent(payloadType: OutboxPayloadType, limit: Int = 200): List<OutboxMessage>
    fun getQueue(payloadType: OutboxPayloadType, limit: Int = 200): List<OutboxMessage>
    fun getErrors(payloadType: OutboxPayloadType, limit: Int = 200): List<OutboxMessage>
    fun countByStatus(payloadType: OutboxPayloadType, status: OutboxStatus): Int
    fun markSent(id: Long)
    fun markAttemptFailure(id: Long, error: String, attemptsToAdd: Int, maxAttempts: Int = 3)
    fun deleteById(id: Long)
    fun deleteSent(payloadType: OutboxPayloadType)
    fun deleteQueue(payloadType: OutboxPayloadType)
    fun deleteErrors(payloadType: OutboxPayloadType)
    fun hasRecentDuplicate(
        payloadType: OutboxPayloadType,
        senderAddress: String,
        messageBody: String,
        sourceSim: String?,
        withinMillis: Long = 5_000
    ): Boolean
}
