package com.familyevents.explore

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
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
    var selectedDateFilter by remember { mutableStateOf<DateFilter?>(null) }
    var selectedAgeFilter by remember { mutableStateOf<AgeFilter?>(null) }
    var isFreeOnly by remember { mutableStateOf(false) }

    val dateRange = remember(selectedDateFilter) { selectedDateFilter?.dateRange() }

    val eventQuery = remember(cityId, query, selectedDateFilter, selectedAgeFilter, isFreeOnly, dateRange) {
        EventQuery(
            cityId = cityId,
            search = query.takeIf { it.isNotBlank() },
            ageMin = selectedAgeFilter?.min,
            ageMax = selectedAgeFilter?.max,
            isFree = if (isFreeOnly) true else null,
            dateFrom = dateRange?.first,
            dateTo = dateRange?.second,
        )
    }

    val events by eventRepository.observeEventList(eventQuery).collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(eventQuery) {
        runCatching { eventRepository.refreshEventList(eventQuery) }
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            DateFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedDateFilter == filter,
                    onClick = { selectedDateFilter = if (selectedDateFilter == filter) null else filter },
                    label = { Text(filter.label) },
                )
            }
            AgeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedAgeFilter == filter,
                    onClick = { selectedAgeFilter = if (selectedAgeFilter == filter) null else filter },
                    label = { Text(filter.label) },
                )
            }
            FilterChip(
                selected = isFreeOnly,
                onClick = { isFreeOnly = !isFreeOnly },
                label = { Text("Free") },
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
            items(events, key = { it.id.rawValue }) { event ->
                EventCard(event.title, event.venueName ?: "Family event", event.tags.firstOrNull()?.label, imageUrl = event.imageUrl) {
                    onOpenEvent(event.id)
                }
            }
        }
    }
}
