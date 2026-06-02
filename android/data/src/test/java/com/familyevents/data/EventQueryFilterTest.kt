package com.familyevents.data

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.GeoCoordinate
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventQueryFilterTest {
    private val freeEvent = EventDto(
        id = EventId("free-1"),
        title = "Free Storytime",
        description = null,
        startsAt = Instant.parse("2026-06-10T15:00:00Z"),
        endsAt = null,
        venueName = "Library",
        address = null,
        ageMin = 0,
        ageMax = 5,
        isFree = true,
        imageUrl = null,
        sourceUrl = null,
        cityId = CityId("test"),
        coordinate = null,
    )

    private val paidEvent = EventDto(
        id = EventId("paid-1"),
        title = "Paid Concert",
        description = null,
        startsAt = Instant.parse("2026-06-12T19:00:00Z"),
        endsAt = null,
        venueName = "Arena",
        address = null,
        ageMin = 8,
        ageMax = 16,
        price = 25.0,
        isFree = false,
        imageUrl = null,
        sourceUrl = null,
        cityId = CityId("test"),
        coordinate = null,
    )

    private val allAgesEvent = EventDto(
        id = EventId("all-ages"),
        title = "Festival",
        description = null,
        startsAt = Instant.parse("2026-06-11T10:00:00Z"),
        endsAt = null,
        venueName = "Park",
        address = null,
        ageMin = null,
        ageMax = null,
        isFree = false,
        imageUrl = null,
        sourceUrl = null,
        cityId = CityId("test"),
        coordinate = null,
    )

    private fun filterEvents(query: EventQuery, events: List<EventDto>): List<EventDto> {
        return events
            .filter { query.search.isNullOrBlank() || it.title.contains(query.search, ignoreCase = true) }
            .filter { query.ageMin == null || (it.ageMax ?: Int.MAX_VALUE) >= query.ageMin }
            .filter { query.ageMax == null || (it.ageMin ?: 0) <= query.ageMax }
            .filter { query.isFree == null || it.isFree == query.isFree }
            .filter { query.dateFrom == null || !it.startsAt.isBefore(query.dateFrom) }
            .filter { query.dateTo == null || !it.startsAt.isAfter(query.dateTo) }
    }

    @Test
    fun freeFilterReturnsFreeEventsOnly() {
        val query = EventQuery(cityId = null, isFree = true)
        val result = filterEvents(query, listOf(freeEvent, paidEvent, allAgesEvent))
        assertEquals(1, result.size)
        assertEquals("free-1", result[0].id.rawValue)
    }

    @Test
    fun ageFilterMatchesOverlappingRange() {
        val query = EventQuery(cityId = null, ageMin = 0, ageMax = 3)
        val result = filterEvents(query, listOf(freeEvent, paidEvent, allAgesEvent))
        assertEquals(2, result.size)
        assertTrue(result.any { it.id.rawValue == "free-1" })
        assertTrue(result.any { it.id.rawValue == "all-ages" })
    }

    @Test
    fun ageFilterExcludesNonOverlappingRange() {
        val query = EventQuery(cityId = null, ageMin = 6, ageMax = 7)
        val result = filterEvents(query, listOf(freeEvent, paidEvent, allAgesEvent))
        assertEquals(1, result.size)
        assertEquals("all-ages", result[0].id.rawValue)
    }

    @Test
    fun dateRangeFilterNarrowsResults() {
        val query = EventQuery(
            cityId = null,
            dateFrom = Instant.parse("2026-06-10T00:00:00Z"),
            dateTo = Instant.parse("2026-06-11T23:59:59Z"),
        )
        val result = filterEvents(query, listOf(freeEvent, paidEvent, allAgesEvent))
        assertEquals(2, result.size)
        assertTrue(result.none { it.id.rawValue == "paid-1" })
    }

    @Test
    fun combinedFiltersStack() {
        val query = EventQuery(
            cityId = null,
            isFree = true,
            ageMin = 0,
            ageMax = 3,
        )
        val result = filterEvents(query, listOf(freeEvent, paidEvent, allAgesEvent))
        assertEquals(1, result.size)
        assertEquals("free-1", result[0].id.rawValue)
    }

    @Test
    fun noFiltersReturnAll() {
        val query = EventQuery(cityId = null)
        val result = filterEvents(query, listOf(freeEvent, paidEvent, allAgesEvent))
        assertEquals(3, result.size)
    }

    @Test
    fun inMemoryRepositoryAppliesFilters() = runBlocking {
        val repo = InMemoryEventRepository()
        val allEvents = repo.observeEventList(EventQuery(cityId = null)).first()
        assertTrue(allEvents.isNotEmpty())

        val freeOnly = repo.observeEventList(EventQuery(cityId = null, isFree = true)).first()
        assertTrue(freeOnly.all { it.isFree })
    }
}
