package com.ghostgramlabs.pettibox.ui.screens.home

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostgramlabs.pettibox.data.local.CategoryEntity
import com.ghostgramlabs.pettibox.data.local.SaveItemEntity
import com.ghostgramlabs.pettibox.ui.components.CategoryChip
import com.ghostgramlabs.pettibox.ui.components.EmptyState
import com.ghostgramlabs.pettibox.ui.components.QuickActionSheet
import com.ghostgramlabs.pettibox.ui.components.ReminderCustomDialog
import com.ghostgramlabs.pettibox.ui.components.ReminderPickerSheet
import com.ghostgramlabs.pettibox.ui.components.SaveCard
import com.ghostgramlabs.pettibox.ui.components.ScreenHeading
import com.ghostgramlabs.pettibox.ui.components.SearchPill
import com.ghostgramlabs.pettibox.ui.components.SectionHeader
import com.ghostgramlabs.pettibox.ui.screens.save.IncomingShare
import com.ghostgramlabs.pettibox.ui.screens.save.SaveSheet

@Composable
fun HomeScreen(
    onOpenItem: (Long) -> Unit,
    onOpenSearch: (String) -> Unit,
    onOpenSource: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenAllCategories: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    // Routing state for the manual-add flow. The chooser sheet sets one of
    // these; SaveSheet renders when [pendingShare] is non-null.
    var showChooser by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var pendingShare by remember { mutableStateOf<IncomingShare?>(null) }

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            pendingShare = IncomingShare(imageUris = uris, mimeType = "image/*")
        }
    }
    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Take a persistable read grant so SaveSheet can ingest the file
            // even if the user navigates away first.
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val mime = ctx.contentResolver.getType(uri)
            pendingShare = IncomingShare(fileUris = listOf(uri), mimeType = mime)
        }
    }

    val openQuickNote = { pendingShare = IncomingShare() }

    if (state.showOnboarding && !state.isLoading) {
        HomeOnboardingDialog(
            onDismiss = viewModel::completeOnboarding,
            onAdd = {
                viewModel.completeOnboarding()
                showChooser = true
            }
        )
    }

    pendingShare?.let { share ->
        SaveSheet(
            incoming = share,
            onDismiss = { pendingShare = null },
            onSaved = { pendingShare = null }
        )
    }

    if (showChooser) {
        AddChooserSheet(
            onDismiss = { showChooser = false },
            onPickNote = {
                showChooser = false
                openQuickNote()
            },
            onPickLink = {
                showChooser = false
                showLinkDialog = true
            },
            onPickImage = {
                showChooser = false
                pickImages.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickFile = {
                showChooser = false
                pickFile.launch(arrayOf("*/*"))
            }
        )
    }

    if (showLinkDialog) {
        AddLinkDialog(
            initial = clipboardUrl(ctx).orEmpty(),
            onDismiss = { showLinkDialog = false },
            onConfirm = { url ->
                showLinkDialog = false
                pendingShare = IncomingShare(
                    text = url,
                    urls = listOf(url),
                    mimeType = "text/plain"
                )
            }
        )
    }

    // Long-press quick-action plumbing. quickActionItem is the item the
    // sheet is opened for; reminderItem routes the picker. They never
    // overlap (the sheet dismisses before opening the picker).
    var quickActionItem by remember { mutableStateOf<SaveItemEntity?>(null) }
    var reminderItem by remember { mutableStateOf<SaveItemEntity?>(null) }
    var customReminderItem by remember { mutableStateOf<SaveItemEntity?>(null) }

    quickActionItem?.let { item ->
        QuickActionSheet(
            item = item,
            categories = state.categories,
            onTogglePin = { viewModel.togglePinned(item) },
            onToggleFavorite = { viewModel.toggleFavorite(item) },
            onToggleArchive = { viewModel.toggleArchived(item) },
            onRemind = { reminderItem = item },
            onMoveTo = { id -> viewModel.moveTo(item, id) },
            onDelete = { viewModel.delete(item) },
            onDismiss = { quickActionItem = null }
        )
    }

    reminderItem?.let { item ->
        ReminderPickerSheet(
            currentRemindAt = item.remindAt,
            onPick = { at ->
                reminderItem = null
                viewModel.setRemindAt(item, at)
            },
            onCustom = {
                reminderItem = null
                customReminderItem = item
            },
            onDismiss = { reminderItem = null }
        )
    }

    customReminderItem?.let { item ->
        ReminderCustomDialog(
            onConfirm = { at ->
                viewModel.setRemindAt(item, at)
                customReminderItem = null
            },
            onDismiss = { customReminderItem = null }
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        // The NavGraph's outer Scaffold already consumed status-bar and
        // bottom-nav insets — re-applying them here is what created the
        // double-padded gap above the bottom nav and below the status bar.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        floatingActionButton = {
            if (!state.isLoading && state.totalCount > 0) {
                FloatingActionButton(
                    onClick = { showChooser = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(14.dp)
                ) { Icon(Icons.Rounded.Add, contentDescription = "Add to shelf") }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                Greeting()
                Text(
                    "Loading your shelf...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            return@Scaffold
        }

        if (state.recent.isEmpty() && state.pinned.isEmpty() && state.favorites.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                Greeting()
                FirstRunGuide(
                    onAddNote = openQuickNote,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
                EmptyState(
                    emoji = "\uD83D\uDCE5",
                    headline = "PettiBox lives in your Share menu",
                    body = "In YouTube, Instagram, Chrome, Photos, or Files, tap Share and choose PettiBox. Pick a collection and it becomes searchable.",
                    cta = "Add something now",
                    onCta = openQuickNote,
                    fillScreen = false
                )
            }
            return@Scaffold
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top = 0.dp, bottom = padding.calculateBottomPadding() + 96.dp
            ),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            // Header - full-width span
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                    Greeting()
                    Spacer(Modifier.height(4.dp))
                    SearchPill(
                        onClick = { onOpenSearch("") },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    if (state.isIndexingText) {
                        Spacer(Modifier.height(10.dp))
                        IndexingStatusChip(modifier = Modifier.padding(horizontal = 20.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    if (state.categories.isNotEmpty()) {
                        SectionHeader(
                            "Browse",
                            subtitle = "Tap a vibe to explore",
                            trailing = {
                                androidx.compose.material3.TextButton(onClick = onOpenAllCategories) {
                                    androidx.compose.material3.Text("See all")
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        CategoryStrip(
                            categories = state.categories,
                            onClickCategory = onOpenCategory
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                    if (state.sources.isNotEmpty()) {
                        SectionHeader("From", subtitle = "Where you saved it from")
                        Spacer(Modifier.height(10.dp))
                        SourceStrip(
                            sources = state.sources,
                            onClick = onOpenSource
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                    if (state.pinned.isNotEmpty()) {
                        SectionHeader("Pinned", subtitle = "Stuff you didn't want to lose")
                        Spacer(Modifier.height(8.dp))
                        // Pinned items render as a leaning "shelf" instead of
                        // joining the staggered grid: alternating tilt, varying
                        // widths, and a hand-drawn shelf line beneath. Reads
                        // like books on a shelf rather than items in a grid.
                        PinnedShelf(
                            items = state.pinned,
                            categories = state.categories,
                            onOpenItem = onOpenItem,
                            onLongPress = { quickActionItem = it }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            if (state.recent.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader(
                            "Recent saves",
                            subtitle = "Fresh from the share sheet"
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            items(state.recent, key = { "r-${it.id}" }) { item ->
                CardForItem(
                    item = item,
                    categories = state.categories,
                    onOpen = onOpenItem,
                    onLongPress = { quickActionItem = it }
                )
            }
        }
    }
}

@Composable
private fun HomeOnboardingDialog(
    onDismiss: () -> Unit,
    onAdd: () -> Unit
) {
    var page by remember { mutableStateOf(0) }
    val steps = listOf(
        OnboardingStep(
            icon = Icons.Rounded.Share,
            title = "Save from any app",
            body = "Tap Share in YouTube, Instagram, Chrome, Photos, or Files, then choose PettiBox."
        ),
        OnboardingStep(
            icon = Icons.Rounded.GridView,
            title = "Pick a collection",
            body = "Choose a collection while saving so links, screenshots, PDFs, and notes stay organized."
        ),
        OnboardingStep(
            icon = Icons.Rounded.Search,
            title = "Find it later",
            body = "Search titles, notes, tags, links, and text extracted from images or PDFs."
        )
    )
    val current = steps[page]
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(current.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        title = { Text(current.title, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Text(current.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.indices.forEach { i ->
                        Box(
                            Modifier
                                .size(width = if (i == page) 24.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == page) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (page < steps.lastIndex) page++ else onAdd()
                }
            ) {
                Text(if (page < steps.lastIndex) "Next" else "Add first save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Skip") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

private data class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val body: String
)

@Composable
private fun IndexingStatusChip(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "text-indexing")
    val dotOne by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot-one"
    )
    val dotTwo by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, delayMillis = 160),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot-two"
    )
    val dotThree by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, delayMillis = 320),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot-three"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.primary.copy(alpha = 0.11f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(dotOne, dotTwo, dotThree).forEach { alpha ->
            Box(
                Modifier
                    .padding(end = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(scheme.primary.copy(alpha = alpha))
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "Indexing saved text",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = scheme.primary
        )
    }
}

@Composable
private fun FirstRunGuide(
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surface)
            .padding(16.dp)
    ) {
        Text(
            "Save from any app",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = scheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        GuideStep(
            icon = Icons.Rounded.Share,
            title = "Tap Share",
            body = "When something is worth keeping, tap Share in the app you're using."
        )
        Spacer(Modifier.height(10.dp))
        GuideStep(
            icon = Icons.Rounded.GridView,
            title = "Choose PettiBox",
            body = "Pick a collection and it saves instantly to your shelf."
        )
        Spacer(Modifier.height(10.dp))
        GuideStep(
            icon = Icons.Rounded.Search,
            title = "Find it later",
            body = "Search links, notes, files, screenshots, PDFs, and text inside images."
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.primary)
                .clickable(onClick = onAddNote)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Add something now",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = scheme.onPrimary
            )
        }
    }
}

@Composable
private fun GuideStep(
    icon: ImageVector,
    title: String,
    body: String
) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Greeting() {
    ScreenHeading(title = "What's on the shelf?")
}

@Composable
private fun SourceStrip(
    sources: List<SourceCount>,
    onClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sources, key = { it.source }) { s ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onClick(s.source) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(s.emoji, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    s.display,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${s.count}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryStrip(
    categories: List<CategoryEntity>,
    onClickCategory: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories) { c ->
            // Alternating tilt - gives the row a hand-arranged feel.
            val tilt = if (c.sortOrder % 2 == 0) -2f else 1.5f
            CategoryChip(
                label = c.name,
                emoji = c.emoji,
                color = Color(c.colorHex),
                selected = false,
                tilt = tilt,
                onClick = { onClickCategory(c.id) }
            )
        }
    }
}

@Composable
private fun CardForItem(
    item: SaveItemEntity,
    categories: List<CategoryEntity>,
    onOpen: (Long) -> Unit,
    onLongPress: (SaveItemEntity) -> Unit = {}
) {
    val cat = categories.firstOrNull { it.id == item.categoryId }
    SaveCard(
        item = item,
        accent = cat?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary,
        categoryEmoji = cat?.emoji,
        categoryName = cat?.name,
        onClick = { onOpen(item.id) },
        onLongClick = { onLongPress(item) }
    )
}

/**
 * Pinned items rendered as a leaning shelf: a horizontally-scrolling row
 * of cards with alternating tilts, alternating top-padding (so some sit
 * lower like they slumped against the next one), varying widths, and a
 * single hand-drawn ink line beneath as the shelf board. Visually the
 * opposite of a grid — that's the point.
 */
@Composable
private fun PinnedShelf(
    items: List<SaveItemEntity>,
    categories: List<CategoryEntity>,
    onOpenItem: (Long) -> Unit,
    onLongPress: (SaveItemEntity) -> Unit = {}
) {
    val tilts = listOf(-3f, 2.5f, -1.5f, 3f, -2.5f, 1.5f)
    val widthsDp = listOf(170, 150, 175, 158, 165, 152)
    Column(Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items, key = { _, it -> "p-${it.id}" }) { idx, item ->
                val tilt = tilts[idx % tilts.size]
                val width = widthsDp[idx % widthsDp.size].dp
                // Alternating cards "slump" lower so the row reads as
                // hand-arranged, not aligned-by-grid.
                val lift = if (idx % 2 == 1) 10.dp else 0.dp
                val cat = categories.firstOrNull { it.id == item.categoryId }
                Box(
                    Modifier
                        .padding(top = lift, bottom = 0.dp)
                        .width(width)
                        .rotate(tilt)
                ) {
                    SaveCard(
                        item = item,
                        accent = cat?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary,
                        categoryEmoji = cat?.emoji,
                        categoryName = cat?.name,
                        onClick = { onOpenItem(item.id) },
                        onLongClick = { onLongPress(item) }
                    )
                }
            }
        }
        ShelfLine(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        )
    }
}

/** Wavy single-stroke ink line — the "shelf board." */
@Composable
private fun ShelfLine(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.height(8.dp)) {
        val midY = size.height / 2f
        val amp = 1.4f
        val period = 16f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, midY)
            var x = 0f
            while (x <= size.width) {
                val y = midY + amp * kotlin.math.sin(x / period * Math.PI.toFloat() * 2f)
                lineTo(x, y)
                x += 2f
            }
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddChooserSheet(
    onDismiss: () -> Unit,
    onPickNote: () -> Unit,
    onPickLink: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Add to your shelf",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick what you're saving.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            ChooserRow(
                icon = Icons.Rounded.EditNote,
                title = "Note",
                subtitle = "Jot a quick thought",
                onClick = onPickNote
            )
            Spacer(Modifier.height(8.dp))
            ChooserRow(
                icon = Icons.Rounded.Link,
                title = "Link",
                subtitle = "Paste a URL — we'll fetch the title",
                onClick = onPickLink
            )
            Spacer(Modifier.height(8.dp))
            ChooserRow(
                icon = Icons.Rounded.Image,
                title = "Picture",
                subtitle = "From your gallery",
                onClick = onPickImage
            )
            Spacer(Modifier.height(8.dp))
            ChooserRow(
                icon = Icons.Rounded.AttachFile,
                title = "File",
                subtitle = "PDF, document, anything",
                onClick = onPickFile
            )
        }
    }
}

@Composable
private fun ChooserRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddLinkDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val trimmed = text.trim()
    val isValid = trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save a link") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (initial.isNotBlank() && initial == text) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pulled from your clipboard.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed) },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

private fun clipboardUrl(ctx: Context): String? {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val text = runCatching {
        cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString()
    }.getOrNull()?.trim().orEmpty()
    if (text.isBlank()) return null
    val starts = text.startsWith("http://", ignoreCase = true) ||
        text.startsWith("https://", ignoreCase = true)
    return if (starts) text else null
}
