package com.familyevents.data

import com.familyevents.core.AppError
import com.familyevents.core.CityId
import com.familyevents.core.EnvConfig
import com.familyevents.core.EventId
import com.familyevents.core.GeoCoordinate
import com.familyevents.core.UserId
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer

interface SupabaseConsumerApi {
    suspend fun signIn(email: String, password: String): PersistedSession
    suspend fun signUp(email: String, password: String): PersistedSession
    suspend fun resetPassword(email: String)
    suspend fun signOut()
    suspend fun cities(): List<CityDto>
    suspend fun events(query: EventQuery): List<EventDto>
    suspend fun event(id: EventId): EventDto?
    suspend fun planEvents(userId: UserId, cityId: CityId?): List<PlanEventRowDto>
    suspend fun profile(userId: UserId): ProfileContext?
    suspend fun updateProfile(profile: ProfileContext)
    suspend fun favorite(userId: UserId, eventId: EventId)
    suspend fun unfavorite(userId: UserId, eventId: EventId)
    suspend fun deleteAccount()
}

class KtorSupabaseConsumerApi(
    private val config: EnvConfig,
    private val sessionStore: SessionStore,
    private val client: HttpClient = HttpClient(OkHttp),
) : SupabaseConsumerApi {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val baseUrl = config.supabaseUrl.trimEnd('/')

    override suspend fun signIn(email: String, password: String): PersistedSession {
        val response = client.post("$baseUrl/auth/v1/token") {
            parameter("grant_type", "password")
            baseHeaders()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthRequest.serializer(), AuthRequest(email, password)))
        }
        return response.requireOk().decode<AuthResponse>().toPersistedSession()
    }

    override suspend fun signUp(email: String, password: String): PersistedSession {
        val response = client.post("$baseUrl/auth/v1/signup") {
            baseHeaders()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthRequest.serializer(), AuthRequest(email, password)))
        }
        return response.requireOk().decode<AuthResponse>().toPersistedSession()
    }

    override suspend fun resetPassword(email: String) {
        client.post("$baseUrl/auth/v1/recover") {
            baseHeaders()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RecoverRequest.serializer(), RecoverRequest(email)))
        }.requireOk()
    }

    override suspend fun signOut() {
        client.post("$baseUrl/auth/v1/logout") {
            baseHeaders()
            bearer()
        }.requireOkOrNoContent()
    }

    override suspend fun cities(): List<CityDto> {
        val response = client.get("$baseUrl/rest/v1/cities") {
            baseHeaders()
            bearer(optional = true)
            parameter("select", "id,name,state")
            parameter("is_active", "eq.true")
            parameter("order", "name.asc")
        }
        return response.requireOk().decodeList<CityRow>().map { CityDto(CityId(it.id), it.name, it.state) }
    }

    override suspend fun events(query: EventQuery): List<EventDto> {
        val response = client.post("$baseUrl/rest/v1/rpc/events_enriched") {
            baseHeaders()
            bearer(optional = true)
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    query.cityId?.let { put("p_city_id", it.rawValue) }
                    put("p_status", "published")
                    put("p_limit", query.limit)
                    put("p_offset", query.offset)
                }.toString(),
            )
        }
        return response.requireOk()
            .decodeList<EventRow>()
            .mapNotNull { it.toDto() }
            .filter { event -> query.search.isNullOrBlank() || event.title.contains(query.search, ignoreCase = true) }
            .filter { event -> query.tagIds.isEmpty() || event.tags.any { it.id in query.tagIds } }
            .filter { event -> query.dateKey == null || event.startsAt.toString().startsWith(query.dateKey) }
    }

    override suspend fun event(id: EventId): EventDto? = eventsByIds(listOf(id)).firstOrNull()

    override suspend fun planEvents(userId: UserId, cityId: CityId?): List<PlanEventRowDto> {
        val response = client.post("$baseUrl/rest/v1/rpc/plan_events_for_user") {
            baseHeaders()
            bearer(optional = true)
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("p_user_id", userId.rawValue)
                    cityId?.let { put("p_city_id", it.rawValue) }
                    put("p_limit", 6)
                }.toString(),
            )
        }
        val rows = response.requireOk().decodeList<PlanRow>()
        val eventsById = eventsByIds(rows.map { EventId(it.eventId) }).associateBy { it.id.rawValue }
        return rows.mapIndexedNotNull { index, row ->
            eventsById[row.eventId]?.let { event ->
                PlanEventRowDto(event, section = if (index == 0) "Hero" else "Saturday", rank = index)
            }
        }
    }

    override suspend fun profile(userId: UserId): ProfileContext? {
        val response = client.get("$baseUrl/rest/v1/user_profiles") {
            baseHeaders()
            bearer(optional = true)
            parameter("select", "id,city_preference_id,child_age")
            parameter("id", "eq.${userId.rawValue}")
            parameter("limit", "1")
        }
        return response.requireOk().decodeList<ProfileRow>().firstOrNull()?.toDto()
    }

    override suspend fun updateProfile(profile: ProfileContext) {
        client.patch("$baseUrl/rest/v1/user_profiles") {
            baseHeaders()
            bearer()
            header("Prefer", "return=minimal")
            parameter("id", "eq.${profile.userId.rawValue}")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    profile.currentCityId?.let { put("city_preference_id", it.rawValue) }
                    profile.kidAge?.let { put("child_age", it) }
                }.toString(),
            )
        }.requireOkOrNoContent()
    }

    override suspend fun favorite(userId: UserId, eventId: EventId) {
        client.post("$baseUrl/rest/v1/favorites") {
            baseHeaders()
            bearer()
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("user_id", userId.rawValue)
                    put("event_id", eventId.rawValue)
                }.toString(),
            )
        }.requireOkOrNoContent()
    }

    override suspend fun unfavorite(userId: UserId, eventId: EventId) {
        client.delete("$baseUrl/rest/v1/favorites") {
            baseHeaders()
            bearer()
            parameter("user_id", "eq.${userId.rawValue}")
            parameter("event_id", "eq.${eventId.rawValue}")
        }.requireOkOrNoContent()
    }

    override suspend fun deleteAccount() {
        client.post("$baseUrl/rest/v1/rpc/delete_my_account") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.requireOkOrNoContent()
    }

    private suspend fun eventsByIds(ids: List<EventId>): List<EventDto> {
        val uniqueIds = ids.distinctBy { it.rawValue }
        if (uniqueIds.isEmpty()) return emptyList()
        val response = client.post("$baseUrl/rest/v1/rpc/events_enriched") {
            baseHeaders()
            bearer(optional = true)
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("p_event_ids", buildJsonArray { uniqueIds.forEach { add(JsonPrimitive(it.rawValue)) } })
                    put("p_status", "published")
                    put("p_limit", uniqueIds.size)
                    put("p_offset", 0)
                }.toString(),
            )
        }
        return response.requireOk().decodeList<EventRow>().mapNotNull { it.toDto() }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.baseHeaders() {
        headers {
            append("apikey", config.supabaseAnonKey)
        }
    }

    private suspend fun io.ktor.client.request.HttpRequestBuilder.bearer(optional: Boolean = false) {
        val token = sessionStore.readAccessToken()
        if (token == null) {
            if (!optional) throw AppError.AuthRequired
            header(HttpHeaders.Authorization, "Bearer ${config.supabaseAnonKey}")
            return
        }
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private suspend fun HttpResponse.requireOk(): HttpResponse {
        if (status.value in 200..299) return this
        throw AppError.Remote(bodyAsText().ifBlank { "Supabase request failed with HTTP ${status.value}" })
    }

    private suspend fun HttpResponse.requireOkOrNoContent(): HttpResponse {
        if (status == HttpStatusCode.NoContent || status.value in 200..299) return this
        throw AppError.Remote(bodyAsText().ifBlank { "Supabase request failed with HTTP ${status.value}" })
    }

    private suspend inline fun <reified T> HttpResponse.decode(): T =
        json.decodeFromString<T>(bodyAsText())

    private suspend inline fun <reified T> HttpResponse.decodeList(): List<T> =
        json.decodeFromString(ListSerializer(serializer<T>()), bodyAsText())
}

@Serializable
private data class AuthRequest(val email: String, val password: String)

@Serializable
private data class RecoverRequest(val email: String)

@Serializable
private data class AuthResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: AuthUser? = null,
) {
    fun toPersistedSession(): PersistedSession {
        val userId = user?.id?.takeIf { it.isNotBlank() } ?: throw AppError.AuthRequired
        return PersistedSession(UserId(userId), accessToken, refreshToken)
    }
}

@Serializable
private data class AuthUser(val id: String)

@Serializable
private data class CityRow(val id: String, val name: String, val state: String? = null)

@Serializable
private data class ProfileRow(
    val id: String,
    @SerialName("city_preference_id") val cityPreferenceId: String? = null,
    @SerialName("child_age") val childAge: Int? = null,
) {
    fun toDto(): ProfileContext = ProfileContext(UserId(id), cityPreferenceId?.let(::CityId), childAge, notificationsEnabled = false)
}

@Serializable
private data class PlanRow(@SerialName("event_id") val eventId: String)

@Serializable
private data class EventRow(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    @SerialName("start_datetime") val startDatetime: String? = null,
    @SerialName("end_datetime") val endDatetime: String? = null,
    @SerialName("venue_name") val venueName: String? = null,
    val address: String? = null,
    val images: JsonElement? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("city_id") val cityId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tags: JsonElement? = null,
    @SerialName("is_favorited") val isFavorited: Boolean? = null,
) {
    fun toDto(): EventDto? {
        val parsedId = id?.takeIf { it.isNotBlank() } ?: return null
        val parsedTitle = title?.takeIf { it.isNotBlank() } ?: return null
        val parsedStart = startDatetime?.let(Instant::parse) ?: return null
        val parsedCity = cityId?.takeIf { it.isNotBlank() } ?: "unknown"
        return EventDto(
            id = EventId(parsedId),
            title = parsedTitle,
            description = description,
            startsAt = parsedStart,
            endsAt = endDatetime?.let(Instant::parse),
            venueName = venueName,
            address = address,
            imageUrl = images.firstImageUrl(),
            sourceUrl = sourceUrl,
            cityId = CityId(parsedCity),
            coordinate = latitude?.let { lat -> longitude?.let { lng -> GeoCoordinate(lat, lng) } },
            tags = tags.toTags(),
            isFavorited = isFavorited ?: false,
        )
    }
}

private fun JsonElement?.firstImageUrl(): String? = when (this) {
    null, JsonNull -> null
    is JsonPrimitive -> contentOrNull?.takeIf { it.startsWith("http") }
    is JsonArray -> firstNotNullOfOrNull { it.firstImageUrl() }
    is JsonObject -> this["url"]?.jsonPrimitive?.contentOrNull
        ?: this["src"]?.jsonPrimitive?.contentOrNull
        ?: this["image_url"]?.jsonPrimitive?.contentOrNull
}

private fun JsonElement?.toTags(): List<EventTagDto> = when (this) {
    is JsonArray -> mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val id = obj["id"]?.jsonPrimitive?.contentOrNull
            ?: obj["slug"]?.jsonPrimitive?.contentOrNull
            ?: obj["name"]?.jsonPrimitive?.contentOrNull
            ?: return@mapNotNull null
        val label = obj["label"]?.jsonPrimitive?.contentOrNull
            ?: obj["name"]?.jsonPrimitive?.contentOrNull
            ?: id
        EventTagDto(id, label)
    }
    else -> emptyList()
}
