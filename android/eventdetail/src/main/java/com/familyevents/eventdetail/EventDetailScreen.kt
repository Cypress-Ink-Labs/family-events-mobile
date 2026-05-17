package com.familyevents.eventdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventRepository
import com.familyevents.data.FavoriteRepository
import com.familyevents.designsystem.EmptyState
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
    onShare: (EventId) -> Unit,
    onDirections: (String) -> Unit,
    onAddToCalendar: (String, Long, Long?) -> Unit,
) {
    val event by eventRepository.observeEventDetail(eventId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    if (event == null) {
        EmptyState("Event not found")
        return
    }

    val current = event!!
    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4),
        modifier = Modifier
            .fillMaxSize()
            .padding(Tokens.Space.S4),
    ) {
        Text(current.title, style = FamilyTypography.TitleLarge)
        Text(current.venueName ?: "Location TBA", style = FamilyTypography.Body)
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
    }
}
