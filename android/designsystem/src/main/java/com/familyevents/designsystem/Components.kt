package com.familyevents.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
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
