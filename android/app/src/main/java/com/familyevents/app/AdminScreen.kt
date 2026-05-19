package com.familyevents.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familyevents.core.decodeHtmlEntities
import com.familyevents.core.CityId
import com.familyevents.core.DateFormatting
import com.familyevents.core.EventId
import com.familyevents.data.AdminCommentDto
import com.familyevents.data.AdminEventAiTraceDto
import com.familyevents.data.AdminEventFacetsDto
import com.familyevents.data.AdminEventListItemDto
import com.familyevents.data.AdminInviteCodeListDto
import com.familyevents.data.AdminInviteRequestDto
import com.familyevents.data.AdminRepository
import com.familyevents.data.AdminSourceDto
import com.familyevents.data.AdminStatsDto
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.ErrorState
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.LoadingState
import com.familyevents.designsystem.TagPill
import com.familyevents.designsystem.generated.Tokens

private fun text(s: String?): String = decodeHtmlEntities(s ?: "")

@Composable
fun AdminScreen(
    adminRepository: AdminRepository,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Tokens.Space.S4),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4),
    ) {
        Text("Admin", style = FamilyTypography.TitleLarge)
        AdminDashboardSection(adminRepository)
        AdminEventsSection(adminRepository)
        AdminCommentsSection(adminRepository)
        AdminSourcesSection(adminRepository)
        AdminInvitesSection(adminRepository)
    }
}

@Composable
private fun AdminDashboardSection(adminRepository: AdminRepository) {
    var stats by remember { mutableStateOf<AdminStatsDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { adminRepository.stats() }
            .onSuccess { stats = it }
            .onFailure { error = it.message ?: "Failed to load stats" }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("Dashboard", style = FamilyTypography.TitleMedium)
        when {
            error != null -> ErrorState(message = error!!)
            stats == null -> LoadingState("Loading stats")
            else -> {
                val s = stats!!
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
                    maxItemsInEachRow = 2,
                ) {
                    StatTile(label = "Total Events", value = s.totalEvents.toString(), modifier = Modifier.weight(1f))
                    StatTile(label = "Pending", value = s.pendingReview.toString(), modifier = Modifier.weight(1f))
                    StatTile(label = "Published", value = s.published.toString(), modifier = Modifier.weight(1f))
                    StatTile(label = "Sources", value = s.activeSources.toString(), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Space.S4),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S1),
        ) {
            Text(
                text = label,
                style = FamilyTypography.Caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                text = value,
                style = FamilyTypography.TitleLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

private val eventStatuses = listOf("draft", "published", "rejected", "archived")

@Composable
private fun AdminEventsSection(adminRepository: AdminRepository) {
    var facets by remember { mutableStateOf<AdminEventFacetsDto?>(null) }
    var events by remember { mutableStateOf<List<AdminEventListItemDto>>(emptyList()) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("draft") }
    var cityId by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var bulkInFlight by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<AdminEventListItemDto?>(null) }
    val scope = rememberCoroutineScope()

    // Initial facets load
    LaunchedEffect(Unit) {
        runCatching { adminRepository.listEventFacets() }
            .onSuccess { facets = it }
    }

    // Debounced list reload
    LaunchedEffect(keyword, status, cityId, refreshKey) {
        delay(300)
        try {
            loading = true
            events = adminRepository.listEvents(
                keyword = keyword.trim().ifEmpty { null },
                status = status,
                cityId = cityId?.let { CityId(it) },
            )
        } catch (e: Exception) {
            feedback = e.message ?: "Failed to load events"
        } finally {
            loading = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("Events", style = FamilyTypography.TitleMedium)

        // 1. Status filter chips
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
            eventStatuses.forEach { s ->
                FilterChip(
                    selected = status == s,
                    onClick = { status = s; selected = emptySet() },
                    label = { Text("$s (${facets?.statusCounts?.get(s) ?: 0})") },
                )
            }
        }

        // 2. Keyword search
        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text("Search title or description") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // 3. City filter chips (only when facets have cities)
        val cities = facets?.cityCounts
        if (!cities.isNullOrEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                FilterChip(
                    selected = cityId == null,
                    onClick = { cityId = null },
                    label = { Text("All cities (${cities.values.sum()})") },
                )
                cities.forEach { (cId, count) ->
                    FilterChip(
                        selected = cityId == cId,
                        onClick = { cityId = cId },
                        label = { Text("$cId ($count)") },
                    )
                }
            }
        }

        // 4. Bulk action bar
        if (selected.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                Text("${selected.size} selected", style = FamilyTypography.BodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                    listOf("published" to "Publish", "rejected" to "Reject", "archived" to "Archive").forEach { (target, label) ->
                        Button(
                            onClick = {
                                scope.launch {
                                    bulkInFlight = true
                                    try {
                                        adminRepository.bulkUpdateEventStatus(selected.map { EventId(it) }, target)
                                        selected = emptySet()
                                        feedback = null
                                        refreshKey++
                                    } catch (e: Exception) {
                                        feedback = e.message ?: "Bulk update failed"
                                    } finally {
                                        bulkInFlight = false
                                    }
                                }
                            },
                            enabled = !bulkInFlight,
                        ) { Text(label, softWrap = false, maxLines = 1) }
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        enabled = !bulkInFlight,
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("Delete", softWrap = false, maxLines = 1) }
                    TextButton(onClick = { selected = emptySet() }) { Text("Clear") }
                }
            }
        }

        // 5. Feedback
        feedback?.let {
            Text(it, style = FamilyTypography.BodySmall, color = MaterialTheme.colorScheme.error)
        }

        // 6. Event list
        when {
            loading && events.isEmpty() -> LoadingState("Loading events")
            !loading && events.isEmpty() -> EmptyState("No events for filter.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                events.forEach { event ->
                    AdminEventCard(
                        event = event,
                        isSelected = event.id.rawValue in selected,
                        onToggleSelect = {
                            selected = if (event.id.rawValue in selected) {
                                selected - event.id.rawValue
                            } else {
                                selected + event.id.rawValue
                            }
                        },
                        onEdit = { editingEvent = event },
                        onChangeStatus = { target ->
                            scope.launch {
                                try {
                                    adminRepository.bulkUpdateEventStatus(listOf(event.id), target)
                                    feedback = null
                                    refreshKey++
                                } catch (e: Exception) {
                                    feedback = e.message ?: "Status update failed"
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                try {
                                    adminRepository.deleteEvent(event.id)
                                    feedback = null
                                    refreshKey++
                                } catch (e: Exception) {
                                    feedback = e.message ?: "Delete failed"
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    // Bulk delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selected.size} events permanently?") },
            text = { Text("This cannot be undone.", style = FamilyTypography.BodySmall) },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = selected.toSet()
                        showDeleteConfirm = false
                        scope.launch {
                            bulkInFlight = true
                            try {
                                adminRepository.bulkDeleteEvent(toDelete.map { EventId(it) })
                                selected = emptySet()
                                feedback = null
                                refreshKey++
                            } catch (e: Exception) {
                                feedback = e.message ?: "Delete failed"
                            } finally {
                                bulkInFlight = false
                            }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    // Event editor dialog
    editingEvent?.let { current ->
        AdminEventEditorDialog(
            event = current,
            adminRepository = adminRepository,
            onDismiss = { editingEvent = null },
            onSaved = { editingEvent = null; refreshKey++ },
        )
    }
}

@Composable
private fun AdminEventCard(
    event: AdminEventListItemDto,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onChangeStatus: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showDelete by remember { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Tokens.Space.S3),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(Tokens.Touch.Min),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit() },
                ) {
                    Text(
                        text = text(event.title),
                        style = FamilyTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    Text(
                        text = "${DateFormatting.cardSubtitle(event.startsAt)} • ${text(event.venueName ?: "Venue TBA")}",
                        style = FamilyTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                    TagPill(label = event.status)
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S1)) {
                listOf("published" to "Publish", "rejected" to "Reject", "archived" to "Archive").forEach { (target, label) ->
                    TextButton(
                        onClick = { onChangeStatus(target) },
                        enabled = event.status != target,
                    ) { Text(label, softWrap = false, maxLines = 1) }
                }
                TextButton(
                    onClick = { showDelete = true },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete", softWrap = false, maxLines = 1) }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete event permanently?") },
            text = { Text("This cannot be undone.", style = FamilyTypography.BodySmall) },
            confirmButton = {
                Button(
                    onClick = { showDelete = false; onDelete() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminEventEditorDialog(
    event: AdminEventListItemDto,
    adminRepository: AdminRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(event.title) }
    var description by rememberSaveable { mutableStateOf(event.description.orEmpty()) }
    var venueName by rememberSaveable { mutableStateOf(event.venueName.orEmpty()) }
    var isFree by rememberSaveable { mutableStateOf(event.isFree) }
    var price by rememberSaveable { mutableStateOf(event.price?.toString().orEmpty()) }
    var ageMin by rememberSaveable { mutableStateOf(event.ageMin?.toString().orEmpty()) }
    var ageMax by rememberSaveable { mutableStateOf(event.ageMax?.toString().orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var aiTraces by remember(event.id.rawValue) { mutableStateOf<List<AdminEventAiTraceDto>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(event.id.rawValue) {
        aiTraces = runCatching { adminRepository.listEventAiTraces(event.id, limit = 3) }.getOrDefault(emptyList())
    }

    fun saveEdits() {
        if (title.trim().isEmpty()) {
            feedback = "Title cannot be empty."
            return
        }
        val parsedPrice = price.toDoubleOrNull()
        if (parsedPrice != null && parsedPrice < 0) {
            feedback = "Price cannot be negative."
            return
        }
        val parsedAgeMin = ageMin.toIntOrNull()
        val parsedAgeMax = ageMax.toIntOrNull()
        if (parsedAgeMin != null && parsedAgeMax != null && parsedAgeMin > parsedAgeMax) {
            feedback = "Age min cannot exceed age max."
            return
        }
        val patch = buildJsonObject {
            if (title != event.title) put("title", title)
            if (description != event.description.orEmpty()) {
                if (description.isEmpty()) put("description", JsonNull) else put("description", description)
            }
            if (venueName != event.venueName.orEmpty()) {
                if (venueName.isEmpty()) put("venue_name", JsonNull) else put("venue_name", venueName)
            }
            if (isFree != event.isFree) put("is_free", isFree)
            if (parsedPrice != event.price) {
                if (parsedPrice == null) put("price", JsonNull) else put("price", parsedPrice)
            }
            if (parsedAgeMin != event.ageMin) {
                if (parsedAgeMin == null) put("age_min", JsonNull) else put("age_min", parsedAgeMin)
            }
            if (parsedAgeMax != event.ageMax) {
                if (parsedAgeMax == null) put("age_max", JsonNull) else put("age_max", parsedAgeMax)
            }
        }
        if (patch.isEmpty()) {
            feedback = "No changes."
            return
        }
        saving = true
        feedback = null
        scope.launch {
            try {
                adminRepository.updateEvent(
                    eventId = event.id,
                    patchJson = patch.toString(),
                    tagIds = emptyList(),
                    lockEditedFields = true,
                )
                onSaved()
            } catch (e: Throwable) {
                feedback = "Save failed: ${e.message ?: "unknown error"}"
            } finally {
                saving = false
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(Tokens.Radius.Lg),
            modifier = Modifier.fillMaxWidth(0.95f),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(Tokens.Space.S5),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
            ) {
                // Header
                Text("Edit event", style = FamilyTypography.TitleLarge)
                TagPill(label = event.status)
                if (event.aiConfidence != null) {
                    Text(
                        "Locked fields: tap Unlock to clear",
                        style = FamilyTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }

                // Editable fields
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                )
                OutlinedTextField(
                    value = venueName,
                    onValueChange = { venueName = it },
                    label = { Text("Venue") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
                ) {
                    Text("Free event", style = FamilyTypography.BodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = isFree, onCheckedChange = { isFree = it })
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFree,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                    OutlinedTextField(
                        value = ageMin,
                        onValueChange = { ageMin = it },
                        label = { Text("Age min") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = ageMax,
                        onValueChange = { ageMax = it },
                        label = { Text("Age max") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }

                // Feedback
                feedback?.let {
                    Text(it, style = FamilyTypography.BodySmall, color = MaterialTheme.colorScheme.error)
                }

                // AI trace panel
                Text("AI traces", style = FamilyTypography.TitleMedium)
                if (aiTraces.isEmpty()) {
                    Text(
                        "No AI traces recorded.",
                        style = FamilyTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                        aiTraces.forEach { trace ->
                            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(Tokens.Space.S3),
                                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                                ) {
                                    Text(
                                        text = "${trace.provider ?: "unknown"} · ${DateFormatting.cardSubtitle(trace.createdAt)}",
                                        style = FamilyTypography.Caption,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                    trace.inputSummary?.let { summary ->
                                        Text(
                                            text = decodeHtmlEntities(summary),
                                            style = FamilyTypography.BodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                            maxLines = 3,
                                        )
                                    }
                                    trace.outputSummary?.let { summary ->
                                        Text(
                                            text = decodeHtmlEntities(summary),
                                            style = FamilyTypography.BodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                            maxLines = 3,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action row
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val ok = adminRepository.unlockEventFields(event.id)
                                feedback = if (ok) "Unlocked." else "Already unlocked."
                            }
                        },
                        enabled = !saving,
                    ) { Text("Unlock fields") }
                    Button(onClick = { saveEdits() }, enabled = !saving) {
                        Text(if (saving) "Saving…" else "Save")
                    }
                }
            }
        }
    }
}

private val filters = listOf("all" to "All", "flagged" to "Flagged", "pending" to "Pending", "approved" to "Approved")

@Composable
private fun AdminCommentsSection(adminRepository: AdminRepository) {
    var filter by rememberSaveable { mutableStateOf("flagged") }
    var comments by remember { mutableStateOf<List<AdminCommentDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(filter, refreshKey) {
        loading = true
        error = null
        runCatching { adminRepository.listComments(filter) }
            .onSuccess { comments = it; loading = false }
            .onFailure { error = it.message ?: "Failed to load comments"; loading = false }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("Comments", style = FamilyTypography.TitleMedium)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            filters.forEach { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(label) },
                )
            }
        }

        when {
            error != null -> ErrorState(message = error!!)
            loading -> LoadingState("Loading comments")
            comments.isEmpty() -> Text(
                "No comments",
                style = FamilyTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                comments.forEach { comment ->
                    AdminCommentCard(
                        comment = comment,
                        onApprove = {
                            scope.launch {
                                runCatching {
                                    adminRepository.moderateComment(comment.id, approved = true, flagged = false)
                                }
                                refreshKey++
                            }
                        },
                        onRemove = {
                            scope.launch {
                                runCatching {
                                    adminRepository.deleteComment(comment.id)
                                }
                                refreshKey++
                            }
                        },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sources
// ---------------------------------------------------------------------------

@Composable
private fun AdminSourcesSection(adminRepository: AdminRepository) {
    var sources by remember { mutableStateOf<List<AdminSourceDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        runCatching { adminRepository.listSources() }
            .onSuccess { sources = it; loading = false }
            .onFailure { error = it.message ?: "Failed to load sources"; loading = false }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("Sources", style = FamilyTypography.TitleMedium)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
            OutlinedButton(onClick = {
                scope.launch {
                    runCatching { adminRepository.bulkSetAutoApprove(true) }
                    refreshKey++
                }
            }) { Text("Enable auto-approve (all)", softWrap = false, maxLines = 1) }
            OutlinedButton(onClick = {
                scope.launch {
                    runCatching { adminRepository.bulkSetAutoApprove(false) }
                    refreshKey++
                }
            }) { Text("Disable auto-approve (all)", softWrap = false, maxLines = 1) }
        }

        when {
            error != null -> ErrorState(message = error!!)
            loading -> LoadingState("Loading sources")
            sources.isEmpty() -> EmptyState("No sources configured.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                sources.forEach { source ->
                    AdminSourceCard(
                        source = source,
                        onToggleActive = { active ->
                            scope.launch {
                                runCatching { adminRepository.updateSourceActive(source.id, active) }
                                refreshKey++
                            }
                        },
                        onToggleAutoApprove = { autoApprove ->
                            scope.launch {
                                runCatching { adminRepository.updateSourceAutoApprove(source.id, autoApprove) }
                                refreshKey++
                            }
                        },
                        onRunNow = {
                            scope.launch {
                                runCatching { adminRepository.runSource(source.id) }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminSourceCard(
    source: AdminSourceDto,
    onToggleActive: (Boolean) -> Unit,
    onToggleAutoApprove: (Boolean) -> Unit,
    onRunNow: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Tokens.Space.S4),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text(source.name),
                    style = FamilyTypography.BodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                source.lastStatus?.let { TagPill(label = it) }
            }
            if (source.url != null) {
                Text(
                    text = text(source.url),
                    style = FamilyTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
            ) {
                Text("Active", style = FamilyTypography.BodySmall, modifier = Modifier.weight(1f))
                Switch(checked = source.isActive, onCheckedChange = onToggleActive)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
            ) {
                Text("Auto-approve", style = FamilyTypography.BodySmall, modifier = Modifier.weight(1f))
                Switch(checked = source.autoApprove, onCheckedChange = onToggleAutoApprove)
            }
            Button(onClick = onRunNow) {
                Text("Run now", softWrap = false, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Invites
// ---------------------------------------------------------------------------

@Composable
private fun AdminInvitesSection(adminRepository: AdminRepository) {
    var inviteView by rememberSaveable { mutableStateOf("codes") }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("Invites", style = FamilyTypography.TitleMedium)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
            FilterChip(
                selected = inviteView == "codes",
                onClick = { inviteView = "codes" },
                label = { Text("Codes") },
            )
            FilterChip(
                selected = inviteView == "requests",
                onClick = { inviteView = "requests" },
                label = { Text("Requests") },
            )
        }

        if (inviteView == "codes") {
            AdminInviteCodesView(adminRepository)
        } else {
            AdminInviteRequestsView(adminRepository)
        }
    }
}

@Composable
private fun AdminInviteCodesView(adminRepository: AdminRepository) {
    var codes by remember { mutableStateOf<List<AdminInviteCodeListDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        runCatching { adminRepository.listInviteCodes() }
            .onSuccess { codes = it; loading = false }
            .onFailure { error = it.message ?: "Failed to load invite codes"; loading = false }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Button(onClick = { showCreateDialog = true }) {
            Text("Create new code")
        }

        when {
            error != null -> ErrorState(message = error!!)
            loading -> LoadingState("Loading codes")
            codes.isEmpty() -> EmptyState("No invite codes.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                if (feedback != null) {
                    Text(
                        text = feedback!!,
                        style = FamilyTypography.BodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                codes.forEach { code ->
                    AdminInviteCodeCard(
                        code = code,
                        onRevoke = {
                            scope.launch {
                                val ok = adminRepository.revokeInvite(code.id)
                                if (ok) { refreshKey++ } else { feedback = "Revoke failed (not found or already revoked)." }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AdminCreateInviteCodeDialog(
            adminRepository = adminRepository,
            onDismiss = { showCreateDialog = false; refreshKey++ },
        )
    }
}

@Composable
private fun AdminCreateInviteCodeDialog(
    adminRepository: AdminRepository,
    onDismiss: () -> Unit,
) {
    var maxUses by rememberSaveable { mutableStateOf("1") }
    var expiresAt by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var createdCode by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (createdCode == null) "Create invite code" else "Code created") },
        text = {
            if (createdCode != null) {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                    Text("Copy this code — it will not be shown again.", style = FamilyTypography.BodySmall)
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = createdCode!!,
                            style = FamilyTypography.Body,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(Tokens.Space.S4),
                        )
                    }
                    Button(onClick = { clipboard.setText(AnnotatedString(createdCode!!)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Copy code")
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                    if (errorMsg != null) {
                        Text(errorMsg!!, style = FamilyTypography.BodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedTextField(
                        value = maxUses,
                        onValueChange = { maxUses = it },
                        label = { Text("Max uses") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = expiresAt,
                        onValueChange = { expiresAt = it },
                        label = { Text("Expires at (ISO, optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (createdCode != null) {
                Button(onClick = onDismiss) { Text("Done") }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            errorMsg = null
                            val maxUsesInt = maxUses.trim().toIntOrNull() ?: 1
                            val expiresAtIso = expiresAt.trim().ifEmpty { null }
                            val notesVal = notes.trim().ifEmpty { null }
                            runCatching { adminRepository.upsertInvite(maxUsesInt, expiresAtIso, notesVal) }
                                .onSuccess { result -> createdCode = result.code }
                                .onFailure { errorMsg = it.message ?: "Failed to create code" }
                            loading = false
                        }
                    },
                    enabled = !loading,
                ) { Text("Create") }
            }
        },
        dismissButton = {
            if (createdCode == null) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun AdminInviteCodeCard(
    code: AdminInviteCodeListDto,
    onRevoke: () -> Unit,
) {
    val isRevoked = code.revokedAt != null
    Card(modifier = Modifier.fillMaxWidth().alpha(if (isRevoked) 0.5f else 1f)) {
        Column(
            modifier = Modifier.padding(Tokens.Space.S4),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = code.id.take(8) + "…",
                    style = FamilyTypography.BodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (isRevoked) {
                    TagPill("Revoked")
                }
            }
            Text(
                text = "${code.usedCount} / ${code.maxUses} uses" +
                    if (code.expiresAt != null) " · expires ${code.expiresAt}" else "",
                style = FamilyTypography.Caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            if (code.notes != null) {
                Text(
                    text = text(code.notes),
                    style = FamilyTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            OutlinedButton(onClick = onRevoke, enabled = !isRevoked) {
                Text(if (isRevoked) "Revoked" else "Revoke", softWrap = false, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AdminInviteRequestsView(adminRepository: AdminRepository) {
    var requestFilter by rememberSaveable { mutableStateOf("pending") }
    var requests by remember { mutableStateOf<List<AdminInviteRequestDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var approvedCode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(requestFilter, refreshKey) {
        loading = true
        error = null
        runCatching { adminRepository.listInviteRequests(requestFilter) }
            .onSuccess { requests = it; loading = false }
            .onFailure { error = it.message ?: "Failed to load requests"; loading = false }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
            listOf("pending" to "Pending", "approved" to "Approved", "rejected" to "Rejected", "all" to "All").forEach { (key, label) ->
                FilterChip(
                    selected = requestFilter == key,
                    onClick = { requestFilter = key },
                    label = { Text(label) },
                )
            }
        }

        when {
            error != null -> ErrorState(message = error!!)
            loading -> LoadingState("Loading requests")
            requests.isEmpty() -> EmptyState("No invite requests.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                requests.forEach { request ->
                    AdminInviteRequestCard(
                        request = request,
                        onApprove = {
                            scope.launch {
                                runCatching { adminRepository.approveInviteRequest(request.id) }
                                    .onSuccess { result -> approvedCode = result.code }
                                refreshKey++
                            }
                        },
                        onReject = {
                            scope.launch {
                                runCatching { adminRepository.rejectInviteRequest(request.id) }
                                refreshKey++
                            }
                        },
                    )
                }
            }
        }
    }

    if (approvedCode != null) {
        val code = approvedCode!!
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { approvedCode = null },
            title = { Text("Request approved") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                    Text("Copy this code — it will not be shown again.", style = FamilyTypography.BodySmall)
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = code,
                            style = FamilyTypography.Body,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(Tokens.Space.S4),
                        )
                    }
                    Button(onClick = { clipboard.setText(AnnotatedString(code)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Copy code")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { approvedCode = null }) { Text("Done") }
            },
        )
    }
}

@Composable
private fun AdminInviteRequestCard(
    request: AdminInviteRequestDto,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    var inFlight by remember { mutableStateOf(false) }
    val isPending = request.status == "pending"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Tokens.Space.S4),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text(request.email),
                    style = FamilyTypography.BodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TagPill(label = request.status)
            }
            if (request.message != null) {
                Text(
                    text = text(request.message),
                    style = FamilyTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            if (isPending) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                    Button(
                        onClick = { inFlight = true; onApprove() },
                        enabled = !inFlight,
                    ) { Text("Approve", softWrap = false, maxLines = 1) }
                    OutlinedButton(
                        onClick = { inFlight = true; onReject() },
                        enabled = !inFlight,
                    ) { Text("Reject", softWrap = false, maxLines = 1) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Comments
// ---------------------------------------------------------------------------

@Composable
private fun AdminCommentCard(
    comment: AdminCommentDto,
    onApprove: () -> Unit,
    onRemove: () -> Unit,
) {
    val decodedName = text(comment.authorDisplayName).ifEmpty { "Anonymous" }
    val initial = decodedName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    val decodedBody = text(comment.body)
    val decodedEventTitle = text(comment.eventTitle).ifEmpty { "(unknown event)" }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Tokens.Space.S4),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { contentDescription = "Avatar for $decodedName" },
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = initial,
                            style = FamilyTypography.BodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.width(Tokens.Space.S2))
                Column {
                    Text(decodedName, style = FamilyTypography.BodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        decodedEventTitle,
                        style = FamilyTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            Text(decodedBody, style = FamilyTypography.Body)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            ) {
                Button(
                    onClick = onApprove,
                    enabled = !comment.isApproved,
                ) {
                    Text("Approve", softWrap = false, maxLines = 1)
                }
                OutlinedButton(onClick = onRemove) {
                    Text("Remove", softWrap = false, maxLines = 1)
                }
            }
        }
    }
}
