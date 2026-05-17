package com.familyevents.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventRepository
import com.familyevents.data.FavoriteRepository
import com.familyevents.data.WeatherRepository
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.EventCard
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.generated.Tokens
import kotlinx.coroutines.launch

@Composable
fun PlanScreen(
    userId: UserId,
    cityId: CityId?,
    eventRepository: EventRepository,
    favoriteRepository: FavoriteRepository,
    weatherRepository: WeatherRepository,
    onOpenEvent: (EventId) -> Unit,
    onSetCity: () -> Unit,
) {
    val rows by eventRepository.observePlanEvents(userId, cityId).collectAsState(initial = emptyList())
    val emptyForecast = remember { mutableStateOf(emptyList<com.familyevents.data.WeatherSnapshotDto>()) }
    val forecast by cityId?.let { weatherRepository.observeForecast(it).collectAsState(initial = emptyList()) } ?: emptyForecast
    val scope = rememberCoroutineScope()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4),
        modifier = Modifier
            .fillMaxSize()
            .padding(Tokens.Space.S4),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                Text("Saturday Plan", style = FamilyTypography.TitleLarge)
                Text(forecast.firstOrNull()?.summary ?: "Pick a few easy events for this weekend.", style = FamilyTypography.BodySmall)
                Button(onClick = { scope.launch { eventRepository.refreshPlan(userId, cityId) } }) {
                    Text("Refresh")
                }
            }
        }
        if (cityId == null || rows.isEmpty()) {
            item {
                EmptyState("Set your city or broaden filters.", "Set city", onSetCity)
            }
        } else {
            items(rows, key = { it.event.id.rawValue }) { row ->
                EventCard(
                    title = row.event.title,
                    subtitle = row.event.venueName ?: row.section,
                    badge = row.event.tags.firstOrNull()?.label,
                    onClick = { onOpenEvent(row.event.id) },
                )
            }
        }
    }
}
