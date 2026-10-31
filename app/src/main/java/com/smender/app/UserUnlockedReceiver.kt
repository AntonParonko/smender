package com.smender.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smender.app.feature.bridge.worker.DeliveryWorkScheduler

class UserUnlockedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_UNLOCKED) return
        DeliveryWorkScheduler.ensurePeriodic(context)
        DeliveryWorkScheduler.enqueueInboundImmediate(context)
        DeliveryWorkScheduler.enqueueImmediate(context)
    }
}
