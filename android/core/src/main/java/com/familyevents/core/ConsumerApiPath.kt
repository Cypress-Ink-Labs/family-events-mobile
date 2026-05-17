package com.familyevents.core

sealed interface ConsumerApiPath {
    val value: String

    data object Events : ConsumerApiPath {
        override val value = "/api/v1/events"
    }

    data object Favorites : ConsumerApiPath {
        override val value = "/api/v1/favorites"
    }

    data object Profile : ConsumerApiPath {
        override val value = "/api/v1/profile"
    }

    data class EventDetail(val id: EventId) : ConsumerApiPath {
        override val value = "/api/v1/events/${id.rawValue}"
    }
}
