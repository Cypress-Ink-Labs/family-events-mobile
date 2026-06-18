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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.familyevents.designsystem.FavoriteButton
import com.familyevents.designsystem.InfoGrid
import com.familyevents.designsystem.InfoGridItem
import com.familyevents.designsystem.TagPill
import com.familyevents.designsystem.generated.Tokens
import java.util.Locale
import kotlinx.coroutines.launch

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
    onAddToCalendar: (String, Long, Long?, String?, String?) -> Unit,
) {
    val event by eventRepository.observeEventDetail(eventId).collectAsStateWithLifecycle(initialValue = null)
    val favoriteIds by userId?.let {
        favoriteRepository.observeFavoriteIds(it).collectAsStateWithLifecycle(initialValue = emptySet())
    } ?: remember { mutableStateOf(emptySet<EventId>()) }
    val isFavorited = eventId in favoriteIds
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var userRating by rememberSaveable(eventId.rawValue, userId?.rawValue) { mutableIntStateOf(0) }
    var comments by remember(eventId.rawValue) { mutableStateOf(emptyList<CommentDto>()) }
    var draftComment by rememberSaveable(eventId.rawValue) { mutableStateOf("") }
    var feedback by remember(eventId.rawValue) { mutableStateOf<String?>(null) }
    var attendees by rememberSaveable(eventId.rawValue) { mutableIntStateOf(1) }
    var ratingInFlight by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(eventId.rawValue) {
        runCatching { eventRepository.refreshEventDetail(eventId) }
    }

    LaunchedEffect(eventId.rawValue, userId?.rawValue) {
        if (userId != null) {
            userRating = runCatching { ratingRepository?.userRating(userId, eventId)?.score ?: 0 }.getOrDefault(0)
        }
        commentRepository?.observeComments(eventId)?.collect { polled ->
            comments = polled
        } ?: run { comments = emptyList() }
    }

    if (event == null) {
        EmptyState("Event not found")
        return
    }

    val current = event!!

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
                    value = "%.1f (%d)".format(Locale.US, current.avgRating, current.ratingCount),
                    icon = "⭐",
                ))
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                runCatching { eventRepository.refreshEventDetail(eventId) }
                comments = runCatching { commentRepository?.comments(eventId).orEmpty() }.getOrDefault(comments)
                if (userId != null) {
                    userRating = runCatching { ratingRepository?.userRating(userId, eventId)?.score ?: 0 }.getOrDefault(userRating)
                }
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
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
                if (userId != null) {
                    FavoriteButton(
                        isFavorited = isFavorited,
                        onToggle = {
                            scope.launch {
                                runCatching {
                                    if (isFavorited) favoriteRepository.unfavorite(userId, eventId)
                                    else favoriteRepository.favorite(userId, eventId)
                                }
                            }
                        },
                    )
                }
            }
            EventHeroImage(title = text(current.title), imageUrl = current.imageUrl)

            if (current.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                    current.tags.forEach { TagPill(it.label) }
                }
            }

            current.description?.let { Text(text(it), style = FamilyTypography.Body) }

            // Location section
            val venueName = text(current.venueName)
            val address = text(current.address)
            if (venueName.isNotEmpty() || address.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                    Text("Location", style = FamilyTypography.TitleMedium)
                    if (venueName.isNotEmpty()) {
                        Text(venueName, style = FamilyTypography.Body)
                    }
                    if (address.isNotEmpty()) {
                        Text(address, style = FamilyTypography.BodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                    }
                }
            }

            // Source link
            current.sourceUrl?.takeIf { it.isNotBlank() }?.let { url ->
                TextButton(onClick = { runCatching { uriHandler.openUri(url) } }) {
                    Text("View original source")
                }
            }

            InfoGrid(items = infoItems)

            AttendeeStepper(
                value = attendees,
                onValueChange = { attendees = it },
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            ) {
                Button(onClick = { onShare(eventId) }) {
                    Text("Share", softWrap = false, maxLines = 1)
                }
                Button(onClick = {
                    onDirections(text(current.address).ifEmpty { text(current.venueName).ifEmpty { text(current.title) } })
                }) {
                    Text("Directions", softWrap = false, maxLines = 1)
                }
                Button(onClick = {
                    val calendarLocation = listOf(text(current.venueName), text(current.address))
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                        .ifBlank { null }
                    val calendarNotes = listOfNotNull(
                        text(current.description).ifBlank { null },
                        current.sourceUrl?.takeIf { it.isNotBlank() }?.let { "Source: $it" },
                    ).joinToString("\n\n").ifBlank { null }
                    onAddToCalendar(
                        text(current.title),
                        current.startsAt.toEpochMilli(),
                        current.endsAt?.toEpochMilli(),
                        calendarLocation,
                        calendarNotes,
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
}
