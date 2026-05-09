package com.savepetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A category chip with a slight tilt so a row of chips looks
 * hand-placed, not aligned-by-grid. Selected chips fill with the category
 * color; unselected use a soft tint of it on the cream background.
 */
@Composable
fun CategoryChip(
    label: String,
    emoji: String?,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    tilt: Float = 0f,
    onClick: () -> Unit
) {
    val bg = if (selected) color else color.copy(alpha = 0.14f)
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .rotate(tilt)
            .background(bg, RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = color.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        if (!emoji.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (selected) Color.White.copy(alpha = 0.18f) else color.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = fg
        )
    }
}
