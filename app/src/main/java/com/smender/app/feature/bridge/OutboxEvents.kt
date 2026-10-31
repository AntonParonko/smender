package com.smender.app.feature.bridge

import android.content.Context
import android.content.Intent

object OutboxEvents {
    const val ACTION_OUTBOX_UPDATED = "com.smender.app.ACTION_OUTBOX_UPDATED"

    fun notifyUpdated(context: Context) {
        context.sendBroadcast(
            Intent(ACTION_OUTBOX_UPDATED).setPackage(context.packageName)
        )
    }
}
