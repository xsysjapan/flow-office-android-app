package jp.co.xsys.flowoffice.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import jp.co.xsys.flowoffice.domain.error.AppError
import jp.co.xsys.flowoffice.domain.punch.PunchType
import org.json.JSONException
import org.json.JSONObject

class PunchApiClient {
    fun sendPunch(request: PunchRequest) {
        val endpoint = URL(buildDevicePunchUrl(request.apiBaseUrl))
        val connection = endpoint.openConnection() as HttpURLConnection

        try {
            val requestJson = JSONObject()
                .put("punch_type", request.punchType.apiValue)
                .put("punched_at", request.punchedAt)
                .put("authentication_key_value", request.authenticationKeyValue)
                .put("idempotency_key", request.idempotencyKey)
                .toString()

            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer ${request.token}")
            connection.setRequestProperty("X-Flow-Office-App-Instance-Id", request.appInstanceId)

            connection.outputStream.use { output ->
                output.write(requestJson.toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                throw PunchApiException(
                    statusCode = statusCode,
                    error = parseError(statusCode),
                )
            }
        } catch (exception: PunchApiException) {
            throw exception
        } catch (exception: SocketTimeoutException) {
            throw PunchApiException(
                statusCode = null,
                error = AppError.PunchTimeout,
                cause = exception,
            )
        } catch (exception: IOException) {
            throw PunchApiException(
                statusCode = null,
                error = AppError.PunchConnectionFailed,
                cause = exception,
            )
        } catch (exception: JSONException) {
            throw PunchApiException(
                statusCode = null,
                error = AppError.PunchResponseInvalid,
                cause = exception,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun buildDevicePunchUrl(apiBaseUrl: String): String {
        val baseUrl = apiBaseUrl.trim().trimEnd('/')
        return if (baseUrl.endsWith("/api")) {
            "$baseUrl/device-punches"
        } else {
            "$baseUrl/api/device-punches"
        }
    }

    private fun parseError(statusCode: Int): AppError = when (statusCode) {
        401 -> AppError.PunchUnauthorized
        403 -> AppError.PunchForbidden
        422 -> AppError.PunchValidationFailed
        429 -> AppError.PunchRateLimited
        in 500..599 -> AppError.PunchServerError
        else -> AppError.PunchUnknown
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}

data class PunchRequest(
    val apiBaseUrl: String,
    val token: String,
    val appInstanceId: String,
    val punchType: PunchType,
    val punchedAt: String,
    val authenticationKeyValue: String,
    val idempotencyKey: String,
)

class PunchApiException(
    val statusCode: Int?,
    val error: AppError,
    cause: Throwable? = null,
) : Exception(error.name, cause)
