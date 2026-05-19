package com.familyevents.admin

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familyevents.core.CityId
import com.familyevents.core.decodeHtmlEntities
import com.familyevents.core.DateFormatting
import com.familyevents.core.UserId
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.ErrorState
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.LoadingState
import com.familyevents.designsystem.TagPill
import com.familyevents.designsystem.generated.Tokens
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun text(s: String?): String = decodeHtmlEntities(s ?: "")

@Composable
fun AdminScreen(
    adminRepository: AdminRepository,
    currentUserId: UserId? = null,
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
        AdminCronsSection(adminRepository)
        AdminCitiesSection(adminRepository)
        AdminRatingsSection(adminRepository)
        AdminAccessSection(currentUserId = currentUserId, adminRepository = adminRepository)
        AdminLogsSection(adminRepository = adminRepository)
    }
}

private data class WeekdayBar(
    val label: String,
    val dayOfWeek: DayOfWeek,
    val imported: Int,
    val skipped: Int,
    val errors: Int,
)

@Composable
private fun AdminDashboardSection(adminRepository: AdminRepository) {
    var stats by remember { mutableStateOf<AdminStatsDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var recentRuns by remember { mutableStateOf<List<AdminSourceRunDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { adminRepository.stats() }
            .onSuccess { stats = it }
            .onFailure { error = it.message ?: "Failed to load stats" }
        runCatching { adminRepository.listSourceRuns(limit = 50) }
            .onSuccess { recentRuns = it }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("Dashboard", style = FamilyTypography.TitleMedium)
        when {
            error != null -> ErrorState(message = error!!)
            stats == null -> LoadingState("Loading stats")
            else -> {
                val s = stats!!
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4)) {
                    // 4-tile stat grid
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

                    // AI confidence buckets
                    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                        Text("AI confidence", style = FamilyTypography.BodySmall, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    "High ${s.aiHigh}",
                                    style = FamilyTypography.Caption,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = Tokens.Space.S2, vertical = Tokens.Space.S1),
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    "Medium ${s.aiMedium}",
                                    style = FamilyTypography.Caption,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = Tokens.Space.S2, vertical = Tokens.Space.S1),
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    "Low ${s.aiLow}",
                                    style = FamilyTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = Tokens.Space.S2, vertical = Tokens.Space.S1),
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    "Untagged ${s.aiUntagged}",
                                    style = FamilyTypography.Caption,
                                    color = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.padding(horizontal = Tokens.Space.S2, vertical = Tokens.Space.S1),
                                )
                            }
                        }
                    }

                    // Ingestion chart (last 50 runs by weekday)
                    if (recentRuns.isNotEmpty()) {
                        IngestionChart(recentRuns)
                    }

                    // Recent runs panel
                    RecentRunsPanel(recentRuns)
                }
            }
        }
    }
}

@Composable
private fun IngestionChart(runs: List<AdminSourceRunDto>) {
    val zone = ZoneId.systemDefault()
    val orderedDays = listOf(
        DayOfWeek.MONDAY to "M",
        DayOfWeek.TUESDAY to "T",
        DayOfWeek.WEDNESDAY to "W",
        DayOfWeek.THURSDAY to "T",
        DayOfWeek.FRIDAY to "F",
        DayOfWeek.SATURDAY to "S",
        DayOfWeek.SUNDAY to "S",
    )
    val byDay: Map<DayOfWeek, List<AdminSourceRunDto>> = runs.groupBy { run ->
        LocalDate.ofInstant(run.startedAt, zone).dayOfWeek
    }
    val days = orderedDays.map { (dow, label) ->
        val dayRuns = byDay[dow] ?: emptyList()
        WeekdayBar(
            label = label,
            dayOfWeek = dow,
            imported = dayRuns.sumOf { it.eventsImported },
            skipped = dayRuns.sumOf { it.eventsSkipped },
            errors = dayRuns.count { it.status == "error" },
        )
    }
    val maxValue = days.maxOfOrNull { it.imported + it.skipped + it.errors }?.coerceAtLeast(1) ?: 1
    val chartHeight = 100.dp

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
        Text("Ingestion (last 50 runs)", style = FamilyTypography.BodySmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .height(chartHeight + 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val errH = (100f * day.errors / maxValue).dp
                    val skipH = (100f * day.skipped / maxValue).dp
                    val impH = (100f * day.imported / maxValue).dp
                    if (day.errors > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(errH)
                                .background(MaterialTheme.colorScheme.error),
                        )
                    }
                    if (day.skipped > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(skipH)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                    if (day.imported > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(impH)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Text(
                        day.label,
                        style = FamilyTypography.Caption,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
            listOf(
                MaterialTheme.colorScheme.primary to "Imported",
                MaterialTheme.colorScheme.surfaceVariant to "Skipped",
                MaterialTheme.colorScheme.error to "Errors",
            ).forEach { (color, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S1),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(color, CircleShape),
                    )
                    Text(label, style = FamilyTypography.Caption)
                }
            }
        }
    }
}

@Composable
private fun RecentRunsPanel(runs: List<AdminSourceRunDto>) {
    val latest = runs.take(4)
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
        Text("Recent runs", style = FamilyTypography.BodySmall, fontWeight = FontWeight.Bold)
        if (latest.isEmpty()) {
            Text(
                "No recent runs.",
                style = FamilyTypography.Caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            latest.forEach { run ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(Tokens.Space.S3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TagPill(label = run.status)
                        Spacer(Modifier.width(Tokens.Space.S2))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = decodeHtmlEntities(run.sourceName ?: "(unknown)"),
                                style = FamilyTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${run.eventsImported} imported · ${run.eventsSkipped} skipped · ${formatInstant(run.startedAt)}",
                                style = FamilyTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
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
                codes.forEach { code ->
                    AdminInviteCodeCard(
                        code = code,
                        onRevoke = {
                            scope.launch {
                                runCatching { adminRepository.revokeInvite(code.id) }
                                refreshKey++
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Tokens.Space.S4),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            Text(
                text = code.id.take(8) + "…",
                style = FamilyTypography.BodySmall,
                fontWeight = FontWeight.Bold,
            )
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
            OutlinedButton(onClick = onRevoke) {
                Text("Revoke", softWrap = false, maxLines = 1)
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
                            runCatching { adminRepository.approveInviteRequest(request.id) }
                                .onSuccess { result -> approvedCode = result.code }
                            refreshKey++
                        },
                        onReject = {
                            runCatching { adminRepository.rejectInviteRequest(request.id) }
                            refreshKey++
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
    onApprove: suspend () -> Unit,
    onReject: suspend () -> Unit,
) {
    var inFlight by remember { mutableStateOf(false) }
    val isPending = request.status == "pending"
    val cardScope = rememberCoroutineScope()

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
                        onClick = {
                            inFlight = true
                            cardScope.launch {
                                try {
                                    onApprove()
                                } finally {
                                    inFlight = false
                                }
                            }
                        },
                        enabled = !inFlight,
                    ) { Text("Approve", softWrap = false, maxLines = 1) }
                    OutlinedButton(
                        onClick = {
                            inFlight = true
                            cardScope.launch {
                                try {
                                    onReject()
                                } finally {
                                    inFlight = false
                                }
                            }
                        },
                        enabled = !inFlight,
                    ) { Text("Reject", softWrap = false, maxLines = 1) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Crons
// ---------------------------------------------------------------------------

private fun formatDuration(ms: Double): String =
    if (ms >= 1000.0) "${"%.1f".format(ms / 1000.0)}s" else "${ms.toLong()}ms"

private fun formatInstant(instant: java.time.Instant?): String =
    if (instant == null) "—" else DateFormatting.cardSubtitle(instant)

@Composable
private fun AdminCronsSection(adminRepository: AdminRepository) {
    var jobs by remember { mutableStateOf<List<AdminCronJobDto>>(emptyList()) }
    var history by remember { mutableStateOf<List<AdminCronRunDto>>(emptyList()) }
    var historyFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var inFlight by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    var editingJob by remember { mutableStateOf<AdminCronJobDto?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        try {
            loading = true
            jobs = adminRepository.listCronJobs()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            feedback = "Failed to load jobs: ${e.message ?: "unknown"}"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(historyFilter, refreshKey) {
        try {
            history = adminRepository.cronRunHistory(historyFilter, limit = 50)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            feedback = "Failed to load history: ${e.message ?: "unknown"}"
        }
    }

    fun runDueScrapes() {
        scope.launch {
            inFlight = true
            try {
                adminRepository.runDueScrapes()
                refreshKey++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                feedback = "Failed to run scrapes: ${e.message ?: "unknown"}"
            } finally {
                inFlight = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("Crons", style = FamilyTypography.TitleMedium)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
            Button(onClick = { runDueScrapes() }, enabled = !inFlight) {
                Text("Run All Due Now", softWrap = false, maxLines = 1)
            }
        }

        feedback?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error, style = FamilyTypography.BodySmall)
        }

        when {
            loading && jobs.isEmpty() -> LoadingState("Loading cron jobs")
            !loading && jobs.isEmpty() -> EmptyState("No cron jobs configured.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                jobs.forEach { job ->
                    CronJobCard(
                        job = job,
                        onToggleActive = { newActive ->
                            scope.launch {
                                try {
                                    adminRepository.toggleCronJob(job.jobname, newActive)
                                    refreshKey++
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    feedback = "Failed to toggle job: ${e.message ?: "unknown"}"
                                }
                            }
                        },
                        onEditSchedule = { editingJob = job },
                    )
                }
            }
        }

        Text("Run history", style = FamilyTypography.BodySmall, fontWeight = FontWeight.Bold)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
            FilterChip(
                selected = historyFilter == null,
                onClick = { historyFilter = null },
                label = { Text("All") },
            )
            jobs.map { it.jobname }.distinct().forEach { name ->
                FilterChip(
                    selected = historyFilter == name,
                    onClick = { historyFilter = name },
                    label = { Text(name) },
                )
            }
        }

        if (history.isEmpty()) {
            Text(
                "No run history.",
                style = FamilyTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                history.forEach { run ->
                    val statusColor = when (run.status.lowercase()) {
                        "succeeded", "success" -> MaterialTheme.colorScheme.primary
                        "failed", "failure" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(Tokens.Space.S3),
                            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S1),
                        ) {
                            Text(
                                text = "${run.jobname} · " +
                                    run.status +
                                    (run.durationMs?.let { " · ${formatDuration(it)}" } ?: ""),
                                style = FamilyTypography.BodySmall,
                                color = statusColor,
                            )
                            Text(
                                text = formatInstant(run.startTime),
                                style = FamilyTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            val runMsg = run.returnMessage
                            if (!runMsg.isNullOrBlank()) {
                                Text(
                                    text = runMsg,
                                    style = FamilyTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingJob != null) {
        CronScheduleDialog(
            job = editingJob!!,
            adminRepository = adminRepository,
            onDismiss = { editingJob = null },
            onSaved = { editingJob = null; refreshKey++ },
        )
    }
}

@Composable
private fun CronJobCard(
    job: AdminCronJobDto,
    onToggleActive: (Boolean) -> Unit,
    onEditSchedule: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Tokens.Space.S4),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = job.jobname,
                    style = FamilyTypography.BodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TagPill(label = if (job.active) "active" else "paused")
            }
            Text(
                text = "schedule: ${job.schedule}",
                style = FamilyTypography.Caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                text = "last: ${job.lastRunStatus ?: "—"} at ${formatInstant(job.lastRunStart)}",
                style = FamilyTypography.Caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            val jobMsg = job.lastRunMessage
            if (!jobMsg.isNullOrBlank()) {
                Text(
                    text = jobMsg,
                    style = FamilyTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                ) {
                    Text("Active", style = FamilyTypography.BodySmall)
                    Switch(checked = job.active, onCheckedChange = onToggleActive)
                }
                OutlinedButton(onClick = onEditSchedule) {
                    Text("Edit schedule")
                }
            }
        }
    }
}

@Composable
private fun CronScheduleDialog(
    job: AdminCronJobDto,
    adminRepository: AdminRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var schedule by rememberSaveable { mutableStateOf(job.schedule) }
    var inFlight by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val quickPicks = listOf(
        "0 * * * *" to "hourly",
        "0 3 * * *" to "3am daily",
        "*/15 * * * *" to "every 15 min",
        "0 0 * * 0" to "Sunday midnight",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit schedule for ${job.jobname}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                errorMsg?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = FamilyTypography.BodySmall)
                }
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("Cron expression") },
                    supportingText = { Text("5-field cron, e.g. 0 3 * * *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                ) {
                    quickPicks.forEach { (expr, label) ->
                        FilterChip(
                            selected = schedule == expr,
                            onClick = { schedule = expr },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        inFlight = true
                        errorMsg = null
                        try {
                            adminRepository.setCronSchedule(job.jobname, schedule.trim())
                            onSaved()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            errorMsg = e.message ?: "Failed to update schedule"
                        } finally {
                            inFlight = false
                        }
                    }
                },
                enabled = !inFlight && schedule.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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

// ---------------------------------------------------------------------------
// Cities
// ---------------------------------------------------------------------------

@Composable
private fun AdminCitiesSection(adminRepository: AdminRepository) {
    var cities by remember { mutableStateOf<List<AdminCityDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showInactive by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        runCatching { adminRepository.listCities() }
            .onSuccess { cities = it; loading = false }
            .onFailure { feedback = it.message ?: "Failed to load cities"; loading = false }
    }

    fun toggleActive(cityId: CityId, newActive: Boolean) {
        scope.launch {
            val patch = """{"is_active":$newActive}"""
            runCatching { adminRepository.updateCity(cityId, patch) }
                .onFailure { feedback = it.message ?: "Failed to update city" }
            refreshKey++
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("Cities", style = FamilyTypography.TitleMedium)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            Text(
                text = "${cities.size} cities · ${cities.count { it.isActive }} active",
                style = FamilyTypography.Caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Button(onClick = { showAddDialog = true }) { Text("Add city") }
        }

        FilterChip(
            selected = showInactive,
            onClick = { showInactive = !showInactive },
            label = { Text("Show inactive") },
        )

        feedback?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error, style = FamilyTypography.BodySmall)
        }

        val displayed = if (showInactive) cities else cities.filter { it.isActive }

        when {
            loading && cities.isEmpty() -> LoadingState("Loading cities")
            !loading && displayed.isEmpty() -> EmptyState("No cities.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                displayed.forEach { city ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(Tokens.Space.S4),
                            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                        ) {
                            Text(
                                text = "${city.name}${if (city.state != null) ", ${city.state}" else ""}",
                                style = FamilyTypography.TitleMedium,
                            )
                            Text(
                                text = "${city.slug} · ${city.timezone} · ${city.country}",
                                style = FamilyTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                            ) {
                                Text("Active", style = FamilyTypography.BodySmall, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = city.isActive,
                                    onCheckedChange = { newActive -> toggleActive(city.id, newActive) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AdminAddCityDialog(
            adminRepository = adminRepository,
            onDismiss = { showAddDialog = false },
            onCreated = { showAddDialog = false; refreshKey++ },
        )
    }
}

@Composable
private fun AdminAddCityDialog(
    adminRepository: AdminRepository,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("US") }
    var slug by rememberSaveable { mutableStateOf("") }
    var timezone by rememberSaveable { mutableStateOf("America/Chicago") }
    var inFlight by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add city") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                errorMsg?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = FamilyTypography.BodySmall)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (required)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State (optional, 2-letter)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it },
                    label = { Text("Slug (required, no-spaces)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = { Text("Timezone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        inFlight = true
                        errorMsg = null
                        runCatching {
                            adminRepository.createCity(
                                name = name.trim(),
                                state = state.trim().ifEmpty { null },
                                country = country.trim().ifEmpty { "US" },
                                slug = slug.trim(),
                                timezone = timezone.trim().ifEmpty { "America/Chicago" },
                            )
                        }
                            .onSuccess { onCreated() }
                            .onFailure { errorMsg = it.message ?: "Failed to create city" }
                        inFlight = false
                    }
                },
                enabled = !inFlight && name.isNotBlank() && slug.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ---------------------------------------------------------------------------
// Ratings
// ---------------------------------------------------------------------------

@Composable
private fun AdminRatingsSection(adminRepository: AdminRepository) {
    var ratings by remember { mutableStateOf<List<AdminRatingDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        runCatching { adminRepository.listRatings() }
            .onSuccess { ratings = it; loading = false }
            .onFailure { feedback = it.message ?: "Failed to load ratings"; loading = false }
    }

    val avgScore = if (ratings.isEmpty()) 0.0 else ratings.sumOf { it.score } / ratings.size.toDouble()

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("Ratings", style = FamilyTypography.TitleMedium)

        Text(
            text = "${ratings.size} ratings · avg ${"%.1f".format(avgScore)}",
            style = FamilyTypography.Caption,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        feedback?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error, style = FamilyTypography.BodySmall)
        }

        when {
            loading && ratings.isEmpty() -> LoadingState("Loading ratings")
            !loading && ratings.isEmpty() -> EmptyState("No ratings yet.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                ratings.forEach { rating ->
                    AdminRatingCard(
                        rating = rating,
                        onDelete = {
                            scope.launch {
                                runCatching { adminRepository.deleteRating(rating.id) }
                                    .onFailure { feedback = it.message ?: "Failed to delete rating" }
                                refreshKey++
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminRatingCard(
    rating: AdminRatingDto,
    onDelete: () -> Unit,
) {
    val decodedName = decodeHtmlEntities(rating.authorDisplayName ?: "Anonymous")
    val decodedEvent = decodeHtmlEntities(rating.eventTitle ?: "(unknown event)")
    val initial = decodedName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    val stars = "★".repeat(rating.score) + "☆".repeat((5 - rating.score).coerceAtLeast(0))
    var showDeleteDialog by remember { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Tokens.Space.S4),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = initial,
                            style = FamilyTypography.BodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(decodedName, style = FamilyTypography.BodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        decodedEvent,
                        style = FamilyTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text = stars,
                        style = FamilyTypography.BodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatInstant(rating.createdAt),
                        style = FamilyTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Delete", softWrap = false, maxLines = 1)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete rating?") },
            text = { Text("This will permanently remove the rating. Continue?") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Access
// ---------------------------------------------------------------------------

@Composable
private fun AdminAccessSection(
    currentUserId: UserId?,
    adminRepository: AdminRepository,
) {
    var users by remember { mutableStateOf<List<AdminUserAccessDto>>(emptyList()) }
    var keyword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var disableTarget by remember { mutableStateOf<AdminUserAccessDto?>(null) }
    var disableReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        runCatching { adminRepository.listUserAccess() }
            .onSuccess { users = it; loading = false }
            .onFailure { feedback = it.message ?: "Failed to load users"; loading = false }
    }

    val filtered = users.filter {
        val q = keyword.trim().lowercase()
        q.isEmpty() ||
            (it.displayName?.lowercase()?.contains(q) == true) ||
            (it.email?.lowercase()?.contains(q) == true) ||
            it.role.lowercase().contains(q)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("User access", style = FamilyTypography.TitleMedium)

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text("Search by name, email, or role") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        feedback?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error, style = FamilyTypography.BodySmall)
        }

        when {
            loading && users.isEmpty() -> LoadingState("Loading users")
            !loading && filtered.isEmpty() -> EmptyState("No users found.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                filtered.forEach { user ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(Tokens.Space.S4),
                            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                            ) {
                                Text(
                                    text = decodeHtmlEntities(user.displayName ?: "Unknown"),
                                    style = FamilyTypography.BodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                val roleColor = if (user.role.lowercase() == "admin")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                TagPill(label = user.role)
                            }
                            Text(
                                text = user.email ?: "—",
                                style = FamilyTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                            ) {
                                TagPill(label = if (user.isEnabled) "enabled" else "disabled")
                                val reason = user.disabledReason
                                if (!user.isEnabled && reason != null) {
                                    Text(
                                        text = decodeHtmlEntities(reason),
                                        style = FamilyTypography.Caption,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 2,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            if (user.isEnabled) {
                                val isSelf = user.userId == currentUserId
                                OutlinedButton(
                                    onClick = { if (!isSelf) { disableTarget = user; disableReason = "" } },
                                    enabled = !isSelf,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text(
                                        if (isSelf) "Cannot disable self" else "Disable",
                                        softWrap = false,
                                        maxLines = 1,
                                    )
                                }
                            } else {
                                Button(onClick = {
                                    scope.launch {
                                        runCatching { adminRepository.updateUserAccess(user.userId, true, null) }
                                            .onFailure { feedback = it.message ?: "Failed to enable user" }
                                        refreshKey++
                                    }
                                }) {
                                    Text("Enable", softWrap = false, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val target = disableTarget
    if (target != null) {
        var inFlight by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { disableTarget = null },
            title = { Text("Disable ${decodeHtmlEntities(target.displayName ?: "Unknown")}?") },
            text = {
                OutlinedTextField(
                    value = disableReason,
                    onValueChange = { disableReason = it },
                    label = { Text("Reason") },
                    supportingText = { Text("Why is this account being disabled?") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            inFlight = true
                            runCatching { adminRepository.updateUserAccess(target.userId, false, disableReason.trim()) }
                                .onSuccess {
                                    disableTarget = null
                                    refreshKey++
                                }
                                .onFailure { feedback = it.message ?: "Failed to disable user" }
                            inFlight = false
                        }
                    },
                    enabled = !inFlight && disableReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = { disableTarget = null }) { Text("Cancel") }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Logs
// ---------------------------------------------------------------------------

@Composable
private fun AdminLogsSection(adminRepository: AdminRepository) {
    var runs by remember { mutableStateOf<List<AdminSourceRunDto>>(emptyList()) }
    var tagQueue by remember { mutableStateOf<List<AdminTagQueueSummaryRowDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var expandedRunId by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        loading = true
        runCatching {
            runs = adminRepository.listSourceRuns(50)
            tagQueue = adminRepository.listTagQueueSummary()
            loading = false
        }.onFailure { feedback = it.message ?: "Failed to load logs"; loading = false }

        while (isActive && runs.any { it.status == "running" }) {
            delay(3000)
            val pollResult = runCatching {
                runs = adminRepository.listSourceRuns(50)
                tagQueue = adminRepository.listTagQueueSummary()
            }
            if (pollResult.isFailure) {
                feedback = pollResult.exceptionOrNull()?.message ?: "Failed to refresh running jobs"
                break
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        Text("Logs", style = FamilyTypography.TitleMedium)

        // Tag queue panel
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Tokens.Space.S4),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            ) {
                Text("Tag queue", style = FamilyTypography.BodySmall, fontWeight = FontWeight.Bold)
                if (tagQueue.isEmpty()) {
                    Text(
                        "Queue is idle.",
                        style = FamilyTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                        tagQueue.forEach { row ->
                            TagPill(label = "${row.status}: ${row.rowCount}")
                        }
                    }
                    val oldest = tagQueue.mapNotNull { it.oldestEnqueuedAt }.minOrNull()
                    oldest?.let {
                        Text(
                            "Oldest pending: ${formatInstant(it)}",
                            style = FamilyTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    val lastDead = tagQueue.mapNotNull { it.lastDeadLetterAt }.maxOrNull()
                    lastDead?.let {
                        Text(
                            "Last dead-letter: ${formatInstant(it)}",
                            style = FamilyTypography.Caption,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // Source runs panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Source runs",
                style = FamilyTypography.BodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { refreshKey++ }) { Text("Refresh") }
        }

        feedback?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error, style = FamilyTypography.BodySmall)
        }

        when {
            loading && runs.isEmpty() -> LoadingState("Loading source runs")
            !loading && runs.isEmpty() -> EmptyState("No source runs.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                runs.forEach { run ->
                    val isExpanded = expandedRunId == run.id
                    val now = Instant.now()
                    val elapsedMs = Duration.between(run.startedAt, now).toMillis()
                    val isStale = run.status == "running" && Duration.between(run.startedAt, now).toMinutes() > 15

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedRunId = if (isExpanded) null else run.id },
                    ) {
                        Column(
                            modifier = Modifier.padding(Tokens.Space.S3),
                            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S1),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                            ) {
                                Text(
                                    text = decodeHtmlEntities(run.sourceName ?: "(unknown)"),
                                    style = FamilyTypography.BodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                val statusColor = when (run.status.lowercase()) {
                                    "success", "completed" -> MaterialTheme.colorScheme.primary
                                    "error", "failed" -> MaterialTheme.colorScheme.error
                                    "running" -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                                TagPill(label = run.status)
                            }
                            val caption = buildString {
                                append("started ${formatInstant(run.startedAt)}")
                                if (run.completedAt != null) {
                                    val ms = Duration.between(run.startedAt, run.completedAt).toMillis()
                                    append(" · ${formatDuration(ms.toDouble())}")
                                } else if (run.status == "running") {
                                    append(" · ${formatDuration(elapsedMs.toDouble())} elapsed")
                                }
                            }
                            Text(
                                text = caption,
                                style = FamilyTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Text(
                                text = "${run.eventsImported} imported · ${run.eventsSkipped} skipped" +
                                    if (run.eventsFound > 0) " (of ${run.eventsFound})" else "",
                                style = FamilyTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            if (isStale) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TagPill(label = "timed out?")
                                    Text(
                                        "Has been running over 15 minutes.",
                                        style = FamilyTypography.Caption,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            val errorLog = run.errorLog
                            if (isExpanded && errorLog != null) {
                                Text(
                                    text = decodeHtmlEntities(errorLog),
                                    style = FamilyTypography.Caption,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                        .padding(Tokens.Space.S2),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
