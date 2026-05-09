package com.savepetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A non-editable search "pill" that opens the search screen on tap.
 * Use this on screens where typing is not the primary action (e.g. Home);
 * it prevents the surface from stealing focus and avoids the keystroke-eats-
 * navigation bug we hit when [SearchField] was used as a navigation trigger.
 */
@Composable
fun SearchPill(
    placeholder: String = "Search anything you saved",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surface)
            .border(1.5.dp, scheme.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
    ) {
        Icon(Icons.Rounded.Search, null, tint = scheme.primary)
        Text(
            placeholder,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
