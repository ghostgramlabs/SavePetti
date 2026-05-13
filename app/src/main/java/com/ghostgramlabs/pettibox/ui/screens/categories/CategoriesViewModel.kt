package com.ghostgramlabs.pettibox.ui.screens.categories

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ghostgramlabs.pettibox.data.local.CategoryEntity
import com.ghostgramlabs.pettibox.data.local.SaveItemEntity
import com.ghostgramlabs.pettibox.data.reminders.ReminderWorker
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesState(
    val selectedId: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val countsByCategory: Map<String, Int> = emptyMap(),
    val showArchived: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo: SaveRepository,
    @ApplicationContext private val appContext: Context,
    handle: SavedStateHandle
) : ViewModel() {

    private val _selected = MutableStateFlow(
        handle.get<String>("cid")?.takeIf { it.isNotBlank() }
    )
    private val _showArchived = MutableStateFlow(false)

    val state: StateFlow<CategoriesState> = combine(
        _selected, repo.observeCategories(), repo.observeCategoryCounts(), _showArchived
    ) { sel, cats, counts, showArchived ->
        CategoriesState(
            selectedId = sel,
            categories = cats,
            countsByCategory = counts.associate { it.categoryId to it.count },
            showArchived = showArchived
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesState())

    /**
     * Paged stream of items for the currently-drilled-into category.
     * PagingSource is rebuilt whenever the selection OR the archived
     * toggle changes; cachedIn keeps the active page window across
     * recompositions.
     */
    val drillItems: Flow<PagingData<SaveItemEntity>> =
        combine(_selected, _showArchived) { id, archived -> id to archived }
            .flatMapLatest { (id, archived) ->
                if (id == null) flowOf(PagingData.empty())
                else Pager(
                    config = PagingConfig(
                        pageSize = 30,
                        prefetchDistance = 10,
                        initialLoadSize = 60,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = { repo.pagedByCategory(id, includeArchived = archived) }
                ).flow
            }.cachedIn(viewModelScope)

    fun select(id: String?) {
        _selected.value = id
        // Reset archived toggle when entering / leaving a collection so
        // the user never lands on an "archived" view unexpectedly.
        _showArchived.value = false
    }

    fun toggleArchivedView() {
        _showArchived.value = !_showArchived.value
    }

    fun deleteSelectedCategory() = viewModelScope.launch {
        val id = _selected.value ?: return@launch
        val category = state.value.categories.firstOrNull { it.id == id } ?: return@launch
        if (!category.userCreated) return@launch
        repo.deleteCategory(id)
        _selected.value = null
    }

    fun updateSelectedCategory(name: String, emoji: String, colorHex: Long) = viewModelScope.launch {
        val id = _selected.value ?: return@launch
        val category = state.value.categories.firstOrNull { it.id == id } ?: return@launch
        if (!category.userCreated || name.isBlank()) return@launch
        repo.upsertCategory(
            category.copy(
                name = name.trim().take(28),
                emoji = emoji,
                colorHex = colorHex
            )
        )
    }

    // ── Quick actions (long-press) ───────────────────────────────────────
    fun toggleFavorite(item: SaveItemEntity) = viewModelScope.launch {
        repo.setFavorite(item.id, !item.isFavorite)
    }
    fun togglePinned(item: SaveItemEntity) = viewModelScope.launch {
        repo.setPinned(item.id, !item.isPinned)
    }
    fun toggleArchived(item: SaveItemEntity) = viewModelScope.launch {
        repo.setArchived(item.id, !item.isArchived)
        if (!item.isArchived && item.remindAt != null) {
            repo.setRemindAt(item.id, null)
            ReminderWorker.cancel(appContext, item.id)
        }
    }
    fun setRemindAt(item: SaveItemEntity, at: Long?) = viewModelScope.launch {
        repo.setRemindAt(item.id, at)
        if (at != null) ReminderWorker.schedule(appContext, item.id, at)
        else ReminderWorker.cancel(appContext, item.id)
    }
    fun moveTo(item: SaveItemEntity, categoryId: String?) = viewModelScope.launch {
        repo.update(item.copy(categoryId = categoryId, updatedAt = System.currentTimeMillis()))
    }
    fun delete(item: SaveItemEntity) = viewModelScope.launch {
        ReminderWorker.cancel(appContext, item.id)
        repo.delete(item.id)
    }
}
