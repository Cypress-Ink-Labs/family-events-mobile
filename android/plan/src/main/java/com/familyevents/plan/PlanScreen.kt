package com.familyevents.plan

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.familyevents.platform.LocationProvider
import kotlinx.coroutines.launch

@Composable
fun PlanScreen(
    userId: UserId,
    cityId: CityId?,
    kidAge: Int? = null,
    eventRepository: EventRepository,
    favoriteRepository: FavoriteRepository,
    weatherRepository: WeatherRepository,
    locationProvider: LocationProvider,
    onOpenEvent: (EventId) -> Unit,
    onSetCity: () -> Unit,
) {
    val rows by eventRepository.observePlanEvents(userId, cityId).collectAsState(initial = emptyList())
    val emptyForecast = remember { mutableStateOf(emptyList<com.familyevents.data.WeatherSnapshotDto>()) }
    val forecast by cityId?.let { weatherRepository.observeForecast(it).collectAsState(initial = emptyList()) } ?: emptyForecast
    val scope = rememberCoroutineScope()
    var permissionAsked by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            val coord = if (granted) locationProvider.lastKnownLocation() else null
            eventRepository.refreshPlan(userId, cityId, kidAge, coord?.latitude, coord?.longitude)
        }
    }

    LaunchedEffect(userId, cityId) {
        val coord = locationProvider.lastKnownLocation()
        if (coord == null && !permissionAsked) {
            permissionAsked = true
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            eventRepository.refreshPlan(userId, cityId, kidAge, coord?.latitude, coord?.longitude)
        }
    }

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
                Button(onClick = {
                    scope.launch {
                        val coord = locationProvider.lastKnownLocation()
                        eventRepository.refreshPlan(userId, cityId, kidAge, coord?.latitude, coord?.longitude)
                    }
                }) {
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
                    imageUrl = row.event.imageUrl,
                    onClick = { onOpenEvent(row.event.id) },
                )
            }
        }
    }
}
