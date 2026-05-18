package com.familyevents.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.familyevents.designsystem.generated.Tokens

@Composable
fun EventCard(
    title: String,
    subtitle: String,
    badge: String?,
    imageUrl: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val resolvedImageUrl = imageUrl?.takeIf { it.isNotBlank() }
        ?: "https://picsum.photos/seed/${title.hashCode().toUInt()}/600/400"
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Tokens.Radius.Md),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            modifier = Modifier.padding(Tokens.Space.S4),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(Tokens.Radius.Md))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = resolvedImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                Text(title, style = FamilyTypography.TitleMedium, modifier = Modifier.weight(1f))
                if (badge != null) {
                    TagPill(label = badge)
                }
            }
            Text(subtitle, style = FamilyTypography.BodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
    }
}

@Composable
fun EventHeroImage(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val resolvedImageUrl = imageUrl?.takeIf { it.isNotBlank() }
        ?: "https://picsum.photos/seed/${title.hashCode().toUInt()}/1200/630"
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = resolvedImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun FavoriteButton(isFavorited: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .size(Tokens.Touch.Min)
            .semantics {
                role = Role.Button
                contentDescription = if (isFavorited) "Unfavorite" else "Favorite"
            },
    ) {
        Text(if (isFavorited) "♥" else "♡", style = FamilyTypography.TitleMedium)
    }
}

@Composable
fun TagPill(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
    ) {
        Text(label, style = FamilyTypography.Caption, modifier = Modifier.padding(horizontal = Tokens.Space.S2, vertical = Tokens.Space.S1))
    }
}

@Composable
fun LoadingState(label: String = "Loading") {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
        CircularProgressIndicator()
        Text(label, style = FamilyTypography.BodySmall)
    }
}

@Composable
fun EmptyState(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(Tokens.Space.S5), verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
            Text(title, style = FamilyTypography.TitleMedium)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f))) {
        Column(modifier = Modifier.padding(Tokens.Space.S5), verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
            Text(message, style = FamilyTypography.Body)
            if (onRetry != null) {
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// InfoGrid
// ---------------------------------------------------------------------------

data class InfoGridItem(
    val label: String,
    val value: String,
    /** Emoji or short glyph; no icon dependency needed for v1. */
    val icon: String,
)

/**
 * Renders [items] in a 2-column card grid mirroring the web EventDetailInfoGrid.
 * Uses [FlowRow] (stable in compose-bom ≥ 2024.06 / compose-foundation 1.6+;
 * this project targets compose-bom 2026.05.00 so FlowRow is available).
 */
@Composable
fun InfoGrid(
    items: List<InfoGridItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
        maxItemsInEachRow = 2,
    ) {
        items.forEach { item ->
            OutlinedCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(Tokens.Radius.Md),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.padding(Tokens.Space.S4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 36dp icon square — mirrors web `size-9` (36px)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(Tokens.Radius.Md))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(item.icon, style = FamilyTypography.TitleMedium)
                    }
                    Spacer(modifier = Modifier.size(Tokens.Space.S3))
                    Column {
                        Text(
                            text = item.label,
                            style = FamilyTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Text(
                            text = item.value,
                            style = FamilyTypography.Body,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AttendeeStepper
// ---------------------------------------------------------------------------

/**
 * Decrement / count / increment row with 44dp touch targets, mirroring the
 * web AttendeeStepper. Caller owns the value; buttons only guard emission.
 */
@Composable
fun AttendeeStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 8,
    label: String = "Attendees",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = FamilyTypography.Body,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val atMin = value <= min
            val atMax = value >= max
            IconButton(
                onClick = { if (!atMin) onValueChange(value - 1) },
                enabled = !atMin,
                modifier = Modifier.size(Tokens.Touch.Min),
            ) {
                Text(
                    text = "−",
                    style = FamilyTypography.TitleMedium,
                    color = if (atMin) LocalContentColor.current.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "$value",
                style = FamilyTypography.TitleMedium,
                modifier = Modifier.widthIn(min = 24.dp),
                textAlign = TextAlign.Center,
            )
            IconButton(
                onClick = { if (!atMax) onValueChange(value + 1) },
                enabled = !atMax,
                modifier = Modifier.size(Tokens.Touch.Min),
            ) {
                Text(
                    text = "+",
                    style = FamilyTypography.TitleMedium,
                    color = if (atMax) LocalContentColor.current.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
