package com.smender.core.domain.feature.home.domain.model

data class HomeUiState(
    val smsSummary: DeliverySummaryUiState,
    val callsSummary: DeliverySummaryUiState,
    val telegramConfigured: Boolean,
    val whatsappConfigured: Boolean,
    val permissions: List<PermissionUiState>
)

data class DeliverySummaryUiState(
    val sent: Int,
    val pending: Int,
    val error: Int
)

data class PermissionUiState(
    val type: PermissionType,
    val title: String,
    val subtitle: String,
    val status: PermissionStatus
) {
    val isGranted: Boolean get() = status == PermissionStatus.GRANTED
    val isAvailable: Boolean get() = status != PermissionStatus.UNAVAILABLE
}

enum class PermissionType {
    BATTERY_OPTIMIZATION,
    BACKGROUND_ACTIVITY,
    SMS,
    CALLS,
    INTERNET,
    AUTOSTART
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    UNAVAILABLE
}
