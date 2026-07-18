package jp.co.xsys.flowoffice.data.security

import org.json.JSONException
import org.json.JSONObject

data class DeviceCapabilities(
    val canPunch: Boolean,
)

object DeviceCapabilitiesParser {
    fun parse(deviceJson: String?): DeviceCapabilities {
        if (deviceJson.isNullOrBlank()) return DeviceCapabilities(canPunch = false)

        return try {
            val json = JSONObject(deviceJson)
            val roles = json.optJSONArray("roles").toStringSet()
            val scopes = json.optJSONArray("scopes").toStringSet()
            DeviceCapabilities(
                canPunch = roles.any { it in PUNCH_ROLES } || PUNCH_SCOPE in scopes,
            )
        } catch (_: JSONException) {
            DeviceCapabilities(canPunch = false)
        }
    }

    private fun org.json.JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return buildSet {
            repeat(length()) { index ->
                optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }

    private const val PUNCH_SCOPE = "attendance:clock"
    private val PUNCH_ROLES = setOf(
        "attendance_reader",
        "personal_operation",
        "admin_operation",
    )
}
