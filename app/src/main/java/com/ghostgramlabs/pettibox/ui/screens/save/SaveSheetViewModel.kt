package com.ghostgramlabs.pettibox.ui.screens.save

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.ghostgramlabs.pettibox.data.local.AttachmentEntity
import com.ghostgramlabs.pettibox.data.local.CategoryEntity
import com.ghostgramlabs.pettibox.data.local.SaveItemEntity
import com.ghostgramlabs.pettibox.data.metadata.MetadataFetcher
import com.ghostgramlabs.pettibox.data.ocr.OcrWorker
import com.ghostgramlabs.pettibox.data.ocr.PdfTextWorker
import com.ghostgramlabs.pettibox.data.preferences.OcrPreferences
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import com.ghostgramlabs.pettibox.data.util.AttachmentStore
import com.ghostgramlabs.pettibox.data.util.TextUtils
import com.ghostgramlabs.pettibox.domain.model.ContentType
import com.ghostgramlabs.pettibox.domain.model.SourceApp
import com.ghostgramlabs.pettibox.ui.components.NewCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    // Reminder picked at save-time. Null means no reminder. The picker
    // sheet writes this, and save()/saveToCategory() persists it plus
    // schedules the worker.
    val remindAt: Long? = null,
    val selectedCategory: String? = null,
    // Category id we'd suggest based on URL/title heuristics — null
    // when we have no good guess. The chip with this id renders with
    // a primary-colored border so the user notices, but is NOT
    // pre-selected (auto-saving to the wrong place is worse than no
    // suggestion at all).
    val suggestedCategory: String? = null,
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
    private val ocrPreferences: OcrPreferences,
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
            isResolving = firstUrl != null,
            suggestedCategory = suggestCategoryId(firstUrl, source, initialTitle)
        )

        if (firstUrl != null) {
            viewModelScope.launch {
                val meta = metadata.fetch(firstUrl)
                val richerTitle = meta?.title ?: _state.value.title
                _state.value = _state.value.copy(
                    title = richerTitle,
                    previewImage = meta?.imageUrl,
                    description = meta?.description,
                    isResolving = false,
                    // Re-run the heuristic with the metadata-resolved title
                    // (often more descriptive than the raw share text) so a
                    // YouTube URL whose share text was just the link still
                    // suggests "Music" once we know the page title.
                    suggestedCategory = _state.value.suggestedCategory
                        ?: suggestCategoryId(firstUrl, source, richerTitle)
                )
            }
        }
    }

    /**
     * Best-effort category suggestion based on the URL host, source app,
     * and title text. Only returns one of the seeded default category
     * ids — user-created collections aren't auto-suggested because they
     * have no semantic mapping. Returns null when the signal is too weak,
     * so the user gets a neutral chip row instead of a wrong nudge.
     */
    private fun suggestCategoryId(url: String?, source: SourceApp, title: String?): String? {
        val haystack = listOfNotNull(url, title).joinToString(" ").lowercase()
        if (haystack.isBlank()) return null
        return when {
            // Music / video — these dominate share traffic for many users
            Regex("youtube\\.com|youtu\\.be|soundcloud|spotify\\.com|music\\.apple|bandcamp")
                .containsMatchIn(haystack) -> "music"
            // Recipes — strong signal from URL or "recipe"/"ingredients" in title
            Regex("allrecipes|foodnetwork|food52|epicurious|seriouseats|smittenkitchen|nytimes\\.com/recipes|cooking\\.nytimes|bonappetit\\.com")
                .containsMatchIn(haystack) ||
                "recipe" in haystack ||
                "ingredients" in haystack -> "recipes"
            // Travel
            Regex("airbnb|booking\\.com|kayak|expedia|tripadvisor|hotels\\.com|skyscanner|lonelyplanet")
                .containsMatchIn(haystack) -> "travel"
            // Long-form reads — major article platforms + news
            Regex("medium\\.com|substack\\.com|longreads|nytimes\\.com|theguardian\\.com|wired\\.com|theatlantic|newyorker\\.com|technologyreview")
                .containsMatchIn(haystack) -> "read_later"
            // Fitness
            Regex("strava\\.com|peloton|fitbod|myfitnesspal").containsMatchIn(haystack) ||
                "workout" in haystack || "fitness " in haystack -> "fitness"
            // Finance
            Regex("bloomberg|investing\\.com|marketwatch|finance\\.yahoo|wsj\\.com")
                .containsMatchIn(haystack) -> "finance"
            else -> null
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

    /**
     * Stash-style one-tap save: picking a collection both selects it and
     * commits the save. The user opened the share sheet to save something —
     * making them pick a collection AND then hit a Save button is one tap
     * too many.
     */
    fun saveToCategory(id: String) {
        _state.value = _state.value.copy(selectedCategory = id)
        save()
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
        // Same one-tap intent: if the user just made a collection during a
        // save flow, they almost certainly want to save into it now.
        _state.value = _state.value.copy(selectedCategory = id)
        save()
    }

    private fun update(block: (SaveSheetState) -> SaveSheetState) {
        _state.value = block(_state.value)
    }

    fun setRemindAt(at: Long?) = update { it.copy(remindAt = at) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank() && s.url.isNullOrBlank() && s.localUri.isNullOrBlank()) return@launch

        // Copy any foreign content URIs into our own filesDir so they survive
        // permission revocation and process death. http(s) URLs and existing
        // file:// URIs are passed through unchanged.
        val ownLocalUri = s.localUri?.let { ingestIfForeign(it) }
        val ownAttachments = s.attachments.map { ingestIfForeign(it) ?: it }

        val entity = SaveItemEntity(
            title = s.title.ifBlank { "Untitled" },
            url = s.url,
            localUri = ownLocalUri,
            thumbnailUri = s.previewImage,
            contentType = s.contentType.name,
            sourceApp = s.sourceApp.name,
            categoryId = s.selectedCategory,
            notes = s.notes.ifBlank { null },
            isFavorite = s.isFavorite,
            remindAt = s.remindAt
        )
        val id = repo.insert(entity)
        // Schedule the reminder worker now that we have a real row id —
        // a remindAt set on the SaveSheet means the user explicitly asked
        // to be nudged about this save later. Notification permission is
        // requested at the UI layer before we ever get here.
        s.remindAt?.let { at ->
            com.ghostgramlabs.pettibox.data.reminders.ReminderScheduler.schedule(appContext, id, at)
        }
        val tagNames = parseTagInput(s.tagsInput)
        if (tagNames.isNotEmpty()) repo.setTagsForItem(id, tagNames)

        val attachmentRows = ownAttachments.mapIndexed { i, uri ->
            AttachmentEntity(
                itemId = id,
                uri = uri,
                kind = (if (s.contentType == ContentType.IMAGE) ContentType.IMAGE else s.contentType).name,
                sortOrder = i
            )
        }
        val attachmentIds = repo.insertAttachments(attachmentRows)

        if (ocrPreferences.autoScan.first()) {
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

        if (ocrPreferences.autoScan.first()) {
            if (s.contentType == ContentType.IMAGE) {
                rows.zip(attIds).forEach { (row, id) ->
                    OcrWorker.enqueueForAttachment(appContext, target.id, id, row.uri)
                }
            }
            if (s.contentType == ContentType.PDF) {
                rows.zip(attIds).forEach { (row, id) ->
                    PdfTextWorker.enqueue(appContext, target.id, row.uri, id)
                }
            }
        }

        _state.value = s.copy(isSaved = true)
    }

    private fun parseTagInput(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        return input.split(Regex("[,\\n]+"))
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() && it.length <= 24 }
            .distinct()
    }
}
