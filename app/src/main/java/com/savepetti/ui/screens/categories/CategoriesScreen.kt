package com.savepetti.ui.screens.categories

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savepetti.data.local.CategoryEntity
import com.savepetti.ui.components.CollectionColorSeeds
import com.savepetti.ui.components.CollectionEmojiSeeds
import com.savepetti.ui.components.EmptyState
import com.savepetti.ui.components.SaveCard
import com.savepetti.ui.components.toLongHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onOpenItem: (Long) -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected = state.categories.firstOrNull { it.id == state.selectedId }
    var showDeleteCategory by remember { mutableStateOf(false) }
    var showEditCategory by remember { mutableStateOf(false) }

    if (showDeleteCategory && selected?.userCreated == true) {
        AlertDialog(
            onDismissRequest = { showDeleteCategory = false },
            title = { Text("Delete ${selected.name}?") },
            text = { Text("Saves in this collection will stay in SavePetti, but the collection will be removed.") },
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

    if (showEditCategory && selected?.userCreated == true) {
        EditCollectionDialog(
            category = selected,
            onDismiss = { showEditCategory = false },
            onSave = { name, emoji, colorHex ->
                showEditCategory = false
                viewModel.updateSelectedCategory(name, emoji, colorHex)
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selected != null) "${selected.emoji} ${selected.name}" else "Browse",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.selectedId != null) viewModel.select(null) else onBack()
                    }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (selected?.userCreated == true) {
                        IconButton(onClick = { showEditCategory = true }) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit collection"
                            )
                        }
                        IconButton(onClick = { showDeleteCategory = true }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete collection",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (state.selectedId == null) {
            CategoryGrid(
                categories = state.categories,
                counts = state.countsByCategory,
                onSelect = viewModel::select,
                modifier = Modifier.padding(padding)
            )
        } else {
            val drillItems = viewModel.drillItems.collectAsLazyPagingItems()
            val isEmpty = drillItems.itemCount == 0 &&
                drillItems.loadState.refresh is androidx.paging.LoadState.NotLoading

            if (isEmpty) {
                Column(Modifier.padding(padding).fillMaxSize()) {
                    EmptyState(
                        emoji = "\uD83D\uDCEC",
                        headline = "Nothing here yet",
                        body = "Save something into ${selected?.name ?: "this"} from the share sheet, then it'll show up.",
                        accent = selected?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 4.dp),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(padding).fillMaxSize()
                ) {
                    items(
                        count = drillItems.itemCount,
                        key = { idx -> drillItems.peek(idx)?.id ?: idx }
                    ) { idx ->
                        val item = drillItems[idx] ?: return@items
                        SaveCard(
                            item = item,
                            accent = selected?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary,
                            categoryEmoji = selected?.emoji,
                            categoryName = selected?.name,
                            onClick = { onOpenItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<CategoryEntity>,
    counts: Map<String, Int>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(categories, key = { it.id }) { c ->
            CategoryTile(
                c = c,
                count = counts[c.id] ?: 0,
                onClick = { onSelect(c.id) }
            )
        }
    }
}

@Composable
private fun CategoryTile(c: CategoryEntity, count: Int, onClick: () -> Unit) {
    val color = Color(c.colorHex)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 168.dp)
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
