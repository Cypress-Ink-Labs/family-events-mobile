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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventQuery
import com.familyevents.data.EventRepository
import com.familyevents.data.FavoriteRepository
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.EventCard
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.generated.Tokens
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private enum class SavedMode { List, Calendar }

@Composable
fun SavedScreen(
    userId: UserId,
    eventRepository: EventRepository,
    favoriteRepository: FavoriteRepository,
    onOpenEvent: (EventId) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val favorites by favoriteRepository.observeFavorites(userId).collectAsState(initial = emptyList())
    val events by eventRepository.observeEventList(EventQuery(cityId = null)).collectAsState(initial = emptyList())
    val eventsById = events.associateBy { it.id }
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(SavedMode.List) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now(ZoneId.systemDefault())) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(LocalDate.now(ZoneId.systemDefault())) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4),
        modifier = Modifier
            .fillMaxSize()
            .padding(Tokens.Space.S4),
    ) {
        Text("Saved", style = FamilyTypography.TitleLarge)
        Button(onClick = onOpenProfile) { Text("Profile") }

        // List / Calendar toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
        ) {
            SavedMode.entries.forEach { item ->
                FilterChip(
                    selected = mode == item,
                    onClick = { mode = item },
                    label = { Text(item.name) },
                )
            }
        }

        if (favorites.isEmpty()) {
            EmptyState("Saved events will appear here.")
        } else {
            when (mode) {
                SavedMode.List -> LazyColumn(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                    items(favorites, key = { it.eventId.rawValue }) { fav ->
                        val event = eventsById[fav.eventId]
                        EventCard(
                            title = event?.title ?: fav.eventId.rawValue,
                            subtitle = event?.venueName ?: "Saved event",
                            badge = "Saved",
                            imageUrl = event?.imageUrl,
                            onClick = { onOpenEvent(fav.eventId) },
                        )
                        Button(onClick = { scope.launch { favoriteRepository.unfavorite(userId, fav.eventId) } }) {
                            Text("Unsave")
                        }
                    }
                }
                SavedMode.Calendar -> SavedCalendarView(
                    favorites = favorites,
                    eventsById = eventsById,
                    selectedMonth = selectedMonth,
                    selectedDay = selectedDay,
                    onSelectMonth = { selectedMonth = it },
                    onSelectDay = { selectedDay = it },
                    onOpenEvent = onOpenEvent,
                )
            }
        }
    }
}
