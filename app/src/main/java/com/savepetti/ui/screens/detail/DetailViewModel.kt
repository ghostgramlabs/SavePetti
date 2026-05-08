package com.savepetti.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savepetti.data.local.AttachmentEntity
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveItemEntity
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
    val attachments: List<AttachmentEntity> = emptyList()
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
        repo.observeAttachments(itemId)
    ) { item, cats, atts ->
        DetailState(
            item = item,
            category = item?.categoryId?.let { cid -> cats.firstOrNull { it.id == cid } },
            categories = cats,
            attachments = atts
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
    /**
     * Returns the deleted entity + its attachments so the caller can hold them
     * in a Snackbar "Undo" handler. Restoration is best-effort: on undo we
     * insert a fresh row (Room reassigns the id) plus its attachments. This
     * makes undo robust even after the user navigates away.
     */
    suspend fun deleteWithSnapshot(): Snapshot? {
        val it = state.value.item ?: return null
        val attachments = repo.attachmentsFor(it.id)
        repo.delete(it.id)
        return Snapshot(it, attachments)
    }

    suspend fun restore(snapshot: Snapshot) {
        val newId = repo.insertWithId(snapshot.item.copy(id = 0))
        if (snapshot.attachments.isNotEmpty()) {
            repo.insertAttachments(snapshot.attachments.map { it.copy(id = 0, itemId = newId) })
        }
    }

    data class Snapshot(
        val item: SaveItemEntity,
        val attachments: List<AttachmentEntity>
    )
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
        val current = it.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
        if (clean in current) return@launch
        val merged = (current + clean).joinToString(",")
        repo.update(it.copy(tags = merged, updatedAt = System.currentTimeMillis()))
    }

    fun removeTag(tag: String) = viewModelScope.launch {
        val it = state.value.item ?: return@launch
        val current = it.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
        val next = current.filterNot { it.equals(tag, ignoreCase = true) }
        repo.update(it.copy(tags = next.joinToString(",").ifBlank { null }, updatedAt = System.currentTimeMillis()))
    }
}
