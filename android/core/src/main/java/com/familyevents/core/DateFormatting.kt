package com.familyevents.core

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateFormatting {
    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayDateKey(zoneId: ZoneId = ZoneId.systemDefault(), clock: Clock = Clock.system(zoneId)): String =
        LocalDate.now(clock.withZone(zoneId)).format(isoDate)

    fun addDays(toDateKey: String, days: Long): String =
        LocalDate.parse(toDateKey, isoDate).plusDays(days).format(isoDate)

    fun cardSubtitle(start: Instant, zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(zoneId)
            .format(start)
}
