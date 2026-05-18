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
import io.ktor.client.request.put
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
    suspend fun changePassword(email: String, currentPassword: String, newPassword: String)
    suspend fun signOut()
    suspend fun cities(): List<CityDto>
    suspend fun events(query: EventQuery): List<EventDto>
    suspend fun event(id: EventId): EventDto?
    suspend fun planEvents(userId: UserId, cityId: CityId?): List<PlanEventRowDto>
    suspend fun profile(userId: UserId): UserProfile?
    suspend fun updateProfile(userId: UserId, update: UserProfileUpdate): UserProfile
    suspend fun favorite(userId: UserId, eventId: EventId)
    suspend fun unfavorite(userId: UserId, eventId: EventId)
    suspend fun userRating(userId: UserId, eventId: EventId): RatingDto? = null
    suspend fun upsertRating(userId: UserId, eventId: EventId, score: Int): RatingDto =
        throw AppError.Remote("Ratings are unavailable.")
    suspend fun comments(eventId: EventId): List<CommentDto> = emptyList()
    suspend fun addComment(userId: UserId, eventId: EventId, body: String): CommentDto =
        throw AppError.Remote("Comments are unavailable.")
    suspend fun adminStats(): AdminStatsDto = AdminStatsDto(0, 0, 0, 0, 0)
    suspend fun adminUpdateEvent(eventId: EventId, patchJson: String) {}
    suspend fun adminModerateComment(commentId: String, approved: Boolean, flagged: Boolean) {}
    suspend fun adminCreateInvite(maxUses: Int?, expiresAtIso: String?, note: String?): String = ""
    suspend fun adminRevokeInvite(inviteId: String) {}
    suspend fun adminRunSource(sourceId: String?) {}
    suspend fun adminRunCron(jobName: String?) {}
    suspend fun deleteAccount()
    suspend fun invitesRequired(): Boolean = true
    suspend fun requestInvite(email: String, message: String?): Boolean = false
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

    override suspend fun changePassword(email: String, currentPassword: String, newPassword: String) {
        val verified = signIn(email, currentPassword)
        val accessToken = verified.accessToken ?: throw AppError.AuthRequired
        client.put("$baseUrl/auth/v1/user") {
            baseHeaders()
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("password", newPassword) }.toString())
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
        val response = client.post("$baseUrl/rest/v1/rpc/plan_events_first_nonempty_window") {
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

    override suspend fun profile(userId: UserId): UserProfile? {
        val response = client.get("$baseUrl/rest/v1/user_profiles") {
            baseHeaders()
            bearer(optional = true)
            parameter("select", "id,email,display_name,avatar_url,city_preference_id,child_name,child_age,role")
            parameter("id", "eq.${userId.rawValue}")
            parameter("limit", "1")
        }
        return response.requireOk().decodeList<ProfileRow>().firstOrNull()?.toDto()
    }

    override suspend fun updateProfile(userId: UserId, update: UserProfileUpdate): UserProfile {
        val response = client.patch("$baseUrl/rest/v1/user_profiles") {
            baseHeaders()
            bearer()
            header("Prefer", "return=representation")
            parameter("id", "eq.${userId.rawValue}")
            parameter("select", "id,email,display_name,avatar_url,city_preference_id,child_name,child_age,role")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    putNullable("display_name", update.displayName)
                    putNullable("city_preference_id", update.currentCityId?.rawValue)
                    putNullable("child_name", update.childName)
                    putNullable("child_age", update.childAge)
                }.toString(),
            )
        }.requireOk()
        return response.decodeList<ProfileRow>().firstOrNull()?.toDto() ?: profile(userId) ?: throw AppError.AuthRequired
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

    override suspend fun userRating(userId: UserId, eventId: EventId): RatingDto? {
        val response = client.get("$baseUrl/rest/v1/ratings") {
            baseHeaders()
            bearer()
            parameter("select", "id,user_id,event_id,score,created_at")
            parameter("user_id", "eq.${userId.rawValue}")
            parameter("event_id", "eq.${eventId.rawValue}")
            parameter("limit", "1")
        }
        return response.requireOk().decodeList<RatingRow>().firstOrNull()?.toDto()
    }

    override suspend fun upsertRating(userId: UserId, eventId: EventId, score: Int): RatingDto {
        val response = client.post("$baseUrl/rest/v1/ratings") {
            baseHeaders()
            bearer()
            header("Prefer", "resolution=merge-duplicates,return=representation")
            parameter("on_conflict", "user_id,event_id")
            parameter("select", "id,user_id,event_id,score,created_at")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("user_id", userId.rawValue)
                    put("event_id", eventId.rawValue)
                    put("score", score.coerceIn(1, 5))
                }.toString(),
            )
        }.requireOk()
        return response.decodeList<RatingRow>().firstOrNull()?.toDto()
            ?: throw AppError.Remote("Rating was saved but not returned.")
    }

    override suspend fun comments(eventId: EventId): List<CommentDto> {
        val response = client.get("$baseUrl/rest/v1/comments") {
            baseHeaders()
            bearer(optional = true)
            parameter("select", "id,user_id,event_id,body,is_approved,is_flagged,created_at,updated_at,user_profiles(display_name,avatar_url)")
            parameter("event_id", "eq.${eventId.rawValue}")
            parameter("is_approved", "eq.true")
            parameter("order", "created_at.desc")
        }
        return response.requireOk().decodeList<CommentRow>().map { it.toDto() }
    }

    override suspend fun addComment(userId: UserId, eventId: EventId, body: String): CommentDto {
        val response = client.post("$baseUrl/rest/v1/comments") {
            baseHeaders()
            bearer()
            header("Prefer", "return=representation")
            parameter("select", "id,user_id,event_id,body,is_approved,is_flagged,created_at,updated_at")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("user_id", userId.rawValue)
                    put("event_id", eventId.rawValue)
                    put("body", body)
                    put("is_approved", true)
                    put("is_flagged", false)
                }.toString(),
            )
        }.requireOk()
        return response.decodeList<CommentRow>().firstOrNull()?.toDto()
            ?: throw AppError.Remote("Comment was posted but not returned.")
    }

    override suspend fun adminStats(): AdminStatsDto {
        val eventsResponse = client.get("$baseUrl/rest/v1/events") {
            baseHeaders()
            bearer()
            parameter("select", "status,ai_confidence")
        }.requireOk()
        val sourcesResponse = client.get("$baseUrl/rest/v1/event_sources") {
            baseHeaders()
            bearer()
            parameter("select", "is_active,last_status")
        }.requireOk()
        val events = eventsResponse.decodeList<AdminEventStatsRow>()
        val sources = sourcesResponse.decodeList<AdminSourceStatsRow>()
        return AdminStatsDto(
            totalEvents = events.size,
            pendingReview = events.count { it.status == "draft" },
            published = events.count { it.status == "published" },
            activeSources = sources.count { it.isActive },
            sourceErrors = sources.count { it.isActive && it.lastStatus == "error" },
        )
    }

    override suspend fun adminUpdateEvent(eventId: EventId, patchJson: String) {
        throw AppError.Remote("Event management is unavailable on Android.")
    }

    override suspend fun adminModerateComment(commentId: String, approved: Boolean, flagged: Boolean) {
        client.patch("$baseUrl/rest/v1/comments") {
            baseHeaders()
            bearer()
            parameter("id", "eq.$commentId")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("is_approved", approved); put("is_flagged", flagged) }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminCreateInvite(maxUses: Int?, expiresAtIso: String?, note: String?): String {
        throw AppError.Remote("Invite management is unavailable on Android.")
    }

    override suspend fun adminRevokeInvite(inviteId: String) {
        client.patch("$baseUrl/rest/v1/invite_codes") {
            baseHeaders()
            bearer()
            parameter("id", "eq.$inviteId")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("is_active", false) }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminRunSource(sourceId: String?) {
        client.post("$baseUrl/functions/v1/scrape-source") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { sourceId?.let { put("sourceId", it) } }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminRunCron(jobName: String?) {
        throw AppError.Remote("Scheduled job controls are unavailable on Android.")
    }

    override suspend fun deleteAccount() {
        client.post("$baseUrl/rest/v1/rpc/delete_my_account") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.requireOkOrNoContent()
    }

    override suspend fun invitesRequired(): Boolean {
        val response = client.post("$baseUrl/rest/v1/rpc/invites_required") {
            baseHeaders()
            bearer(optional = true)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        val body = response.requireOk().bodyAsText().trim()
        return body.toBooleanStrictOrNull() ?: true
    }

    override suspend fun requestInvite(email: String, message: String?): Boolean {
        val response = client.post("$baseUrl/rest/v1/rpc/request_invite") {
            baseHeaders()
            bearer(optional = true)
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("p_email", email)
                    if (message != null) put("p_message", message)
                }.toString(),
            )
        }
        val body = response.requireOk().bodyAsText().trim()
        return body.toBooleanStrictOrNull() ?: false
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
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("city_preference_id") val cityPreferenceId: String? = null,
    @SerialName("child_name") val childName: String? = null,
    @SerialName("child_age") val childAge: Int? = null,
    val role: String? = null,
) {
    fun toDto(): UserProfile = UserProfile(
        userId = UserId(id),
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        currentCityId = cityPreferenceId?.let(::CityId),
        childName = childName,
        childAge = childAge,
        notificationsEnabled = false,
        role = role ?: "user",
    )
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
    @SerialName("age_min") val ageMin: Int? = null,
    @SerialName("age_max") val ageMax: Int? = null,
    val price: Double? = null,
    @SerialName("is_free") val isFree: Boolean? = null,
    val images: JsonElement? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("city_id") val cityId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tags: JsonElement? = null,
    @SerialName("avg_rating") val avgRating: Double? = null,
    @SerialName("rating_count") val ratingCount: Int? = null,
    @SerialName("is_favorited") val isFavorited: Boolean? = null,
    @SerialName("is_in_calendar") val isInCalendar: Boolean? = null,
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
            ageMin = ageMin,
            ageMax = ageMax,
            price = price,
            isFree = isFree ?: false,
            imageUrl = images.firstImageUrl(),
            sourceUrl = sourceUrl,
            cityId = CityId(parsedCity),
            coordinate = latitude?.let { lat -> longitude?.let { lng -> GeoCoordinate(lat, lng) } },
            tags = tags.toTags(),
            avgRating = avgRating ?: 0.0,
            ratingCount = ratingCount ?: 0,
            isFavorited = isFavorited ?: false,
            isInCalendar = isInCalendar ?: false,
        )
    }
}

@Serializable
private data class RatingRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("event_id") val eventId: String,
    val score: Int,
    @SerialName("created_at") val createdAt: String,
) {
    fun toDto(): RatingDto = RatingDto(id, UserId(userId), EventId(eventId), score, Instant.parse(createdAt))
}

@Serializable
private data class CommentProfileRow(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class CommentRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("event_id") val eventId: String,
    val body: String,
    @SerialName("is_approved") val isApproved: Boolean = true,
    @SerialName("is_flagged") val isFlagged: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("user_profiles") val profile: CommentProfileRow? = null,
) {
    fun toDto(): CommentDto = CommentDto(
        id = id,
        userId = UserId(userId),
        eventId = EventId(eventId),
        body = body,
        isApproved = isApproved,
        isFlagged = isFlagged,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt ?: createdAt),
        authorDisplayName = profile?.displayName,
        authorAvatarUrl = profile?.avatarUrl,
    )
}

@Serializable
private data class AdminEventStatsRow(
    val status: String,
    @SerialName("ai_confidence") val aiConfidence: Double? = null,
)

@Serializable
private data class AdminSourceStatsRow(
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("last_status") val lastStatus: String? = null,
)

@Serializable
private data class InviteCodeRow(val code: String)

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

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Int?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: JsonElement?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}
