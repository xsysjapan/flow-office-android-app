package jp.co.xsys.flowoffice.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingClaimPayloadParserTest {
    @Test
    fun `parses claim URL with token from QR`() {
        val payload = PairingClaimPayloadParser.fromQrUrl(
            "https://office.example.jp/flow-office/api/devices/pairing/claim?claim_token=1%7Csecret",
        )

        requireNotNull(payload)
        assertEquals("https://office.example.jp/flow-office/api/devices/pairing/claim", payload.claimUrl)
        assertEquals("1|secret", payload.claimToken)
    }

    @Test
    fun `rejects a QR with a different endpoint`() {
        assertNull(
            PairingClaimPayloadParser.fromQrUrl(
                "https://evil.example/api/token?claim_token=1%7Csecret",
            ),
        )
    }

    @Test
    fun `rejects a QR without a claim token`() {
        assertNull(
            PairingClaimPayloadParser.fromQrUrl(
                "https://office.example.jp/flow-office/api/devices/pairing/claim",
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
