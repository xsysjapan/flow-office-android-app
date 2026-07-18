package jp.co.xsys.flowoffice.data.repository

import java.net.URI
import org.json.JSONException
import org.json.JSONObject

data class PairingClaimPayload(
    val claimUrl: String,
    val claimToken: String,
    val apiBaseUrl: String,
)

object PairingClaimPayloadParser {
    private const val CLAIM_PATH = "/api/devices/pairing/claim"

    fun fromQrJson(rawValue: String): PairingClaimPayload? = try {
        val json = JSONObject(rawValue)
        fromClaimUrl(
            claimUrl = json.optString("url"),
            claimToken = json.optString("claim_token"),
        )
    } catch (_: JSONException) {
        null
    }

    fun fromManualInput(apiBaseUrl: String, claimToken: String): PairingClaimPayload? {
        val normalizedBaseUrl = apiBaseUrl.trim().trimEnd('/')
        val claimUrl = if (normalizedBaseUrl.endsWith("/api")) {
            "$normalizedBaseUrl/devices/pairing/claim"
        } else {
            "$normalizedBaseUrl$CLAIM_PATH"
        }
        return fromClaimUrl(claimUrl, claimToken)
    }

    private fun fromClaimUrl(claimUrl: String, claimToken: String): PairingClaimPayload? = try {
        val normalizedUrl = claimUrl.trim()
        val uri = URI(normalizedUrl)
        if (
            uri.scheme !in setOf("http", "https") ||
            uri.host.isNullOrBlank() ||
            uri.userInfo != null ||
            uri.query != null ||
            uri.fragment != null ||
            uri.path != CLAIM_PATH ||
            claimToken.isBlank()
        ) {
            return null
        }

        PairingClaimPayload(
            claimUrl = normalizedUrl,
            claimToken = claimToken.trim(),
            apiBaseUrl = normalizedUrl.removeSuffix("/devices/pairing/claim"),
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}
