package com.savepetti.data.repository

import androidx.paging.PagingSource
import com.savepetti.data.local.AttachmentDao
import com.savepetti.data.local.AttachmentEntity
import com.savepetti.data.local.CategoryCount
import com.savepetti.data.local.CategoryDao
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.ItemTagCrossRef
import com.savepetti.data.local.SaveDao
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.data.local.SourceCount
import com.savepetti.data.local.TagDao
import com.savepetti.data.local.TagEntity
import com.savepetti.data.local.TagWithCount
import com.savepetti.data.util.AttachmentStore
import com.savepetti.domain.model.CategoryPalette
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveRepository @Inject constructor(
    private val saveDao: SaveDao,
    private val categoryDao: CategoryDao,
    private val attachmentDao: AttachmentDao,
    private val tagDao: TagDao,
    private val attachmentStore: AttachmentStore
) {

    suspend fun seedCategoriesIfEmpty() {
        val defaults = CategoryPalette.Defaults.mapIndexed { i, p ->
            CategoryEntity(
                id = p.id,
                name = p.name,
                emoji = p.emoji,
                colorHex = p.colorHex,
                sortOrder = i
            )
        }
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(defaults)
        } else {
            defaults.forEach { category ->
                val existing = categoryDao.getById(category.id)
                if (existing == null) {
                    categoryDao.upsert(category)
                } else if (!existing.userCreated) {
                    categoryDao.upsert(category.copy(createdAt = existing.createdAt))
                }
            }
        }
    }

    // ── Save items ────────────────────────────────────────────────────────

    suspend fun insert(item: SaveItemEntity): Long = saveDao.insert(item)
    suspend fun update(item: SaveItemEntity) = saveDao.update(item)
    suspend fun getById(id: Long) = saveDao.getById(id)
    fun observeById(id: Long) = saveDao.observeById(id)

    /**
     * Cleans up local files (attachments + the item's own localUri) before
     * letting the row's CASCADE fire. Without this, files in filesDir/
     * accumulate forever as users delete saves.
     */
    suspend fun delete(id: Long) {
        val uris = attachmentDao.urisForItem(id)
        saveDao.delete(id) // cascades attachments + item_tags
        attachmentStore.deleteByUris(uris)
    }

    fun observeRecent(limit: Int = 20) = saveDao.observeRecent(limit)
    fun observeFavorites() = saveDao.observeFavorites()
    fun observePinned() = saveDao.observePinned()
    suspend fun browseForSearch(): List<SaveItemEntity> = saveDao.browseForSearch()

    // Paged browses for large lists.
    fun pagedAll(): PagingSource<Int, SaveItemEntity> = saveDao.pagedAll()
    fun pagedByCategory(categoryId: String): PagingSource<Int, SaveItemEntity> =
        saveDao.pagedByCategory(categoryId)
    fun pagedBySource(sourceApp: String): PagingSource<Int, SaveItemEntity> =
        saveDao.pagedBySource(sourceApp)

    // Aggregates — never load full rows just to count.
    fun observeSourceCounts(): Flow<List<SourceCount>> = saveDao.observeSourceCounts()
    fun observeCategoryCounts(): Flow<List<CategoryCount>> = saveDao.observeCategoryCounts()
    fun observeTotal(): Flow<Int> = saveDao.observeTotal()

    suspend fun setFavorite(id: Long, fav: Boolean) = saveDao.setFavorite(id, fav)
    suspend fun setPinned(id: Long, pin: Boolean) = saveDao.setPinned(id, pin)
    suspend fun touchOpened(id: Long) = saveDao.touchOpened(id)
    suspend fun setOcrText(id: Long, text: String) = saveDao.setOcrText(id, text)

    suspend fun imageItemsNeedingOcr(): List<SaveItemEntity> = saveDao.imageItemsNeedingOcr()
    suspend fun pdfItemsNeedingOcr(): List<SaveItemEntity> = saveDao.pdfItemsNeedingOcr()

    suspend fun search(rawQuery: String): List<SaveItemEntity> {
        val q = sanitizeFtsQuery(rawQuery)
        if (q.isBlank()) return emptyList()
        return runCatching { saveDao.search(q) }.getOrDefault(emptyList())
    }

    // ── Categories ────────────────────────────────────────────────────────

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    suspend fun upsertCategory(c: CategoryEntity) = categoryDao.upsert(c)
    suspend fun getCategory(id: String) = categoryDao.getById(id)
    suspend fun deleteCategory(id: String) = categoryDao.delete(id)

    suspend fun exportBackupJson(): String {
        val root = JSONObject()
            .put("schema", 1)
            .put("exportedAt", System.currentTimeMillis())
        root.put("categories", JSONArray().apply {
            categoryDao.allForExport().forEach { c ->
                put(JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("emoji", c.emoji)
                    .put("colorHex", c.colorHex)
                    .put("sortOrder", c.sortOrder)
                    .put("parentId", c.parentId)
                    .put("userCreated", c.userCreated)
                    .put("createdAt", c.createdAt))
            }
        })
        root.put("saves", JSONArray().apply {
            saveDao.allForExport().forEach { s ->
                put(JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("url", s.url)
                    .put("localUri", s.localUri)
                    .put("thumbnailUri", s.thumbnailUri)
                    .put("contentType", s.contentType)
                    .put("sourceApp", s.sourceApp)
                    .put("categoryId", s.categoryId)
                    .put("notes", s.notes)
                    .put("ocrText", s.ocrText)
                    .put("metadataJson", s.metadataJson)
                    .put("favorite", s.isFavorite)
                    .put("pinned", s.isPinned)
                    .put("createdAt", s.createdAt)
                    .put("updatedAt", s.updatedAt)
                    .put("openedAt", s.openedAt))
            }
        })
        root.put("attachments", JSONArray().apply {
            attachmentDao.allForExport().forEach { a ->
                put(JSONObject()
                    .put("id", a.id)
                    .put("itemId", a.itemId)
                    .put("uri", a.uri)
                    .put("kind", a.kind)
                    .put("ocrText", a.ocrText)
                    .put("sortOrder", a.sortOrder)
                    .put("createdAt", a.createdAt))
            }
        })
        root.put("tags", JSONArray().apply {
            tagDao.allForExport().forEach { t ->
                put(JSONObject().put("id", t.id).put("name", t.name).put("createdAt", t.createdAt))
            }
        })
        root.put("itemTags", JSONArray().apply {
            tagDao.linksForExport().forEach { ref ->
                put(JSONObject().put("itemId", ref.itemId).put("tagId", ref.tagId))
            }
        })
        return root.toString(2)
    }

    // ── Attachments ───────────────────────────────────────────────────────

    suspend fun insertAttachments(items: List<AttachmentEntity>): List<Long> =
        if (items.isEmpty()) emptyList() else attachmentDao.insertAll(items)

    fun observeAttachments(itemId: Long) = attachmentDao.observeForItem(itemId)
    suspend fun attachmentsFor(itemId: Long) = attachmentDao.forItem(itemId)
    suspend fun imageAttachmentsNeedingOcr(): List<AttachmentEntity> =
        attachmentDao.imageAttachmentsNeedingOcr()
    suspend fun pdfAttachmentsNeedingOcr(): List<AttachmentEntity> =
        attachmentDao.pdfAttachmentsNeedingOcr()
    suspend fun setAttachmentOcr(id: Long, text: String) = attachmentDao.setOcrText(id, text)
    suspend fun deleteAttachment(id: Long) {
        val attachment = attachmentDao.getById(id) ?: return
        attachmentDao.delete(id)
        attachmentStore.deleteByUris(listOf(attachment.uri))
    }

    // ── Tags ──────────────────────────────────────────────────────────────

    suspend fun setTagsForItem(itemId: Long, tagNames: List<String>) {
        tagDao.unlinkAll(itemId)
        for (raw in tagNames) {
            val tagId = tagDao.upsert(raw)
            if (tagId > 0) tagDao.link(ItemTagCrossRef(itemId, tagId))
        }
    }

    suspend fun addTag(itemId: Long, name: String) {
        val tagId = tagDao.upsert(name)
        if (tagId > 0) tagDao.link(ItemTagCrossRef(itemId, tagId))
    }

    suspend fun removeTag(itemId: Long, name: String) {
        val id = tagDao.findIdByName(name) ?: return
        tagDao.unlink(itemId, id)
    }

    fun observeTagsForItem(itemId: Long): Flow<List<TagEntity>> =
        tagDao.observeTagsForItem(itemId)

    fun observeTopTags(limit: Int = 20): Flow<List<TagWithCount>> =
        tagDao.observeTopTags(limit)

    suspend fun itemIdsForTag(name: String): List<Long> = tagDao.itemIdsForTag(name)

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Sanitises raw user input for SQLite's FTS MATCH grammar. Strips control
     * chars, requires tokens of >=2 chars, appends a prefix wildcard so partial
     * words match. Returning a blank string short-circuits the search to an
     * empty result without ever touching the DB.
     */
    private fun sanitizeFtsQuery(input: String): String {
        val cleaned = input.trim().replace("\"", " ").replace(Regex("[^\\p{L}\\p{N} ]+"), " ")
        val tokens = cleaned.split(Regex("\\s+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "$it*" }
    }
}
