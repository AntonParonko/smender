package com.smender.app.feature.bridge.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OutboxDatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE message_outbox (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender_address TEXT NOT NULL,
                source_sim TEXT,
                message_body TEXT NOT NULL,
                received_at_epoch_ms INTEGER NOT NULL,
                payload_type TEXT NOT NULL,
                channel TEXT NOT NULL,
                status TEXT NOT NULL,
                attempts INTEGER NOT NULL DEFAULT 0,
                last_error TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX idx_message_outbox_status_received ON message_outbox(status, received_at_epoch_ms)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS message_outbox")
            onCreate(db)
        }
    }

    companion object {
        private const val DATABASE_NAME = "smender_outbox.db"
        private const val DATABASE_VERSION = 3

        @Volatile
        private var instance: OutboxDatabaseHelper? = null

        fun getInstance(context: Context): OutboxDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: OutboxDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }
}
