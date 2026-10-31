package com.smender.app.feature.bridge.worker

import android.content.Context

object InboundSyncState {
    private const val PREFS_NAME = "inbound_sync_state"
    private const val KEY_LAST_SMS_AT = "last_sms_at"
    private const val KEY_LAST_CALL_AT = "last_call_at"

    fun lastSmsAt(context: Context): Long = prefs(context).getLong(KEY_LAST_SMS_AT, 0L)

    fun lastCallAt(context: Context): Long = prefs(context).getLong(KEY_LAST_CALL_AT, 0L)

    fun markSmsAtLeast(context: Context, value: Long) {
        updateMax(context, KEY_LAST_SMS_AT, value)
    }

    fun markCallAtLeast(context: Context, value: Long) {
        updateMax(context, KEY_LAST_CALL_AT, value)
    }

    private fun updateMax(context: Context, key: String, value: Long) {
        if (value <= 0L) return
        val pref = prefs(context)
        val current = pref.getLong(key, 0L)
        if (value > current) {
            pref.edit().putLong(key, value).apply()
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
