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
    fun sendPunch(request: PunchRequest): PunchResponse {
        val endpoint = URL(buildDevicePunchUrl(request.apiBaseUrl))
        val connection = endpoint.openConnection() as HttpURLConnection

        try {
            val requestJson = request.toJsonString()

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
            return parsePunchResponse(connection.inputStream.bufferedReader().use { it.readText() })
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

    fun sendHeartbeat(request: HeartbeatRequest) {
        val endpoint = URL(buildApiUrl(request.apiBaseUrl, "devices/heartbeat"))
        val connection = endpoint.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer ${request.token}")
            connection.setRequestProperty("X-Flow-Office-App-Instance-Id", request.appInstanceId)
            connection.outputStream.use { output ->
                output.write(JSONObject().put("app_version", request.appVersion).toString().toByteArray(Charsets.UTF_8))
            }
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                throw PunchApiException(statusCode = statusCode, error = parseError(statusCode))
            }
        } catch (exception: PunchApiException) {
            throw exception
        } catch (exception: SocketTimeoutException) {
            throw PunchApiException(null, AppError.PunchTimeout, exception)
        } catch (exception: IOException) {
            throw PunchApiException(null, AppError.PunchConnectionFailed, exception)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildDevicePunchUrl(apiBaseUrl: String): String {
        return buildApiUrl(apiBaseUrl, "device-punches")
    }

    private fun buildApiUrl(apiBaseUrl: String, path: String): String {
        val baseUrl = apiBaseUrl.trim().trimEnd('/')
        return if (baseUrl.endsWith("/api")) "$baseUrl/$path" else "$baseUrl/api/$path"
    }

    private fun parsePunchResponse(body: String): PunchResponse {
        val json = JSONObject(body)
        val summary = json.optJSONObject("attendance_summary")
        return PunchResponse(
            employeeName = json.optString("user_name").takeIf(String::isNotBlank),
            punchedAt = json.optString("punched_at").takeIf(String::isNotBlank),
            workMinutes = summary?.takeIf { it.has("work_minutes") && !it.isNull("work_minutes") }
                ?.optInt("work_minutes")
                ?.takeIf { it >= 0 },
            missingPunchCount = summary?.optInt("missing_punch_count") ?: 0,
            currentDayIncomplete = summary?.optBoolean("current_day_incomplete") ?: false,
        )
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

data class PunchResponse(
    val employeeName: String?,
    val punchedAt: String?,
    val workMinutes: Int?,
    val missingPunchCount: Int,
    val currentDayIncomplete: Boolean,
)

data class HeartbeatRequest(
    val apiBaseUrl: String,
    val token: String,
    val appInstanceId: String,
    val appVersion: String,
)

data class PunchRequest(
    val apiBaseUrl: String,
    val token: String,
    val appInstanceId: String,
    val punchType: PunchType,
    val workDate: String,
    val punchedAt: String,
    val authenticationKeyValue: String,
    val idempotencyKey: String,
) {
    fun toJsonString(): String = JSONObject()
        .put("work_date", workDate)
        .put("punch_type", punchType.apiValue)
        .put("punched_at", punchedAt)
        .put("authentication_key_value", authenticationKeyValue)
        .put("offline", false)
        .put("idempotency_key", idempotencyKey)
        .toString()
}

class PunchApiException(
    val statusCode: Int?,
    val error: AppError,
    cause: Throwable? = null,
) : Exception(error.name, cause)
