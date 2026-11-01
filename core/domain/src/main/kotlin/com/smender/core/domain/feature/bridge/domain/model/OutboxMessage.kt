package com.smender.core.domain.feature.bridge.domain.model

data class OutboxMessage(
    val id: Long,
    val senderAddress: String,
    val sourceSim: String?,
    val messageBody: String,
    val receivedAtEpochMillis: Long,
    val payloadType: OutboxPayloadType,
    val channel: DeliveryChannel,
    val status: OutboxStatus,
    val attempts: Int,
    val lastError: String?
)
