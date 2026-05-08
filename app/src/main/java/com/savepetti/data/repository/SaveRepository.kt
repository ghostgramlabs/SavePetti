package com.savepetti.data.repository

import com.savepetti.data.local.AttachmentDao
import com.savepetti.data.local.AttachmentEntity
import com.savepetti.data.local.CategoryDao
import com.savepetti.data.local.CategoryEntity
import com.savepetti.data.local.SaveDao
import com.savepetti.data.local.SaveItemEntity
import com.savepetti.domain.model.CategoryPalette
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveRepository @Inject constructor(
    private val saveDao: SaveDao,
    private val categoryDao: CategoryDao,
    private val attachmentDao: AttachmentDao
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

    suspend fun insert(item: SaveItemEntity): Long = saveDao.insert(item)
    suspend fun update(item: SaveItemEntity) = saveDao.update(item)
    suspend fun delete(id: Long) = saveDao.delete(id)
    suspend fun getById(id: Long) = saveDao.getById(id)
    fun observeById(id: Long) = saveDao.observeById(id)

    fun observeAll(): Flow<List<SaveItemEntity>> = saveDao.observeAll()
    fun observeRecent(limit: Int = 12) = saveDao.observeRecent(limit)
    fun observeFavorites() = saveDao.observeFavorites()
    fun observePinned() = saveDao.observePinned()
    fun observeRecentlyOpened() = saveDao.observeRecentlyOpened()
    fun observeByCategory(id: String) = saveDao.observeByCategory(id)
    fun observeCount() = saveDao.observeCount()

    suspend fun setFavorite(id: Long, fav: Boolean) = saveDao.setFavorite(id, fav)
    suspend fun setPinned(id: Long, pin: Boolean) = saveDao.setPinned(id, pin)
    suspend fun touchOpened(id: Long) = saveDao.touchOpened(id)
    suspend fun setOcrText(id: Long, text: String) = saveDao.setOcrText(id, text)

    suspend fun search(rawQuery: String): List<SaveItemEntity> {
        val q = sanitizeFtsQuery(rawQuery)
        if (q.isBlank()) return emptyList()
        return runCatching { saveDao.search(q) }.getOrDefault(emptyList())
    }

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    suspend fun upsertCategory(c: CategoryEntity) = categoryDao.upsert(c)
    suspend fun getCategory(id: String) = categoryDao.getById(id)

    suspend fun insertAttachments(items: List<AttachmentEntity>) =
        if (items.isEmpty()) emptyList() else attachmentDao.insertAll(items)
    fun observeAttachments(itemId: Long) = attachmentDao.observeForItem(itemId)
    suspend fun attachmentsFor(itemId: Long) = attachmentDao.forItem(itemId)
    suspend fun setAttachmentOcr(id: Long, text: String) = attachmentDao.setOcrText(id, text)

    /**
     * FTS MATCH is picky about input. We strip control chars, split on
     * whitespace, drop tokens shorter than 2 chars, and append a prefix
     * wildcard so partial words match ("piz" → "pizza"). Quotes are stripped
     * to avoid phrase-mode footguns.
     */
    private fun sanitizeFtsQuery(input: String): String {
        val cleaned = input.trim().replace("\"", " ").replace(Regex("[^\\p{L}\\p{N} ]+"), " ")
        val tokens = cleaned.split(Regex("\\s+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "$it*" }
    }
}
