package com.smender.app.feature.bridge.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object DeliveryWorkScheduler {
    private const val PERIODIC_DELIVERY_WORK_NAME = "smender-periodic-delivery"
    private const val PERIODIC_INBOUND_SYNC_WORK_NAME = "smender-periodic-inbound-sync"
    private const val IMMEDIATE_DELIVERY_WORK_NAME = "smender-immediate-delivery"
    private const val IMMEDIATE_INBOUND_SYNC_WORK_NAME = "smender-immediate-inbound-sync"

    fun enqueueImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<OutboxDeliveryWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_DELIVERY_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueInboundImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<InboundSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_INBOUND_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun ensurePeriodic(context: Context) {
        val deliveryRequest = PeriodicWorkRequestBuilder<OutboxDeliveryWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_DELIVERY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            deliveryRequest
        )

        val inboundSyncRequest = PeriodicWorkRequestBuilder<InboundSyncWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_INBOUND_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            inboundSyncRequest
        )
    }
}
