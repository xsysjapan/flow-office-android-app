package jp.co.xsys.flowoffice.domain.punch

import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PunchTimestampFormatterTest {
    @Test
    fun `formats work date and offset timestamp required by backend`() {
        val timestamp = PunchTimestampFormatter.format(
            OffsetDateTime.parse("2026-07-19T09:00:00+09:00"),
        )

        assertEquals("2026-07-19", timestamp.workDate)
        assertEquals("2026-07-19T09:00:00.000+09:00", timestamp.punchedAt)
    }
}
