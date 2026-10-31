package com.smender.app.feature.bridge.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.smender.app.R
import com.smender.app.feature.bridge.OutboxEvents
import com.smender.app.feature.bridge.data.local.OutboxDatabaseHelper
import com.smender.app.feature.bridge.data.repository.OutboxRepository
import com.smender.app.feature.bridge.domain.model.OutboxPayloadType
import com.smender.app.feature.bridge.worker.DeliveryWorkScheduler
import com.smender.app.feature.bridge.worker.InboundSyncState
import com.smender.app.feature.settings.data.BridgeSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsRepository = BridgeSettingsRepository(context)
                val channels = settingsRepository.channelsForPayload(OutboxPayloadType.CALL)
                if (channels.isEmpty()) return@launch
                val repository = OutboxRepository(OutboxDatabaseHelper.getInstance(context))

                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
                        if (isUnknownSender(incomingNumber)) return@launch
                        if (shouldSkipDuplicate(incomingNumber)) return@launch
                        val enqueued = enqueueCall(
                            context = context,
                            repository,
                            channels,
                            incomingNumber,
                            System.currentTimeMillis()
                        )
                        InboundSyncState.markCallAtLeast(context, System.currentTimeMillis())
                        if (enqueued) {
                            DeliveryWorkScheduler.enqueueImmediate(context)
                            OutboxEvents.notifyUpdated(context)
                        }
                        saveLastKnownRingAt(context, System.currentTimeMillis())
                    }

                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        if (hasRecentKnownRing(context)) return@launch
                        val callFromLog = readLatestIncomingCall(context) ?: return@launch
                        if (callFromLog.receivedAtEpochMillis <= lastProcessedCallLogAt(context)) return@launch
                        saveLastProcessedCallLogAt(context, callFromLog.receivedAtEpochMillis)
                        InboundSyncState.markCallAtLeast(context, callFromLog.receivedAtEpochMillis)
                        if (isUnknownSender(callFromLog.number)) return@launch
                        if (shouldSkipDuplicate(callFromLog.number)) return@launch
                        val enqueued = enqueueCall(
                            context = context,
                            repository = repository,
                            channels = channels,
                            incomingNumber = callFromLog.number,
                            receivedAt = callFromLog.receivedAtEpochMillis
                        )
                        if (enqueued) {
                            DeliveryWorkScheduler.enqueueImmediate(context)
                            OutboxEvents.notifyUpdated(context)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun enqueueCall(
        context: Context,
        repository: OutboxRepository,
        channels: List<com.smender.app.feature.bridge.domain.model.DeliveryChannel>,
        incomingNumber: String,
        receivedAt: Long
    ): Boolean {
        val body = context.getString(R.string.incoming_call_format, incomingNumber)
        if (repository.hasRecentDuplicate(
                payloadType = OutboxPayloadType.CALL,
                senderAddress = incomingNumber,
                messageBody = body,
                sourceSim = null
            )) {
            return false
        }
        repository.enqueue(
            senderAddress = incomingNumber,
            messageBody = body,
            receivedAtEpochMillis = receivedAt,
            payloadType = OutboxPayloadType.CALL,
            channels = channels,
            sourceSim = null
        )
        return true
    }

    private fun shouldSkipDuplicate(incomingNumber: String): Boolean {
        val now = System.currentTimeMillis()
        return synchronized(lock) {
            val isUnknown = isUnknownSender(incomingNumber)
            val isFastDuplicate = lastNumber == incomingNumber &&
                (now - lastEventAtMs) < DUPLICATE_WINDOW_MS
            val isUnknownAfterKnown = isUnknown &&
                lastNumber != null &&
                lastNumber != "Unknown" &&
                (now - lastEventAtMs) < UNKNOWN_AFTER_KNOWN_WINDOW_MS

            if (isFastDuplicate || isUnknownAfterKnown) {
                true
            } else {
                lastNumber = incomingNumber
                lastEventAtMs = now
                false
            }
        }
    }

    private fun readLatestIncomingCall(context: Context): LoggedCall? {
        if (!hasCallLogPermission(context)) return null
        val resolver = context.contentResolver
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )
        val selection = "${CallLog.Calls.TYPE} IN (?, ?)"
        val args = arrayOf(
            CallLog.Calls.INCOMING_TYPE.toString(),
            CallLog.Calls.MISSED_TYPE.toString()
        )
        val sortOrder = "${CallLog.Calls.DATE} DESC"
        val cursor = resolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            args,
            sortOrder
        ) ?: return null

        return cursor.use {
            if (!it.moveToFirst()) return null
            val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)).orEmpty()
            val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
            LoggedCall(
                number = number,
                receivedAtEpochMillis = date
            )
        }
    }

    private fun hasCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasRecentKnownRing(context: Context): Boolean {
        val lastRingAt = prefs(context).getLong(KEY_LAST_KNOWN_RING_AT, 0L)
        return lastRingAt > 0L && (System.currentTimeMillis() - lastRingAt) < KNOWN_RING_GRACE_MS
    }

    private fun saveLastKnownRingAt(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_LAST_KNOWN_RING_AT, value).apply()
    }

    private fun lastProcessedCallLogAt(context: Context): Long {
        return prefs(context).getLong(KEY_LAST_CALL_LOG_AT, 0L)
    }

    private fun saveLastProcessedCallLogAt(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_LAST_CALL_LOG_AT, value).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun isUnknownSender(value: String): Boolean {
        if (value.isBlank()) return true
        val normalized = value.trim().lowercase()
        return normalized == "unknown" ||
            normalized == "private number" ||
            normalized == "anonymous" ||
            normalized == "unavailable" ||
            normalized.contains("неизвест")
    }

    companion object {
        private const val PREFS_NAME = "call_receiver_state"
        private const val KEY_LAST_KNOWN_RING_AT = "last_known_ring_at"
        private const val KEY_LAST_CALL_LOG_AT = "last_call_log_at"
        private const val KNOWN_RING_GRACE_MS = 120_000L
        private const val DUPLICATE_WINDOW_MS = 1_200L
        private const val UNKNOWN_AFTER_KNOWN_WINDOW_MS = 8_000L
        private val lock = Any()
        private var lastNumber: String? = null
        private var lastEventAtMs: Long = 0L
    }

    private data class LoggedCall(
        val number: String,
        val receivedAtEpochMillis: Long
    )
}
