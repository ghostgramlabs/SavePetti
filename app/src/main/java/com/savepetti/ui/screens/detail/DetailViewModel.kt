package com.savepetti.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savepetti.data.local.AttachmentEntity
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.data.local.TagEntity
import com.savepetti.data.repository.SaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailState(
    val item: SaveItemEntity? = null,
    val category: CategoryEntity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList()
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: SaveRepository,
    handle: SavedStateHandle
) : ViewModel() {

    private val itemId: Long = handle.get<Long>("id") ?: -1L

    val state: StateFlow<DetailState> = combine(
        repo.observeById(itemId),
        repo.observeCategories(),
        repo.observeAttachments(itemId),
        repo.observeTagsForItem(itemId)
    ) { item, cats, atts, tags ->
        DetailState(
            item = item,
            category = item?.categoryId?.let { cid -> cats.firstOrNull { it.id == cid } },
            categories = cats,
            attachments = atts,
            tags = tags
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())

    init {
        viewModelScope.launch { if (itemId > 0) repo.touchOpened(itemId) }
    }

    fun toggleFavorite() = viewModelScope.launch {
        val it = state.value.item ?: return@launch
        repo.setFavorite(it.id, !it.isFavorite)
    }
    fun togglePinned() = viewModelScope.launch {
        val it = state.value.item ?: return@launch
        repo.setPinned(it.id, !it.isPinned)
    }
    fun delete() = viewModelScope.launch {
        val it = state.value.item ?: return@launch
        repo.delete(it.id)
    }
    fun setCategory(id: String?) = viewModelScope.launch {
        val it = state.value.item ?: return@launch
        repo.update(it.copy(categoryId = id, updatedAt = System.currentTimeMillis()))
    }
    fun updateNotes(notes: String) = viewModelScope.launch {
        val it = state.value.item ?: return@launch
        repo.update(it.copy(notes = notes.ifBlank { null }, updatedAt = System.currentTimeMillis()))
    }
    fun updateTitle(title: String) = viewModelScope.launch {
        val it = state.value.item ?: return@launch
        val clean = title.trim().ifBlank { "Untitled" }
        if (clean == it.title) return@launch
        repo.update(it.copy(title = clean, updatedAt = System.currentTimeMillis()))
    }

    fun addTag(tag: String) = viewModelScope.launch {
        val it = state.value.item ?: return@launch
        val clean = tag.trim().removePrefix("#")
        if (clean.isBlank() || clean.length > 24) return@launch
        repo.addTag(it.id, clean)
    }

    fun removeTag(tag: String) = viewModelScope.launch {
        val it = state.value.item ?: return@launch
        repo.removeTag(it.id, tag)
    }

    fun deleteAttachment(id: Long) = viewModelScope.launch {
        repo.deleteAttachment(id)
    }
}
