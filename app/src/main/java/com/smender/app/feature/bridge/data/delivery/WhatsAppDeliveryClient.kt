package com.smender.app.feature.bridge.data.delivery

import android.content.Context
import android.os.Build
import com.smender.app.R
import com.smender.core.domain.feature.settings.domain.repository.BridgeSettingsGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL

class WhatsAppDeliveryClient(
    private val context: Context,
    private val settingsRepository: BridgeSettingsGateway
) {
    suspend fun send(text: String): DeliveryResult = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getWhatsAppSettings()
        val accessToken = settings.accessToken
        val phoneNumberId = settings.phoneNumberId
        val toPhone = settings.toPhone

        if (accessToken.isBlank() || phoneNumberId.isBlank() || toPhone.isBlank()) {
            return@withContext DeliveryResult.Failure(context.getString(R.string.delivery_whatsapp_not_configured))
        }
        val textWithMeta = context.getString(
            R.string.delivery_telegram_device_prefix,
            systemDeviceName(),
            BatteryStatusHelper.getBatteryStatus(context),
            text
        )

        val url = URL("https://graph.facebook.com/v20.0/$phoneNumberId/messages")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        return@withContext try {
            val payload = JSONObject()
                .put("messaging_product", "whatsapp")
                .put("to", toPhone)
                .put("type", "text")
                .put("text", JSONObject().put("body", textWithMeta))
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
                    reason = "WhatsApp HTTP ${connection.responseCode}",
                    isRetryable = isRetryable
                )
            }
        } catch (_: UnknownHostException) {
            DeliveryResult.Failure(context.getString(R.string.delivery_whatsapp_dns_error), isRetryable = true)
        } catch (_: SocketTimeoutException) {
            DeliveryResult.Failure(context.getString(R.string.delivery_whatsapp_timeout), isRetryable = true)
        } catch (_: ConnectException) {
            DeliveryResult.Failure(context.getString(R.string.delivery_whatsapp_no_connection), isRetryable = true)
        } catch (_: IOException) {
            DeliveryResult.Failure(context.getString(R.string.delivery_whatsapp_network_error), isRetryable = true)
        } catch (e: Exception) {
            DeliveryResult.Failure(e.message ?: context.getString(R.string.delivery_whatsapp_generic_error))
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
