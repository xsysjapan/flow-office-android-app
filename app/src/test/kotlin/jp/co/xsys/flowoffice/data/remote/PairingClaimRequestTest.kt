package jp.co.xsys.flowoffice.data.remote

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class PairingClaimRequestTest {
    @Test
    fun `encodes the app instance id as id`() {
        val json = JSONObject(
            PairingClaimRequest(id = "550e8400-e29b-41d4-a716-446655440000").toJsonString(),
        )

        assertEquals("550e8400-e29b-41d4-a716-446655440000", json.getString("id"))
    }
}
