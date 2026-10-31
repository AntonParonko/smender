package com.smender.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smender.app.feature.bridge.worker.DeliveryWorkScheduler

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            return
        }
        DeliveryWorkScheduler.ensurePeriodic(context)
        DeliveryWorkScheduler.enqueueInboundImmediate(context)
        DeliveryWorkScheduler.enqueueImmediate(context)
    }
}
