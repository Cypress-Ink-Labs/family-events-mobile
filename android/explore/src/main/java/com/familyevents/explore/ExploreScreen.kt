package com.familyevents.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun ExploreScreen(
    cityId: CityId?,
    eventRepository: EventRepository,
    onOpenEvent: (EventId) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val events by eventRepository.observeEventList(EventQuery(cityId = cityId, search = query)).collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(cityId, query) {
        runCatching { eventRepository.refreshEventList(EventQuery(cityId = cityId, search = query)) }
    }

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
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
            items(events, key = { it.id.rawValue }) { event ->
                EventCard(event.title, event.venueName ?: "Family event", event.tags.firstOrNull()?.label, imageUrl = event.imageUrl) {
                    onOpenEvent(event.id)
                }
            }
        }
    }
}
