package com.savepetti.ui.screens.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.data.repository.SaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val countsByCategory: Map<String, Int> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo: SaveRepository,
    handle: SavedStateHandle
) : ViewModel() {

    private val _selected = MutableStateFlow(
        handle.get<String>("cid")?.takeIf { it.isNotBlank() }
    )

    val state: StateFlow<CategoriesState> = combine(
        _selected, repo.observeCategories(), repo.observeCategoryCounts()
    ) { sel, cats, counts ->
        CategoriesState(
            selectedId = sel,
            categories = cats,
            countsByCategory = counts.associate { it.categoryId to it.count }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesState())

    /**
     * Paged stream of items for the currently-drilled-into category. PagingSource
     * is rebuilt whenever the selection changes; cachedIn keeps the active page
     * window across recompositions.
     */
    val drillItems: Flow<PagingData<SaveItemEntity>> = _selected.flatMapLatest { id ->
        if (id == null) flowOf(PagingData.empty())
        else Pager(
            config = PagingConfig(
                pageSize = 30,
                prefetchDistance = 10,
                initialLoadSize = 60,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { repo.pagedByCategory(id) }
        ).flow
    }.cachedIn(viewModelScope)

    fun select(id: String?) { _selected.value = id }

    fun deleteSelectedCategory() = viewModelScope.launch {
        val id = _selected.value ?: return@launch
        val category = state.value.categories.firstOrNull { it.id == id } ?: return@launch
        if (!category.userCreated) return@launch
        repo.deleteCategory(id)
        _selected.value = null
    }
}
