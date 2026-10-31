package com.smender.app.feature.bridge.data.delivery

import android.content.Context
import com.smender.app.R
import com.smender.app.feature.settings.data.BridgeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.os.Build
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL

class TelegramDeliveryClient(
    private val context: Context
) {
    private val settingsRepository = BridgeSettingsRepository(context)

    suspend fun send(text: String): DeliveryResult = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getTelegramSettings()
        val botToken = settings.botToken.trim()
        val chatId = settings.chatId.trim()
        val deviceName = settings.deviceName.trim().ifBlank { systemDeviceName() }
        sendInternal(botToken, chatId, deviceName, text)
    }

    suspend fun sendWithCredentials(
        botToken: String,
        chatId: String,
        deviceName: String,
        text: String
    ): DeliveryResult = withContext(Dispatchers.IO) {
        val normalizedToken = botToken.trim()
        val normalizedChatId = chatId.trim()
        val normalizedDeviceName = deviceName.trim().ifBlank { systemDeviceName() }
        sendInternal(normalizedToken, normalizedChatId, normalizedDeviceName, text)
    }

    private fun sendInternal(
        botToken: String,
        chatId: String,
        deviceName: String,
        text: String
    ): DeliveryResult {
        if (botToken.isBlank() || chatId.isBlank()) {
            return DeliveryResult.Failure(context.getString(R.string.delivery_telegram_not_configured))
        }
        val textWithDevice = if (deviceName.isBlank()) {
            text
        } else {
            context.getString(R.string.delivery_telegram_device_prefix, deviceName, text)
        }

        val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        return try {
            val payload = JSONObject()
                .put("chat_id", chatId)
                .put("text", textWithDevice)
                .toString()
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload)
            }

            if (connection.responseCode in 200..299) {
                DeliveryResult.Success
            } else {
                val isRetryable = connection.responseCode in 500..599 ||
                    connection.responseCode == HttpURLConnection.HTTP_CLIENT_TIMEOUT ||
                    connection.responseCode == 429
                DeliveryResult.Failure(
                    reason = "Telegram HTTP ${connection.responseCode}",
                    isRetryable = isRetryable
                )
            }
        } catch (_: UnknownHostException) {
            DeliveryResult.Failure(context.getString(R.string.delivery_telegram_dns_error), isRetryable = true)
        } catch (_: SocketTimeoutException) {
            DeliveryResult.Failure(context.getString(R.string.delivery_telegram_timeout), isRetryable = true)
        } catch (_: ConnectException) {
            DeliveryResult.Failure(context.getString(R.string.delivery_telegram_no_connection), isRetryable = true)
        } catch (_: IOException) {
            DeliveryResult.Failure(context.getString(R.string.delivery_telegram_network_error), isRetryable = true)
        } catch (e: Exception) {
            DeliveryResult.Failure(e.message ?: context.getString(R.string.delivery_telegram_generic_error))
        } finally {
            connection.disconnect()
        }
    }

    private fun systemDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        if (manufacturer.isBlank()) return model.ifBlank { context.getString(R.string.default_device_name) }
        if (model.isBlank()) return manufacturer
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }
}
