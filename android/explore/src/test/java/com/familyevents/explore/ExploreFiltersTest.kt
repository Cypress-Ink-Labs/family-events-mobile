package com.familyevents.explore

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreFiltersTest {
    private val zone = ZoneId.of("America/Chicago")

    @Test
    fun todayDateRangeStartsAtMidnightAndEndsAtEndOfDay() {
        val (from, to) = DateFilter.TODAY.dateRange(zone)
        val today = LocalDate.now(zone)
        val expectedStart = today.atStartOfDay(zone).toInstant()
        assertEquals(expectedStart, from)
        assertTrue(to.isAfter(from))
        val nextDayStart = today.plusDays(1).atStartOfDay(zone).toInstant()
        assertTrue(to.isBefore(nextDayStart))
    }

    @Test
    fun thisWeekDateRangeEndsOnSunday() {
        val (from, to) = DateFilter.THIS_WEEK.dateRange(zone)
        val today = LocalDate.now(zone)
        val expectedStart = today.atStartOfDay(zone).toInstant()
        assertEquals(expectedStart, from)
        val endDate = ZonedDateTime.ofInstant(to, zone).toLocalDate()
        assertEquals(DayOfWeek.SUNDAY, endDate.dayOfWeek)
    }

    @Test
    fun thisWeekendDateRangeCoversWeekend() {
        val (from, to) = DateFilter.THIS_WEEKEND.dateRange(zone)
        val startDate = ZonedDateTime.ofInstant(from, zone).toLocalDate()
        val endDate = ZonedDateTime.ofInstant(to, zone).toLocalDate()
        assertEquals(DayOfWeek.SATURDAY, startDate.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, endDate.dayOfWeek)
    }

    @Test
    fun ageFilterEnumsHaveExpectedValues() {
        assertEquals(0, AgeFilter.BABY.min)
        assertEquals(3, AgeFilter.BABY.max)
        assertEquals(4, AgeFilter.KID.min)
        assertEquals(8, AgeFilter.KID.max)
        assertEquals(9, AgeFilter.OLDER.min)
        assertEquals(null, AgeFilter.OLDER.max)
    }

    @Test
    fun dateFilterEnumsHaveLabels() {
        assertEquals("Today", DateFilter.TODAY.label)
        assertEquals("This Weekend", DateFilter.THIS_WEEKEND.label)
        assertEquals("This Week", DateFilter.THIS_WEEK.label)
    }
}
