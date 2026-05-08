package com.savepetti.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.ui.components.CategoryChip
import com.savepetti.ui.components.EmptyState
import com.savepetti.ui.components.SaveCard
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
    var showQuickNote by remember { mutableStateOf(false) }

    if (showQuickNote) {
        SaveSheet(
            incoming = IncomingShare(),
            onDismiss = { showQuickNote = false },
            onSaved = { showQuickNote = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickNote = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50)
            ) { Icon(Icons.Rounded.Add, contentDescription = "New note") }
        }
    ) { padding ->
        if (state.recent.isEmpty() && state.pinned.isEmpty() && state.favorites.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                Greeting()
                EmptyState(
                    emoji = "🪺",
                    headline = "Your shelf is empty",
                    body = "Share something into SavePetti from any app — links, screenshots, recipes, anything you want to keep for later."
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
            // Header — full-width span
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                    Greeting()
                    Spacer(Modifier.height(4.dp))
                    SearchPill(
                        onClick = { onOpenSearch("") },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
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
private fun Greeting() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            "Hey there 👋",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "What's on the shelf?",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
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
            // Alternating tilt — gives the row a hand-arranged feel.
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
