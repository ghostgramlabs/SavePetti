package com.ghostgramlabs.pettibox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.pettibox.data.local.CategoryEntity

/**
 * Edit-a-collection dialog. Lives in `components` (not a screen) because
 * both Browse and Settings open it — duplicating it would mean the rename /
 * emoji / color UX drifts between the two surfaces.
 *
 * Built-in (non-user-created) collections render in a locked, read-only
 * variant: name field is disabled, emoji / color rows aren't shown, and
 * the Save / Delete actions are hidden. A short explanation tells the
 * user why. This is preferable to making the dialog a different shape
 * for built-ins — same mental model, gated affordances.
 */
@Composable
fun EditCollectionDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(category.id) { mutableStateOf(category.name) }
    var emoji by remember(category.id) { mutableStateOf(category.emoji) }
    var color by remember(category.id) { mutableStateOf(Color(category.colorHex)) }
    val editable = category.userCreated

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editable) "Edit collection" else "Built-in collection")
        },
        text = {
            Column {
                if (!editable) {
                    Text(
                        "Built-in collections can't be renamed or removed — only ones you create can be edited.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(28) },
                    placeholder = { Text("Collection name") },
                    singleLine = true,
                    enabled = editable,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (editable) {
                    Spacer(Modifier.height(16.dp))
                    Text("Emoji", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(CollectionEmojiSeeds) { e ->
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (e == emoji) color.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(e, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Color", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CollectionColorSeeds) { c ->
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(c)
                                    .border(
                                        width = if (c == color) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { color = c }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (editable) {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = { onSave(name, emoji, color.toLongHex()) }
                ) { Text("Save") }
            } else {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            if (editable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
