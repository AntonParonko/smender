package com.smender.app.feature.bridge.data.repository

import android.content.ContentValues
import com.smender.app.feature.bridge.data.local.OutboxDatabaseHelper
import com.smender.app.feature.bridge.domain.model.DeliveryChannel
import com.smender.app.feature.bridge.domain.model.OutboxMessage
import com.smender.app.feature.bridge.domain.model.OutboxPayloadType
import com.smender.app.feature.bridge.domain.model.OutboxStatus

class OutboxRepository(
    private val dbHelper: OutboxDatabaseHelper
) {

    fun enqueue(
        senderAddress: String,
        messageBody: String,
        receivedAtEpochMillis: Long,
        payloadType: OutboxPayloadType,
        channels: List<DeliveryChannel>,
        sourceSim: String? = null
    ) {
        if (channels.isEmpty()) return
        val db = dbHelper.writableDatabase
        channels.forEach { channel ->
            if (isDuplicateForChannel(
                    payloadType = payloadType,
                    channel = channel,
                    senderAddress = senderAddress,
                    messageBody = messageBody,
                    sourceSim = sourceSim,
                    receivedAtEpochMillis = receivedAtEpochMillis
                )) {
                return@forEach
            }
            val values = ContentValues().apply {
                put("sender_address", senderAddress)
                put("source_sim", sourceSim)
                put("message_body", messageBody)
                put("received_at_epoch_ms", receivedAtEpochMillis)
                put("payload_type", payloadType.name)
                put("channel", channel.name)
                put("status", OutboxStatus.PENDING.name)
                put("attempts", 0)
            }
            db.insert("message_outbox", null, values)
        }
    }

    fun getPending(limit: Int = 50): List<OutboxMessage> {
        return getByStatuses(
            statuses = listOf(OutboxStatus.PENDING),
            orderBy = "received_at_epoch_ms ASC",
            limit = limit
        )
    }

    fun getSent(payloadType: OutboxPayloadType, limit: Int = 200): List<OutboxMessage> {
        return getByStatusesAndType(
            statuses = listOf(OutboxStatus.SENT),
            payloadType = payloadType,
            orderBy = "received_at_epoch_ms DESC",
            limit = limit
        )
    }

    fun getQueue(payloadType: OutboxPayloadType, limit: Int = 200): List<OutboxMessage> {
        return getByStatusesAndType(
            statuses = listOf(OutboxStatus.PENDING),
            payloadType = payloadType,
            orderBy = "received_at_epoch_ms DESC",
            limit = limit
        )
    }

    fun getErrors(payloadType: OutboxPayloadType, limit: Int = 200): List<OutboxMessage> {
        return getByStatusesAndType(
            statuses = listOf(OutboxStatus.ERROR),
            payloadType = payloadType,
            orderBy = "received_at_epoch_ms DESC",
            limit = limit
        )
    }

    fun countByStatus(payloadType: OutboxPayloadType, status: OutboxStatus): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT COUNT(*)
            FROM message_outbox
            WHERE payload_type = ? AND status = ?
            """.trimIndent(),
            arrayOf(payloadType.name, status.name)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun markSent(id: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("status", OutboxStatus.SENT.name)
            put("last_error", null as String?)
        }
        db.update("message_outbox", values, "id = ?", arrayOf(id.toString()))
    }

    fun markAttemptFailure(
        id: Long,
        error: String,
        attemptsToAdd: Int,
        maxAttempts: Int = 3
    ) {
        val db = dbHelper.writableDatabase
        db.execSQL(
            """
            UPDATE message_outbox
            SET attempts = attempts + ?,
                status = CASE WHEN attempts + ? >= ? THEN ? ELSE ? END,
                last_error = ?
            WHERE id = ?
            """.trimIndent(),
            arrayOf(
                attemptsToAdd,
                attemptsToAdd,
                maxAttempts,
                OutboxStatus.ERROR.name,
                OutboxStatus.PENDING.name,
                error,
                id.toString()
            )
        )
    }

    fun deleteById(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete("message_outbox", "id = ?", arrayOf(id.toString()))
    }

    fun deleteSent(payloadType: OutboxPayloadType) {
        deleteByTypeAndStatus(payloadType, OutboxStatus.SENT)
    }

    fun deleteQueue(payloadType: OutboxPayloadType) {
        deleteByTypeAndStatus(payloadType, OutboxStatus.PENDING)
    }

    fun deleteErrors(payloadType: OutboxPayloadType) {
        deleteByTypeAndStatus(payloadType, OutboxStatus.ERROR)
    }

    fun hasRecentDuplicate(
        payloadType: OutboxPayloadType,
        senderAddress: String,
        messageBody: String,
        sourceSim: String?,
        withinMillis: Long = 5_000
    ): Boolean {
        val threshold = System.currentTimeMillis() - withinMillis
        val normalizedSourceSim = sourceSim.orEmpty()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT 1
            FROM message_outbox
            WHERE payload_type = ?
              AND sender_address = ?
              AND message_body = ?
              AND IFNULL(source_sim, '') = IFNULL(?, '')
              AND received_at_epoch_ms >= ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                payloadType.name,
                senderAddress,
                messageBody,
                normalizedSourceSim,
                threshold.toString()
            )
        )
        return cursor.use { it.moveToFirst() }
    }

    private fun isDuplicateForChannel(
        payloadType: OutboxPayloadType,
        channel: DeliveryChannel,
        senderAddress: String,
        messageBody: String,
        sourceSim: String?,
        receivedAtEpochMillis: Long
    ): Boolean {
        val normalizedSourceSim = sourceSim.orEmpty()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT 1
            FROM message_outbox
            WHERE payload_type = ?
              AND channel = ?
              AND sender_address = ?
              AND message_body = ?
              AND IFNULL(source_sim, '') = IFNULL(?, '')
              AND received_at_epoch_ms = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                payloadType.name,
                channel.name,
                senderAddress,
                messageBody,
                normalizedSourceSim,
                receivedAtEpochMillis.toString()
            )
        )
        return cursor.use { it.moveToFirst() }
    }

    private fun getByStatusesAndType(
        statuses: List<OutboxStatus>,
        payloadType: OutboxPayloadType,
        orderBy: String,
        limit: Int
    ): List<OutboxMessage> {
        val placeholders = statuses.joinToString(",") { "?" }
        val args = buildList {
            add(payloadType.name)
            addAll(statuses.map { it.name })
        }.toTypedArray()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "message_outbox",
            arrayOf(
                "id",
                "sender_address",
                "source_sim",
                "message_body",
                "received_at_epoch_ms",
                "payload_type",
                "channel",
                "status",
                "attempts",
                "last_error"
            ),
            "payload_type = ? AND status IN ($placeholders)",
            args,
            null,
            null,
            orderBy,
            limit.toString()
        )
        return readMessages(cursor)
    }

    private fun getByStatuses(
        statuses: List<OutboxStatus>,
        orderBy: String,
        limit: Int
    ): List<OutboxMessage> {
        val placeholders = statuses.joinToString(",") { "?" }
        val args = statuses.map { it.name }.toTypedArray()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "message_outbox",
            arrayOf(
                "id",
                "sender_address",
                "source_sim",
                "message_body",
                "received_at_epoch_ms",
                "payload_type",
                "channel",
                "status",
                "attempts",
                "last_error"
            ),
            "status IN ($placeholders)",
            args,
            null,
            null,
            orderBy,
            limit.toString()
        )
        return readMessages(cursor)
    }

    private fun readMessages(cursor: android.database.Cursor): List<OutboxMessage> {
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        OutboxMessage(
                            id = it.getLong(it.getColumnIndexOrThrow("id")),
                            senderAddress = it.getString(it.getColumnIndexOrThrow("sender_address")),
                            sourceSim = it.getString(it.getColumnIndexOrThrow("source_sim")),
                            messageBody = it.getString(it.getColumnIndexOrThrow("message_body")),
                            receivedAtEpochMillis = it.getLong(
                                it.getColumnIndexOrThrow("received_at_epoch_ms")
                            ),
                            payloadType = OutboxPayloadType.valueOf(
                                it.getString(it.getColumnIndexOrThrow("payload_type"))
                            ),
                            channel = DeliveryChannel.valueOf(
                                it.getString(it.getColumnIndexOrThrow("channel"))
                            ),
                            status = OutboxStatus.valueOf(
                                it.getString(it.getColumnIndexOrThrow("status"))
                            ),
                            attempts = it.getInt(it.getColumnIndexOrThrow("attempts")),
                            lastError = it.getString(it.getColumnIndexOrThrow("last_error"))
                        )
                    )
                }
            }
        }
    }

    private fun deleteByTypeAndStatus(payloadType: OutboxPayloadType, status: OutboxStatus) {
        val db = dbHelper.writableDatabase
        db.delete(
            "message_outbox",
            "payload_type = ? AND status = ?",
            arrayOf(payloadType.name, status.name)
        )
    }

}
