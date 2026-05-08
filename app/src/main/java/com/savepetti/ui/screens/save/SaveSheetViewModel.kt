package com.savepetti.ui.screens.save

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savepetti.data.local.AttachmentEntity
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.data.metadata.MetadataFetcher
import com.savepetti.data.ocr.OcrWorker
import com.savepetti.data.ocr.PdfTextWorker
import com.savepetti.data.repository.SaveRepository
import com.savepetti.data.util.TextUtils
import com.savepetti.domain.model.ContentType
import com.savepetti.domain.model.SourceApp
import com.savepetti.ui.components.NewCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SaveSheetState(
    val title: String = "",
    val previewImage: String? = null,
    val description: String? = null,
    val notes: String = "",
    val tagsInput: String = "",
    val sourceApp: SourceApp = SourceApp.UNKNOWN,
    val contentType: ContentType = ContentType.NOTE,
    val url: String? = null,
    val localUri: String? = null,
    val attachments: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val selectedCategory: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val isResolving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class SaveSheetViewModel @Inject constructor(
    private val repo: SaveRepository,
    private val metadata: MetadataFetcher,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SaveSheetState())

    val state: StateFlow<SaveSheetState> = combine(_state, repo.observeCategories()) { s, cats ->
        s.copy(categories = cats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _state.value)

    fun ingest(share: IncomingShare) {
        val firstUrl = share.urls.firstOrNull() ?: TextUtils.extractFirstUrl(share.text)
        val allImages = share.imageUris.map { it.toString() }
        val firstImage = allImages.firstOrNull()
        val firstFile = share.fileUris.firstOrNull()?.toString()

        val type = when {
            firstUrl != null -> ContentType.LINK
            firstImage != null -> ContentType.IMAGE
            firstFile != null -> ContentType.fromMime(share.mimeType)
            !share.text.isNullOrBlank() -> ContentType.TEXT
            else -> ContentType.NOTE
        }
        val source = SourceApp.fromUrl(firstUrl)

        val initialTitle = TextUtils.smartTitle(
            share.text,
            fallback = TextUtils.hostOf(firstUrl) ?: when (type) {
                ContentType.IMAGE -> if (allImages.size > 1) "${allImages.size} images" else "Saved image"
                ContentType.PDF -> "Saved PDF"
                ContentType.FILE -> "Saved file"
                else -> "Quick save"
            }
        )

        // Build a fresh state so a reused ViewModel (e.g. FAB → save → FAB
        // again on Home) doesn't carry over isSaved, notes, tags, etc.
        _state.value = SaveSheetState(
            title = initialTitle,
            url = firstUrl,
            localUri = firstImage ?: firstFile,
            attachments = allImages,
            sourceApp = source,
            contentType = type,
            isResolving = firstUrl != null
        )

        if (firstUrl != null) {
            viewModelScope.launch {
                val meta = metadata.fetch(firstUrl)
                _state.value = _state.value.copy(
                    title = meta?.title ?: _state.value.title,
                    previewImage = meta?.imageUrl,
                    description = meta?.description,
                    isResolving = false
                )
            }
        }
    }

    fun setTitle(t: String) = update { it.copy(title = t) }
    fun setNotes(n: String) = update { it.copy(notes = n) }
    fun setTags(t: String) = update { it.copy(tagsInput = t) }
    fun toggleFavorite() = update { it.copy(isFavorite = !it.isFavorite) }
    fun selectCategory(id: String?) = update {
        it.copy(selectedCategory = if (it.selectedCategory == id) null else id)
    }

    fun createCollection(nc: NewCollection) = viewModelScope.launch {
        val id = "user_" + UUID.randomUUID().toString().take(8)
        val maxOrder = (_state.value.categories.maxOfOrNull { it.sortOrder } ?: 0) + 1
        repo.upsertCategory(
            CategoryEntity(
                id = id,
                name = nc.name,
                emoji = nc.emoji,
                colorHex = nc.colorHex,
                sortOrder = maxOrder,
                userCreated = true
            )
        )
        update { it.copy(selectedCategory = id) }
    }

    private fun update(block: (SaveSheetState) -> SaveSheetState) {
        _state.value = block(_state.value)
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank() && s.url.isNullOrBlank() && s.localUri.isNullOrBlank()) return@launch

        val tagsCsv = parseTagsToCsv(s.tagsInput)
        val entity = SaveItemEntity(
            title = s.title.ifBlank { "Untitled" },
            url = s.url,
            localUri = s.localUri,
            thumbnailUri = s.previewImage,
            contentType = s.contentType.name,
            sourceApp = s.sourceApp.name,
            categoryId = s.selectedCategory,
            notes = s.notes.ifBlank { null },
            tags = tagsCsv,
            isFavorite = s.isFavorite
        )
        val id = repo.insert(entity)

        // Bundle multi-photo / file attachments under the same item
        val attachmentRows = s.attachments.mapIndexed { i, uri ->
            AttachmentEntity(
                itemId = id,
                uri = uri,
                kind = (if (s.contentType == ContentType.IMAGE) ContentType.IMAGE else s.contentType).name,
                sortOrder = i
            )
        }
        val attachmentIds = repo.insertAttachments(attachmentRows)

        // Fire-and-forget OCR per attachment, plus parent if it's a single-image save.
        if (s.contentType == ContentType.IMAGE) {
            if (attachmentRows.isEmpty() && !s.localUri.isNullOrBlank()) {
                OcrWorker.enqueueForItem(appContext, id, s.localUri)
            } else {
                attachmentRows.zip(attachmentIds).forEach { (row, attId) ->
                    OcrWorker.enqueueForAttachment(appContext, id, attId, row.uri)
                }
            }
        }
        if (s.contentType == ContentType.PDF && !s.localUri.isNullOrBlank()) {
            PdfTextWorker.enqueue(appContext, id, s.localUri)
        }

        _state.value = s.copy(isSaved = true)
    }

    private fun parseTagsToCsv(input: String): String? {
        if (input.isBlank()) return null
        return input.split(Regex("[,\\n]+"))
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() && it.length <= 24 }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString(",")
    }
}
