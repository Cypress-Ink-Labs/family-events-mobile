package com.familyevents.core

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormattingTest {
    @Test
    fun dateKeysUseIsoDates() {
        val clock = Clock.fixed(Instant.parse("2026-05-16T12:00:00Z"), ZoneOffset.UTC)
        assertEquals("2026-05-16", DateFormatting.todayDateKey(ZoneOffset.UTC, clock))
        assertEquals("2026-05-18", DateFormatting.addDays("2026-05-16", 2))
    }
}
