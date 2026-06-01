package com.familyevents.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventQuery
import com.familyevents.data.EventRepository
import com.familyevents.data.FavoriteRepository
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.EventCard
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.generated.Tokens
import java.time.Instant
import kotlinx.coroutines.launch

private enum class SavedFilter(val label: String) {
    Upcoming("Upcoming"),
    Past("Past"),
    All("All");
}

@Composable
fun SavedScreen(
    userId: UserId,
    eventRepository: EventRepository,
    favoriteRepository: FavoriteRepository,
    onOpenEvent: (EventId) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val favorites by favoriteRepository.observeFavorites(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    val events by eventRepository.observeEventList(EventQuery(cityId = null)).collectAsStateWithLifecycle(initialValue = emptyList())
    val eventsById = events.associateBy { it.id }
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf(SavedFilter.Upcoming) }
    var isRefreshing by remember { mutableStateOf(false) }

    val now = remember { Instant.now() }
    val filtered = favorites.filter { fav ->
        val event = eventsById[fav.eventId]
        when (filter) {
            SavedFilter.All -> true
            SavedFilter.Upcoming -> event == null || !event.startsAt.isBefore(now)
            SavedFilter.Past -> event != null && event.startsAt.isBefore(now)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                runCatching { eventRepository.refreshEventList(EventQuery(cityId = null)) }
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4),
            modifier = Modifier
                .fillMaxSize()
                .padding(Tokens.Space.S4),
        ) {
            Text("Saved", style = FamilyTypography.TitleLarge)
            Button(onClick = onOpenProfile) { Text("Profile") }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            ) {
                SavedFilter.entries.forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item.label) },
                    )
                }
            }

            if (filtered.isEmpty()) {
                val message = when (filter) {
                    SavedFilter.Upcoming -> "No upcoming saved events."
                    SavedFilter.Past -> "No past saved events."
                    SavedFilter.All -> "Saved events will appear here."
                }
                EmptyState(message)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                    items(filtered, key = { it.eventId.rawValue }) { fav ->
                        val event = eventsById[fav.eventId]
                        EventCard(
                            title = event?.title ?: fav.eventId.rawValue,
                            subtitle = event?.venueName ?: "Saved event",
                            badge = null,
                            imageUrl = event?.imageUrl,
                            onClick = { onOpenEvent(fav.eventId) },
                        )
                    }
                }
            }
        }
    }
}
