package com.familyevents.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.data.EventQuery
import com.familyevents.data.EventRepository
import com.familyevents.designsystem.EventCard
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.generated.Tokens

enum class ExploreMode { List, Map, Calendar }

@Composable
fun ExploreScreen(
    cityId: CityId?,
    eventRepository: EventRepository,
    onOpenEvent: (EventId) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ExploreMode.List) }
    val events by eventRepository.observeEventList(EventQuery(cityId = cityId, search = query)).collectAsState(initial = emptyList())

    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4),
        modifier = Modifier
            .fillMaxSize()
            .padding(Tokens.Space.S4),
    ) {
        Text("Explore", style = FamilyTypography.TitleLarge)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
            ExploreMode.entries.forEach { item ->
                FilterChip(
                    selected = mode == item,
                    onClick = { mode = item },
                    label = { Text(item.name) },
                )
            }
        }
        when (mode) {
            ExploreMode.List -> LazyColumn(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                items(events, key = { it.id.rawValue }) { event ->
                    EventCard(event.title, event.venueName ?: "Family event", event.tags.firstOrNull()?.label) {
                        onOpenEvent(event.id)
                    }
                }
            }
            ExploreMode.Map -> MapLibrePreview(events.mapNotNull { it.coordinate?.let { coord -> it.title to coord } })
            ExploreMode.Calendar -> LazyColumn(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                items(events, key = { it.id.rawValue }) { event ->
                    EventCard(event.title, event.startsAt.toString(), "Calendar") { onOpenEvent(event.id) }
                }
            }
        }
    }
}

@Composable
private fun MapLibrePreview(points: List<Pair<String, com.familyevents.core.GeoCoordinate>>) {
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
        Text("Map", style = FamilyTypography.TitleMedium)
        points.forEach { (title, coord) ->
            Text("$title (${coord.latitude}, ${coord.longitude})", style = FamilyTypography.BodySmall)
        }
    }
}
