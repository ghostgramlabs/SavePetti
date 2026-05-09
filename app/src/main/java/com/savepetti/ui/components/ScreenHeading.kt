package com.savepetti.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The bold serif "page title" used at the top of every primary screen.
 * Mirrors the Home greeting so Search, Browse, and Settings feel like the
 * same app rather than three different layouts under one theme.
 *
 * `leading` and `trailing` are optional slots for things like a back
 * IconButton (left) or a row of action IconButtons (right). They sit on
 * the same baseline as the headline so the page reads as one block.
 */
@Composable
fun ScreenHeading(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (leading != null) {
            Box(Modifier.padding(top = 6.dp, end = 4.dp)) { leading() }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Box(Modifier.padding(top = 6.dp)) { trailing() }
        }
    }
}
