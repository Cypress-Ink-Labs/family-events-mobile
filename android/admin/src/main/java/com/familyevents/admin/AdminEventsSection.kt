package com.familyevents.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.familyevents.core.CityId
import com.familyevents.core.DateFormatting
import com.familyevents.core.EventId
import com.familyevents.core.decodeHtmlEntities
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.ErrorState
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.LoadingState
import com.familyevents.designsystem.TagPill
import com.familyevents.designsystem.generated.Tokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

private val eventStatuses = listOf("draft", "published", "rejected", "archived")

@Composable
internal fun AdminEventsSection(adminRepository: AdminRepository) {
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
            events = emptyList()
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

        EventStatusFilterChips(
            status = status,
            statusCounts = facets?.statusCounts.orEmpty(),
            onStatusChange = { nextStatus ->
                status = nextStatus
                selected = emptySet()
            },
        )

        OutlinedTextField(
            value = keyword,
            onValueChange = {
                keyword = it
                selected = emptySet()
            },
            label = { Text("Search title or description") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        CityFilterChips(
            cityId = cityId,
            cityCounts = facets?.cityCounts.orEmpty(),
            onCityChange = {
                cityId = it
                selected = emptySet()
            },
        )

        EventBulkActionBar(
            selected = selected,
            bulkInFlight = bulkInFlight,
            onBulkUpdate = { target ->
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
            onShowDelete = { showDeleteConfirm = true },
            onClearSelection = { selected = emptySet() },
        )

        feedback?.let {
            Text(it, style = FamilyTypography.BodySmall, color = MaterialTheme.colorScheme.error)
        }

        EventListContent(
            loading = loading,
            events = events,
            selected = selected,
            onToggleSelect = { event ->
                selected = if (event.id.rawValue in selected) {
                    selected - event.id.rawValue
                } else {
                    selected + event.id.rawValue
                }
            },
            onEdit = { editingEvent = it },
            onChangeStatus = { event, target ->
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
            onDelete = { event ->
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
private fun EventStatusFilterChips(
    status: String,
    statusCounts: Map<String, Int>,
    onStatusChange: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
        eventStatuses.forEach { s ->
            FilterChip(
                selected = status == s,
                onClick = { onStatusChange(s) },
                label = { Text("$s (${statusCounts[s] ?: 0})") },
            )
        }
    }
}

@Composable
private fun CityFilterChips(
    cityId: String?,
    cityCounts: Map<String, Int>,
    onCityChange: (String?) -> Unit,
) {
    if (cityCounts.isEmpty()) return

    FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
        FilterChip(
            selected = cityId == null,
            onClick = { onCityChange(null) },
            label = { Text("All cities (${cityCounts.values.sum()})") },
        )
        cityCounts.forEach { (cId, count) ->
            FilterChip(
                selected = cityId == cId,
                onClick = { onCityChange(cId) },
                label = { Text("$cId ($count)") },
            )
        }
    }
}

@Composable
private fun EventBulkActionBar(
    selected: Set<String>,
    bulkInFlight: Boolean,
    onBulkUpdate: (String) -> Unit,
    onShowDelete: () -> Unit,
    onClearSelection: () -> Unit,
) {
    if (selected.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
        Text("${selected.size} selected", style = FamilyTypography.BodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
            listOf("published" to "Publish", "rejected" to "Reject", "archived" to "Archive").forEach { (target, label) ->
                Button(
                    onClick = { onBulkUpdate(target) },
                    enabled = !bulkInFlight,
                ) { Text(label, softWrap = false, maxLines = 1) }
            }
            OutlinedButton(
                onClick = onShowDelete,
                enabled = !bulkInFlight,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error,
                ),
            ) { Text("Delete", softWrap = false, maxLines = 1) }
            TextButton(onClick = onClearSelection) { Text("Clear") }
        }
    }
}

@Composable
private fun EventListContent(
    loading: Boolean,
    events: List<AdminEventListItemDto>,
    selected: Set<String>,
    onToggleSelect: (AdminEventListItemDto) -> Unit,
    onEdit: (AdminEventListItemDto) -> Unit,
    onChangeStatus: (AdminEventListItemDto, String) -> Unit,
    onDelete: (AdminEventListItemDto) -> Unit,
) {
    when {
        loading && events.isEmpty() -> LoadingState("Loading events")
        !loading && events.isEmpty() -> EmptyState("No events for filter.")
        else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
            events.forEach { event ->
                AdminEventCard(
                    event = event,
                    isSelected = event.id.rawValue in selected,
                    onToggleSelect = { onToggleSelect(event) },
                    onEdit = { onEdit(event) },
                    onChangeStatus = { target -> onChangeStatus(event, target) },
                    onDelete = { onDelete(event) },
                )
            }
        }
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
                        text = decodeHtmlEntities(event.title),
                        style = FamilyTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    Text(
                        text = "${DateFormatting.cardSubtitle(event.startsAt)} • ${decodeHtmlEntities(event.venueName ?: "Venue TBA")}",
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
        if (price.isNotBlank() && parsedPrice == null) {
            feedback = "Price must be a number."
            return
        }
        if (parsedPrice != null && parsedPrice < 0) {
            feedback = "Price cannot be negative."
            return
        }
        val parsedAgeMin = ageMin.toIntOrNull()
        val parsedAgeMax = ageMax.toIntOrNull()
        if (ageMin.isNotBlank() && parsedAgeMin == null) {
            feedback = "Age min must be a whole number."
            return
        }
        if (ageMax.isNotBlank() && parsedAgeMax == null) {
            feedback = "Age max must be a whole number."
            return
        }
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
            if (isFree && event.price != null) {
                put("price", JsonNull)
            } else if (parsedPrice != event.price) {
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
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
                    Switch(
                        checked = isFree,
                        onCheckedChange = {
                            isFree = it
                            if (it) price = ""
                        },
                    )
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
                                try {
                                    val ok = adminRepository.unlockEventFields(event.id)
                                    feedback = if (ok) "Unlocked." else "Already unlocked."
                                } catch (e: Exception) {
                                    feedback = "Failed to unlock: ${e.message ?: "unknown error"}"
                                }
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

