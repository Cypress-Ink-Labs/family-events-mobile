package com.familyevents.eventdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.CommentDto
import com.familyevents.data.CommentRepository
import com.familyevents.data.EventRepository
import com.familyevents.data.FavoriteRepository
import com.familyevents.data.RatingRepository
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.EventHeroImage
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.TagPill
import com.familyevents.designsystem.generated.Tokens
import kotlinx.coroutines.launch

@Composable
fun EventDetailScreen(
    eventId: EventId,
    userId: UserId?,
    eventRepository: EventRepository,
    favoriteRepository: FavoriteRepository,
    ratingRepository: RatingRepository? = null,
    commentRepository: CommentRepository? = null,
    onBack: () -> Unit,
    onShare: (EventId) -> Unit,
    onDirections: (String) -> Unit,
    onAddToCalendar: (String, Long, Long?) -> Unit,
) {
    val event by eventRepository.observeEventDetail(eventId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var userRating by rememberSaveable(eventId.rawValue, userId?.rawValue) { mutableIntStateOf(0) }
    var comments by remember(eventId.rawValue) { mutableStateOf(emptyList<CommentDto>()) }
    var draftComment by rememberSaveable(eventId.rawValue) { mutableStateOf("") }
    var feedback by remember(eventId.rawValue) { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    LaunchedEffect(eventId.rawValue) {
        eventRepository.refreshEventDetail(eventId)
    }

    LaunchedEffect(eventId.rawValue, userId?.rawValue) {
        comments = runCatching { commentRepository?.comments(eventId).orEmpty() }.getOrDefault(emptyList())
        if (userId != null) {
            userRating = runCatching { ratingRepository?.userRating(userId, eventId)?.score ?: 0 }.getOrDefault(0)
        }
    }

    if (event == null) {
        EmptyState("Event not found")
        return
    }

    val current = event!!
    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Tokens.Space.S4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(current.title, style = FamilyTypography.TitleLarge, modifier = Modifier.weight(1f))
        }
        EventHeroImage(title = current.title, imageUrl = current.imageUrl)
        Text(current.venueName ?: "Location TBA", style = FamilyTypography.Body)
        Text(
            listOfNotNull(
                if (current.isFree) "Free" else current.price?.let { "$$it" },
                current.ageMin?.let { low -> current.ageMax?.let { high -> "Ages $low-$high" } ?: "Ages $low+" },
                current.avgRating.takeIf { it > 0 }?.let { "${"%.1f".format(it)} (${current.ratingCount})" },
            ).ifEmpty { listOf("See details") }.joinToString(" • "),
            style = FamilyTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        current.description?.let { Text(it, style = FamilyTypography.Body) }
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
            current.tags.forEach { TagPill(it.label) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
            if (userId != null) {
                Button(onClick = { scope.launch { favoriteRepository.favorite(userId, eventId) } }) { Text("Save") }
            }
            Button(onClick = { onShare(eventId) }) { Text("Share") }
            Button(onClick = { onDirections(current.address ?: current.venueName ?: current.title) }) { Text("Directions") }
            Button(onClick = {
                onAddToCalendar(
                    current.title,
                    current.startsAt.toEpochMilli(),
                    current.endsAt?.toEpochMilli(),
                )
            }) { Text("Calendar") }
        }
        Text("Reviews", style = FamilyTypography.TitleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S1)) {
            (1..5).forEach { score ->
                TextButton(
                    onClick = {
                        if (userId == null || ratingRepository == null) {
                            feedback = "Sign in to rate events."
                            return@TextButton
                        }
                        userRating = score
                        scope.launch {
                            runCatching { ratingRepository.upsertRating(userId, eventId, score) }
                                .onFailure { feedback = it.message ?: "Rating failed." }
                        }
                    },
                ) {
                    Text(if (score <= userRating) "★" else "☆", style = FamilyTypography.TitleMedium)
                }
            }
        }
        OutlinedTextField(
            value = draftComment,
            onValueChange = { draftComment = it },
            label = { Text("Comment") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Button(
            enabled = draftComment.isNotBlank(),
            onClick = {
                val body = draftComment.trim()
                if (userId == null || commentRepository == null) {
                    feedback = "Sign in to comment."
                    return@Button
                }
                draftComment = ""
                scope.launch {
                    runCatching { commentRepository.addComment(userId, eventId, body) }
                        .onSuccess { comments = listOf(it) + comments }
                        .onFailure { feedback = it.message ?: "Comment failed." }
                }
            },
        ) { Text("Post comment") }
        feedback?.let { Text(it, style = FamilyTypography.BodySmall, color = MaterialTheme.colorScheme.error) }
        comments.forEach { comment ->
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S1)) {
                Text(comment.authorDisplayName ?: "Family Events member", style = FamilyTypography.BodySmall)
                Text(comment.body, style = FamilyTypography.Body)
            }
        }
    }
}
