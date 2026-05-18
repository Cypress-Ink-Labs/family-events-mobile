package com.familyevents.eventdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.core.decodeHtmlEntities
import com.familyevents.data.CommentDto
import com.familyevents.data.CommentRepository
import com.familyevents.data.EventRepository
import com.familyevents.data.FavoriteRepository
import com.familyevents.data.RatingRepository
import com.familyevents.designsystem.AttendeeStepper
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.EventHeroImage
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.InfoGrid
import com.familyevents.designsystem.InfoGridItem
import com.familyevents.designsystem.TagPill
import com.familyevents.designsystem.generated.Tokens
import kotlinx.coroutines.launch

/** Decode nullable string for display; empty string when null. */
private fun text(s: String?): String = decodeHtmlEntities(s ?: "")

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
    var attendees by rememberSaveable(eventId.rawValue) { mutableIntStateOf(1) }
    var ratingInFlight by remember { mutableStateOf(false) }

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

    // ── InfoGrid items ──────────────────────────────────────────────────────
    val infoItems = remember(current) {
        buildList {
        current.endsAt?.let { end ->
            val totalMinutes = ((end.toEpochMilli() - current.startsAt.toEpochMilli()) / 60_000).toInt()
            if (totalMinutes > 0) {
                val h = totalMinutes / 60
                val m = totalMinutes % 60
                val formatted = if (h > 0) "${h}h ${m}m" else "${m}m"
                add(InfoGridItem(label = "DURATION", value = formatted, icon = "⏱"))
            }
        }
        if (current.isFree) {
            add(InfoGridItem(label = "PRICE", value = "Free", icon = "💲"))
        } else {
            current.price?.let { add(InfoGridItem(label = "PRICE", value = "$$it", icon = "💲")) }
        }
        val agesValue = when {
            current.ageMin != null && current.ageMax != null -> "Ages ${current.ageMin}–${current.ageMax}"
            current.ageMin != null -> "Ages ${current.ageMin}+"
            else -> null
        }
        agesValue?.let { add(InfoGridItem(label = "AGES", value = it, icon = "👨‍👩‍👧")) }
        if (current.avgRating > 0) {
            add(InfoGridItem(
                label = "RATING",
                value = "%.1f (%d)".format(current.avgRating, current.ratingCount),
                icon = "⭐",
            ))
        }
    }
}

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
            Text(text(current.title), style = FamilyTypography.TitleLarge, modifier = Modifier.weight(1f))
        }
        EventHeroImage(title = text(current.title), imageUrl = current.imageUrl)
        Text(text(current.venueName).ifEmpty { "Location TBA" }, style = FamilyTypography.Body)

        // Tags — no "See details" fallback
        if (current.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                current.tags.forEach { TagPill(it.label) }
            }
        }

        current.description?.let { Text(text(it), style = FamilyTypography.Body) }

        // InfoGrid (Duration / Price / Ages / Rating)
        InfoGrid(items = infoItems)

        // AttendeeStepper
        AttendeeStepper(
            value = attendees,
            onValueChange = { attendees = it },
        )

        // Action row — FlowRow so button labels never wrap
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            if (userId != null) {
                Button(onClick = { scope.launch { favoriteRepository.favorite(userId, eventId) } }) {
                    Text("Save", softWrap = false, maxLines = 1)
                }
            }
            Button(onClick = { onShare(eventId) }) {
                Text("Share", softWrap = false, maxLines = 1)
            }
            Button(onClick = {
                onDirections(text(current.address).ifEmpty { text(current.venueName).ifEmpty { text(current.title) } })
            }) {
                Text("Directions", softWrap = false, maxLines = 1)
            }
            Button(onClick = {
                onAddToCalendar(
                    text(current.title),
                    current.startsAt.toEpochMilli(),
                    current.endsAt?.toEpochMilli(),
                )
            }) {
                Text("Calendar", softWrap = false, maxLines = 1)
            }
        }

        Text("Reviews", style = FamilyTypography.TitleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S1)) {
            (1..5).forEach { score ->
                TextButton(
                    enabled = !ratingInFlight,
                    onClick = {
                        if (userId == null || ratingRepository == null) {
                            feedback = "Sign in to rate events."
                            return@TextButton
                        }
                        val previousRating = userRating
                        userRating = score
                        ratingInFlight = true
                        scope.launch {
                            runCatching { ratingRepository.upsertRating(userId, eventId, score) }
                                .onSuccess { ratingInFlight = false }
                                .onFailure {
                                    ratingInFlight = false
                                    userRating = previousRating
                                    feedback = it.message ?: "Rating failed."
                                }
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
            val decodedName = text(comment.authorDisplayName).ifEmpty { "Family Events member" }
            val initial = decodedName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S1)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .semantics { contentDescription = "Avatar for $decodedName" },
                    ) {
                        Text(
                            text = initial,
                            style = FamilyTypography.BodySmall,
                            modifier = Modifier.fillMaxSize(),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.width(Tokens.Space.S2))
                    Column {
                        Text(decodedName, style = FamilyTypography.BodySmall)
                        Text(text(comment.body), style = FamilyTypography.Body)
                    }
                }
            }
        }
    }
}
