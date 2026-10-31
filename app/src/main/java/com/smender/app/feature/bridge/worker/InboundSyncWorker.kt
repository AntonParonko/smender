package com.smender.app.feature.bridge.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smender.app.R
import com.smender.app.feature.bridge.OutboxEvents
import com.smender.app.feature.bridge.data.local.OutboxDatabaseHelper
import com.smender.app.feature.bridge.data.repository.OutboxRepository
import com.smender.app.feature.bridge.domain.model.OutboxPayloadType
import com.smender.app.feature.settings.data.BridgeSettingsRepository

class InboundSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settingsRepository = BridgeSettingsRepository(applicationContext)
        val outboxRepository = OutboxRepository(OutboxDatabaseHelper.getInstance(applicationContext))

        var hasNewRows = false
        hasNewRows = syncSms(settingsRepository, outboxRepository) || hasNewRows
        hasNewRows = syncCalls(settingsRepository, outboxRepository) || hasNewRows

        if (hasNewRows) {
            OutboxEvents.notifyUpdated(applicationContext)
            DeliveryWorkScheduler.enqueueImmediate(applicationContext)
        }
        return Result.success()
    }

    private fun syncSms(
        settingsRepository: BridgeSettingsRepository,
        outboxRepository: OutboxRepository
    ): Boolean {
        if (!hasPermission(Manifest.permission.READ_SMS)) return false
        val channels = settingsRepository.channelsForPayload(OutboxPayloadType.SMS)
        if (channels.isEmpty()) return false

        val lastSeen = InboundSyncState.lastSmsAt(applicationContext)
        if (lastSeen == 0L) {
            val latest = readLatestSmsTimestamp()
            if (latest > 0L) {
                InboundSyncState.markSmsAtLeast(applicationContext, latest)
            }
            return false
        }

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val selection = "${Telephony.Sms.DATE} > ? AND ${Telephony.Sms.TYPE} = ?"
        val selectionArgs = arrayOf(
            lastSeen.toString(),
            Telephony.Sms.MESSAGE_TYPE_INBOX.toString()
        )
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        val cursor = applicationContext.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        ) ?: return false

        var maxSeen = lastSeen
        var inserted = false
        cursor.use {
            var processed = 0
            while (it.moveToNext() && processed < SYNC_LIMIT) {
                processed++
                val sender = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)).orEmpty()
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)).orEmpty()
                val receivedAt = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                if (receivedAt > maxSeen) maxSeen = receivedAt
                if (isUnknownSender(sender) || body.isBlank()) continue

                outboxRepository.enqueue(
                    senderAddress = sender,
                    messageBody = body,
                    receivedAtEpochMillis = receivedAt,
                    payloadType = OutboxPayloadType.SMS,
                    channels = channels,
                    sourceSim = null
                )
                inserted = true
            }
        }

        if (maxSeen > lastSeen) {
            InboundSyncState.markSmsAtLeast(applicationContext, maxSeen)
        }
        return inserted
    }

    private fun syncCalls(
        settingsRepository: BridgeSettingsRepository,
        outboxRepository: OutboxRepository
    ): Boolean {
        if (!hasPermission(Manifest.permission.READ_CALL_LOG)) return false
        val channels = settingsRepository.channelsForPayload(OutboxPayloadType.CALL)
        if (channels.isEmpty()) return false

        val lastSeen = InboundSyncState.lastCallAt(applicationContext)
        if (lastSeen == 0L) {
            val latest = readLatestCallTimestamp()
            if (latest > 0L) {
                InboundSyncState.markCallAtLeast(applicationContext, latest)
            }
            return false
        }

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )
        val selection = "${CallLog.Calls.DATE} > ? AND ${CallLog.Calls.TYPE} IN (?, ?)"
        val selectionArgs = arrayOf(
            lastSeen.toString(),
            CallLog.Calls.INCOMING_TYPE.toString(),
            CallLog.Calls.MISSED_TYPE.toString()
        )
        val sortOrder = "${CallLog.Calls.DATE} ASC"
        val cursor = applicationContext.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        ) ?: return false

        var maxSeen = lastSeen
        var inserted = false
        cursor.use {
            var processed = 0
            while (it.moveToNext() && processed < SYNC_LIMIT) {
                processed++
                val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)).orEmpty()
                val receivedAt = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                if (receivedAt > maxSeen) maxSeen = receivedAt
                if (isUnknownSender(number)) continue

                val body = applicationContext.getString(R.string.incoming_call_format, number)
                outboxRepository.enqueue(
                    senderAddress = number,
                    messageBody = body,
                    receivedAtEpochMillis = receivedAt,
                    payloadType = OutboxPayloadType.CALL,
                    channels = channels,
                    sourceSim = null
                )
                inserted = true
            }
        }

        if (maxSeen > lastSeen) {
            InboundSyncState.markCallAtLeast(applicationContext, maxSeen)
        }
        return inserted
    }

    private fun readLatestSmsTimestamp(): Long {
        val cursor = applicationContext.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.DATE),
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        ) ?: return 0L

        return cursor.use {
            if (!it.moveToFirst()) 0L else it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
        }
    }

    private fun readLatestCallTimestamp(): Long {
        val cursor = applicationContext.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.DATE),
            "${CallLog.Calls.TYPE} IN (?, ?)",
            arrayOf(
                CallLog.Calls.INCOMING_TYPE.toString(),
                CallLog.Calls.MISSED_TYPE.toString()
            ),
            "${CallLog.Calls.DATE} DESC"
        ) ?: return 0L

        return cursor.use {
            if (!it.moveToFirst()) 0L else it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

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
        private const val SYNC_LIMIT = 100
    }
}
