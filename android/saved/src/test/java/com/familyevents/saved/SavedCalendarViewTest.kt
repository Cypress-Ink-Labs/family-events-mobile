package com.familyevents.saved

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventDto
import com.familyevents.data.FavoriteDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SavedCalendarViewTest {
    private val zoneId: ZoneId = ZoneId.of("America/Chicago")

    @Test
    fun getEventsForDay_includesSameDayLocalEvent() {
        val event = event(id = "evt_same_day", startsAt = Instant.parse("2026-03-10T15:30:00Z"))

        val result = getEventsForDay(
            date = LocalDate.of(2026, 3, 10),
            favorites = listOf(favorite(event.id)),
            eventsById = mapOf(event.id to event),
            zoneId = zoneId,
        )

        assertEquals(listOf(event), result)
    }

    @Test
    fun getEventsForDay_excludesEventOutsideSelectedDate() {
        val event = event(id = "evt_next_day", startsAt = Instant.parse("2026-03-11T15:30:00Z"))

        val result = getEventsForDay(
            date = LocalDate.of(2026, 3, 10),
            favorites = listOf(favorite(event.id)),
            eventsById = mapOf(event.id to event),
            zoneId = zoneId,
        )

        assertEquals(emptyList<EventDto>(), result)
    }

    @Test
    fun getEventsForDay_handlesMidnightBoundaryInLocalZone() {
        val beforeMidnight = event(id = "evt_before_midnight", startsAt = Instant.parse("2026-03-11T04:59:59Z"))
        val atMidnight = event(id = "evt_at_midnight", startsAt = Instant.parse("2026-03-11T05:00:00Z"))

        val result = getEventsForDay(
            date = LocalDate.of(2026, 3, 11),
            favorites = listOf(favorite(beforeMidnight.id), favorite(atMidnight.id)),
            eventsById = mapOf(beforeMidnight.id to beforeMidnight, atMidnight.id to atMidnight),
            zoneId = zoneId,
        )

        assertEquals(listOf(atMidnight), result)
    }

    @Test
    fun getEventsForDay_handlesDstTransitionUsingLocalDate() {
        val preJump = event(id = "evt_pre_jump", startsAt = Instant.parse("2026-03-08T07:30:00Z"))
        val postJump = event(id = "evt_post_jump", startsAt = Instant.parse("2026-03-08T08:30:00Z"))
        val utcNextDayButLocalSameDay = event(id = "evt_utc_next_day", startsAt = Instant.parse("2026-03-09T04:30:00Z"))

        val result = getEventsForDay(
            date = LocalDate.of(2026, 3, 8),
            favorites = listOf(favorite(preJump.id), favorite(postJump.id), favorite(utcNextDayButLocalSameDay.id)),
            eventsById = mapOf(preJump.id to preJump, postJump.id to postJump, utcNextDayButLocalSameDay.id to utcNextDayButLocalSameDay),
            zoneId = zoneId,
        )

        assertEquals(listOf(preJump, postJump, utcNextDayButLocalSameDay), result)
    }

    private fun event(id: String, startsAt: Instant): EventDto = EventDto(
        id = EventId(id),
        title = "Test Event",
        description = null,
        startsAt = startsAt,
        endsAt = null,
        venueName = null,
        address = null,
        imageUrl = null,
        sourceUrl = null,
        cityId = CityId("city_1"),
        coordinate = null,
    )

    private fun favorite(eventId: EventId): FavoriteDto = FavoriteDto(
        eventId = eventId,
        userId = UserId("user_1"),
        createdAt = Instant.EPOCH,
    )
}
