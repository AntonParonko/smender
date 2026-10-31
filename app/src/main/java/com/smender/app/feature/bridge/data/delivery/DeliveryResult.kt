package com.smender.app.feature.bridge.data.delivery

sealed interface DeliveryResult {
    data object Success : DeliveryResult
    data class Failure(
        val reason: String,
        val isRetryable: Boolean = false
    ) : DeliveryResult
}
