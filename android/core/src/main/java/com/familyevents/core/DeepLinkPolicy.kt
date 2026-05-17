package com.familyevents.core

sealed interface DeepLinkTarget {
    data class Event(val id: EventId) : DeepLinkTarget
    data class City(val id: CityId) : DeepLinkTarget
    data class Share(val id: EventId) : DeepLinkTarget
    data class ResetPassword(val token: String) : DeepLinkTarget
}

object DeepLinkPolicy {
    fun parse(raw: String): DeepLinkTarget? {
        val uri = java.net.URI(raw)
        val host = uri.host.orEmpty()
        val segments = uri.path.trim('/').split('/').filter { it.isNotBlank() }
        return when {
            uri.scheme == "familyevents" && host == "event" && segments.size == 1 -> DeepLinkTarget.Event(EventId(segments[0]))
            uri.scheme == "familyevents" && host == "city" && segments.size == 1 -> DeepLinkTarget.City(CityId(segments[0]))
            uri.scheme == "familyevents" && host == "reset-password" -> {
                val token = uri.query.orEmpty().split('&').firstNotNullOfOrNull {
                    val parts = it.split('=', limit = 2)
                    parts.takeIf { p -> p.size == 2 && p[0] == "token" }?.get(1)
                }
                token?.takeIf { it.isNotBlank() }?.let(DeepLinkTarget::ResetPassword)
            }
            uri.scheme == "https" && host == "familyevents.app" && segments.size == 2 && segments[0] == "share" ->
                DeepLinkTarget.Share(EventId(segments[1]))
            else -> null
        }
    }
}
