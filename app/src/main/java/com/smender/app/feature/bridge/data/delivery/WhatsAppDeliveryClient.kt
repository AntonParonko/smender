package com.smender.app.feature.bridge.data.delivery

import android.content.Context
import com.smender.app.R
import com.smender.app.feature.settings.data.BridgeSettingsRepository
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
    private val context: Context
) {
    private val settingsRepository = BridgeSettingsRepository(context)

    suspend fun send(text: String): DeliveryResult = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getWhatsAppSettings()
        val accessToken = settings.accessToken
        val phoneNumberId = settings.phoneNumberId
        val toPhone = settings.toPhone

        if (accessToken.isBlank() || phoneNumberId.isBlank() || toPhone.isBlank()) {
            return@withContext DeliveryResult.Failure(context.getString(R.string.delivery_whatsapp_not_configured))
        }

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
                .put("text", JSONObject().put("body", text))
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
}
