package com.familyevents.data

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventQueryFilterTest {
    private fun event(
        id: String = "e",
        title: String = "Storytime",
        startsAt: Instant = Instant.parse("2026-06-10T15:00:00Z"),
        ageMin: Int? = null,
        ageMax: Int? = null,
        isFree: Boolean = false,
        tags: List<EventTagDto> = emptyList(),
    ) = EventDto(
        id = EventId(id),
        title = title,
        description = null,
        startsAt = startsAt,
        endsAt = null,
        venueName = null,
        address = null,
        ageMin = ageMin,
        ageMax = ageMax,
        isFree = isFree,
        imageUrl = null,
        sourceUrl = null,
        cityId = CityId("test"),
        coordinate = null,
        tags = tags,
    )

    @Test
    fun emptyQueryMatchesAll() {
        val query = EventQuery(cityId = null)
        assertTrue(event().matchesQuery(query))
        assertTrue(event(ageMin = 8, ageMax = 16, isFree = true).matchesQuery(query))
    }

    @Test
    fun searchIsCaseInsensitive() {
        val event = event(title = "Park Music Hour")
        assertTrue(event.matchesQuery(EventQuery(cityId = null, search = "music")))
        assertTrue(event.matchesQuery(EventQuery(cityId = null, search = "MUSIC")))
        assertFalse(event.matchesQuery(EventQuery(cityId = null, search = "library")))
    }

    @Test
    fun tagIdsMatchAndNonMatch() {
        val event = event(tags = listOf(EventTagDto("music", "Music"), EventTagDto("outdoor", "Outdoor")))
        assertTrue(event.matchesQuery(EventQuery(cityId = null, tagIds = listOf("music"))))
        assertTrue(event.matchesQuery(EventQuery(cityId = null, tagIds = listOf("free", "outdoor"))))
        assertFalse(event.matchesQuery(EventQuery(cityId = null, tagIds = listOf("indoor"))))
        // Empty tagIds is a no-op (matches everything).
        assertTrue(event.matchesQuery(EventQuery(cityId = null, tagIds = emptyList())))
    }

    @Test
    fun dateKeyMatchesPrefix() {
        val event = event(startsAt = Instant.parse("2026-06-10T15:00:00Z"))
        assertTrue(event.matchesQuery(EventQuery(cityId = null, dateKey = "2026-06-10")))
        assertFalse(event.matchesQuery(EventQuery(cityId = null, dateKey = "2026-06-11")))
    }

    @Test
    fun ageRangeOverlapIncludingBoundary() {
        val event = event(ageMin = 5, ageMax = 8)
        // Boundary: query ageMax equals event ageMin -> overlap.
        assertTrue(event.matchesQuery(EventQuery(cityId = null, ageMin = 0, ageMax = 5)))
        // Boundary: query ageMin equals event ageMax -> overlap.
        assertTrue(event.matchesQuery(EventQuery(cityId = null, ageMin = 8, ageMax = 12)))
        // No overlap below.
        assertFalse(event.matchesQuery(EventQuery(cityId = null, ageMin = 0, ageMax = 4)))
        // No overlap above.
        assertFalse(event.matchesQuery(EventQuery(cityId = null, ageMin = 9, ageMax = 12)))
    }

    @Test
    fun ageFilterWithNullEventBounds() {
        val event = event(ageMin = null, ageMax = null)
        // Null event bounds are treated as fully open (0..MAX), so any query overlaps.
        assertTrue(event.matchesQuery(EventQuery(cityId = null, ageMin = 6, ageMax = 7)))
    }

    @Test
    fun isFreeTrueAndFalse() {
        val freeEvent = event(isFree = true)
        val paidEvent = event(isFree = false)
        assertTrue(freeEvent.matchesQuery(EventQuery(cityId = null, isFree = true)))
        assertFalse(paidEvent.matchesQuery(EventQuery(cityId = null, isFree = true)))
        assertTrue(paidEvent.matchesQuery(EventQuery(cityId = null, isFree = false)))
        assertFalse(freeEvent.matchesQuery(EventQuery(cityId = null, isFree = false)))
    }

    @Test
    fun dateFromAndDateToAreInclusive() {
        val event = event(startsAt = Instant.parse("2026-06-10T15:00:00Z"))
        // Inclusive lower boundary.
        assertTrue(event.matchesQuery(EventQuery(cityId = null, dateFrom = Instant.parse("2026-06-10T15:00:00Z"))))
        assertFalse(event.matchesQuery(EventQuery(cityId = null, dateFrom = Instant.parse("2026-06-10T15:00:01Z"))))
        // Inclusive upper boundary.
        assertTrue(event.matchesQuery(EventQuery(cityId = null, dateTo = Instant.parse("2026-06-10T15:00:00Z"))))
        assertFalse(event.matchesQuery(EventQuery(cityId = null, dateTo = Instant.parse("2026-06-10T14:59:59Z"))))
    }

    @Test
    fun combinedFiltersStack() {
        val event = event(title = "Free Storytime", ageMin = 0, ageMax = 5, isFree = true)
        assertTrue(
            event.matchesQuery(
                EventQuery(cityId = null, search = "story", isFree = true, ageMin = 0, ageMax = 3),
            ),
        )
        assertFalse(
            event.matchesQuery(
                EventQuery(cityId = null, search = "story", isFree = false, ageMin = 0, ageMax = 3),
            ),
        )
    }

    @Test
    fun inMemoryRepositoryAppliesSharedFilters() = runBlocking {
        val repo = InMemoryEventRepository()
        val allEvents = repo.observeEventList(EventQuery(cityId = null)).first()
        assertTrue(allEvents.isNotEmpty())

        val freeOnly = repo.observeEventList(EventQuery(cityId = null, isFree = true)).first()
        assertTrue(freeOnly.all { it.isFree })

        // Newly aligned with production: in-memory double now filters tagIds + dateKey.
        val musicOnly = repo.observeEventList(EventQuery(cityId = null, tagIds = listOf("music"))).first()
        assertTrue(musicOnly.isNotEmpty())
        assertTrue(musicOnly.all { event -> event.tags.any { it.id == "music" } })

        val byDate = repo.observeEventList(EventQuery(cityId = null, dateKey = "2026-05-16")).first()
        assertEquals(allEvents.size, byDate.size)
        val noDate = repo.observeEventList(EventQuery(cityId = null, dateKey = "1999-01-01")).first()
        assertTrue(noDate.isEmpty())
    }
}
