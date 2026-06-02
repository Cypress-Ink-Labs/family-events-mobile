package com.familyevents.explore

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class DateFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEKEND("This Weekend"),
    THIS_WEEK("This Week"),
}

enum class AgeFilter(val label: String, val min: Int, val max: Int?) {
    BABY("0-3", 0, 3),
    KID("4-8", 4, 8),
    OLDER("9+", 9, null),
}

internal fun DateFilter.dateRange(zone: ZoneId = ZoneId.systemDefault()): Pair<java.time.Instant, java.time.Instant> {
    val today = LocalDate.now(zone)
    val (from, to) = when (this) {
        DateFilter.TODAY -> today to today
        DateFilter.THIS_WEEKEND -> {
            val saturday = if (today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY) {
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
            } else {
                today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
            }
            saturday to saturday.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        }
        DateFilter.THIS_WEEK -> today to today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    }
    return from.atStartOfDay(zone).toInstant() to to.atTime(LocalTime.MAX).atZone(zone).toInstant()
}
