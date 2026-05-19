package com.familyevents.data

import com.familyevents.core.CityId
import com.familyevents.core.EventId
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

    @Test
    fun signInPersistsRemoteSessionTokens() = runTest {
        val store = MemorySessionStore()
        val repository = LocalAuthRepository(
            store,
            object : FakeConsumerApi() {
                override suspend fun signIn(email: String, password: String): PersistedSession =
                    PersistedSession(UserId("remote-user"), accessToken = "access", refreshToken = "refresh")
            },
        )

        repository.signIn("parent@example.com", "password")

        assertEquals(SessionState.SignedIn(UserId("remote-user")), repository.sessionState.first())
        assertEquals("access", store.readAccessToken())
    }

    @Test
    fun changePasswordDelegatesToRemoteApi() = runTest {
        val api = RecordingConsumerApi()
        val repository = LocalAuthRepository(MemorySessionStore(), api)

        repository.changePassword("parent@example.com", "oldpass", "newpass")

        assertEquals(Triple("parent@example.com", "oldpass", "newpass"), api.lastChangePassword)
    }

    @Test
    fun invitesRequiredDelegatesToApiWhenPresent() = runTest {
        val api = object : FakeConsumerApi() {
            override suspend fun invitesRequired(): Boolean = false
        }
        val repository = LocalAuthRepository(MemorySessionStore(), api)

        assertEquals(false, repository.invitesRequired())
    }

    @Test
    fun invitesRequiredReturnsTrueWhenApiIsAbsent() = runTest {
        val repository = LocalAuthRepository(MemorySessionStore(), api = null)

        assertEquals(true, repository.invitesRequired())
    }

    @Test
    fun requestInviteDelegatesToApiWithEmailAndMessage() = runTest {
        val api = RecordingConsumerApi()
        val repository = LocalAuthRepository(MemorySessionStore(), api)

        val result = repository.requestInvite("user@example.com", "Hello!")

        assertEquals("user@example.com" to "Hello!", api.lastRequestInvite)
        assertEquals(true, result)
    }

    @Test
    fun requestInviteDelegatesToApiWithNullMessage() = runTest {
        val api = RecordingConsumerApi()
        val repository = LocalAuthRepository(MemorySessionStore(), api)

        val result = repository.requestInvite("user@example.com", null)

        assertEquals("user@example.com" to null, api.lastRequestInvite)
        assertEquals(true, result)
    }

    @Test
    fun requestInviteReturnsFalseWhenApiIsAbsent() = runTest {
        val repository = LocalAuthRepository(MemorySessionStore(), api = null)

        assertEquals(false, repository.requestInvite("user@example.com", null))
    }
}

private class RecordingConsumerApi : FakeConsumerApi() {
    var lastChangePassword: Triple<String, String, String>? = null
    var lastRequestInvite: Pair<String, String?>? = null

    override suspend fun changePassword(email: String, currentPassword: String, newPassword: String) {
        lastChangePassword = Triple(email, currentPassword, newPassword)
    }

    override suspend fun requestInvite(email: String, message: String?): Boolean {
        lastRequestInvite = email to message
        return true
    }
}

private open class FakeConsumerApi : SupabaseConsumerApi {
    override suspend fun signIn(email: String, password: String): PersistedSession = unsupported()
    override suspend fun signUp(email: String, password: String): PersistedSession = unsupported()
    override suspend fun signInWithIdToken(provider: String, idToken: String, nonce: String?): PersistedSession = unsupported()
    override suspend fun resetPassword(email: String) = unsupported<Unit>()
    override suspend fun changePassword(email: String, currentPassword: String, newPassword: String) = unsupported<Unit>()
    override suspend fun signOut() = Unit
    override suspend fun cities(): List<CityDto> = unsupported()
    override suspend fun events(query: EventQuery): List<EventDto> = unsupported()
    override suspend fun event(id: EventId): EventDto? = unsupported()
    override suspend fun planEvents(userId: UserId, cityId: CityId?, kidAge: Int?, lat: Double?, lng: Double?): List<PlanEventRowDto> = unsupported()
    override suspend fun profile(userId: UserId): UserProfile? = unsupported()
    override suspend fun updateProfile(userId: UserId, update: UserProfileUpdate): UserProfile = unsupported()
    override suspend fun favorite(userId: UserId, eventId: EventId) = unsupported<Unit>()
    override suspend fun unfavorite(userId: UserId, eventId: EventId) = unsupported<Unit>()
    override suspend fun deleteAccount() = unsupported<Unit>()

    private fun <T> unsupported(): T = throw UnsupportedOperationException()
}
