package jp.co.xsys.flowoffice.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import jp.co.xsys.flowoffice.domain.error.AppError
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class DeviceAdminApiClient {
    fun getBootstrap(request: DeviceAdminRequest): AdminBootstrap =
        execute(request, "GET", "/devices/me/admin-bootstrap") { AdminJsonParser.parseBootstrap(it) }

    fun registerBootstrapCard(
        request: DeviceAdminRequest,
        adminUserId: Long?,
        rawKeyValue: String,
    ): AdminSession = execute(
        request,
        "POST",
        "/devices/me/admin-bootstrap/authentication-keys",
        cardBody(rawKeyValue, "管理者ICカード").apply {
            adminUserId?.let { put("admin_user_id", it) }
        },
    ) { AdminJsonParser.parseSession(JSONObject(it).getJSONObject("admin_session")) }

    fun startSession(request: DeviceAdminRequest, rawKeyValue: String): AdminSession = execute(
        request,
        "POST",
        "/devices/me/admin-sessions",
        JSONObject().put("raw_key_value", rawKeyValue),
    ) { AdminJsonParser.parseSession(JSONObject(it).getJSONObject("admin_session")) }

    fun endSession(request: DeviceAdminRequest) {
        execute(request, "POST", "/devices/me/admin-sessions/current/end", JSONObject()) { Unit }
    }

    fun getUsers(request: DeviceAdminRequest, query: String): List<AdminUser> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        return execute(request, "GET", "/devices/me/admin/users?q=$encoded") {
            AdminJsonParser.parseUsers(it)
        }
    }

    fun getAuthenticationKeys(
        request: DeviceAdminRequest,
        userId: Long,
    ): List<AuthenticationKeySummary> = execute(
        request,
        "GET",
        "/devices/me/admin/users/$userId/authentication-keys",
    ) { AdminJsonParser.parseAuthenticationKeys(it) }

    fun registerAuthenticationKey(
        request: DeviceAdminRequest,
        userId: Long,
        rawKeyValue: String,
    ): AuthenticationKeySummary = execute(
        request,
        "POST",
        "/devices/me/admin/users/$userId/authentication-keys",
        cardBody(rawKeyValue, "社員証NFC"),
    ) { AdminJsonParser.parseAuthenticationKeyResponse(it) }

    private fun cardBody(rawKeyValue: String, displayName: String) = JSONObject()
        .put("key_type", "nfc_uid")
        .put("display_name", displayName)
        .put("raw_key_value", rawKeyValue)

    private fun <T> execute(
        request: DeviceAdminRequest,
        method: String,
        path: String,
        body: JSONObject? = null,
        parser: (String) -> T,
    ): T {
        val connection = URL(buildApiUrl(request.apiBaseUrl, path)).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${request.token}")
            connection.setRequestProperty("X-Flow-Office-App-Instance-Id", request.appInstanceId)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }

            val statusCode = connection.responseCode
            val responseBody = readResponseBody(connection, statusCode)
            if (statusCode !in 200..299) {
                throw DeviceAdminApiException(statusCode, parseError(statusCode))
            }
            return parser(responseBody)
        } catch (exception: DeviceAdminApiException) {
            throw exception
        } catch (exception: SocketTimeoutException) {
            throw DeviceAdminApiException(null, AppError.AdminTimeout, exception)
        } catch (exception: IOException) {
            throw DeviceAdminApiException(null, AppError.AdminConnectionFailed, exception)
        } catch (exception: JSONException) {
            throw DeviceAdminApiException(null, AppError.AdminResponseInvalid, exception)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildApiUrl(apiBaseUrl: String, path: String): String {
        val base = apiBaseUrl.trim().trimEnd('/')
        return if (base.endsWith("/api")) "$base$path" else "$base/api$path"
    }

    private fun readResponseBody(connection: HttpURLConnection, statusCode: Int): String {
        val stream = if (statusCode in 200..299) connection.inputStream
        else connection.errorStream ?: connection.inputStream
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun parseError(statusCode: Int): AppError = when (statusCode) {
        401 -> AppError.AdminUnauthorized
        403 -> AppError.AdminForbidden
        422 -> AppError.AdminValidationFailed
        429 -> AppError.AdminRateLimited
        in 500..599 -> AppError.AdminServerError
        else -> AppError.AdminUnknown
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}

object AdminJsonParser {
    fun parseBootstrap(body: String): AdminBootstrap {
        val json = JSONObject(body)
        return when (json.getString("mode")) {
            "self" -> AdminBootstrap.Self(parseUser(json.getJSONObject("admin_user")))
            "select" -> AdminBootstrap.Select(parseUserArray(json.getJSONArray("admin_users")))
            else -> throw JSONException("Unknown bootstrap mode")
        }
    }

    fun parseUsers(body: String): List<AdminUser> {
        val value = org.json.JSONTokener(body).nextValue()
        val array = when (value) {
            is JSONArray -> value
            is JSONObject -> value.optJSONArray("data") ?: throw JSONException("Missing users")
            else -> throw JSONException("Invalid users")
        }
        return parseUserArray(array)
    }

    fun parseAuthenticationKeys(body: String): List<AuthenticationKeySummary> {
        val value = org.json.JSONTokener(body).nextValue()
        val array = when (value) {
            is JSONArray -> value
            is JSONObject -> value.optJSONArray("data") ?: throw JSONException("Missing keys")
            else -> throw JSONException("Invalid keys")
        }
        return List(array.length()) { parseAuthenticationKey(array.getJSONObject(it)) }
    }

    fun parseSession(json: JSONObject) = AdminSession(
        id = json.getLong("id"),
        adminUser = parseUser(json.getJSONObject("admin_user")),
        source = json.getString("source"),
        expiresAt = json.getString("expires_at"),
    )

    fun parseAuthenticationKey(json: JSONObject) = AuthenticationKeySummary(
        id = json.getLong("id"),
        displayName = json.optString("display_name", "社員証NFC"),
        status = json.optString("status"),
    )

    fun parseAuthenticationKeyResponse(body: String): AuthenticationKeySummary {
        val json = JSONObject(body)
        return parseAuthenticationKey(json.optJSONObject("data") ?: json)
    }

    private fun parseUserArray(array: JSONArray) = List(array.length()) {
        parseUser(array.getJSONObject(it))
    }

    private fun parseUser(json: JSONObject) = AdminUser(
        id = json.getLong("id"),
        name = json.getString("name"),
        email = json.optionalString("email"),
        department = json.optionalString("department"),
    )

    private fun JSONObject.optionalString(name: String): String =
        if (isNull(name)) "" else optString(name)
}

data class DeviceAdminRequest(
    val apiBaseUrl: String,
    val token: String,
    val appInstanceId: String,
)

sealed interface AdminBootstrap {
    data class Self(val adminUser: AdminUser) : AdminBootstrap
    data class Select(val adminUsers: List<AdminUser>) : AdminBootstrap
}

data class AdminUser(val id: Long, val name: String, val email: String, val department: String)
data class AdminSession(val id: Long, val adminUser: AdminUser, val source: String, val expiresAt: String)
data class AuthenticationKeySummary(val id: Long, val displayName: String, val status: String)

class DeviceAdminApiException(
    val statusCode: Int?,
    val error: AppError,
    cause: Throwable? = null,
) : Exception(error.name, cause)
