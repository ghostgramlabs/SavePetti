package com.savepetti.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.ui.components.CategoryChip
import com.savepetti.ui.components.EmptyState
import com.savepetti.ui.components.SaveCard
import com.savepetti.ui.components.ScreenHeading
import com.savepetti.ui.components.SearchPill
import com.savepetti.ui.components.SectionHeader
import com.savepetti.ui.screens.save.IncomingShare
import com.savepetti.ui.screens.save.SaveSheet

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

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        // The NavGraph's outer Scaffold already consumed status-bar and
        // bottom-nav insets — re-applying them here is what created the
        // double-padded gap above the bottom nav and below the status bar.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showChooser = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(14.dp)
            ) { Icon(Icons.Rounded.Add, contentDescription = "Add to shelf") }
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
                    emoji = "\u2728",
                    headline = "Your shelf is empty",
                    body = "Share something into SavePetti from any app - or tap below to jot a quick note.",
                    cta = "Add a quick note",
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
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            items(state.pinned, key = { "p-${it.id}" }) { item ->
                // Pinned items already carry a tape-strip overlay from
                // SaveCard. Adding a wrapper rotation on top of that was
                // signal piled on signal — cards either side of one
                // another would visually clash. Tape alone is enough.
                CardForItem(item, state.categories, onOpenItem)
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
                CardForItem(item, state.categories, onOpenItem)
            }
        }
    }
}

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
            "SavePetti in 3 steps",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = scheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        GuideStep(
            icon = Icons.Rounded.Share,
            title = "Share into SavePetti",
            body = "Links, screenshots, PDFs, text, and quick notes land here."
        )
        Spacer(Modifier.height(10.dp))
        GuideStep(
            icon = Icons.Rounded.Search,
            title = "Search everything",
            body = "Titles, notes, sources, and OCR text are searchable."
        )
        Spacer(Modifier.height(10.dp))
        GuideStep(
            icon = Icons.Rounded.GridView,
            title = "Browse by collection",
            body = "Use categories to keep saved things easy to find later."
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
                "Try a quick note",
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
    onOpen: (Long) -> Unit
) {
    val cat = categories.firstOrNull { it.id == item.categoryId }
    SaveCard(
        item = item,
        accent = cat?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary,
        categoryEmoji = cat?.emoji,
        categoryName = cat?.name,
        onClick = { onOpen(item.id) }
    )
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
