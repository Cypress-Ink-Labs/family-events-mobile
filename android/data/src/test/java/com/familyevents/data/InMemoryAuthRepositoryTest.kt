package com.familyevents.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryAuthRepositoryTest {

    @Test
    fun invitesRequiredAlwaysReturnsFalse() = runTest {
        val repository = InMemoryAuthRepository()

        assertFalse(repository.invitesRequired())
    }

    @Test
    fun requestInviteAlwaysReturnsTrue() = runTest {
        val repository = InMemoryAuthRepository()

        assertTrue(repository.requestInvite("user@example.com", "Please let me in"))
    }

    @Test
    fun requestInviteReturnsTrueWithNullMessage() = runTest {
        val repository = InMemoryAuthRepository()

        assertTrue(repository.requestInvite("user@example.com", null))
    }

    @Test
    fun requestInviteReturnsTrueForAnyEmail() = runTest {
        val repository = InMemoryAuthRepository()

        assertTrue(repository.requestInvite("another@domain.org", null))
        assertTrue(repository.requestInvite("", null))
    }

    @Test
    fun signInTransitionsToSignedInState() = runTest {
        val repository = InMemoryAuthRepository()

        repository.signIn("parent@example.com", "password")

        val state = repository.sessionState.first()
        assertTrue(state is SessionState.SignedIn)
    }

    @Test
    fun signOutTransitionsToSignedOutState() = runTest {
        val repository = InMemoryAuthRepository()
        repository.signIn("parent@example.com", "password")

        repository.signOut()

        assertEquals(SessionState.SignedOut, repository.sessionState.first())
    }
}