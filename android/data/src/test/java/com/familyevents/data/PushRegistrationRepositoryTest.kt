package com.familyevents.data

import com.familyevents.core.UserId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PushRegistrationRepositoryTest {
    @Test
    fun inMemoryRepositoryRecordsMobileTokenRegistration() = runTest {
        val repository = InMemoryPushRegistrationRepository()
        val userId = UserId("user-1")

        repository.registerMobilePushToken(userId, PushPlatform.Android, "fcm-token")

        assertEquals(userId, repository.lastUserId)
        assertEquals(PushPlatform.Android, repository.lastPlatform)
        assertEquals("fcm-token", repository.lastToken)
        assertEquals(1, repository.registerCallCount)
    }
}
