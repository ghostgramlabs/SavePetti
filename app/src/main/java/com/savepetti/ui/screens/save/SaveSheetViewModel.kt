package com.savepetti.ui.screens.save

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.savepetti.data.local.AttachmentEntity
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.data.metadata.MetadataFetcher
import com.savepetti.data.ocr.OcrWorker
import com.savepetti.data.ocr.PdfTextWorker
import com.savepetti.data.repository.SaveRepository
import com.savepetti.data.util.AttachmentStore
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

enum class SaveMode { NEW, PICK_EXISTING }

data class SaveSheetState(
    val mode: SaveMode = SaveMode.NEW,
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
    val recentItems: List<SaveItemEntity> = emptyList(),
    val isResolving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class SaveSheetViewModel @Inject constructor(
    private val repo: SaveRepository,
    private val metadata: MetadataFetcher,
    private val attachmentStore: AttachmentStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SaveSheetState())

    val state: StateFlow<SaveSheetState> = combine(
        _state, repo.observeCategories(), repo.observeRecent(30)
    ) { s, cats, recent ->
        s.copy(categories = cats, recentItems = recent)
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

    fun setMode(m: SaveMode) = update { it.copy(mode = m) }
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

        // Copy any foreign content URIs into our own filesDir so they survive
        // permission revocation and process death. http(s) URLs and existing
        // file:// URIs are passed through unchanged.
        val ownLocalUri = s.localUri?.let { ingestIfForeign(it) }
        val ownAttachments = s.attachments.map { ingestIfForeign(it) ?: it }

        val tagsCsv = parseTagsToCsv(s.tagsInput)
        val entity = SaveItemEntity(
            title = s.title.ifBlank { "Untitled" },
            url = s.url,
            localUri = ownLocalUri,
            thumbnailUri = s.previewImage,
            contentType = s.contentType.name,
            sourceApp = s.sourceApp.name,
            categoryId = s.selectedCategory,
            notes = s.notes.ifBlank { null },
            tags = tagsCsv,
            isFavorite = s.isFavorite
        )
        val id = repo.insert(entity)

        val attachmentRows = ownAttachments.mapIndexed { i, uri ->
            AttachmentEntity(
                itemId = id,
                uri = uri,
                kind = (if (s.contentType == ContentType.IMAGE) ContentType.IMAGE else s.contentType).name,
                sortOrder = i
            )
        }
        val attachmentIds = repo.insertAttachments(attachmentRows)

        if (s.contentType == ContentType.IMAGE) {
            if (attachmentRows.isEmpty() && !ownLocalUri.isNullOrBlank()) {
                OcrWorker.enqueueForItem(appContext, id, ownLocalUri)
            } else {
                attachmentRows.zip(attachmentIds).forEach { (row, attId) ->
                    OcrWorker.enqueueForAttachment(appContext, id, attId, row.uri)
                }
            }
        }
        if (s.contentType == ContentType.PDF && !ownLocalUri.isNullOrBlank()) {
            PdfTextWorker.enqueue(appContext, id, ownLocalUri)
        }

        _state.value = s.copy(isSaved = true)
    }

    private suspend fun ingestIfForeign(uriString: String): String? {
        return runCatching {
            val uri = Uri.parse(uriString)
            when (uri.scheme) {
                "content" -> attachmentStore.ingest(uri) ?: uriString
                else -> uriString // http, https, file — keep as-is
            }
        }.getOrNull()
    }

    /**
     * Adds the current share's content as attachments + appended note onto an
     * existing item, instead of creating a new one. Used by the "Add to
     * existing" flow on the Save sheet.
     */
    fun saveToExisting(targetItemId: Long) = viewModelScope.launch {
        val s = _state.value
        val target = repo.getById(targetItemId) ?: return@launch

        val ownLocalUri = s.localUri?.let { ingestIfForeign(it) }
        val ownAttachments = s.attachments.map { ingestIfForeign(it) ?: it }

        val urisToAttach = buildList {
            if (s.attachments.isEmpty() && !ownLocalUri.isNullOrBlank()) add(ownLocalUri)
            else addAll(ownAttachments)
        }

        val baseSort = (repo.attachmentsFor(target.id).maxOfOrNull { it.sortOrder } ?: -1) + 1
        val rows = urisToAttach.mapIndexed { i, uri ->
            AttachmentEntity(
                itemId = target.id,
                uri = uri,
                kind = (if (s.contentType == ContentType.IMAGE) ContentType.IMAGE else s.contentType).name,
                sortOrder = baseSort + i
            )
        }
        val attIds = repo.insertAttachments(rows)

        // Merge any incoming notes into the existing item.
        val mergedNotes = listOfNotNull(target.notes, s.notes.ifBlank { null }, s.url)
            .joinToString("\n\n")
            .ifBlank { null }
        if (mergedNotes != target.notes) {
            repo.update(target.copy(notes = mergedNotes, updatedAt = System.currentTimeMillis()))
        }

        if (s.contentType == ContentType.IMAGE) {
            rows.zip(attIds).forEach { (row, id) ->
                OcrWorker.enqueueForAttachment(appContext, target.id, id, row.uri)
            }
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
