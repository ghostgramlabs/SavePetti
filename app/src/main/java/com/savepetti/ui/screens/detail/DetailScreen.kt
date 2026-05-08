package com.savepetti.ui.screens.detail

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import coil.compose.AsyncImage
import com.savepetti.data.util.TimeFormat
import com.savepetti.domain.model.SourceApp
import com.savepetti.ui.components.CategoryChip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.item
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this save?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete().invokeOnCompletion { onDeleted() }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.togglePinned()
                    }) {
                        Icon(
                            if (item?.isPinned == true) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = if (item?.isPinned == true) "Unpin" else "Pin",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleFavorite()
                    }) {
                        Icon(
                            if (item?.isFavorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (item?.isFavorite == true) "Unfavorite" else "Favorite",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        item?.let {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, it.url ?: it.title)
                            }
                            ctx.startActivity(Intent.createChooser(intent, "Share"))
                        }
                    }) { Icon(Icons.Rounded.Share, contentDescription = "Share") }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (item == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        val accent = state.category?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary
        val source = runCatching { SourceApp.valueOf(item.sourceApp) }.getOrDefault(SourceApp.UNKNOWN)

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Big preview — horizontal scroll if there are multiple attachments,
            // single hero otherwise.
            val gallery: List<String> = state.attachments.map { it.uri }
                .ifEmpty { listOfNotNull(item.thumbnailUri ?: item.localUri) }

            if (gallery.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(gallery, key = { it }) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .heightIn(min = 280.dp, max = 380.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(accent.copy(alpha = 0.12f))
                        )
                    }
                }
            } else if (gallery.isNotEmpty()) {
                AsyncImage(
                    model = gallery.first(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(accent.copy(alpha = 0.12f))
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(source.emoji, style = MaterialTheme.typography.displayLarge)
                }
            }

            if (gallery.size > 1) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${gallery.size} items · swipe →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(Modifier.padding(horizontal = 24.dp)) {
                EditableTitle(
                    initial = item.title,
                    onSave = viewModel::updateTitle
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${source.emoji} ${source.displayName} · saved ${TimeFormat.relative(item.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!item.url.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.14f))
                            .clickable {
                                val i = Intent(Intent.ACTION_VIEW, item.url.toUri())
                                runCatching { ctx.startActivity(i) }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.OpenInNew, null, tint = accent,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "Open original",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Category", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.categories, key = { it.id }) { c ->
                        CategoryChip(
                            label = c.name,
                            emoji = c.emoji,
                            color = Color(c.colorHex),
                            selected = item.categoryId == c.id,
                            onClick = {
                                viewModel.setCategory(if (item.categoryId == c.id) null else c.id)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                TagsRow(
                    tags = state.tags.map { it.name },
                    accent = accent,
                    onAdd = viewModel::addTag,
                    onRemove = viewModel::removeTag
                )

                Spacer(Modifier.height(24.dp))
                NotesEditor(initial = item.notes.orEmpty(), onSave = viewModel::updateNotes)

                if (!item.ocrText.isNullOrBlank()) {
                    Spacer(Modifier.height(24.dp))
                    OcrTextSection(text = item.ocrText)
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun OcrTextSection(text: String) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Text(
        "Text from image",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(8.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else 6,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
    if (text.lineSequence().count() > 6 || text.length > 360) {
        androidx.compose.material3.TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Show less" else "Show all")
        }
    }
}

/**
 * Big-headline title that's tap-to-edit. We use [androidx.compose.foundation.text.BasicTextField]
 * (no Material container) so the visual matches the original [Text] until the
 * user focuses it. Saves on focus loss — avoids spamming the DB on every keystroke.
 *
 * A pencil icon hints at editability; a hairline underline appears on focus
 * so the user has visual confirmation they're typing.
 */
@Composable
private fun EditableTitle(initial: String, onSave: (String) -> Unit) {
    var value by androidx.compose.runtime.remember(initial) {
        androidx.compose.runtime.mutableStateOf(initial)
    }
    var focused by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    Row(verticalAlignment = Alignment.Top) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { value = it },
            textStyle = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .let { m ->
                    // Hairline underline only when focused, so the user knows
                    // their keystrokes are landing.
                    if (focused) m.drawBehind {
                        val y = size.height
                        drawLine(
                            color = accent,
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 2f
                        )
                    } else m
                }
                .onFocusChanged { fs ->
                    if (focused && !fs.isFocused) {
                        if (value != initial) onSave(value)
                    }
                    focused = fs.isFocused
                }
        )
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = "Edit title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 6.dp)
                .size(18.dp)
        )
    }
}

@Composable
private fun TagsRow(
    tags: List<String>,
    accent: Color,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var input by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    Column {
        Text("Tags", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (tags.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tags, key = { it }) { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.14f))
                            .clickable { onRemove(tag) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("#$tag", style = MaterialTheme.typography.labelLarge, color = accent)
                        Spacer(Modifier.width(4.dp))
                        Text("✕", style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        androidx.compose.material3.OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = { Text("Add a tag, press enter") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    onAdd(input)
                    input = ""
                }
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            )
        )
    }
}

@Composable
private fun NotesEditor(initial: String, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    Column {
        Text("Your note", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onSave(it)
            },
            placeholder = { Text("Add a thought, a reminder, why you saved this…") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
