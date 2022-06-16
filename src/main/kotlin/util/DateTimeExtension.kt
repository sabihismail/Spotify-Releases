package util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object DateTimeExtension {
    fun LocalDateTime.toEpochSecond(zone: ZoneId = ZoneId.systemDefault()): Long {
        return this.atZone(zone).toEpochSecond()
    }

    fun Long?.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault(), default: LocalDateTime = LocalDateTime.MIN): LocalDateTime {
        return if (this == null) default else Instant.ofEpochSecond(this).atZone(zone).toLocalDateTime()
    }
}