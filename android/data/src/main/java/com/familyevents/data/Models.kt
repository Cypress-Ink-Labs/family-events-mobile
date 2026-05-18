package com.familyevents.data

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.GeoCoordinate
import com.familyevents.core.UserId
import java.time.Instant

data class EventTagDto(val id: String, val label: String)

data class EventDto(
    val id: EventId,
    val title: String,
    val description: String?,
    val startsAt: Instant,
    val endsAt: Instant?,
    val venueName: String?,
    val address: String?,
    val imageUrl: String?,
    val sourceUrl: String?,
    val cityId: CityId,
    val coordinate: GeoCoordinate?,
    val tags: List<EventTagDto> = emptyList(),
    val isFavorited: Boolean = false,
)

data class FavoriteDto(val eventId: EventId, val userId: UserId, val createdAt: Instant)

data class WeatherSnapshotDto(
    val dateKey: String,
    val summary: String,
    val temperatureHighF: Int?,
    val precipitationChance: Double?,
)

data class PlanEventRowDto(
    val event: EventDto,
    val section: String,
    val rank: Int,
)

data class EventQuery(
    val cityId: CityId?,
    val search: String? = null,
    val tagIds: List<String> = emptyList(),
    val dateKey: String? = null,
    val limit: Int = 50,
    val offset: Int = 0,
)

data class ProfileContext(
    val userId: UserId,
    val currentCityId: CityId?,
    val kidAge: Int?,
    val notificationsEnabled: Boolean,
)

data class UserProfile(
    val userId: UserId,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val currentCityId: CityId?,
    val childName: String?,
    val childAge: Int?,
    val notificationsEnabled: Boolean,
) {
    fun toContext(): ProfileContext = ProfileContext(userId, currentCityId, childAge, notificationsEnabled)
}

data class UserProfileUpdate(
    val displayName: String?,
    val currentCityId: CityId?,
    val childName: String?,
    val childAge: Int?,
)

data class CityDto(val id: CityId, val name: String, val region: String?)
