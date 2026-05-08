package com.savepetti.ui.screens.save

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.savepetti.ui.components.CategoryChip
import com.savepetti.ui.components.CreateCollectionDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveSheet(
    incoming: IncomingShare,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SaveSheetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val titleFocus = remember { FocusRequester() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) { viewModel.ingest(incoming) }
    // Deliberately not auto-focusing the title - the primary action is
    // picking a collection. The user opens the sheet, taps a chip, taps Save.
    // Editing the title or adding a note is an optional second step that
    // shouldn't pop the keyboard up unprompted.
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            // System-level confirmation in case the in-sheet animation gets cut off
            Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show()
            // Hold the success state on screen briefly so the user sees the check
            delay(900)
            sheetState.hide()
            onSaved()
        }
    }

    if (showCreate) {
        CreateCollectionDialog(
            onCreate = { nc ->
                viewModel.createCollection(nc)
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    ) {
        if (state.isSaved) {
            SuccessPanel()
            return@ModalBottomSheet
        }
        if (state.mode == SaveMode.PICK_EXISTING) {
            PickExistingBody(
                items = state.recentItems,
                categories = state.categories,
                onPick = { id -> viewModel.saveToExisting(id) },
                onCancel = { viewModel.setMode(SaveMode.NEW) }
            )
            return@ModalBottomSheet
        }
        // Scrollable upper area + sticky action bar at the bottom. Without
        // this split, the keyboard reduces the sheet's available height to a
        // sliver and the Save button falls below the visible region.
        Column(Modifier.fillMaxWidth().imePadding()) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Save this?",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "${state.sourceApp.emoji} ${state.sourceApp.displayName}" +
                    if (state.attachments.size > 1) " - ${state.attachments.size} items" else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Preview
            if (state.attachments.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.attachments) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            } else {
                val img = state.previewImage ?: state.localUri
                if (img != null) {
                    AsyncImage(
                        model = img,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }

            // Resolved title shown as compact text - display only by default,
            // editable on demand. Avoids popping the keyboard up just to view.
            Text(
                state.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (!state.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    state.description!!.take(120),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // Primary action: pick a collection.
            Spacer(Modifier.height(20.dp))
            Text(
                "Save to",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.categories, key = { it.id }) { c ->
                    CategoryChip(
                        label = c.name,
                        emoji = c.emoji,
                        color = Color(c.colorHex),
                        selected = state.selectedCategory == c.id,
                        onClick = { viewModel.selectCategory(c.id) }
                    )
                }
                item {
                    NewCollectionChip(onClick = { showCreate = true })
                }
            }

            // Optional details expand on demand.
            Spacer(Modifier.height(20.dp))
            OptionalDetails(
                title = state.title,
                notes = state.notes,
                tagsInput = state.tagsInput,
                onTitleChange = viewModel::setTitle,
                onNotesChange = viewModel::setNotes,
                onTagsChange = viewModel::setTags,
                titleFocus = titleFocus
            )

            if (state.recentItems.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.setMode(SaveMode.PICK_EXISTING) }
                ) {
                    Text(
                        "or add to an existing save ->",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Bottom padding inside the scroll so the last field clears the
            // sticky action bar even before the user scrolls.
            Spacer(Modifier.height(16.dp))
        }
        // Sticky action bar - always visible regardless of keyboard / content.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            IconButton(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleFavorite()
            }) {
                Icon(
                    if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (state.isFavorite) "Unfavorite" else "Favorite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                if (state.isFavorite) "Loved it" else "Mark favorite",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.save()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text("Save it", fontWeight = FontWeight.Bold)
            }
        }
        }
    }
}

@Composable
private fun PickExistingBody(
    items: List<com.savepetti.data.local.SaveItemEntity>,
    categories: List<com.savepetti.data.local.CategoryEntity>,
    onPick: (Long) -> Unit,
    onCancel: () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Add to which save?",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.TextButton(onClick = onCancel) { Text("Back") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val cat = categories.firstOrNull { it.id == item.categoryId }
                PickRow(
                    title = item.title,
                    subtitle = listOfNotNull(cat?.let { "${it.emoji} ${it.name}" }, item.url).joinToString(" - ").take(80),
                    onClick = { onPick(item.id) }
                )
            }
        }
    }
}

@Composable
private fun PickRow(title: String, subtitle: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                maxLines = 1
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Text("->", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
    }
}

/**
 * Title / notes / tags are all optional on save - Stash's UX. We show one
 * compact row of "Add ..." chips. Tapping one inlines the field; the
 * keyboard only ever appears in response to a tap, never on sheet open.
 */
@Composable
private fun OptionalDetails(
    title: String,
    notes: String,
    tagsInput: String,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    titleFocus: FocusRequester
) {
    var showTitle by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var showTags by remember { mutableStateOf(false) }

    val anyOpen = showTitle || showNotes || showTags

    if (!anyOpen) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AddChip("Edit title") { showTitle = true }
            AddChip("Add note") { showNotes = true }
            AddChip("Add tags") { showTags = true }
        }
        return
    }

    if (showTitle) {
        TextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Title") },
            singleLine = false,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().focusRequester(titleFocus)
        )
        // Focus the title only after the user explicitly asked to edit it.
        LaunchedEffect(showTitle) {
            if (showTitle) {
                kotlinx.coroutines.delay(80)
                runCatching { titleFocus.requestFocus() }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    if (showNotes) {
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            placeholder = { Text("Add a note") },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
        )
        Spacer(Modifier.height(12.dp))
    }
    if (showTags) {
        OutlinedTextField(
            value = tagsInput,
            onValueChange = onTagsChange,
            placeholder = { Text("Tags, comma-separated") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
    }

    // Let the user reveal whichever fields they didn't open yet.
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!showTitle) AddChip("Edit title") { showTitle = true }
        if (!showNotes) AddChip("Add note") { showNotes = true }
        if (!showTags) AddChip("Add tags") { showTags = true }
    }
}

@Composable
private fun AddChip(label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(scheme.surface)
            .border(1.dp, scheme.outline, androidx.compose.foundation.shape.CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text("+ $label", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun SuccessPanel() {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Check, null,
                tint = accent,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Saved!",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Find it from the home screen anytime.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NewCollectionChip(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(scheme.surface)
            .border(1.5.dp, scheme.primary, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Icon(
            Icons.Rounded.Add, null,
            tint = scheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "New",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
