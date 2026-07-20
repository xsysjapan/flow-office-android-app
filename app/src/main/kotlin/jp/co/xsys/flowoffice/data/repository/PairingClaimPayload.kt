package jp.co.xsys.flowoffice.data.repository

import java.net.URI
import java.net.URLDecoder
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets

data class PairingClaimPayload(
    val claimUrl: String,
    val claimToken: String,
)

object PairingClaimPayloadParser {
    private const val CLAIM_PATH = "/devices/pairing/claim"
    private const val CLAIM_TOKEN_QUERY = "claim_token"

    fun fromQrUrl(rawValue: String): PairingClaimPayload? {
        val normalizedUrl = rawValue.trim()
        val uri = parseUri(normalizedUrl) ?: return null
        if (!isValidClaimEndpoint(uri) || uri.rawQuery.isNullOrBlank()) return null

        val queryParameters = parseQuery(uri.rawQuery) ?: return null
        if (queryParameters.keys != setOf(CLAIM_TOKEN_QUERY)) return null
        val claimToken = queryParameters[CLAIM_TOKEN_QUERY]?.singleOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return PairingClaimPayload(
            claimUrl = normalizedUrl.substringBefore('?'),
            claimToken = claimToken,
        )
    }

    fun fromClaimUrl(claimUrl: String, claimToken: String): PairingClaimPayload? {
        val normalizedUrl = claimUrl.trim()
        val uri = parseUri(normalizedUrl) ?: return null
        if (!isValidClaimEndpoint(uri) || uri.query != null || claimToken.isBlank()) return null

        return PairingClaimPayload(
            claimUrl = normalizedUrl,
            claimToken = claimToken.trim(),
        )
    }

    private fun isValidClaimEndpoint(uri: URI): Boolean =
        uri.scheme in setOf("http", "https") &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.fragment == null &&
            uri.path.endsWith(CLAIM_PATH)

    private fun parseUri(value: String): URI? = try {
        URI(value)
    } catch (_: URISyntaxException) {
        null
    }

    private fun parseQuery(rawQuery: String): Map<String, List<String>>? = try {
        rawQuery.split('&').map { parameter ->
            val separatorIndex = parameter.indexOf('=')
            if (separatorIndex < 0) return null
            decode(parameter.substring(0, separatorIndex)) to
                decode(parameter.substring(separatorIndex + 1))
        }.groupBy({ it.first }, { it.second })
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
