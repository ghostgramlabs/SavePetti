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
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(
                CategoryPalette.Defaults.mapIndexed { i, p ->
                    CategoryEntity(
                        id = p.id,
                        name = p.name,
                        emoji = p.emoji,
                        colorHex = p.colorHex,
                        sortOrder = i
                    )
                }
            )
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

    suspend fun search(rawQuery: String): List<SaveItemEntity> {
        val q = sanitizeFtsQuery(rawQuery)
        if (q.isBlank()) return emptyList()
        return runCatching { saveDao.search(q) }.getOrDefault(emptyList())
    }

    // ── Categories ────────────────────────────────────────────────────────

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    suspend fun upsertCategory(c: CategoryEntity) = categoryDao.upsert(c)
    suspend fun getCategory(id: String) = categoryDao.getById(id)

    // ── Attachments ───────────────────────────────────────────────────────

    suspend fun insertAttachments(items: List<AttachmentEntity>): List<Long> =
        if (items.isEmpty()) emptyList() else attachmentDao.insertAll(items)

    fun observeAttachments(itemId: Long) = attachmentDao.observeForItem(itemId)
    suspend fun attachmentsFor(itemId: Long) = attachmentDao.forItem(itemId)
    suspend fun setAttachmentOcr(id: Long, text: String) = attachmentDao.setOcrText(id, text)

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
