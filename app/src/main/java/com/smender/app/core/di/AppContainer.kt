package com.smender.app.core.di

import android.content.Context
import com.smender.app.feature.bridge.data.delivery.MessageDeliveryDispatcher
import com.smender.app.feature.bridge.data.delivery.TelegramDeliveryClient
import com.smender.app.feature.bridge.data.delivery.WhatsAppDeliveryClient
import com.smender.app.feature.bridge.data.local.OutboxDatabaseHelper
import com.smender.app.feature.bridge.data.repository.OutboxRepository
import com.smender.core.domain.feature.bridge.domain.repository.OutboxGateway
import com.smender.app.feature.home.data.repository.HomeRepositoryImpl
import com.smender.core.domain.feature.home.domain.repository.HomeRepository
import com.smender.core.domain.feature.home.domain.usecase.GetHomeUiStateUseCase
import com.smender.core.domain.feature.outbox.domain.usecase.ClearOutboxMessagesUseCase
import com.smender.core.domain.feature.outbox.domain.usecase.DeleteOutboxMessageUseCase
import com.smender.core.domain.feature.outbox.domain.usecase.GetOutboxMessagesUseCase
import com.smender.app.feature.settings.data.BridgeSettingsRepository
import com.smender.core.domain.feature.settings.domain.repository.BridgeSettingsGateway

class AppContainer private constructor(
    appContext: Context
) {
    private val context = appContext.applicationContext

    val bridgeSettingsRepository: BridgeSettingsGateway =
        BridgeSettingsRepository(context)

    val outboxRepository: OutboxGateway =
        OutboxRepository(OutboxDatabaseHelper.getInstance(context))

    val homeRepository: HomeRepository =
        HomeRepositoryImpl(
            context = context,
            outboxRepository = outboxRepository,
            settingsRepository = bridgeSettingsRepository
        )

    val getHomeUiStateUseCase = GetHomeUiStateUseCase(homeRepository)
    val getOutboxMessagesUseCase = GetOutboxMessagesUseCase(outboxRepository)
    val deleteOutboxMessageUseCase = DeleteOutboxMessageUseCase(outboxRepository)
    val clearOutboxMessagesUseCase = ClearOutboxMessagesUseCase(outboxRepository)

    val telegramDeliveryClient = TelegramDeliveryClient(
        context = context,
        settingsRepository = bridgeSettingsRepository
    )

    val whatsappDeliveryClient = WhatsAppDeliveryClient(
        context = context,
        settingsRepository = bridgeSettingsRepository
    )

    val messageDeliveryDispatcher = MessageDeliveryDispatcher(
        context = context,
        telegramClient = telegramDeliveryClient,
        whatsappClient = whatsappDeliveryClient
    )

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun from(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
        }
    }
}
