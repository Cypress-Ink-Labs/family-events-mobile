package com.familyevents.admin

import com.familyevents.core.AppError
import com.familyevents.core.CityId
import com.familyevents.core.EnvConfig
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventDto
import com.familyevents.data.SessionStore
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

interface SupabaseAdminApi {
    suspend fun adminStats(): AdminStatsDto
    suspend fun adminUpdateEvent(eventId: EventId, patchJson: String, tagIds: List<String> = emptyList(), lockEditedFields: Boolean = true): EventDto
    suspend fun adminCreateEvent(patchJson: String, tagIds: List<String> = emptyList()): EventDto
    suspend fun adminUnlockEventFields(eventId: EventId): Boolean
    suspend fun adminModerateComment(commentId: String, approved: Boolean, flagged: Boolean)
    suspend fun adminCreateInviteCode(maxUses: Int = 1, expiresAtIso: String? = null, notes: String? = null): AdminInviteCodeResultDto
    suspend fun adminApproveInviteRequest(requestId: String): AdminInviteApprovalDto
    suspend fun adminRejectInviteRequest(requestId: String, notes: String? = null): Boolean
    suspend fun adminBulkSetAutoApprove(enable: Boolean)
    suspend fun adminRevokeInvite(inviteId: String): Boolean
    suspend fun adminRunSource(sourceId: String?)
    suspend fun adminRetryTagQueue(eventId: EventId): Boolean
    suspend fun adminListCronJobs(): List<AdminCronJobDto>
    suspend fun adminCronRunHistory(jobName: String? = null, limit: Int = 50): List<AdminCronRunDto>
    suspend fun adminToggleCronJob(jobName: String, active: Boolean)
    suspend fun adminSetCronSchedule(jobName: String, schedule: String)
    suspend fun adminRunDueScrapes()
    suspend fun adminListComments(filter: String = "all"): List<AdminCommentDto>
    suspend fun adminDeleteComment(commentId: String)
    suspend fun adminListSources(): List<AdminSourceDto>
    suspend fun adminUpdateSourceActive(sourceId: String, active: Boolean)
    suspend fun adminUpdateSourceAutoApprove(sourceId: String, autoApprove: Boolean)
    suspend fun adminListInviteCodes(): List<AdminInviteCodeListDto>
    suspend fun adminListInviteRequests(status: String = "pending"): List<AdminInviteRequestDto>
    suspend fun adminListCities(): List<AdminCityDto>
    suspend fun adminCreateCity(name: String, state: String?, country: String = "US", slug: String, timezone: String = "America/Chicago"): AdminCityDto
    suspend fun adminUpdateCity(cityId: CityId, patchJson: String)
    suspend fun adminListRatings(limit: Int = 100): List<AdminRatingDto>
    suspend fun adminDeleteRating(ratingId: String)
    suspend fun adminListUserAccess(): List<AdminUserAccessDto>
    suspend fun adminUpdateUserAccess(userId: UserId, isEnabled: Boolean, disabledReason: String? = null)
    suspend fun adminListSourceRuns(limit: Int = 50): List<AdminSourceRunDto>
    suspend fun adminListTagQueueSummary(): List<AdminTagQueueSummaryRowDto>
    suspend fun adminListEvents(
        keyword: String? = null,
        status: String? = null,
        cityId: CityId? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): List<AdminEventListItemDto>
    suspend fun adminListEventFacets(): AdminEventFacetsDto
    suspend fun adminBulkUpdateEventStatus(eventIds: List<EventId>, status: String)
    suspend fun adminBulkDeleteEvent(eventIds: List<EventId>)
    suspend fun adminDeleteEvent(eventId: EventId)
    suspend fun adminListEventAiTraces(eventId: EventId, limit: Int = 5): List<AdminEventAiTraceDto>
}

class KtorSupabaseAdminApi(
    private val config: EnvConfig,
    private val sessionStore: SessionStore,
    private val client: HttpClient,
) : SupabaseAdminApi {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val baseUrl = config.supabaseUrl.trimEnd('/')

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
        val aiHigh = events.count { it.aiConfidence?.let { c -> c in 0.8..1.0 } == true }
        val aiMedium = events.count { it.aiConfidence?.let { c -> c >= 0.5 && c < 0.8 } == true }
        val aiLow = events.count { it.aiConfidence?.let { c -> c >= 0.0 && c < 0.5 } == true }
        val aiUntagged = events.count { val c = it.aiConfidence; c == null || c < 0.0 || c > 1.0 }
        return AdminStatsDto(
            totalEvents = events.size,
            pendingReview = events.count { it.status == "draft" },
            published = events.count { it.status == "published" },
            activeSources = sources.count { it.isActive },
            sourceErrors = sources.count { it.isActive && it.lastStatus == "error" },
            aiHigh = aiHigh,
            aiMedium = aiMedium,
            aiLow = aiLow,
            aiUntagged = aiUntagged,
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

    override suspend fun adminListCities(): List<AdminCityDto> {
        val response = client.get("$baseUrl/rest/v1/cities") {
            baseHeaders()
            bearer()
            parameter("select", "id,name,state,country,slug,is_active,latitude,longitude,timezone,created_at")
            parameter("order", "name.asc")
        }
        return response.requireOk().decodeList<AdminCityRow>().map { it.toDto() }
    }

    override suspend fun adminCreateCity(name: String, state: String?, country: String, slug: String, timezone: String): AdminCityDto {
        val response = client.post("$baseUrl/rest/v1/cities") {
            baseHeaders()
            bearer()
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("name", name)
                    putNullable("state", state)
                    put("country", country)
                    put("slug", slug)
                    put("timezone", timezone)
                    put("is_active", true)
                }.toString(),
            )
        }
        return response.requireOk().decodeList<AdminCityRow>().firstOrNull()?.toDto()
            ?: throw AppError.Remote("City creation returned no data.")
    }

    override suspend fun adminUpdateCity(cityId: CityId, patchJson: String) {
        client.patch("$baseUrl/rest/v1/cities") {
            baseHeaders()
            bearer()
            parameter("id", "eq.${cityId.rawValue}")
            contentType(ContentType.Application.Json)
            setBody(patchJson)
        }.requireOkOrNoContent()
    }

    override suspend fun adminListRatings(limit: Int): List<AdminRatingDto> {
        val response = client.get("$baseUrl/rest/v1/ratings") {
            baseHeaders()
            bearer()
            parameter("select", "id,user_id,event_id,score,created_at,user_profiles(display_name),events(title)")
            parameter("order", "created_at.desc")
            parameter("limit", limit.toString())
        }
        return response.requireOk().decodeList<AdminRatingRow>().map { it.toDto() }
    }

    override suspend fun adminDeleteRating(ratingId: String) {
        client.post("$baseUrl/rest/v1/rpc/admin_delete_rating") {
            baseHeaders()
            bearer()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("p_id", ratingId) }.toString())
        }.requireOkOrNoContent()
    }

    override suspend fun adminListUserAccess(): List<AdminUserAccessDto> {
        val response = client.get("$baseUrl/rest/v1/user_access") {
            baseHeaders()
            bearer()
            parameter("select", "user_id,is_enabled,access_expires_at,enabled_at,disabled_at,disabled_reason,user_profiles(display_name,email,role)")
            parameter("order", "enabled_at.desc.nullslast")
        }
        return response.requireOk().decodeList<AdminUserAccessRow>().map { it.toDto() }
    }

    override suspend fun adminUpdateUserAccess(userId: UserId, isEnabled: Boolean, disabledReason: String?) {
        client.patch("$baseUrl/rest/v1/user_access") {
            baseHeaders()
            bearer()
            parameter("user_id", "eq.${userId.rawValue}")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("is_enabled", isEnabled)
                    putNullable("disabled_reason", if (isEnabled) null else disabledReason)
                    // enabled_at / disabled_at are populated server-side (DB trigger / now()).
                }.toString(),
            )
        }.requireOkOrNoContent()
    }

    override suspend fun adminListSourceRuns(limit: Int): List<AdminSourceRunDto> {
        val response = client.get("$baseUrl/rest/v1/source_runs") {
            baseHeaders()
            bearer()
            parameter("select", "id,source_id,started_at,completed_at,status,events_found,events_imported,events_skipped,error_log,event_sources(name)")
            parameter("order", "started_at.desc")
            parameter("limit", limit.toString())
        }
        return response.requireOk().decodeList<AdminSourceRunRow>().map { it.toDto() }
    }

    override suspend fun adminListTagQueueSummary(): List<AdminTagQueueSummaryRowDto> {
        val response = client.get("$baseUrl/rest/v1/event_tag_queue_summary") {
            baseHeaders()
            bearer()
            parameter("select", "status,row_count,oldest_enqueued_at,newest_enqueued_at,last_dead_letter_at,avg_attempts")
        }
        return response.requireOk().decodeList<AdminTagQueueSummaryRow>().map { it.toDto() }
    }

    override suspend fun adminListEvents(
        keyword: String?,
        status: String?,
        cityId: CityId?,
        limit: Int,
        offset: Int,
    ): List<AdminEventListItemDto> {
        val escapedKeyword = keyword
            ?.trim()
            ?.let(::escapePostgrestIlike)
            ?.takeIf { it.isNotBlank() }
        val response = client.get("$baseUrl/rest/v1/events") {
            baseHeaders()
            bearer()
            parameter("select", "id,title,description,start_datetime,end_datetime,venue_name,city_id,status,ai_confidence,price,is_free,age_min,age_max,images,source_name,created_at,updated_at")
            parameter("order", "start_datetime.desc")
            parameter("limit", limit)
            parameter("offset", offset)
            if (escapedKeyword != null) parameter("or", "(title.ilike.%${escapedKeyword}%,description.ilike.%${escapedKeyword}%)")
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
            .mapNotNull { it.cityId }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
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

    override suspend fun adminBulkDeleteEvent(eventIds: List<EventId>) {
        if (eventIds.isEmpty()) return
        val ids = eventIds.joinToString(",") { it.rawValue }
        client.delete("$baseUrl/rest/v1/events") {
            baseHeaders()
            bearer()
            parameter("id", "in.($ids)")
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
            "user_already_exists", "email_exists" -> AppError.EmailAlreadyInUse
            "weak_password" -> AppError.WeakPassword(message)
            else -> if (status == HttpStatusCode.Unauthorized) AppError.Unauthorized else AppError.Remote(message)
        }
    }

    private suspend inline fun <reified T> HttpResponse.decodeList(): List<T> =
        json.decodeFromString(ListSerializer(serializer<T>()), bodyAsText())
}

// --- Private row types used only by KtorSupabaseAdminApi ---

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
private data class AdminInviteCodeRow(
    val id: String,
    val code: String,
    @SerialName("max_uses") val maxUses: Int,
    @SerialName("expires_at") val expiresAt: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
) {
    fun toDto() = AdminInviteCodeResultDto(
        id = id, code = code, maxUses = maxUses,
        expiresAt = expiresAt?.parseInstant(), notes = notes,
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
        requestId = requestId, code = code, inviteCodeId = inviteCodeId,
        email = email, createdAt = createdAt.parseInstant(),
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
        jobid = jobid, jobname = jobname, schedule = schedule, command = command, active = active,
        lastRunStart = lastRunStart?.parseInstant(), lastRunEnd = lastRunEnd?.parseInstant(),
        lastRunStatus = lastRunStatus, lastRunMessage = lastRunMessage,
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
        runid = runid, jobname = jobname, status = status,
        returnMessage = returnMessage, startTime = startTime.parseInstant(),
        endTime = endTime?.parseInstant(), durationMs = durationMs,
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
        id = id, userId = UserId(userId), eventId = EventId(eventId),
        body = body, isApproved = isApproved, isFlagged = isFlagged,
        createdAt = createdAt.parseInstant(),
        authorDisplayName = userProfiles?.displayName, eventTitle = events?.title,
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
        id = id, name = name, cityId = cityId?.let { CityId(it) }, url = url,
        isActive = isActive, autoApprove = autoApprove, lastStatus = lastStatus,
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
        id = id, maxUses = maxUses, usedCount = usedCount,
        expiresAt = expiresAt?.parseInstant(), notes = notes,
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
        id = id, email = email, message = message, status = status,
        createdAt = createdAt.parseInstant(), reviewedAt = reviewedAt?.parseInstant(),
        adminNotes = adminNotes,
    )
}

@Serializable
private data class AdminCityRow(
    val id: String,
    val name: String,
    val state: String? = null,
    val country: String = "US",
    val slug: String,
    @SerialName("is_active") val isActive: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String = "America/Chicago",
    @SerialName("created_at") val createdAt: String,
) {
    fun toDto() = AdminCityDto(
        id = CityId(id), name = name, state = state, country = country,
        slug = slug, isActive = isActive, timezone = timezone,
        latitude = latitude, longitude = longitude, createdAt = createdAt.parseInstant(),
    )
}

@Serializable
private data class AdminRatingUserProfileRow(
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
private data class AdminRatingEventRow(
    val title: String? = null,
)

@Serializable
private data class AdminRatingRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("event_id") val eventId: String,
    val score: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_profiles") val userProfiles: AdminRatingUserProfileRow? = null,
    val events: AdminRatingEventRow? = null,
) {
    fun toDto() = AdminRatingDto(
        id = id, userId = UserId(userId), eventId = EventId(eventId),
        score = score, createdAt = createdAt.parseInstant(),
        authorDisplayName = userProfiles?.displayName, eventTitle = events?.title,
    )
}

@Serializable
private data class AdminUserProfileAccessRow(
    @SerialName("display_name") val displayName: String? = null,
    val email: String? = null,
    val role: String? = null,
)

@Serializable
private data class AdminUserAccessRow(
    @SerialName("user_id") val userId: String,
    @SerialName("is_enabled") val isEnabled: Boolean = false,
    @SerialName("access_expires_at") val accessExpiresAt: String? = null,
    @SerialName("enabled_at") val enabledAt: String? = null,
    @SerialName("disabled_at") val disabledAt: String? = null,
    @SerialName("disabled_reason") val disabledReason: String? = null,
    @SerialName("user_profiles") val userProfiles: AdminUserProfileAccessRow? = null,
) {
    fun toDto() = AdminUserAccessDto(
        userId = UserId(userId), isEnabled = isEnabled,
        accessExpiresAt = accessExpiresAt?.parseInstant(),
        enabledAt = enabledAt?.parseInstant(),
        disabledAt = disabledAt?.parseInstant(),
        disabledReason = disabledReason,
        displayName = userProfiles?.displayName,
        email = userProfiles?.email,
        role = userProfiles?.role ?: "user",
    )
}

@Serializable
private data class AdminSourceRunSourceRow(
    val name: String? = null,
)

@Serializable
private data class AdminSourceRunRow(
    val id: String,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("started_at") val startedAt: String,
    @SerialName("completed_at") val completedAt: String? = null,
    val status: String,
    @SerialName("events_found") val eventsFound: Int = 0,
    @SerialName("events_imported") val eventsImported: Int = 0,
    @SerialName("events_skipped") val eventsSkipped: Int = 0,
    @SerialName("error_log") val errorLog: String? = null,
    @SerialName("event_sources") val eventSources: AdminSourceRunSourceRow? = null,
) {
    fun toDto() = AdminSourceRunDto(
        id = id, sourceId = sourceId, sourceName = eventSources?.name,
        startedAt = startedAt.parseInstant(), completedAt = completedAt?.parseInstant(),
        status = status, eventsFound = eventsFound, eventsImported = eventsImported,
        eventsSkipped = eventsSkipped, errorLog = errorLog,
    )
}

@Serializable
private data class AdminTagQueueSummaryRow(
    val status: String,
    @SerialName("row_count") val rowCount: Int = 0,
    @SerialName("oldest_enqueued_at") val oldestEnqueuedAt: String? = null,
    @SerialName("newest_enqueued_at") val newestEnqueuedAt: String? = null,
    @SerialName("last_dead_letter_at") val lastDeadLetterAt: String? = null,
    @SerialName("avg_attempts") val avgAttempts: Double? = null,
) {
    fun toDto() = AdminTagQueueSummaryRowDto(
        status = status, rowCount = rowCount,
        oldestEnqueuedAt = oldestEnqueuedAt?.parseInstant(),
        newestEnqueuedAt = newestEnqueuedAt?.parseInstant(),
        lastDeadLetterAt = lastDeadLetterAt?.parseInstant(),
        avgAttempts = avgAttempts,
    )
}

// EventRow is also needed here to decode admin_update_event / admin_create_event results.
// We duplicate only the fields we need (same shape as in data module).
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
            id = com.familyevents.core.EventId(parsedId),
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
            cityId = com.familyevents.core.CityId(parsedCity),
            coordinate = latitude?.let { lat -> longitude?.let { lng -> com.familyevents.core.GeoCoordinate(lat, lng) } },
            tags = tags.toTags(),
            avgRating = avgRating ?: 0.0,
            ratingCount = ratingCount ?: 0,
            isFavorited = isFavorited ?: false,
            isInCalendar = isInCalendar ?: false,
        )
    }
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

private fun JsonElement?.toTags(): List<com.familyevents.data.EventTagDto> = when (this) {
    is JsonArray -> mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val id = obj["id"]?.jsonPrimitive?.contentOrNull
            ?: obj["slug"]?.jsonPrimitive?.contentOrNull
            ?: obj["name"]?.jsonPrimitive?.contentOrNull
            ?: return@mapNotNull null
        val label = obj["label"]?.jsonPrimitive?.contentOrNull
            ?: obj["name"]?.jsonPrimitive?.contentOrNull
            ?: id
        com.familyevents.data.EventTagDto(id, label)
    }
    else -> emptyList()
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}

private fun escapePostgrestIlike(keyword: String): String = buildString {
    keyword.forEach { char ->
        when (char) {
            '\\', '%', '_' -> {
                append('\\')
                append(char)
            }
            '(', ')', ',' -> Unit
            else -> append(char)
        }
    }
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
