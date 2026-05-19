package com.familyevents.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.GeoCoordinate
import com.familyevents.data.EventQuery
import com.familyevents.data.EventRepository
import com.familyevents.designsystem.EventCard
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.generated.Tokens
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

enum class ExploreMode { List, Map, Calendar }

@Composable
fun ExploreScreen(
    cityId: CityId?,
    mapStyleUrl: String,
    eventRepository: EventRepository,
    onOpenEvent: (EventId) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ExploreMode.List) }
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
                    EventCard(event.title, event.venueName ?: "Family event", event.tags.firstOrNull()?.label, imageUrl = event.imageUrl) {
                        onOpenEvent(event.id)
                    }
                }
            }
            ExploreMode.Map -> MapLibreMap(
                styleUrl = mapStyleUrl,
                points = events.mapNotNull { event -> event.coordinate?.let { coord -> event.title to coord } },
            )
            ExploreMode.Calendar -> LazyColumn(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                items(events, key = { it.id.rawValue }) { event ->
                    EventCard(event.title, event.startsAt.toString(), "Calendar", imageUrl = event.imageUrl) { onOpenEvent(event.id) }
                }
            }
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun MapLibreMap(styleUrl: String, points: List<Pair<String, GeoCoordinate>>) {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { it.onCreate(null) }
    }
    androidx.compose.runtime.DisposableEffect(mapView) {
        mapView.onStart()
        onDispose {
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
        Text("Map", style = FamilyTypography.TitleMedium)
        Box(Modifier.fillMaxWidth().height(280.dp)) {
            AndroidView(
                factory = { mapView },
                update = { view ->
                    view.getMapAsync { map ->
                        map.setStyle(Style.Builder().fromUri(styleUrl))
                        map.clear()
                        points.forEach { (title, coord) ->
                            map.addMarker(
                                org.maplibre.android.annotations.MarkerOptions()
                                    .position(LatLng(coord.latitude, coord.longitude))
                                    .title(title),
                            )
                        }
                        points.firstOrNull()?.second?.let { coord ->
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(coord.latitude, coord.longitude), 11.0))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (points.isEmpty()) {
            Text("No mappable events yet.", style = FamilyTypography.BodySmall)
        }
    }
}
