package com.savepetti.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savepetti.domain.model.ContentType
import com.savepetti.domain.model.SourceApp
import com.savepetti.ui.components.CategoryChip
import com.savepetti.ui.components.EmptyState
import com.savepetti.ui.components.SaveCard
import com.savepetti.ui.components.SearchField

@Composable
fun SearchScreen(
    initialQuery: String = "",
    initialSource: String? = null,
    onOpenItem: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(initialQuery, initialSource) {
        if (initialQuery.isNotBlank() && state.query.isBlank()) viewModel.onQuery(initialQuery)
        if (initialSource != null && state.sourceFilter == null) viewModel.toggleSource(initialSource)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Spacer(Modifier.height(8.dp))
            SearchField(
                value = state.query,
                placeholder = "Search anything you saved",
                onValueChange = viewModel::onQuery,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))

            // Three filter rows: type, then categories, then sources, then tags.
            TypeFilterRow(state.typeFilter, viewModel::toggleType)
            Spacer(Modifier.height(8.dp))
            ChipFilterRow {
                items(state.categories, key = { "fc-${it.id}" }) { c ->
                    CategoryChip(
                        label = c.name,
                        emoji = c.emoji,
                        color = Color(c.colorHex),
                        selected = state.categoryFilter == c.id,
                        onClick = { viewModel.toggleCategory(c.id) }
                    )
                }
                items(SourceApp.entries.toList(), key = { "fs-${it.name}" }) { s ->
                    CategoryChip(
                        label = s.displayName,
                        emoji = s.emoji,
                        color = MaterialTheme.colorScheme.secondary,
                        selected = state.sourceFilter == s.name,
                        onClick = { viewModel.toggleSource(s.name) }
                    )
                }
            }
            if (state.knownTags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ChipFilterRow {
                    items(state.knownTags, key = { "ft-$it" }) { t ->
                        CategoryChip(
                            label = "#$t",
                            emoji = "",
                            color = MaterialTheme.colorScheme.tertiary,
                            selected = state.tagFilter.equals(t, ignoreCase = true),
                            onClick = { viewModel.toggleTag(t) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val anyFilter = state.sourceFilter != null || state.categoryFilter != null ||
                state.typeFilter != null || state.tagFilter != null

            when {
                state.query.isBlank() && !anyFilter -> EmptyState(
                    emoji = "🔍",
                    headline = "Find anything",
                    body = "Type a word from a title, note, OCR text from a screenshot, even a recipe ingredient. We'll dig it up."
                )
                state.results.isEmpty() -> EmptyState(
                    emoji = "🤔",
                    headline = "Nothing matched",
                    body = "Try a shorter word, drop a filter, or check spelling. OCR is still indexing recent screenshots — give it a sec."
                )
                else -> {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                "${state.results.size} match${if (state.results.size == 1) "" else "es"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                            )
                        }
                        items(state.results, key = { "s-${it.id}" }) { item ->
                            val cat = state.categories.firstOrNull { it.id == item.categoryId }
                            SaveCard(
                                item = item,
                                accent = cat?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary,
                                categoryEmoji = cat?.emoji,
                                categoryName = cat?.name,
                                onClick = { onOpenItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeFilterRow(
    selected: ContentType?,
    onToggle: (ContentType) -> Unit
) {
    val items = listOf(
        Triple(ContentType.LINK, "Links", "🔗"),
        Triple(ContentType.IMAGE, "Images", "🖼"),
        Triple(ContentType.PDF, "PDFs", "📄"),
        Triple(ContentType.TEXT, "Text", "📝"),
        Triple(ContentType.NOTE, "Notes", "🗒")
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { (t, label, emoji) ->
            CategoryChip(
                label = label,
                emoji = emoji,
                color = MaterialTheme.colorScheme.primary,
                selected = selected == t,
                onClick = { onToggle(t) }
            )
        }
    }
}

@Composable
private fun ChipFilterRow(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}
