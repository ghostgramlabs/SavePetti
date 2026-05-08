package com.savepetti.ui.screens.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.data.repository.SaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategoriesState(
    val selectedId: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val items: List<SaveItemEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo: SaveRepository,
    handle: SavedStateHandle
) : ViewModel() {

    // The nav argument is named "cid" in [Routes.Categories]; an empty string
    // means "show all categories" (the grid), a non-empty string drills into
    // that specific category.
    private val _selected = MutableStateFlow(
        handle.get<String>("cid")?.takeIf { it.isNotBlank() }
    )

    private val items = _selected.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.observeByCategory(id)
    }

    val state: StateFlow<CategoriesState> = combine(
        _selected, repo.observeCategories(), items
    ) { sel, cats, list -> CategoriesState(sel, cats, list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesState())

    fun select(id: String?) { _selected.value = id }
}
