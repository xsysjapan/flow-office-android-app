package jp.co.xsys.flowoffice.data.remote

import jp.co.xsys.flowoffice.domain.punch.PunchType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PunchRequestTest {
    @Test
    fun `encodes the current device punch API contract`() {
        val json = JSONObject(
            PunchRequest(
                apiBaseUrl = "https://office.example.jp/api",
                token = "secret",
                appInstanceId = "app-instance",
                punchType = PunchType.CLOCK_IN,
                workDate = "2026-07-19",
                punchedAt = "2026-07-19T09:00:12+09:00",
                authenticationKeyValue = "04A22419CC2180",
                idempotencyKey = "request-id",
            ).toJsonString(),
        )

        assertEquals("2026-07-19", json.getString("work_date"))
        assertEquals("clock_in", json.getString("punch_type"))
        assertEquals("2026-07-19T09:00:12+09:00", json.getString("punched_at"))
        assertEquals("04A22419CC2180", json.getString("authentication_key_value"))
        assertEquals("request-id", json.getString("idempotency_key"))
        assertFalse(json.getBoolean("offline"))
    }
}
