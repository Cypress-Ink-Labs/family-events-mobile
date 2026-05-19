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
import java.time.OffsetDateTime
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
    suspend fun planEvents(userId: UserId, cityId: CityId?, kidAge: Int? = null): List<PlanEventRowDto>
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
    suspend fun adminUpdateEvent(eventId: EventId, patchJson: String, tagIds: List<String> = emptyList(), lockEditedFields: Boolean = true): EventDto =
        throw AppError.Remote("Event management is unavailable.")
    suspend fun adminCreateEvent(patchJson: String, tagIds: List<String> = emptyList()): EventDto =
        throw AppError.Remote("Event creation is unavailable.")
    suspend fun adminUnlockEventFields(eventId: EventId): Boolean = false
    suspend fun adminModerateComment(commentId: String, approved: Boolean, flagged: Boolean) {}
    suspend fun adminCreateInviteCode(maxUses: Int = 1, expiresAtIso: String? = null, notes: String? = null): AdminInviteCodeResultDto =
        throw AppError.Remote("Invite code creation is unavailable.")
    suspend fun adminApproveInviteRequest(requestId: String): AdminInviteApprovalDto =
        throw AppError.Remote("Invite approval is unavailable.")
    suspend fun adminRejectInviteRequest(requestId: String, notes: String? = null): Boolean = false
    suspend fun adminBulkSetAutoApprove(enable: Boolean): Unit {}
    suspend fun adminRevokeInvite(inviteId: String): Boolean = false
    suspend fun adminRunSource(sourceId: String?) {}
    suspend fun adminRetryTagQueue(eventId: EventId): Boolean = false
    suspend fun adminListCronJobs(): List<AdminCronJobDto> = emptyList()
    suspend fun adminCronRunHistory(jobName: String? = null, limit: Int = 50): List<AdminCronRunDto> = emptyList()
    suspend fun adminToggleCronJob(jobName: String, active: Boolean): Unit {}
    suspend fun adminSetCronSchedule(jobName: String, schedule: String): Unit {}
    suspend fun adminRunDueScrapes(): Unit {}
    suspend fun adminListComments(filter: String = "all"): List<AdminCommentDto> = emptyList()
    suspend fun adminDeleteComment(commentId: String) {}
    suspend fun adminListSources(): List<AdminSourceDto> = emptyList()
    suspend fun adminUpdateSourceActive(sourceId: String, active: Boolean) {}
    suspend fun adminUpdateSourceAutoApprove(sourceId: String, autoApprove: Boolean) {}
    suspend fun adminListInviteCodes(): List<AdminInviteCodeListDto> = emptyList()
    suspend fun adminListInviteRequests(status: String = "pending"): List<AdminInviteRequestDto> = emptyList()
    suspend fun adminListEvents(
        keyword: String? = null,
        status: String? = null,
        cityId: CityId? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): List<AdminEventListItemDto> = emptyList()
    suspend fun adminListEventFacets(): AdminEventFacetsDto = AdminEventFacetsDto(emptyMap(), emptyMap())
    suspend fun adminBulkUpdateEventStatus(eventIds: List<EventId>, status: String) {}
    suspend fun adminDeleteEvent(eventId: EventId) {}
    suspend fun adminListEventAiTraces(eventId: EventId, limit: Int = 5): List<AdminEventAiTraceDto> = emptyList()
    suspend fun deleteAccount()
    suspend fun invitesRequired(): Boolean = true
    suspend fun requestInvite(email: String, message: String?): Boolean = false
    suspend fun publicEvent(id: EventId): EventDto? = null
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

    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private fun String.isUuid(): Boolean = this.length == 36 && uuidRegex.matches(this)

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

    override suspend fun planEvents(userId: UserId, cityId: CityId?, kidAge: Int?): List<PlanEventRowDto> {
        val response = client.post("$baseUrl/rest/v1/rpc/plan_events_first_nonempty_window") {
            baseHeaders()
            bearer(optional = true)
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("p_user_id", userId.rawValue)
                    cityId?.let { put("p_city_id", it.rawValue) }
                    put("p_limit", 6)
                    kidAge?.let { put("p_kid_age", it) }
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
        if (!userId.rawValue.isUuid()) return null
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
        if (!userId.rawValue.isUuid()) throw AppError.AuthRequired
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
        if (!userId.rawValue.isUuid() || !eventId.rawValue.isUuid()) return
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
        if (!userId.rawValue.isUuid() || !eventId.rawValue.isUuid()) return
        client.delete("$baseUrl/rest/v1/favorites") {
            baseHeaders()
            bearer()
            parameter("user_id", "eq.${userId.rawValue}")
            parameter("event_id", "eq.${eventId.rawValue}")
        }.requireOkOrNoContent()
    }

    override suspend fun userRating(userId: UserId, eventId: EventId): RatingDto? {
        if (!userId.rawValue.isUuid() || !eventId.rawValue.isUuid()) return null
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
        if (!userId.rawValue.isUuid() || !eventId.rawValue.isUuid()) throw AppError.Remote("Invalid identifiers")
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
        if (!eventId.rawValue.isUuid()) return emptyList()
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
        if (!userId.rawValue.isUuid() || !eventId.rawValue.isUuid()) throw AppError.Remote("Invalid identifiers")
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

    override suspend fun adminUpdateEvent(eventId: EventId, patchJson: String, tagIds: List<String>, lockEditedFields: Boolean): EventDto {
        val tagArray = buildJsonArray { tagIds.forEach { add(JsonPrimitive(it)) } }
        val response = client.post("$baseUrl/rest/v1/rpc/admin_update_event") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("p_event_id", eventId.rawValue)
                    put("p_patch", json.parseToJsonElement(patchJson))
                    put("p_tag_ids", tagArray)
                    put("p_lock_edited_fields", lockEditedFields)
                }.toString(),
            )
        }
        return response.requireOk().decodeList<EventRow>().firstOrNull()?.toDto()
            ?: throw AppError.Remote("Event update returned no data.")
    }

    override suspend fun adminCreateEvent(patchJson: String, tagIds: List<String>): EventDto {
        val tagArray = buildJsonArray { tagIds.forEach { add(JsonPrimitive(it)) } }
        val response = client.post("$baseUrl/rest/v1/rpc/admin_create_event") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("p_patch", json.parseToJsonElement(patchJson))
                    put("p_tag_ids", tagArray)
                }.toString(),
            )
        }
        return response.requireOk().decodeList<EventRow>().firstOrNull()?.toDto()
            ?: throw AppError.Remote("Event creation returned no data.")
    }

    override suspend fun adminUnlockEventFields(eventId: EventId): Boolean {
        val response = client.post("$baseUrl/rest/v1/rpc/admin_unlock_event_fields") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("p_event_id", eventId.rawValue) }.toString())
        }
        return response.requireOk().bodyAsText().trim().toBooleanStrictOrNull() ?: false
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

    override suspend fun adminCreateInviteCode(maxUses: Int, expiresAtIso: String?, notes: String?): AdminInviteCodeResultDto {
        val response = client.post("$baseUrl/rest/v1/rpc/admin_create_invite_code") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("p_max_uses", maxUses)
                    if (expiresAtIso != null) put("p_expires_at", expiresAtIso)
                    if (notes != null) put("p_notes", notes)
                }.toString(),
            )
        }
        return response.requireOk().decodeList<AdminInviteCodeRow>().firstOrNull()?.toDto()
            ?: throw AppError.Remote("Invite code creation returned no data.")
    }

    override suspend fun adminApproveInviteRequest(requestId: String): AdminInviteApprovalDto {
        val response = client.post("$baseUrl/rest/v1/rpc/admin_approve_invite_request") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("p_request_id", requestId) }.toString())
        }
        return response.requireOk().decodeList<AdminInviteApprovalRow>().firstOrNull()?.toDto()
            ?: throw AppError.Remote("Invite approval returned no data.")
    }

    override suspend fun adminRejectInviteRequest(requestId: String, notes: String?): Boolean {
        val response = client.post("$baseUrl/rest/v1/rpc/admin_reject_invite_request") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("p_request_id", requestId)
                    if (notes != null) put("p_notes", notes)
                }.toString(),
            )
        }
        return response.requireOk().bodyAsText().trim().toBooleanStrictOrNull() ?: false
    }

    override suspend fun adminBulkSetAutoApprove(enable: Boolean) {
        client.post("$baseUrl/rest/v1/rpc/admin_bulk_set_auto_approve") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("enable", enable) }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminRevokeInvite(inviteId: String): Boolean {
        val response = client.post("$baseUrl/rest/v1/rpc/admin_revoke_invite_code") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("p_id", inviteId) }.toString())
        }
        val body = response.requireOk().bodyAsText().trim()
        return body.toBooleanStrictOrNull() ?: false
    }

    override suspend fun adminRunSource(sourceId: String?) {
        client.post("$baseUrl/functions/v1/scrape-source") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { sourceId?.let { put("sourceId", it) } }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminRetryTagQueue(eventId: EventId): Boolean {
        val response = client.post("$baseUrl/rest/v1/rpc/admin_retry_tag_queue") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("p_event_id", eventId.rawValue) }.toString())
        }
        return response.requireOk().bodyAsText().trim().toBooleanStrictOrNull() ?: false
    }

    override suspend fun adminListCronJobs(): List<AdminCronJobDto> {
        val response = client.post("$baseUrl/rest/v1/rpc/admin_list_cron_jobs") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        return response.requireOk().decodeList<AdminCronJobRow>().map { it.toDto() }
    }

    override suspend fun adminCronRunHistory(jobName: String?, limit: Int): List<AdminCronRunDto> {
        val response = client.post("$baseUrl/rest/v1/rpc/admin_cron_run_history") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    if (jobName != null) put("p_job_name", jobName)
                    put("p_limit", limit)
                }.toString(),
            )
        }
        return response.requireOk().decodeList<AdminCronRunRow>().map { it.toDto() }
    }

    override suspend fun adminToggleCronJob(jobName: String, active: Boolean) {
        client.post("$baseUrl/rest/v1/rpc/admin_toggle_cron_job") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("p_job_name", jobName); put("p_active", active) }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminSetCronSchedule(jobName: String, schedule: String) {
        client.post("$baseUrl/rest/v1/rpc/admin_set_cron_schedule") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("p_job_name", jobName); put("p_schedule", schedule) }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminRunDueScrapes() {
        client.post("$baseUrl/rest/v1/rpc/admin_run_due_scrapes") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.requireOkOrNoContent()
    }

    override suspend fun adminListComments(filter: String): List<AdminCommentDto> {
        val response = client.get("$baseUrl/rest/v1/comments") {
            baseHeaders()
            bearer()
            parameter("select", "id,user_id,event_id,body,is_approved,is_flagged,created_at,user_profiles(display_name),events(title)")
            when (filter) {
                "flagged" -> parameter("is_flagged", "eq.true")
                "pending" -> parameter("is_approved", "eq.false")
                "approved" -> parameter("is_approved", "eq.true")
                // "all" — no extra filter
            }
            parameter("order", "created_at.desc")
            parameter("limit", "50")
        }
        return response.requireOk().decodeList<AdminCommentRow>().map { it.toAdminDto() }
    }

    override suspend fun adminDeleteComment(commentId: String) {
        client.delete("$baseUrl/rest/v1/comments") {
            baseHeaders()
            bearer()
            parameter("id", "eq.$commentId")
        }.requireOkOrNoContent()
    }

    override suspend fun adminListSources(): List<AdminSourceDto> {
        val response = client.get("$baseUrl/rest/v1/event_sources") {
            baseHeaders()
            bearer()
            parameter("select", "id,name,city_id,url,is_active,auto_approve,last_status,last_scraped_at")
            parameter("order", "name.asc")
        }
        return response.requireOk().decodeList<AdminSourceRow>().map { it.toDto() }
    }

    override suspend fun adminUpdateSourceActive(sourceId: String, active: Boolean) {
        client.patch("$baseUrl/rest/v1/event_sources") {
            baseHeaders()
            bearer()
            parameter("id", "eq.$sourceId")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("is_active", active) }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminUpdateSourceAutoApprove(sourceId: String, autoApprove: Boolean) {
        client.patch("$baseUrl/rest/v1/event_sources") {
            baseHeaders()
            bearer()
            parameter("id", "eq.$sourceId")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("auto_approve", autoApprove) }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminListInviteCodes(): List<AdminInviteCodeListDto> {
        val response = client.get("$baseUrl/rest/v1/invite_codes") {
            baseHeaders()
            bearer()
            parameter("select", "id,max_uses,used_count,expires_at,notes,created_at,revoked_at")
            parameter("order", "created_at.desc")
        }
        return response.requireOk().decodeList<AdminInviteCodeListRow>().map { it.toDto() }
    }

    override suspend fun adminListInviteRequests(status: String): List<AdminInviteRequestDto> {
        val response = client.get("$baseUrl/rest/v1/invite_requests") {
            baseHeaders()
            bearer()
            parameter("select", "id,email,message,status,created_at,reviewed_at,admin_notes")
            if (status != "all") parameter("status", "eq.$status")
            parameter("order", "created_at.desc")
        }
        return response.requireOk().decodeList<AdminInviteRequestRow>().map { it.toDto() }
    }

    override suspend fun adminListEvents(
        keyword: String?,
        status: String?,
        cityId: CityId?,
        limit: Int,
        offset: Int,
    ): List<AdminEventListItemDto> {
        val response = client.get("$baseUrl/rest/v1/events") {
            baseHeaders()
            bearer()
            parameter("select", "id,title,description,start_datetime,end_datetime,venue_name,city_id,status,ai_confidence,price,is_free,age_min,age_max,images,source_name,created_at,updated_at")
            parameter("order", "start_datetime.desc")
            parameter("limit", limit)
            parameter("offset", offset)
            if (keyword != null) parameter("or", "(title.ilike.%${keyword}%,description.ilike.%${keyword}%)")
            if (status != null) parameter("status", "eq.$status")
            if (cityId != null) parameter("city_id", "eq.${cityId.rawValue}")
        }
        return response.requireOk().decodeList<AdminEventListRow>().map { it.toDto() }
    }

    override suspend fun adminListEventFacets(): AdminEventFacetsDto {
        val statusResponse = client.get("$baseUrl/rest/v1/events") {
            baseHeaders()
            bearer()
            parameter("select", "status")
        }
        val cityResponse = client.get("$baseUrl/rest/v1/events") {
            baseHeaders()
            bearer()
            parameter("select", "city_id")
        }
        val statusRows = statusResponse.requireOk().decodeList<AdminEventStatusRow>()
        val cityRows = cityResponse.requireOk().decodeList<AdminEventCityRow>()
        val statusCounts = statusRows.groupingBy { it.status }.eachCount()
        val cityCounts = cityRows
            .groupingBy { it.cityId ?: "_unknown" }
            .eachCount()
            .let { it + ("_total" to cityRows.size) }
        return AdminEventFacetsDto(statusCounts, cityCounts)
    }

    override suspend fun adminBulkUpdateEventStatus(eventIds: List<EventId>, status: String) {
        if (eventIds.isEmpty()) return
        val ids = eventIds.joinToString(",") { it.rawValue }
        client.patch("$baseUrl/rest/v1/events") {
            baseHeaders()
            bearer()
            parameter("id", "in.($ids)")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("status", status) }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminDeleteEvent(eventId: EventId) {
        client.delete("$baseUrl/rest/v1/events") {
            baseHeaders()
            bearer()
            parameter("id", "eq.${eventId.rawValue}")
        }.requireOkOrNoContent()
    }

    override suspend fun adminListEventAiTraces(eventId: EventId, limit: Int): List<AdminEventAiTraceDto> {
        val response = client.get("$baseUrl/rest/v1/event_ai_traces") {
            baseHeaders()
            bearer()
            parameter("select", "id,event_id,provider,model,created_at,input_title,input_description,reasoning_summary")
            parameter("event_id", "eq.${eventId.rawValue}")
            parameter("order", "created_at.desc")
            parameter("limit", limit)
        }
        return response.requireOk().decodeList<AdminEventAiTraceRow>().map { it.toDto() }
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

    override suspend fun publicEvent(id: EventId): EventDto? {
        val response = client.get("$baseUrl/rest/v1/public_events") {
            baseHeaders()
            header(HttpHeaders.Authorization, "Bearer ${config.supabaseAnonKey}")
            parameter("select", "*")
            parameter("id", "eq.${id.rawValue}")
            parameter("limit", "1")
        }
        return response.requireOk().decodeList<EventRow>().firstOrNull()?.toDto()
    }

    private suspend fun eventsByIds(ids: List<EventId>): List<EventDto> {
        val uniqueIds = ids.distinctBy { it.rawValue }.filter { it.rawValue.isUuid() }
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
        throw toAppError(status, bodyAsText())
    }

    private suspend fun HttpResponse.requireOkOrNoContent(): HttpResponse {
        if (status == HttpStatusCode.NoContent || status.value in 200..299) return this
        throw toAppError(status, bodyAsText())
    }

    private fun toAppError(status: HttpStatusCode, body: String): AppError {
        val payload = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()
        val code = payload?.get("error_code")?.jsonPrimitive?.contentOrNull
            ?: payload?.get("code")?.jsonPrimitive?.contentOrNull
        val message = payload?.get("msg")?.jsonPrimitive?.contentOrNull
            ?: payload?.get("message")?.jsonPrimitive?.contentOrNull
            ?: body.ifBlank { "Supabase request failed with HTTP ${status.value}" }

        return when (code) {
            "invalid_credentials" -> AppError.InvalidCredentials
            "email_not_confirmed" -> AppError.EmailNotConfirmed
            "user_already_exists",
            "email_exists",
            -> AppError.EmailAlreadyInUse
            "weak_password" -> AppError.WeakPassword(message)
            else -> if (status == HttpStatusCode.Unauthorized) AppError.Unauthorized else AppError.Remote(message)
        }
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
        val parsedStart = startDatetime?.parseInstant() ?: return null
        val parsedCity = cityId?.takeIf { it.isNotBlank() } ?: "unknown"
        return EventDto(
            id = EventId(parsedId),
            title = parsedTitle,
            description = description,
            startsAt = parsedStart,
            endsAt = endDatetime?.parseInstant(),
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
    fun toDto(): RatingDto = RatingDto(id, UserId(userId), EventId(eventId), score, createdAt.parseInstant())
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
        createdAt = createdAt.parseInstant(),
        updatedAt = (updatedAt ?: createdAt).parseInstant(),
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

@Serializable
private data class AdminInviteCodeRow(
    val id: String,
    val code: String,
    @SerialName("max_uses") val maxUses: Int,
    @SerialName("expires_at") val expiresAt: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
) {
    fun toDto() = AdminInviteCodeResultDto(
        id = id,
        code = code,
        maxUses = maxUses,
        expiresAt = expiresAt?.parseInstant(),
        notes = notes,
        createdAt = createdAt.parseInstant(),
    )
}

@Serializable
private data class AdminInviteApprovalRow(
    @SerialName("request_id") val requestId: String,
    val code: String,
    @SerialName("invite_code_id") val inviteCodeId: String,
    val email: String,
    @SerialName("created_at") val createdAt: String,
) {
    fun toDto() = AdminInviteApprovalDto(
        requestId = requestId,
        code = code,
        inviteCodeId = inviteCodeId,
        email = email,
        createdAt = createdAt.parseInstant(),
    )
}

@Serializable
private data class AdminCronJobRow(
    val jobid: Long,
    val jobname: String,
    val schedule: String,
    val command: String,
    val active: Boolean,
    @SerialName("last_run_start") val lastRunStart: String? = null,
    @SerialName("last_run_end") val lastRunEnd: String? = null,
    @SerialName("last_run_status") val lastRunStatus: String? = null,
    @SerialName("last_run_message") val lastRunMessage: String? = null,
) {
    fun toDto() = AdminCronJobDto(
        jobid = jobid,
        jobname = jobname,
        schedule = schedule,
        command = command,
        active = active,
        lastRunStart = lastRunStart?.parseInstant(),
        lastRunEnd = lastRunEnd?.parseInstant(),
        lastRunStatus = lastRunStatus,
        lastRunMessage = lastRunMessage,
    )
}

@Serializable
private data class AdminCronRunRow(
    val runid: Long,
    val jobname: String,
    val status: String,
    @SerialName("return_message") val returnMessage: String? = null,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("duration_ms") val durationMs: Double? = null,
) {
    fun toDto() = AdminCronRunDto(
        runid = runid,
        jobname = jobname,
        status = status,
        returnMessage = returnMessage,
        startTime = startTime.parseInstant(),
        endTime = endTime?.parseInstant(),
        durationMs = durationMs,
    )
}

@Serializable
private data class AdminCommentUserProfileRow(
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
private data class AdminCommentEventRow(
    val title: String? = null,
)

@Serializable
private data class AdminCommentRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("event_id") val eventId: String,
    val body: String,
    @SerialName("is_approved") val isApproved: Boolean = false,
    @SerialName("is_flagged") val isFlagged: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_profiles") val userProfiles: AdminCommentUserProfileRow? = null,
    val events: AdminCommentEventRow? = null,
) {
    fun toAdminDto() = AdminCommentDto(
        id = id,
        userId = UserId(userId),
        eventId = EventId(eventId),
        body = body,
        isApproved = isApproved,
        isFlagged = isFlagged,
        createdAt = createdAt.parseInstant(),
        authorDisplayName = userProfiles?.displayName,
        eventTitle = events?.title,
    )
}

@Serializable
private data class AdminSourceRow(
    val id: String,
    val name: String,
    @SerialName("city_id") val cityId: String? = null,
    val url: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("auto_approve") val autoApprove: Boolean = false,
    @SerialName("last_status") val lastStatus: String? = null,
    @SerialName("last_scraped_at") val lastScrapedAt: String? = null,
) {
    fun toDto() = AdminSourceDto(
        id = id,
        name = name,
        cityId = cityId?.let { CityId(it) },
        url = url,
        isActive = isActive,
        autoApprove = autoApprove,
        lastStatus = lastStatus,
        lastScrapedAt = lastScrapedAt?.parseInstant(),
    )
}

@Serializable
private data class AdminInviteCodeListRow(
    val id: String,
    @SerialName("max_uses") val maxUses: Int,
    @SerialName("used_count") val usedCount: Int = 0,
    @SerialName("expires_at") val expiresAt: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("revoked_at") val revokedAt: String? = null,
) {
    fun toDto() = AdminInviteCodeListDto(
        id = id,
        maxUses = maxUses,
        usedCount = usedCount,
        expiresAt = expiresAt?.parseInstant(),
        notes = notes,
        createdAt = createdAt.parseInstant(),
        revokedAt = revokedAt?.parseInstant(),
    )
}

@Serializable
private data class AdminInviteRequestRow(
    val id: String,
    val email: String,
    val message: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("admin_notes") val adminNotes: String? = null,
) {
    fun toDto() = AdminInviteRequestDto(
        id = id,
        email = email,
        message = message,
        status = status,
        createdAt = createdAt.parseInstant(),
        reviewedAt = reviewedAt?.parseInstant(),
        adminNotes = adminNotes,
    )
}

@Serializable
private data class AdminEventListRow(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("start_datetime") val startDatetime: String,
    @SerialName("end_datetime") val endDatetime: String? = null,
    @SerialName("venue_name") val venueName: String? = null,
    @SerialName("city_id") val cityId: String? = null,
    val status: String,
    @SerialName("ai_confidence") val aiConfidence: Double? = null,
    val price: Double? = null,
    @SerialName("is_free") val isFree: Boolean = false,
    @SerialName("age_min") val ageMin: Int? = null,
    @SerialName("age_max") val ageMax: Int? = null,
    val images: JsonElement? = null,
    @SerialName("source_name") val sourceName: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
) {
    fun toDto() = AdminEventListItemDto(
        id = EventId(id),
        title = title,
        description = description,
        startsAt = startDatetime.parseInstant(),
        endsAt = endDatetime?.parseInstant(),
        venueName = venueName,
        cityId = cityId?.let { CityId(it) },
        cityName = null,
        status = status,
        aiConfidence = aiConfidence,
        price = price,
        isFree = isFree,
        ageMin = ageMin,
        ageMax = ageMax,
        imageUrl = images.firstImageUrl(),
        sourceName = sourceName,
        createdAt = createdAt.parseInstant(),
        updatedAt = updatedAt.parseInstant(),
    )
}

@Serializable
private data class AdminEventStatusRow(val status: String)

@Serializable
private data class AdminEventCityRow(@SerialName("city_id") val cityId: String? = null)

@Serializable
private data class AdminEventAiTraceRow(
    val id: String,
    @SerialName("event_id") val eventId: String,
    val provider: String? = null,
    val model: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("input_title") val inputTitle: String? = null,
    @SerialName("input_description") val inputDescription: String? = null,
    @SerialName("reasoning_summary") val reasoningSummary: String? = null,
) {
    fun toDto() = AdminEventAiTraceDto(
        id = id,
        eventId = EventId(eventId),
        provider = provider,
        model = model,
        createdAt = createdAt.parseInstant(),
        inputSummary = inputTitle,
        outputSummary = reasoningSummary,
        confidence = null,
    )
}

private fun String.parseInstant(): Instant = try {
    Instant.parse(this)
} catch (e: Exception) {
    OffsetDateTime.parse(this).toInstant()
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

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Int?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: JsonElement?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}
