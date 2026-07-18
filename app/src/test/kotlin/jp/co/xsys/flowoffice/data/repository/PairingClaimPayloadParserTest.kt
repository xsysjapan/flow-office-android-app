package jp.co.xsys.flowoffice.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingClaimPayloadParserTest {
    @Test
    fun `parses documented QR payload`() {
        val payload = PairingClaimPayloadParser.fromQrJson(
            """{"url":"https://office.example.jp/api/devices/pairing/claim","claim_token":"1|secret"}""",
        )

        requireNotNull(payload)
        assertEquals("https://office.example.jp/api/devices/pairing/claim", payload.claimUrl)
        assertEquals("https://office.example.jp/api", payload.apiBaseUrl)
        assertEquals("1|secret", payload.claimToken)
    }

    @Test
    fun `rejects a QR with a different endpoint`() {
        assertNull(
            PairingClaimPayloadParser.fromQrJson(
                """{"url":"https://evil.example/api/token","claim_token":"1|secret"}""",
            ),
        )
    }

    @Test
    fun `builds claim endpoint from manual API base URL`() {
        val payload = PairingClaimPayloadParser.fromManualInput(
            apiBaseUrl = "https://office.example.jp/api/",
            claimToken = "1|secret",
        )

        requireNotNull(payload)
        assertEquals("https://office.example.jp/api/devices/pairing/claim", payload.claimUrl)
    }
}
