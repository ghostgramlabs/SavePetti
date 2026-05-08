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
    val recent: List<SaveItemEntity> = emptyList(),
    val pinned: List<SaveItemEntity> = emptyList(),
    val favorites: List<SaveItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val recentlyOpened: List<SaveItemEntity> = emptyList(),
    val sources: List<SourceCount> = emptyList(),
    val totalCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: SaveRepository
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        repo.observeRecent(20),
        repo.observePinned(),
        repo.observeFavorites(),
        repo.observeCategories(),
        repo.observeAll()
    ) { recent, pinned, favs, cats, all ->
        val sources = all.groupingBy { it.sourceApp }.eachCount()
            .entries
            .mapNotNull { (name, count) ->
                val sa = runCatching { SourceApp.valueOf(name) }.getOrNull() ?: return@mapNotNull null
                SourceCount(sa.name, sa.emoji, sa.displayName, count)
            }
            .sortedByDescending { it.count }
            .take(8)
        HomeState(
            recent = recent,
            pinned = pinned,
            favorites = favs,
            categories = cats,
            recentlyOpened = emptyList(),
            sources = sources,
            totalCount = all.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun toggleFavorite(item: SaveItemEntity) = viewModelScope.launch {
        repo.setFavorite(item.id, !item.isFavorite)
    }
}
