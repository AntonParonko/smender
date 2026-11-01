package com.smender.app.feature.bridge.data.delivery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.smender.app.R

object BatteryStatusHelper {
    fun getBatteryStatus(context: Context): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val levelFromManager = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val levelFromIntent = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevel = when {
            levelFromManager in 0..100 -> levelFromManager
            levelFromIntent >= 0 && scale > 0 -> (levelFromIntent * 100) / scale
            else -> -1
        }
        if (batteryLevel !in 0..100) return context.getString(R.string.battery_unknown)

        val statusCode = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val status = when (statusCode) {
            BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(R.string.battery_status_charging)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.battery_status_discharging)
            BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.battery_status_full)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(R.string.battery_status_not_charging)
            else -> context.getString(R.string.battery_status_unknown)
        }

        return context.getString(
            R.string.battery_summary,
            batteryLevelIndicator(batteryLevel),
            batteryLevel,
            status
        )
    }

    private fun batteryLevelIndicator(level: Int): String {
        val clamped = level.coerceIn(0, 100)
        val filledCount = ((clamped + 19) / 20).coerceIn(0, 5)
        val filledBlock = when {
            clamped <= 20 -> "🟥"
            clamped <= 40 -> "🟨"
            else -> "🟩"
        }
        val emptyBlock = "⬜️"
        return buildString(10) {
            repeat(filledCount) { append(filledBlock) }
            repeat(5 - filledCount) { append(emptyBlock) }
        }
    }
}
