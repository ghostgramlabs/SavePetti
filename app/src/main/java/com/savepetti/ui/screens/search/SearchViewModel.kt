package com.savepetti.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.data.repository.SaveRepository
import com.savepetti.domain.model.ContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val sourceFilter: String? = null,
    val categoryFilter: String? = null,
    val typeFilter: ContentType? = null,
    val tagFilter: String? = null,
    val results: List<SaveItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val knownTags: List<String> = emptyList()
)

private data class Filters(
    val source: String?,
    val category: String?,
    val type: ContentType?,
    val tag: String?
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: SaveRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _sourceFilter = MutableStateFlow<String?>(null)
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _typeFilter = MutableStateFlow<ContentType?>(null)
    private val _tagFilter = MutableStateFlow<String?>(null)

    private val filters = combine(
        _sourceFilter, _categoryFilter, _typeFilter, _tagFilter
    ) { src, cat, type, tag -> Filters(src, cat, type, tag) }

    private val ftsResults: StateFlow<List<SaveItemEntity>> = _query
        .debounce(180)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            flow { emit(if (q.isBlank()) emptyList() else repo.search(q)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<SearchState> = combine(
        _query, filters, ftsResults, repo.observeCategories(), repo.observeAll()
    ) { q, f, fts, cats, all ->
        val haveFilters = f.source != null || f.category != null || f.type != null || f.tag != null
        val basis = if (q.isBlank() && haveFilters) all else fts
        val filtered = basis.filter { item ->
            (f.source == null || item.sourceApp == f.source) &&
                (f.category == null || item.categoryId == f.category) &&
                (f.type == null || item.contentType == f.type.name) &&
                (f.tag == null || item.tags.orEmpty()
                    .split(",")
                    .map { it.trim() }
                    .any { it.equals(f.tag, ignoreCase = true) })
        }
        val knownTags = all.flatMap { it.tags.orEmpty().split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it.lowercase() }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(20)
            .map { it.key }
        SearchState(
            query = q,
            sourceFilter = f.source,
            categoryFilter = f.category,
            typeFilter = f.type,
            tagFilter = f.tag,
            results = filtered,
            categories = cats,
            knownTags = knownTags
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    fun onQuery(q: String) { _query.value = q }
    fun toggleSource(name: String) {
        _sourceFilter.value = if (_sourceFilter.value == name) null else name
    }
    fun toggleCategory(id: String) {
        _categoryFilter.value = if (_categoryFilter.value == id) null else id
    }
    fun toggleType(t: ContentType) {
        _typeFilter.value = if (_typeFilter.value == t) null else t
    }
    fun toggleTag(t: String) {
        _tagFilter.value = if (_tagFilter.value.equals(t, ignoreCase = true)) null else t
    }
    fun clearFilters() {
        _sourceFilter.value = null
        _categoryFilter.value = null
        _typeFilter.value = null
        _tagFilter.value = null
    }
}
