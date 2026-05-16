package com.ghostgramlabs.pettibox.ui.screens.categories

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostgramlabs.pettibox.data.local.CategoryEntity
import com.ghostgramlabs.pettibox.data.local.SaveItemEntity
import com.ghostgramlabs.pettibox.data.local.TagWithCount
import com.ghostgramlabs.pettibox.ui.components.CollectionColorSeeds
import com.ghostgramlabs.pettibox.ui.components.CollectionEmojiSeeds
import com.ghostgramlabs.pettibox.ui.components.EmptyState
import com.ghostgramlabs.pettibox.ui.components.QuickActionSheet
import com.ghostgramlabs.pettibox.ui.components.ReminderCustomSheet
import com.ghostgramlabs.pettibox.ui.components.ReminderPickerSheet
import com.ghostgramlabs.pettibox.ui.components.SaveCard
import com.ghostgramlabs.pettibox.ui.components.ScreenHeading
import com.ghostgramlabs.pettibox.ui.components.rememberNotificationPermissionRequester
import com.ghostgramlabs.pettibox.ui.components.toLongHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onOpenItem: (Long) -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val destination = state.destination
    val selectedCategory = (destination as? BrowseDestination.Category)?.let { dest ->
        state.categories.firstOrNull { it.id == dest.id }
    }
    var showDeleteCategory by remember { mutableStateOf(false) }
    var showEditCategory by remember { mutableStateOf(false) }
    // O(1) lookup for items rendered in special destinations where we
    // don't have a hand-picked accent color (Favorites, Archive, Tag drill).
    val categoriesById = remember(state.categories) {
        state.categories.associateBy { it.id }
    }

    if (showDeleteCategory && selectedCategory?.userCreated == true) {
        AlertDialog(
            onDismissRequest = { showDeleteCategory = false },
            title = { Text("Delete ${selectedCategory.name}?") },
            text = { Text("Saves in this collection will stay in PettiBox, but the collection will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteCategory = false
                    viewModel.deleteSelectedCategory()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCategory = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showEditCategory && selectedCategory?.userCreated == true) {
        EditCollectionDialog(
            category = selectedCategory,
            onDismiss = { showEditCategory = false },
            onSave = { name, emoji, colorHex ->
                showEditCategory = false
                viewModel.updateSelectedCategory(name, emoji, colorHex)
            }
        )
    }

    // Long-press quick-action plumbing for drilled-in items.
    var quickActionItem by remember { mutableStateOf<SaveItemEntity?>(null) }
    var reminderItem by remember { mutableStateOf<SaveItemEntity?>(null) }
    var customReminderItem by remember { mutableStateOf<SaveItemEntity?>(null) }
    val requestNotificationPermission = rememberNotificationPermissionRequester()

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
                if (at != null) requestNotificationPermission { viewModel.setRemindAt(item, at) }
                else viewModel.setRemindAt(item, null)
            },
            onCustom = {
                reminderItem = null
                customReminderItem = item
            },
            onDismiss = { reminderItem = null }
        )
    }
    customReminderItem?.let { item ->
        ReminderCustomSheet(
            onConfirm = { at ->
                customReminderItem = null
                requestNotificationPermission { viewModel.setRemindAt(item, at) }
            },
            onDismiss = { customReminderItem = null }
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (destination) {
                BrowseDestination.Grid -> BrowseGrid(
                    state = state,
                    onBack = onBack,
                    onSelectCategory = { id -> viewModel.navigate(BrowseDestination.Category(id)) },
                    onOpenFavorites = { viewModel.navigate(BrowseDestination.Favorites) },
                    onOpenArchive = { viewModel.navigate(BrowseDestination.Archive) },
                    onOpenTags = { viewModel.navigate(BrowseDestination.TagList) }
                )

                BrowseDestination.TagList -> TagListView(
                    tags = state.topTags,
                    onBack = { viewModel.navigate(BrowseDestination.Grid) },
                    onPickTag = { name -> viewModel.navigate(BrowseDestination.Tag(name)) }
                )

                else -> DrillView(
                    destination = destination,
                    selectedCategory = selectedCategory,
                    categoriesById = categoriesById,
                    showArchived = state.showArchived,
                    onBack = { viewModel.navigate(BrowseDestination.Grid) },
                    onToggleArchived = { viewModel.toggleArchivedView() },
                    onEditCategory = { showEditCategory = true },
                    onDeleteCategory = { showDeleteCategory = true },
                    onOpenItem = onOpenItem,
                    onLongPressItem = { quickActionItem = it },
                    drillItems = viewModel.drillItems
                )
            }
        }
    }
}

@Composable
private fun BrowseGrid(
    state: CategoriesState,
    onBack: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenTags: () -> Unit
) {
    ScreenHeading(
        title = "Browse",
        leading = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
    )
    // A staggered grid would be wrong here — the special row needs a
    // full-line span so the three pills always sit on one row and the
    // collection grid lives in its own 2-up rhythm beneath.
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Column {
                // Special collections sit above the user's stashes. These
                // are virtual destinations bound to flags rather than
                // category_id — Favorites = is_favorite, Archive =
                // is_archived (regardless of collection, so orphan
                // archived saves are reachable), Tags = the hub.
                SpecialRow(
                    items = listOf(
                        SpecialEntry(
                            icon = Icons.Rounded.Favorite,
                            label = "Favorites",
                            count = state.favoriteCount,
                            accent = Color(0xFFE85A6E),
                            onClick = onOpenFavorites
                        ),
                        SpecialEntry(
                            icon = Icons.Rounded.Archive,
                            label = "Archive",
                            count = state.archivedCount,
                            accent = Color(0xFF8B7355),
                            onClick = onOpenArchive
                        ),
                        SpecialEntry(
                            icon = Icons.Rounded.LocalOffer,
                            label = "Tags",
                            count = state.topTags.size,
                            accent = Color(0xFF5B7BC9),
                            onClick = onOpenTags
                        )
                    )
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Collections",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }
        }
        // Use the count-based overload here because the file has three
        // `items` extensions in scope (regular Lazy, LazyGrid, and
        // LazyStaggeredGrid) and Kotlin can't disambiguate a List<T>
        // overload at this call site without an explicit type ascription.
        items(
            count = state.categories.size,
            key = { idx -> state.categories[idx].id },
            contentType = { "category" }
        ) { idx ->
            val c = state.categories[idx]
            val tilt = if (c.sortOrder % 2 == 0) -1f else 1f
            CategoryTile(
                c = c,
                count = state.countsByCategory[c.id] ?: 0,
                tilt = tilt,
                onClick = { onSelectCategory(c.id) }
            )
        }
    }
}

private data class SpecialEntry(
    val icon: ImageVector,
    val label: String,
    val count: Int,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
private fun SpecialRow(items: List<SpecialEntry>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { e ->
            SpecialPill(
                entry = e,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SpecialPill(entry: SpecialEntry, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(entry.accent.copy(alpha = 0.14f))
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(entry.accent.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                entry.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            entry.label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = scheme.onSurface
        )
        Text(
            if (entry.count == 0) "Empty" else entry.count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TagListView(
    tags: List<TagWithCount>,
    onBack: () -> Unit,
    onPickTag: (String) -> Unit
) {
    ScreenHeading(
        title = "Tags",
        subtitle = "Cross-cutting labels you've added to saves",
        leading = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
    )
    if (tags.isEmpty()) {
        EmptyState(
            emoji = "🏷",
            headline = "No tags yet",
            body = "Tags are short labels (#urgent, #read-later) you can add to a save. They cut across collections — one save can have many tags. Add tags from the save sheet next time you stash something."
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(tags, key = { it.id }) { t ->
            TagRow(name = t.name, count = t.count, onClick = { onPickTag(t.name) })
        }
    }
}

@Composable
private fun TagRow(name: String, count: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.LocalOffer,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "#$name",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                "$count save${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DrillView(
    destination: BrowseDestination,
    selectedCategory: CategoryEntity?,
    categoriesById: Map<String, CategoryEntity>,
    showArchived: Boolean,
    onBack: () -> Unit,
    onToggleArchived: () -> Unit,
    onEditCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    onOpenItem: (Long) -> Unit,
    onLongPressItem: (SaveItemEntity) -> Unit,
    drillItems: kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<SaveItemEntity>>
) {
    val title = when (destination) {
        BrowseDestination.Favorites -> "❤️ Favorites"
        BrowseDestination.Archive -> "🗃 Archive"
        is BrowseDestination.Tag -> "#${destination.name}"
        is BrowseDestination.Category ->
            if (selectedCategory != null) "${selectedCategory.emoji} ${selectedCategory.name}"
            else "Collection"
        else -> ""
    }
    val subtitle = when (destination) {
        BrowseDestination.Favorites -> "Saves you marked with a heart"
        BrowseDestination.Archive -> "Everything you've tucked away"
        is BrowseDestination.Tag -> "Saves carrying this tag"
        is BrowseDestination.Category -> if (showArchived) "Archived saves" else null
        else -> null
    }
    val defaultAccent = MaterialTheme.colorScheme.primary
    val accent: Color = when (destination) {
        BrowseDestination.Favorites -> Color(0xFFE85A6E)
        BrowseDestination.Archive -> Color(0xFF8B7355)
        is BrowseDestination.Tag -> Color(0xFF5B7BC9)
        is BrowseDestination.Category -> selectedCategory?.let { Color(it.colorHex) } ?: defaultAccent
        else -> defaultAccent
    }
    ScreenHeading(
        title = title,
        subtitle = subtitle,
        leading = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        },
        trailing = {
            // Per-category archive toggle is still useful inside a single
            // collection. The first-class Archive destination covers the
            // global case.
            if (destination is BrowseDestination.Category) {
                Row {
                    IconButton(onClick = onToggleArchived) {
                        Icon(
                            if (showArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = if (showArchived) "Show active" else "Show archived",
                            tint = if (showArchived) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (selectedCategory?.userCreated == true) {
                        IconButton(onClick = onEditCategory) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit collection")
                        }
                        IconButton(onClick = onDeleteCategory) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete collection",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    )

    val items = drillItems.collectAsLazyPagingItems()
    val isEmpty = items.itemCount == 0 &&
        items.loadState.refresh is androidx.paging.LoadState.NotLoading

    if (isEmpty) {
        val (emoji, headline, body) = when (destination) {
            BrowseDestination.Favorites -> Triple(
                "❤️",
                "No favorites yet",
                "Tap the heart on a save (long-press a card → ❤️) and it'll live here."
            )
            BrowseDestination.Archive -> Triple(
                "🗃",
                "Nothing archived",
                "Archive a save (long-press → archive) to tuck it away without deleting it."
            )
            is BrowseDestination.Tag -> Triple(
                "🏷",
                "No saves with #${destination.name}",
                "Items tagged #${destination.name} will appear here."
            )
            else -> Triple(
                "📬",
                if (showArchived) "No archived saves" else "Nothing here yet",
                if (showArchived) "Archived items appear here. Toggle off to see your active saves."
                else "Save something into ${selectedCategory?.name ?: "this"} from the share sheet, then it'll show up."
            )
        }
        EmptyState(emoji = emoji, headline = headline, body = body, accent = accent)
        return
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = items.itemCount,
            key = { idx -> items.peek(idx)?.id ?: idx }
        ) { idx ->
            val item = items[idx] ?: return@items
            // In Favorites / Archive / Tag drills the per-item category
            // accent matters: items come from across collections, so a
            // single screen-wide accent would erase the visual cue. Use
            // the item's own category when available; fall back to the
            // destination accent.
            val perItemCategory = item.categoryId?.let { categoriesById[it] }
            val perItemAccent = perItemCategory?.let { Color(it.colorHex) } ?: accent
            SaveCard(
                item = item,
                accent = perItemAccent,
                categoryEmoji = perItemCategory?.emoji,
                categoryName = perItemCategory?.name,
                onClick = { onOpenItem(item.id) },
                onLongClick = { onLongPressItem(item) }
            )
        }
    }
}

@Composable
private fun CategoryTile(c: CategoryEntity, count: Int, tilt: Float, onClick: () -> Unit) {
    val color = Color(c.colorHex)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 168.dp)
            .rotate(tilt)
            .clip(RoundedCornerShape(22.dp))
            .background(color.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(color.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) { Text(c.emoji, style = MaterialTheme.typography.displaySmall) }
        Column {
            Text(
                c.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (count == 0) "Empty" else "$count item${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditCollectionDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, Long) -> Unit
) {
    var name by remember(category.id) { mutableStateOf(category.name) }
    var emoji by remember(category.id) { mutableStateOf(category.emoji) }
    var color by remember(category.id) { mutableStateOf(Color(category.colorHex)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit collection") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(28) },
                    placeholder = { Text("Collection name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
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
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name, emoji, color.toLongHex()) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
