package com.familyevents.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsumerApiPathTest {
    @Test
    fun consumerPathsExcludeAdmin() {
        val paths = listOf(
            ConsumerApiPath.Events.value,
            ConsumerApiPath.Favorites.value,
            ConsumerApiPath.Profile.value,
            ConsumerApiPath.EventDetail(EventId("evt_1")).value,
        )
        assertEquals(listOf("/api/v1/events", "/api/v1/favorites", "/api/v1/profile", "/api/v1/events/evt_1"), paths)
        paths.forEach { path -> kotlin.test.assertFalse(path.contains("admin", ignoreCase = true)) }
    }
}
