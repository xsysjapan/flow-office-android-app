package jp.co.xsys.flowoffice.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import jp.co.xsys.flowoffice.domain.error.AppError
import org.json.JSONException
import org.json.JSONObject

class PairingApiClient {
    fun claimPairing(
        claimUrl: String,
        claimToken: String,
        id: String,
    ): PairingClaimResponse {
        val endpoint = URL(claimUrl)
        val connection = endpoint.openConnection() as HttpURLConnection

        try {
            val requestJson = PairingClaimRequest(id).toJsonString()

            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer $claimToken")
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
                error = parseError(statusCode, responseBody),
            )
        } catch (exception: PairingApiException) {
            throw exception
        } catch (exception: SocketTimeoutException) {
            throw PairingApiException(
                statusCode = null,
                error = AppError.PairingTimeout,
                cause = exception,
            )
        } catch (exception: IOException) {
            throw PairingApiException(
                statusCode = null,
                error = AppError.PairingConnectionFailed,
                cause = exception,
            )
        } catch (exception: JSONException) {
            throw PairingApiException(
                statusCode = null,
                error = AppError.PairingResponseInvalid,
                cause = exception,
            )
        } finally {
            connection.disconnect()
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

    private fun parseSuccess(responseBody: String): PairingClaimResponse {
        val json = JSONObject(responseBody)
        val token = json.optString("token")
        val apiBaseUrl = json.optString("api_base_url")
        if (token.isBlank()) {
            throw PairingApiException(
                statusCode = null,
                error = AppError.PairingTokenMissing,
            )
        }
        if (apiBaseUrl.isBlank()) {
            throw PairingApiException(
                statusCode = null,
                error = AppError.PairingResponseInvalid,
            )
        }

        val device = json.optJSONObject("device")
        val deviceId = device?.optString("id")?.takeIf { it.isNotBlank() }
            ?: throw PairingApiException(
                statusCode = null,
                error = AppError.PairingResponseInvalid,
            )

        return PairingClaimResponse(
            token = token,
            apiBaseUrl = apiBaseUrl,
            deviceId = deviceId,
            deviceJson = device.toString(),
        )
    }

    private fun parseError(statusCode: Int, responseBody: String): AppError {
        return when (statusCode) {
            400 -> AppError.PairingBadRequest
            401 -> AppError.PairingUnauthorized
            403 -> AppError.PairingForbidden
            404 -> AppError.PairingNotFound
            422 -> AppError.PairingValidationFailed
            429 -> AppError.PairingRateLimited
            in 500..599 -> AppError.PairingServerError
            else -> AppError.PairingUnknown
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}

data class PairingClaimRequest(
    val id: String,
) {
    fun toJsonString(): String = JSONObject()
        .put("id", id)
        .toString()
}

data class PairingClaimResponse(
    val token: String,
    val apiBaseUrl: String,
    val deviceId: String,
    val deviceJson: String,
)

class PairingApiException(
    val statusCode: Int?,
    val error: AppError,
    cause: Throwable? = null,
) : Exception(error.name, cause)
