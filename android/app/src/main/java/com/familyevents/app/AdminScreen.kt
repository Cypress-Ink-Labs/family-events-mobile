package com.familyevents.app

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.familyevents.core.decodeHtmlEntities
import com.familyevents.data.AdminCommentDto
import com.familyevents.data.AdminInviteCodeListDto
import com.familyevents.data.AdminInviteRequestDto
import com.familyevents.data.AdminRepository
import com.familyevents.data.AdminSourceDto
import com.familyevents.data.AdminStatsDto
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
