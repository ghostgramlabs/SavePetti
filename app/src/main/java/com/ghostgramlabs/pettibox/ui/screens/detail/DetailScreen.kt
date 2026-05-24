package com.ghostgramlabs.pettibox.ui.screens.detail

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import coil.compose.AsyncImage
import com.ghostgramlabs.pettibox.data.local.AttachmentEntity
import com.ghostgramlabs.pettibox.data.util.TimeFormat
import com.ghostgramlabs.pettibox.domain.model.ContentType
import com.ghostgramlabs.pettibox.domain.model.SourceApp
import com.ghostgramlabs.pettibox.ui.components.CategoryChip
import com.ghostgramlabs.pettibox.ui.components.KeeperMascot
import com.ghostgramlabs.pettibox.ui.components.KeeperPose
import com.ghostgramlabs.pettibox.ui.components.ReminderCustomSheet
import com.ghostgramlabs.pettibox.ui.components.ReminderPickerSheet
import com.ghostgramlabs.pettibox.ui.components.ScreenHeading
import com.ghostgramlabs.pettibox.ui.components.formatReminderAt
import com.ghostgramlabs.pettibox.ui.components.rememberNotificationPermissionRequester
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onOpenTag: (String) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.item
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showCustomReminder by remember { mutableStateOf(false) }
    val requestNotificationPermission = rememberNotificationPermissionRequester()
    // Per-attachment delete is two-stage: a confirm dialog, then an
    // optimistic hide + Undo snackbar. We commit the actual repo delete
    // only after the snackbar dismisses without Undo, so files survive
    // long enough to restore.
    var attachmentDeleteCandidate by remember { mutableStateOf<Long?>(null) }
    var hiddenAttachmentIds by remember { mutableStateOf(emptySet<Long>()) }

    val requestAttachmentDelete: (Long) -> Unit = { id ->
        attachmentDeleteCandidate = id
    }
    val confirmAttachmentDelete: (Long) -> Unit = { id ->
        attachmentDeleteCandidate = null
        hiddenAttachmentIds = hiddenAttachmentIds + id
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Item removed",
                actionLabel = "Undo"
            )
            if (result == SnackbarResult.ActionPerformed) {
                hiddenAttachmentIds = hiddenAttachmentIds - id
            } else {
                hiddenAttachmentIds = hiddenAttachmentIds - id
                viewModel.deleteAttachment(id)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this save?") },
            text = { Text("PettiBox moves it to Archive first and gives you Undo. Archive instead if you want to keep it searchable.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        val stagedItem = viewModel.stageDelete() ?: return@launch
                        val result = snackbarHostState.showSnackbar(
                            message = "Moved to Archive",
                            actionLabel = "Undo"
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoStagedDelete(stagedItem)
                        } else {
                            viewModel.deletePermanently(stagedItem)
                            onDeleted()
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                    if (state.item?.isArchived != true) {
                        TextButton(onClick = {
                            showDeleteConfirm = false
                            viewModel.setArchived(true)
                            scope.launch {
                                snackbarHostState.showSnackbar("Tucked away in Archive")
                            }
                        }) { Text("Archive instead") }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showReminderSheet) {
        ReminderPickerSheet(
            currentRemindAt = item?.remindAt,
            onPick = { at ->
                showReminderSheet = false
                if (at != null) {
                    requestNotificationPermission {
                        viewModel.setRemindAt(at)
                        scope.launch {
                            snackbarHostState.showSnackbar("We'll remind you " + formatReminderAt(at))
                        }
                    }
                } else {
                    viewModel.setRemindAt(null)
                    scope.launch { snackbarHostState.showSnackbar("Reminder cleared") }
                }
            },
            onCustom = {
                showReminderSheet = false
                showCustomReminder = true
            },
            onDismiss = { showReminderSheet = false }
        )
    }

    if (showCustomReminder) {
        ReminderCustomSheet(
            onConfirm = { at ->
                showCustomReminder = false
                requestNotificationPermission {
                    viewModel.setRemindAt(at)
                    scope.launch {
                        snackbarHostState.showSnackbar("We'll remind you " + formatReminderAt(at))
                    }
                }
            },
            onDismiss = { showCustomReminder = false }
        )
    }

    attachmentDeleteCandidate?.let { id ->
        AlertDialog(
            onDismissRequest = { attachmentDeleteCandidate = null },
            title = { Text("Remove this item?") },
            text = { Text("You can undo right after.") },
            confirmButton = {
                TextButton(onClick = { confirmAttachmentDelete(id) }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { attachmentDeleteCandidate = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (item == null) {
            if (state.isLoaded) {
                MissingSaveState(
                    onBack = onBack,
                    modifier = Modifier.padding(padding)
                )
            } else {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }
        val accent = state.category?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary
        val source = runCatching { SourceApp.valueOf(item.sourceApp) }.getOrDefault(SourceApp.UNKNOWN)

        // Opening the saved URL in the browser. Shared by the "Open original"
        // button and — for link saves whose hero is the web preview — a tap on
        // the photo itself.
        val openOriginal: () -> Unit = {
            val url = item.url
            if (!url.isNullOrBlank()) {
                val i = Intent(Intent.ACTION_VIEW, url.toUri())
                runCatching { ctx.startActivity(i) }
                    .onFailure {
                        scope.launch { snackbarHostState.showSnackbar("Couldn't open original") }
                    }
            }
        }

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Bold page heading + a single inline action row replace the
            // generic Material TopAppBar — Detail used to read as a
            // different app from Home/Browse because the topbar lost the
            // hand-drawn voice. Title, source emoji, and "saved …"
            // metadata now sit at the top of the scrolling column with
            // the same displayMedium-Black treatment as every other
            // screen.
            ScreenHeading(
                title = item.title.ifBlank { "Untitled save" },
                subtitle = "${source.emoji} ${source.displayName} · saved ${TimeFormat.relative(item.createdAt)}",
                leading = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            DetailActionBar(
                isPinned = item.isPinned,
                isFavorite = item.isFavorite,
                isArchived = item.isArchived,
                hasReminder = item.remindAt != null,
                onTogglePin = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.togglePinned()
                },
                onToggleFavorite = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleFavorite()
                },
                onRemind = { showReminderSheet = true },
                onToggleArchive = {
                    val nowArchived = item.isArchived
                    viewModel.setArchived(!nowArchived)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = if (!nowArchived) "Tucked away" else "Back on the shelf",
                            actionLabel = "Undo"
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.setArchived(nowArchived)
                        }
                    }
                },
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, item.url ?: item.title)
                    }
                    runCatching {
                        ctx.startActivity(Intent.createChooser(intent, "Share"))
                    }.onFailure {
                        scope.launch { snackbarHostState.showSnackbar("Couldn't share this save") }
                    }
                },
                onDelete = { showDeleteConfirm = true }
            )
            Spacer(Modifier.height(16.dp))

            // Big preview: horizontal scroll if there are multiple attachments,
            // single hero otherwise. Attachments awaiting Undo are filtered
            // out so the user perceives them as removed immediately.
            val visibleAttachments = state.attachments.filter { it.id !in hiddenAttachmentIds }
            val gallery: List<GalleryItem> = visibleAttachments.map { it.toGalleryItem() }
                .ifEmpty {
                    listOfNotNull(item.thumbnailUri ?: item.localUri).map {
                        GalleryItem(uri = it, kind = item.contentType)
                    }
                }
            // When the only "image" is a link's web preview (no real
            // attachments), tapping the hero should open the page, not a photo
            // zoom — an og:image isn't worth zooming, the destination is.
            val heroIsLinkPreview = visibleAttachments.isEmpty() && !item.url.isNullOrBlank()
            viewerIndex = viewerIndex?.takeIf { it in gallery.indices }
            viewerIndex?.let { index ->
                AttachmentViewerDialog(
                    items = gallery,
                    index = index,
                    title = item.title,
                    accent = accent,
                    onSelect = { viewerIndex = it },
                    onDismiss = { viewerIndex = null },
                    onShare = { galleryItem ->
                        if (!shareGalleryItem(ctx, galleryItem, item.title)) {
                            scope.launch { snackbarHostState.showSnackbar("Couldn't share this item") }
                        }
                    },
                    onDelete = { galleryItem ->
                        val id = galleryItem.attachmentId ?: return@AttachmentViewerDialog
                        requestAttachmentDelete(id)
                    }
                )
            }

            if (gallery.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(gallery, key = { it.uri }) { galleryItem ->
                        AttachmentPreview(
                            galleryItem = galleryItem,
                            accent = accent,
                            title = item.title,
                            onOpen = { viewerIndex = gallery.indexOf(galleryItem) },
                            onShare = {
                                if (!shareGalleryItem(ctx, galleryItem, item.title)) {
                                    scope.launch { snackbarHostState.showSnackbar("Couldn't share this item") }
                                }
                            },
                            onDelete = {
                                val id = galleryItem.attachmentId ?: return@AttachmentPreview
                                requestAttachmentDelete(id)
                            },
                            modifier = Modifier
                                .fillParentMaxWidth(0.85f)
                                .heightIn(min = 280.dp, max = 380.dp)
                        )
                    }
                }
            } else if (gallery.isNotEmpty()) {
                AttachmentPreview(
                    galleryItem = gallery.first(),
                    accent = accent,
                    title = item.title,
                    // Link preview → open the page; a real attached image → zoom.
                    onOpen = { if (heroIsLinkPreview) openOriginal() else { viewerIndex = 0 } },
                    linkPreview = heroIsLinkPreview,
                    onShare = {
                        if (!shareGalleryItem(ctx, gallery.first(), item.title)) {
                            scope.launch { snackbarHostState.showSnackbar("Couldn't share this item") }
                        }
                    },
                    onDelete = {
                        val id = gallery.first().attachmentId ?: return@AttachmentPreview
                        requestAttachmentDelete(id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp)
                        .padding(horizontal = 16.dp)
                )
            } else {
                // No preview image. For a link this placeholder is still the
                // natural "tap to open" target, so make it open the page too.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.copy(alpha = 0.18f))
                        .let { m -> if (heroIsLinkPreview) m.clickable(onClick = openOriginal) else m },
                    contentAlignment = Alignment.Center
                ) {
                    if (heroIsLinkPreview) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(source.emoji, style = MaterialTheme.typography.displayLarge)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.OpenInNew, null, tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Open link",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Text(source.emoji, style = MaterialTheme.typography.displayLarge)
                    }
                }
            }

            if (gallery.size > 1) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${gallery.size} items · swipe through, share one",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            // Body uses the same 20.dp horizontal rhythm as Home / Browse /
            // Search. Was 24.dp before — 4 px off looks fine alone but
            // visibly misaligns when the user navigates between screens.
            Column(Modifier.padding(horizontal = 20.dp)) {
                // Editable title still lives in the body so the user can
                // rename their save inline — but it's secondary now: the
                // big page title above shows the canonical name; this
                // field is a tap-to-edit affordance.
                EditableTitle(
                    initial = item.title,
                    onSave = viewModel::updateTitle
                )

                // Reminder callout. Was previously surfaced only as a
                // clock icon in the action bar — the user couldn't see
                // when they'd be nudged without re-opening the picker.
                // Tappable: jumps back into the picker for re-snooze or
                // clear.
                if (item.remindAt != null && item.remindAt > System.currentTimeMillis()) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = 0.16f))
                            .clickable { showReminderSheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AccessTime,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "We'll nudge you " + formatReminderAt(item.remindAt),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = accent
                        )
                    }
                }

                if (!item.url.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.14f))
                            .clickable(onClick = openOriginal)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.OpenInNew, null, tint = accent,
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
                Text(
                    "Collection",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.categories, key = { it.id }) { c ->
                        // Same hand-arranged feel as the Home strip — alternating
                        // tilt by sortOrder so a category leans the same way
                        // every time you see it.
                        val tilt = if (c.sortOrder % 2 == 0) -2f else 1.5f
                        CategoryChip(
                            label = c.name,
                            emoji = c.emoji,
                            color = Color(c.colorHex),
                            selected = item.categoryId == c.id,
                            tilt = tilt,
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
                    onRemove = viewModel::removeTag,
                    onOpenTag = onOpenTag
                )

                Spacer(Modifier.height(24.dp))
                NotesEditor(initial = item.notes.orEmpty(), onSave = viewModel::updateNotes)

                if (!item.ocrText.isNullOrBlank() || hasIndexableAttachments(state.attachments) || isIndexableKind(item.contentType)) {
                    Spacer(Modifier.height(24.dp))
                    OcrTextSections(
                        itemText = item.ocrText,
                        itemKind = item.contentType,
                        attachments = state.attachments,
                        autoScanEnabled = state.ocrAutoScanEnabled
                    )
                } else if (state.ocrAutoScanEnabled && isIndexingText(item.contentType, state.attachments)) {
                    Spacer(Modifier.height(24.dp))
                    IndexingTextSection()
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun MissingSaveState(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeading(
            title = "Save unavailable",
            subtitle = "It may have been deleted or moved since this reminder was created.",
            leading = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Spacer(Modifier.height(48.dp))
        KeeperMascot(size = 128.dp, pose = KeeperPose.MissingSave)
        Spacer(Modifier.height(20.dp))
        Text(
            "This save is no longer available.",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You can head back and keep browsing your shelf.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onBack) {
            Text("Back to PettiBox")
        }
    }
}

/**
 * Horizontal pill-shaped action bar that sits right under the page
 * heading. Six round icon-only targets — pin / favorite / remind /
 * archive / share / delete — that mirror the long-press QuickActionSheet
 * one-to-one. Replaces the prior Material `TopAppBar.actions` row, which
 * lived in a default M3 surface and stripped the rest of the screen's
 * hand-drawn voice.
 *
 * Active states tint with primary (or error on delete); inactive states
 * use a quiet surface so the bar reads as a tool palette rather than as
 * the page header.
 */
@Composable
private fun DetailActionBar(
    isPinned: Boolean,
    isFavorite: Boolean,
    isArchived: Boolean,
    hasReminder: Boolean,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemind: () -> Unit,
    onToggleArchive: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailActionButton(
            icon = if (isPinned) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            label = if (isPinned) "Unpin" else "Pin",
            active = isPinned,
            onClick = onTogglePin
        )
        DetailActionButton(
            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label = if (isFavorite) "Loved" else "Love it",
            active = isFavorite,
            onClick = onToggleFavorite
        )
        DetailActionButton(
            icon = Icons.Rounded.AccessTime,
            label = if (hasReminder) "Reminder set" else "Remind",
            active = hasReminder,
            onClick = onRemind
        )
        DetailActionButton(
            icon = if (isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
            label = if (isArchived) "Unarchive" else "Archive",
            active = isArchived,
            onClick = onToggleArchive
        )
        DetailActionButton(
            icon = Icons.Rounded.Share,
            label = "Share",
            active = false,
            onClick = onShare
        )
        DetailActionButton(
            icon = Icons.Rounded.Delete,
            label = "Delete",
            active = false,
            destructive = true,
            onClick = onDelete
        )
    }
}

@Composable
private fun DetailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val tint = when {
        destructive -> scheme.error
        active -> scheme.primary
        else -> scheme.onSurface
    }
    val bg = when {
        active -> scheme.primary.copy(alpha = 0.14f)
        else -> scheme.surface
    }
    val border = when {
        active -> scheme.primary.copy(alpha = 0.55f)
        else -> scheme.outline.copy(alpha = 0.5f)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
    }
}

private data class GalleryItem(
    val uri: String,
    val kind: String,
    val attachmentId: Long? = null
)

private fun AttachmentEntity.toGalleryItem(): GalleryItem =
    GalleryItem(uri = uri, kind = kind, attachmentId = id)

@Composable
private fun AttachmentViewerDialog(
    items: List<GalleryItem>,
    index: Int,
    title: String,
    accent: Color,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    onShare: (GalleryItem) -> Unit,
    onDelete: (GalleryItem) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = index.coerceIn(items.indices),
        pageCount = { items.size }
    )
    val scope = rememberCoroutineScope()
    val current = items[pagerState.currentPage.coerceIn(items.indices)]

    LaunchedEffect(pagerState.currentPage) {
        onSelect(pagerState.currentPage)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) { page ->
                AsyncImage(
                    model = items[page].uri,
                    contentDescription = "$title, item ${page + 1} of ${items.size}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close")
                }
                Text(
                    "${pagerState.currentPage + 1} of ${items.size}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onShare(current) }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share this item", tint = accent)
                }
                if (current.attachmentId != null) {
                    IconButton(onClick = {
                        onDelete(current)
                    }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete this item", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextButton(
                    enabled = pagerState.currentPage > 0,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                ) { Text("Previous") }
                TextButton(
                    enabled = pagerState.currentPage < items.lastIndex,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                ) { Text("Next") }
            }
        }
    }
}

@Composable
private fun AttachmentPreview(
    galleryItem: GalleryItem,
    accent: Color,
    title: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    // When true, this hero is a link's web preview rather than a real photo:
    // tapping opens the page, and we show an "Open link" affordance so the
    // tap target doesn't masquerade as a zoomable image.
    linkPreview: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(accent.copy(alpha = 0.12f))
    ) {
        AsyncImage(
            model = galleryItem.uri,
            contentDescription = if (linkPreview) "Open $title in browser" else title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onOpen)
        )
        if (linkPreview) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.surface.copy(alpha = 0.92f))
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Open link",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            // No Share button on a link's web preview: it would share the
            // og:image URL as an EXTRA_STREAM with a text/plain mime, which
            // most targets can't consume. The detail action bar already
            // shares the link URL correctly.
            if (!linkPreview) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surface.copy(alpha = 0.92f))
                        .size(48.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share this item", tint = scheme.primary)
                }
            }
            if (galleryItem.attachmentId != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surface.copy(alpha = 0.92f))
                        .size(48.dp)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete this item", tint = scheme.error)
                }
            }
        }
    }
}

private fun shareGalleryItem(ctx: Context, item: GalleryItem, title: String): Boolean {
    val source = Uri.parse(item.uri)
    val shareUri = when (source.scheme) {
        "file" -> {
            val file = File(source.path ?: return false)
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        }
        else -> source
    }
    val mime = mimeForKind(item.kind)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, shareUri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        clipData = ClipData.newUri(ctx.contentResolver, title, shareUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { ctx.startActivity(Intent.createChooser(intent, "Share this item")) }.isSuccess
}

private fun mimeForKind(kind: String): String = when (
    runCatching { ContentType.valueOf(kind) }.getOrDefault(ContentType.FILE)
) {
    ContentType.IMAGE -> "image/*"
    ContentType.PDF -> "application/pdf"
    ContentType.TEXT, ContentType.NOTE -> "text/plain"
    ContentType.LINK -> "text/plain"
    ContentType.FILE -> "*/*"
}

private fun isIndexingText(kind: String, attachments: List<AttachmentEntity>): Boolean {
    val type = runCatching { ContentType.valueOf(kind) }.getOrDefault(ContentType.FILE)
    return when (type) {
        ContentType.IMAGE, ContentType.PDF -> true
        else -> attachments.any {
            val attachmentType = runCatching { ContentType.valueOf(it.kind) }.getOrDefault(ContentType.FILE)
            (attachmentType == ContentType.IMAGE || attachmentType == ContentType.PDF) && it.ocrText.isNullOrBlank()
        }
    }
}

private fun isIndexableKind(kind: String): Boolean {
    val type = runCatching { ContentType.valueOf(kind) }.getOrDefault(ContentType.FILE)
    return type == ContentType.IMAGE || type == ContentType.PDF
}

private fun hasIndexableAttachments(attachments: List<AttachmentEntity>): Boolean =
    attachments.any { isIndexableKind(it.kind) }

@Composable
private fun IndexingTextSection() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            "Indexing text from this save. Search will improve when OCR finishes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class OcrDocText(
    val title: String,
    val subtitle: String,
    val text: String?,
    val status: String?
)

@Composable
private fun OcrTextSections(
    itemText: String?,
    itemKind: String,
    attachments: List<AttachmentEntity>,
    autoScanEnabled: Boolean
) {
    val attachmentDocs = attachments
        .filter { isIndexableKind(it.kind) }
        .mapIndexed { index, attachment ->
            val type = runCatching { ContentType.valueOf(attachment.kind) }.getOrDefault(ContentType.FILE)
            OcrDocText(
                title = "${type.displayLabel()} ${index + 1}",
                subtitle = if (type == ContentType.PDF) "Extracted from this PDF" else "Extracted from this image",
                text = attachment.ocrText,
                status = ocrStatusText(attachment.ocrText, autoScanEnabled, type)
            )
        }

    val docs = if (attachmentDocs.isNotEmpty()) {
        val hasAttachmentText = attachmentDocs.any { !it.text.isNullOrBlank() }
        if (hasAttachmentText) attachmentDocs else {
            attachmentDocs + OcrDocText(
                title = "Combined extracted text",
                subtitle = "Older saves may store OCR as one combined result",
                text = itemText,
                status = ocrStatusText(itemText, autoScanEnabled, runCatching { ContentType.valueOf(itemKind) }.getOrDefault(ContentType.FILE))
            )
        }
    } else {
        listOf(
            OcrDocText(
                title = "Extracted text",
                subtitle = "From this ${runCatching { ContentType.valueOf(itemKind) }.getOrDefault(ContentType.FILE).displayLabel().lowercase()}",
                text = itemText,
                status = ocrStatusText(itemText, autoScanEnabled, runCatching { ContentType.valueOf(itemKind) }.getOrDefault(ContentType.FILE))
            )
        )
    }

    Text(
        "Extracted text",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        docs.forEach { doc ->
            OcrTextSection(doc)
        }
    }
}

private fun ocrStatusText(text: String?, autoScanEnabled: Boolean, type: ContentType): String? {
    if (!text.isNullOrBlank()) return null
    if (!autoScanEnabled) return "Text recognition is off for this save."
    return if (type == ContentType.PDF) {
        "No extracted text yet. It may still be indexing, the PDF may be scanned/locked, or text may be outside the selected page limit."
    } else {
        "No extracted text yet. It may still be indexing or the image may not contain readable text."
    }
}

private fun ContentType.displayLabel(): String = when (this) {
    ContentType.IMAGE -> "Image"
    ContentType.PDF -> "PDF"
    ContentType.TEXT -> "Text"
    ContentType.NOTE -> "Note"
    ContentType.LINK -> "Link"
    ContentType.FILE -> "File"
}

@Composable
private fun OcrTextSection(doc: OcrDocText) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    doc.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    doc.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!doc.text.isNullOrBlank()) {
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(doc.text))
                        Toast.makeText(ctx, "Extracted text copied", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy all extracted text")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (doc.text.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KeeperMascot(size = 54.dp, pose = KeeperPose.Ocr)
                Spacer(Modifier.width(12.dp))
                Text(
                    doc.status.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Text(
                "Long-press to select a line or drag for multiple lines.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            SelectionContainer {
                Text(
                    doc.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 6,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (doc.text.lineSequence().count() > 6 || doc.text.length > 360) {
                androidx.compose.material3.TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show less" else "Show all")
                }
            }
        }
    }
}

/**
 * Big-headline title that's tap-to-edit. We use [androidx.compose.foundation.text.BasicTextField]
 * (no Material container) so the visual matches the original [Text] until the
 * user focuses it. Saves on focus loss - avoids spamming the DB on every keystroke.
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

    Column {
        Text(
            "Title",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Top) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = { value = it },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
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
}

@Composable
private fun TagsRow(
    tags: List<String>,
    accent: Color,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpenTag: (String) -> Unit
) {
    var input by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    Column {
        Text("Tags", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (tags.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tags, key = { it }) { tag ->
                    // The previous chip swallowed every tap into onRemove,
                    // which made tags read like buttons that destroy
                    // themselves. Splitting the body from the trailing
                    // "✕" lets a tap on the chip navigate to the tag's
                    // drill — discovering other saves with the same tag —
                    // while keeping a small explicit remove affordance.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.14f))
                    ) {
                        Text(
                            "#$tag",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            modifier = Modifier
                                .clickable { onOpenTag(tag) }
                                .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                        )
                        Text(
                            "✕",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clickable { onRemove(tag) }
                                .padding(start = 2.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                        )
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
            placeholder = { Text("Add a thought, a reminder, why you saved this...") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
