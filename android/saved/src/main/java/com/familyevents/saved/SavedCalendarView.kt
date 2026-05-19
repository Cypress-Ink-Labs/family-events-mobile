package com.familyevents.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familyevents.core.EventId
import com.familyevents.core.decodeHtmlEntities
import com.familyevents.data.EventDto
import com.familyevents.data.FavoriteDto
import com.familyevents.designsystem.EventCard
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.generated.Tokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private val DAY_HEADERS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun SavedCalendarView(
    favorites: List<FavoriteDto>,
    eventsById: Map<EventId, EventDto>,
    selectedMonth: YearMonth,
    selectedDay: LocalDate?,
    onSelectMonth: (YearMonth) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onOpenEvent: (EventId) -> Unit,
) {
    val zoneId = ZoneId.systemDefault()
    val locale: Locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now(zoneId)

    // Build the 6-week grid window: start of week containing month-first (Sunday),
    // through end of week containing month-last (Saturday).
    val monthStart = selectedMonth.atDay(1)
    val monthEnd = selectedMonth.atEndOfMonth()
    val gridStart = monthStart.minusDays(monthStart.dayOfWeek.let {
        if (it == DayOfWeek.SUNDAY) 0L else it.value.toLong()
    })
    val gridEnd = monthEnd.plusDays(
        if (monthEnd.dayOfWeek == DayOfWeek.SATURDAY) 0L
        else (6 - monthEnd.dayOfWeek.value).toLong()
    )
    val gridDays = buildList {
        var d = gridStart
        while (!d.isAfter(gridEnd)) {
            add(d)
            d = d.plusDays(1)
        }
    }

    val dayEvents = selectedDay?.let { getEventsForDay(it, favorites, eventsById, zoneId) } ?: emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        // Month header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onSelectMonth(selectedMonth.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
            }
            Text(
                text = "%s %d".format(
                    selectedMonth.month.getDisplayName(TextStyle.FULL, locale),
                    selectedMonth.year,
                ),
                style = FamilyTypography.TitleMedium,
            )
            IconButton(onClick = { onSelectMonth(selectedMonth.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
            }
        }

        // Day-of-week labels (S M T W T F S)
        Row(modifier = Modifier.fillMaxWidth()) {
            DAY_HEADERS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = FamilyTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }

        // Month grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false,
        ) {
            items(gridDays) { date ->
                val inMonth = date.month == selectedMonth.month && date.year == selectedMonth.year
                val isToday = date == today
                val isSelected = date == selectedDay
                val hasDot = getEventsForDay(date, favorites, eventsById, zoneId).isNotEmpty()

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .size(40.dp)
                        .then(if (!inMonth) Modifier.alpha(0.3f) else Modifier)
                        .padding(2.dp)
                        .then(
                            when {
                                isSelected -> Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else -> Modifier
                            }
                        )
                        .clickable { onSelectDay(date) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = FamilyTypography.Caption,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
                        )
                        if (hasDot) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary,
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }
        }

        // Events for selected day
        if (selectedDay != null && dayEvents.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(dayEvents, key = { it.id.rawValue }) { event ->
                    EventCard(
                        title = decodeHtmlEntities(event.title),
                        subtitle = event.venueName ?: "Family event",
                        badge = null,
                        imageUrl = event.imageUrl,
                        onClick = { onOpenEvent(event.id) },
                    )
                }
            }
        }
    }
}

private fun getEventsForDay(
    date: LocalDate,
    favorites: List<FavoriteDto>,
    eventsById: Map<EventId, EventDto>,
    zoneId: ZoneId,
): List<EventDto> = favorites.mapNotNull { fav ->
    eventsById[fav.eventId]?.takeIf { event ->
        event.startsAt.atZone(zoneId).toLocalDate() == date
    }
}
