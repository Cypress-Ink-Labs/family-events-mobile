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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familyevents.core.decodeHtmlEntities
import com.familyevents.data.AdminCommentDto
import com.familyevents.data.AdminRepository
import com.familyevents.data.AdminStatsDto
import com.familyevents.designsystem.ErrorState
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.LoadingState
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
