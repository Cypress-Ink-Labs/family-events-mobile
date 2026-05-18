package com.familyevents.data

import com.familyevents.core.CityId
import com.familyevents.core.UserId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRepositoryTest {
    @Test
    fun inMemoryProfileUpdatePersistsEditableFields() = runTest {
        val repository = InMemoryProfileRepository()
        val userId = UserId("parent@example.com")

        val updated = repository.updateProfile(
            userId,
            UserProfileUpdate(
                displayName = "Parent",
                currentCityId = CityId("madison"),
                childName = "Kid",
                childAge = 8,
            ),
        )

        assertEquals("Parent", updated.displayName)
        assertEquals(CityId("madison"), repository.currentContext(userId).currentCityId)
        assertEquals(8, repository.currentContext(userId).kidAge)
    }

    @Test
    fun inMemoryProfileUpdateCanClearNullableFields() = runTest {
        val repository = InMemoryProfileRepository()
        val userId = UserId("parent@example.com")
        repository.updateProfile(userId, UserProfileUpdate("Parent", CityId("madison"), "Kid", 8))

        val updated = repository.updateProfile(userId, UserProfileUpdate(null, null, null, null))

        assertNull(updated.displayName)
        assertNull(updated.currentCityId)
        assertNull(updated.childName)
        assertNull(updated.childAge)
    }
}
