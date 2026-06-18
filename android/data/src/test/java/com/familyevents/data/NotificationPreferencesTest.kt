package com.familyevents.data

import com.familyevents.core.UserId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPreferencesTest {
    @Test
    fun defaultsMatchIosContract() {
        val prefs = NotificationPreferences()
        assertTrue(prefs.reminderEmail)
        assertTrue(prefs.reminderPush)
        assertTrue(prefs.changeEmail)
        assertTrue(prefs.changePush)
        assertTrue(prefs.digestEmail)
        // iOS defaults digestPush to false; keep Android in lockstep.
        assertFalse(prefs.digestPush)
    }

    @Test
    fun inMemoryFetchReturnsDefaultsWhenUnset() = runTest {
        val repository = InMemoryProfileRepository()
        val prefs = repository.fetchNotificationPreferences(UserId("parent@example.com"))
        assertEquals(NotificationPreferences(), prefs)
    }

    @Test
    fun inMemoryUpdateThenFetchRoundTrips() = runTest {
        val repository = InMemoryProfileRepository()
        val userId = UserId("parent@example.com")
        val updated = NotificationPreferences(
            reminderEmail = false,
            reminderPush = true,
            changeEmail = false,
            changePush = false,
            digestEmail = true,
            digestPush = true,
        )

        val echoed = repository.updateNotificationPreferences(userId, updated)

        assertEquals(updated, echoed)
        assertEquals(updated, repository.fetchNotificationPreferences(userId))
    }
}
