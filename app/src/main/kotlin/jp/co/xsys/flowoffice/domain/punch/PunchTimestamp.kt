package jp.co.xsys.flowoffice.domain.punch

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class PunchTimestamp(
    val workDate: String,
    val punchedAt: String,
)

object PunchTimestampFormatter {
    private val offsetDateTimeFormatter =
        DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXX")

    fun format(value: OffsetDateTime): PunchTimestamp = PunchTimestamp(
        workDate = value.toLocalDate().toString(),
        punchedAt = value.format(offsetDateTimeFormatter),
    )
}
