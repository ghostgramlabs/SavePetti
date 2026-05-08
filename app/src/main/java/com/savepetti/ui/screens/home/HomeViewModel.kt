package com.savepetti.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.data.repository.SaveRepository
import com.savepetti.domain.model.SourceApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceCount(val source: String, val emoji: String, val display: String, val count: Int)

data class HomeState(
    val isLoading: Boolean = true,
    val recent: List<SaveItemEntity> = emptyList(),
    val pinned: List<SaveItemEntity> = emptyList(),
    val favorites: List<SaveItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val sources: List<SourceCount> = emptyList(),
    val totalCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: SaveRepository
) : ViewModel() {

    /**
     * Each component flow is bounded — recent/pinned/favorites are LIMITed in
     * SQL, sources comes from a GROUP BY aggregate, total is a COUNT(*). The
     * home screen never loads the full row set, even at 100k saves.
     */
    val state: StateFlow<HomeState> = combine(
        repo.observeRecent(20),
        repo.observePinned(),
        repo.observeFavorites(),
        repo.observeCategories(),
        repo.observeSourceCounts(),
        repo.observeTotal()
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val recent = args[0] as List<SaveItemEntity>
        val pinned = args[1] as List<SaveItemEntity>
        val favs = args[2] as List<SaveItemEntity>
        val cats = args[3] as List<CategoryEntity>
        val rawCounts = args[4] as List<com.savepetti.data.local.SourceCount>
        val total = args[5] as Int

        val sources = rawCounts.mapNotNull { sc ->
            val sa = runCatching { SourceApp.valueOf(sc.source) }.getOrNull() ?: return@mapNotNull null
            SourceCount(sa.name, sa.emoji, sa.displayName, sc.count)
        }.take(8)

        HomeState(
            isLoading = false,
            recent = recent,
            pinned = pinned,
            favorites = favs,
            categories = cats,
            sources = sources,
            totalCount = total
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun toggleFavorite(item: SaveItemEntity) = viewModelScope.launch {
        repo.setFavorite(item.id, !item.isFavorite)
    }

    suspend fun exportBackupJson(): String = repo.exportBackupJson()
}
