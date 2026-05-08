package com.savepetti.ui.screens.search

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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.savepetti.domain.model.ContentType
import com.savepetti.domain.model.SourceApp
import com.savepetti.ui.components.CategoryChip
import com.savepetti.ui.components.EmptyState
import com.savepetti.ui.components.SaveCard
import com.savepetti.ui.components.SearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String = "",
    initialSource: String? = null,
    onOpenItem: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(initialQuery, initialSource) {
        if (initialQuery.isNotBlank() && state.query.isBlank()) viewModel.onQuery(initialQuery)
        if (initialSource != null && state.sourceFilter == null) viewModel.toggleSource(initialSource)
    }

    val activeFilterCount = listOfNotNull(
        state.typeFilter, state.categoryFilter, state.sourceFilter, state.tagFilter
    ).size

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            FilterSheetBody(
                state = state,
                onToggleType = viewModel::toggleType,
                onToggleCategory = viewModel::toggleCategory,
                onToggleSource = viewModel::toggleSource,
                onToggleTag = viewModel::toggleTag,
                onClear = viewModel::clearFilters,
                onClose = { showFilters = false }
            )
        }
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

            // Single compact filter row: a Filters button (with active count) +
            // a Clear chip when anything is set + a row of currently-active chips.
            FilterToolbar(
                state = state,
                activeFilterCount = activeFilterCount,
                onOpen = { showFilters = true },
                onClear = viewModel::clearFilters,
                onToggleType = viewModel::toggleType,
                onToggleCategory = viewModel::toggleCategory,
                onToggleSource = viewModel::toggleSource,
                onToggleTag = viewModel::toggleTag
            )

            Spacer(Modifier.height(8.dp))

            val anyFilter = activeFilterCount > 0
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
private fun FilterToolbar(
    state: SearchState,
    activeFilterCount: Int,
    onOpen: () -> Unit,
    onClear: () -> Unit,
    onToggleType: (ContentType) -> Unit,
    onToggleCategory: (String) -> Unit,
    onToggleSource: (String) -> Unit,
    onToggleTag: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (activeFilterCount > 0) scheme.primary.copy(alpha = 0.14f)
                    else scheme.surface
                )
                .border(
                    width = 1.5.dp,
                    color = if (activeFilterCount > 0) scheme.primary else scheme.outline,
                    shape = RoundedCornerShape(50)
                )
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription = "Open filters",
                tint = if (activeFilterCount > 0) scheme.primary else scheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (activeFilterCount > 0) "Filters · $activeFilterCount" else "Filters",
                style = MaterialTheme.typography.labelLarge,
                color = if (activeFilterCount > 0) scheme.primary else scheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (activeFilterCount > 0) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onClear) {
                Text("Clear", color = scheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.width(8.dp))

        // Compact horizontal display of currently-active chips (read-only summary,
        // tap to remove individually).
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.typeFilter?.let { t ->
                item { ActivePill("${t.label()} ✕") { onToggleType(t) } }
            }
            state.categoryFilter?.let { cid ->
                val c = state.categories.firstOrNull { it.id == cid }
                if (c != null) {
                    item { ActivePill("${c.emoji} ${c.name} ✕") { onToggleCategory(cid) } }
                }
            }
            state.sourceFilter?.let { sf ->
                val sa = runCatching { SourceApp.valueOf(sf) }.getOrNull()
                if (sa != null) {
                    item { ActivePill("${sa.emoji} ${sa.displayName} ✕") { onToggleSource(sf) } }
                }
            }
            state.tagFilter?.let { tag ->
                item { ActivePill("#$tag ✕") { onToggleTag(tag) } }
            }
        }
    }
}

@Composable
private fun ActivePill(label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(scheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FilterSheetBody(
    state: SearchState,
    onToggleType: (ContentType) -> Unit,
    onToggleCategory: (String) -> Unit,
    onToggleSource: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Filters",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Clear all") }
            TextButton(onClick = onClose) { Text("Done") }
        }

        Spacer(Modifier.height(16.dp))
        FilterGroupTitle("Type")
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(typeChoices()) { (t, label, emoji) ->
                CategoryChip(
                    label = label, emoji = emoji,
                    color = MaterialTheme.colorScheme.primary,
                    selected = state.typeFilter == t,
                    onClick = { onToggleType(t) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        FilterGroupTitle("Collection")
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.categories, key = { it.id }) { c ->
                CategoryChip(
                    label = c.name, emoji = c.emoji,
                    color = Color(c.colorHex),
                    selected = state.categoryFilter == c.id,
                    onClick = { onToggleCategory(c.id) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        FilterGroupTitle("Source")
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SourceApp.entries.toList(), key = { it.name }) { s ->
                CategoryChip(
                    label = s.displayName, emoji = s.emoji,
                    color = MaterialTheme.colorScheme.secondary,
                    selected = state.sourceFilter == s.name,
                    onClick = { onToggleSource(s.name) }
                )
            }
        }

        if (state.knownTags.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            FilterGroupTitle("Tags")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.knownTags, key = { it }) { t ->
                    CategoryChip(
                        label = "#$t",
                        emoji = null,
                        color = MaterialTheme.colorScheme.tertiary,
                        selected = state.tagFilter.equals(t, ignoreCase = true),
                        onClick = { onToggleTag(t) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterGroupTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun typeChoices() = listOf(
    Triple(ContentType.LINK, "Links", "🔗"),
    Triple(ContentType.IMAGE, "Images", "🖼"),
    Triple(ContentType.PDF, "PDFs", "📄"),
    Triple(ContentType.TEXT, "Text", "📝"),
    Triple(ContentType.NOTE, "Notes", "🗒")
)

private fun ContentType.label(): String = when (this) {
    ContentType.LINK -> "Link"
    ContentType.IMAGE -> "Image"
    ContentType.PDF -> "PDF"
    ContentType.TEXT -> "Text"
    ContentType.NOTE -> "Note"
    ContentType.FILE -> "File"
}
