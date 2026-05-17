package com.familyevents.data

import com.familyevents.core.UserId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalAuthRepositoryTest {
    @Test
    fun restoreSessionUsesPersistedUser() = runTest {
        val store = MemorySessionStore()
        store.writeUserId(UserId("parent@example.com"))
        val repository = LocalAuthRepository(store)

        repository.restoreSession()

        assertEquals(SessionState.SignedIn(UserId("parent@example.com")), repository.sessionState.first())
    }

    @Test
    fun signOutClearsPersistedUser() = runTest {
        val store = MemorySessionStore()
        val repository = LocalAuthRepository(store)

        repository.signIn("Parent@Example.com", "password")
        repository.signOut()
        repository.restoreSession()

        assertEquals(SessionState.SignedOut, repository.sessionState.first())
    }
}
