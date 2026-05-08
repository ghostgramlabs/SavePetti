package com.savepetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.data.util.TimeFormat
import com.savepetti.domain.model.ContentType
import com.savepetti.domain.model.SourceApp

/**
 * Pinterest-style card. Aspect ratio varies based on content type so the
 * staggered grid breathes — links are tall, plain text is short, images
 * adopt their natural ratio (capped to a sensible range).
 */
@Composable
fun SaveCard(
    item: SaveItemEntity,
    accent: Color,
    categoryEmoji: String?,
    categoryName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = runCatching { ContentType.valueOf(item.contentType) }.getOrDefault(ContentType.NOTE)
    val source = runCatching { SourceApp.valueOf(item.sourceApp) }.getOrDefault(SourceApp.UNKNOWN)
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outline, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        // Visual header — image, accent block, or text snippet
        when {
            !item.thumbnailUri.isNullOrBlank() -> {
                AsyncImage(
                    model = item.thumbnailUri,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(headerRatio(type))
                        .background(accent.copy(alpha = 0.12f))
                )
            }
            !item.localUri.isNullOrBlank() && type == ContentType.IMAGE -> {
                AsyncImage(
                    model = item.localUri,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(headerRatio(type))
                        .background(accent.copy(alpha = 0.12f))
                )
            }
            else -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(headerRatio(type))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconFor(type),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Column(Modifier.padding(14.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                color = scheme.onSurface
            )
            if (!item.notes.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (categoryEmoji != null && categoryName != null) {
                    Box(
                        Modifier
                            .background(accent.copy(alpha = 0.16f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "$categoryEmoji $categoryName",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                }
                Text(
                    "${source.emoji} ${TimeFormat.relative(item.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (item.isPinned) {
                    Icon(Icons.Rounded.Bookmark, null, tint = accent, modifier = Modifier.size(16.dp))
                }
                if (item.isFavorite) {
                    Spacer(Modifier.size(4.dp))
                    Icon(
                        Icons.Rounded.Favorite, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun iconFor(type: ContentType) = when (type) {
    ContentType.LINK -> Icons.Rounded.Link
    ContentType.IMAGE -> Icons.Rounded.Image
    ContentType.PDF -> Icons.Rounded.PictureAsPdf
    ContentType.NOTE, ContentType.TEXT -> Icons.Rounded.Notes
    ContentType.FILE -> Icons.Rounded.Notes
}

private fun headerRatio(type: ContentType): Float = when (type) {
    ContentType.LINK -> 1.4f
    ContentType.IMAGE -> 1.0f
    ContentType.PDF -> 1.6f
    ContentType.TEXT -> 2.2f
    ContentType.NOTE -> 2.4f
    ContentType.FILE -> 1.8f
}
