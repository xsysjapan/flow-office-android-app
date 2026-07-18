package jp.co.xsys.flowoffice.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import org.json.JSONException
import org.json.JSONObject

class PairingApiClient {
    fun exchangePairingCode(
        apiBaseUrl: String,
        deviceId: Long,
        pairingCode: String,
    ): PairingExchangeResponse {
        val endpoint = URL(buildPairingExchangeUrl(apiBaseUrl))
        val connection = endpoint.openConnection() as HttpURLConnection

        try {
            val requestJson = JSONObject()
                .put("device_id", deviceId)
                .put("pairing_code", pairingCode)
                .toString()

            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            connection.outputStream.use { output ->
                output.write(requestJson.toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            val responseBody = readResponseBody(connection, statusCode)

            if (statusCode in 200..299) {
                return parseSuccess(responseBody)
            }

            throw PairingApiException(
                statusCode = statusCode,
                safeMessage = parseErrorMessage(statusCode, responseBody),
            )
        } catch (exception: PairingApiException) {
            throw exception
        } catch (exception: SocketTimeoutException) {
            throw PairingApiException(
                statusCode = null,
                safeMessage = "接続がタイムアウトしました。ネットワークまたはAPIサーバーURLを確認してください。",
                cause = exception,
            )
        } catch (exception: IOException) {
            throw PairingApiException(
                statusCode = null,
                safeMessage = "APIサーバーに接続できませんでした。URLとネットワークを確認してください。",
                cause = exception,
            )
        } catch (exception: JSONException) {
            throw PairingApiException(
                statusCode = null,
                safeMessage = "APIサーバーの応答を読み取れませんでした。サーバー設定を確認してください。",
                cause = exception,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun buildPairingExchangeUrl(apiBaseUrl: String): String {
        val baseUrl = apiBaseUrl.trim().trimEnd('/')
        return if (baseUrl.endsWith("/api")) {
            "$baseUrl/devices/pairing/exchange"
        } else {
            "$baseUrl/api/devices/pairing/exchange"
        }
    }

    private fun readResponseBody(connection: HttpURLConnection, statusCode: Int): String {
        val stream = if (statusCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun parseSuccess(responseBody: String): PairingExchangeResponse {
        val json = JSONObject(responseBody)
        val token = json.optString("token")
        if (token.isBlank()) {
            throw PairingApiException(
                statusCode = null,
                safeMessage = "APIサーバーから端末トークンが返りませんでした。",
            )
        }

        return PairingExchangeResponse(
            token = token,
            deviceJson = json.optJSONObject("device")?.toString(),
        )
    }

    private fun parseErrorMessage(statusCode: Int, responseBody: String): String {
        val serverMessage = runCatching {
            JSONObject(responseBody).optString("message").takeIf { it.isNotBlank() }
        }.getOrNull()

        return when (statusCode) {
            400 -> "送信内容が正しくありません。端末IDとコードを確認してください。"
            401 -> "アクティベーションコードが無効です。管理者に再発行を依頼してください。"
            403 -> "この端末にはアクティベーション権限がありません。管理者に確認してください。"
            404 -> "端末が見つかりません。端末IDを確認してください。"
            422 -> serverMessage ?: "端末IDまたはコードが正しくありません。"
            429 -> "試行回数が多すぎます。しばらく待ってから再試行してください。"
            in 500..599 -> "APIサーバーでエラーが発生しました。時間を置いて再試行してください。"
            else -> serverMessage ?: "アクティベーションに失敗しました。HTTP $statusCode"
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}

data class PairingExchangeResponse(
    val token: String,
    val deviceJson: String?,
)

class PairingApiException(
    val statusCode: Int?,
    val safeMessage: String,
    cause: Throwable? = null,
) : Exception(safeMessage, cause)
