package com.familyevents.data

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import kotlinx.coroutines.flow.Flow

sealed interface SessionState {
    data object Restoring : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val userId: UserId) : SessionState
}

interface AuthRepository {
    val sessionState: Flow<SessionState>
    suspend fun restoreSession()
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun resetPassword(email: String)
    suspend fun changePassword(email: String, currentPassword: String, newPassword: String)
    suspend fun signOut()
}

interface ProfileRepository {
    fun observeProfile(userId: UserId): Flow<ProfileContext?>
    suspend fun currentContext(userId: UserId): ProfileContext
    suspend fun profile(userId: UserId): UserProfile
    suspend fun updateProfile(userId: UserId, update: UserProfileUpdate): UserProfile
    suspend fun updateContext(userId: UserId, cityId: CityId?, kidAge: Int?)
    suspend fun updateNotificationPreference(userId: UserId, enabled: Boolean)
    suspend fun deleteAccount(userId: UserId)
}

interface EventRepository {
    fun observePlanEvents(userId: UserId, cityId: CityId?): Flow<List<PlanEventRowDto>>
    fun observeEventList(query: EventQuery): Flow<List<EventDto>>
    fun observeEventDetail(id: EventId): Flow<EventDto?>
    suspend fun refreshPlan(userId: UserId, cityId: CityId?)
    suspend fun refreshEventList(query: EventQuery)
    suspend fun refreshEventDetail(id: EventId)
}

interface FavoriteRepository {
    fun observeFavorites(userId: UserId): Flow<List<FavoriteDto>>
    fun observeFavoriteIds(userId: UserId): Flow<Set<EventId>>
    suspend fun favorite(userId: UserId, eventId: EventId)
    suspend fun unfavorite(userId: UserId, eventId: EventId)
}

interface CityRepository {
    fun observeCities(): Flow<List<CityDto>>
    suspend fun cityName(id: CityId): String?
    suspend fun refreshCities()
}

interface WeatherRepository {
    fun observeForecast(cityId: CityId): Flow<List<WeatherSnapshotDto>>
    suspend fun refreshForecast(cityId: CityId)
}
