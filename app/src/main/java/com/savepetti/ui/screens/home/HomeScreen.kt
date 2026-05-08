package com.savepetti.ui.screens.home

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.data.preferences.ThemeMode
import com.savepetti.ui.components.CategoryChip
import com.savepetti.ui.components.EmptyState
import com.savepetti.ui.components.SaveCard
import com.savepetti.ui.components.SearchPill
import com.savepetti.ui.components.SectionHeader
import com.savepetti.ui.screens.save.IncomingShare
import com.savepetti.ui.screens.save.SaveSheet
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenItem: (Long) -> Unit,
    onOpenSearch: (String) -> Unit,
    onOpenSource: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenAllCategories: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showQuickNote by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

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
        if (state.isLoading) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                Greeting()
                ThemeModeSelector(themeMode, onThemeModeChange)
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
                ThemeModeSelector(themeMode, onThemeModeChange)
                FirstRunGuide(
                    onAddNote = { showQuickNote = true },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
                EmptyState(
                    emoji = "\u2728",
                    headline = "Your shelf is empty",
                    body = "Share something into SavePetti from any app - or tap below to jot a quick note.",
                    cta = "Add a quick note",
                    onCta = { showQuickNote = true },
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
                    Greeting(
                        onExport = {
                            scope.launch {
                                runCatching { viewModel.exportBackupJson() }
                                    .onSuccess { shareBackup(ctx, it) }
                                    .onFailure { Toast.makeText(ctx, "Couldn't export backup", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    )
                    ThemeModeSelector(themeMode, onThemeModeChange)
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
private fun FirstRunGuide(
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
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
                .clip(RoundedCornerShape(50))
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
private fun Greeting(onExport: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Hey there \uD83D\uDC4B",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "What's on the shelf?",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (onExport != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onExport)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Export",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val active = selected == mode
            Text(
                mode.label(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

private fun shareBackup(ctx: android.content.Context, json: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "SavePetti backup")
        putExtra(Intent.EXTRA_TEXT, json)
    }
    runCatching { ctx.startActivity(Intent.createChooser(intent, "Export SavePetti backup")) }
        .onFailure { Toast.makeText(ctx, "Couldn't export backup", Toast.LENGTH_SHORT).show() }
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
