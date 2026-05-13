package com.ghostgramlabs.pettibox.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Hot read paths return [Flow] so the UI auto-refreshes on writes; large
 * browses return [PagingSource] so we never materialize more than the visible
 * window plus a small buffer.
 */
@Dao
interface SaveDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SaveItemEntity): Long

    @Update
    suspend fun update(item: SaveItemEntity)

    @Query("DELETE FROM save_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM save_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SaveItemEntity?

    @Query("SELECT * FROM save_items WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<SaveItemEntity?>

    @Query("SELECT * FROM save_items ORDER BY created_at DESC LIMIT :limit")
    suspend fun browseForSearch(limit: Int = 200): List<SaveItemEntity>

    @Query("SELECT * FROM save_items ORDER BY created_at DESC")
    suspend fun allForExport(): List<SaveItemEntity>

    @Query(
        """
        SELECT * FROM save_items
        WHERE content_type = 'IMAGE'
            AND local_uri IS NOT NULL
            AND (ocr_text IS NULL OR ocr_text = '')
            AND NOT EXISTS (
                SELECT 1 FROM attachments
                WHERE attachments.item_id = save_items.id
            )
        ORDER BY created_at DESC
        """
    )
    suspend fun imageItemsNeedingOcr(): List<SaveItemEntity>

    @Query(
        """
        SELECT * FROM save_items
        WHERE content_type = 'PDF'
            AND local_uri IS NOT NULL
            AND (ocr_text IS NULL OR ocr_text = '')
        ORDER BY created_at DESC
        """
    )
    suspend fun pdfItemsNeedingOcr(): List<SaveItemEntity>

    // ── Hot, capped browses (Home) ───────────────────────────────────────
    // Home views hide archived items. Search (FTS + browseForSearch) still
    // returns them so "I'm done with this" doesn't make a save unfindable.

    @Query("SELECT * FROM save_items WHERE is_archived = 0 ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<SaveItemEntity>>

    @Query("SELECT * FROM save_items WHERE is_pinned = 1 AND is_archived = 0 ORDER BY updated_at DESC LIMIT :limit")
    fun observePinned(limit: Int = 12): Flow<List<SaveItemEntity>>

    @Query("SELECT * FROM save_items WHERE is_favorite = 1 AND is_archived = 0 ORDER BY created_at DESC LIMIT :limit")
    fun observeFavorites(limit: Int = 12): Flow<List<SaveItemEntity>>

    // ── Paged browses (Categories drill-in, future "all saves") ──────────

    @Query(
        """
        SELECT * FROM save_items
        WHERE category_id = :categoryId AND is_archived = :includeArchived
        ORDER BY is_pinned DESC, created_at DESC
        """
    )
    fun pagedByCategory(categoryId: String, includeArchived: Boolean = false): PagingSource<Int, SaveItemEntity>

    @Query(
        """
        SELECT * FROM save_items
        WHERE is_archived = :includeArchived
        ORDER BY is_pinned DESC, created_at DESC
        """
    )
    fun pagedAll(includeArchived: Boolean = false): PagingSource<Int, SaveItemEntity>

    @Query(
        """
        SELECT * FROM save_items
        WHERE source_app = :sourceApp AND is_archived = 0
        ORDER BY is_pinned DESC, created_at DESC
        """
    )
    fun pagedBySource(sourceApp: String): PagingSource<Int, SaveItemEntity>

    // ── Aggregate queries: avoid loading rows just to count ───────────────
    // Counts also exclude archived so the user's home dashboard reflects
    // their "live" shelf size.

    @Query(
        """
        SELECT source_app AS source, COUNT(*) AS count
        FROM save_items
        WHERE is_archived = 0
        GROUP BY source_app
        ORDER BY count DESC
        """
    )
    fun observeSourceCounts(): Flow<List<SourceCount>>

    @Query(
        """
        SELECT category_id AS categoryId, COUNT(*) AS count
        FROM save_items
        WHERE category_id IS NOT NULL AND is_archived = 0
        GROUP BY category_id
        """
    )
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM save_items WHERE is_archived = 0")
    fun observeTotal(): Flow<Int>

    // ── Reminders ─────────────────────────────────────────────────────────

    @Query("SELECT * FROM save_items WHERE remind_at IS NOT NULL AND remind_at <= :now AND is_archived = 0 ORDER BY remind_at ASC")
    suspend fun dueReminders(now: Long = System.currentTimeMillis()): List<SaveItemEntity>

    /** All items with a pending reminder, regardless of whether it has fired yet. */
    @Query("SELECT * FROM save_items WHERE remind_at IS NOT NULL AND is_archived = 0 ORDER BY remind_at ASC")
    suspend fun pendingReminders(): List<SaveItemEntity>

    // ── Mutations ────────────────────────────────────────────────────────

    @Query("UPDATE save_items SET is_favorite = :fav, updated_at = :ts WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET is_pinned = :pin, updated_at = :ts WHERE id = :id")
    suspend fun setPinned(id: Long, pin: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET is_archived = :archived, updated_at = :ts WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET remind_at = :at, updated_at = :ts WHERE id = :id")
    suspend fun setRemindAt(id: Long, at: Long?, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET opened_at = :ts WHERE id = :id")
    suspend fun touchOpened(id: Long, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET ocr_text = :text, updated_at = :ts WHERE id = :id")
    suspend fun setOcrText(id: Long, text: String, ts: Long = System.currentTimeMillis())

    // ── FTS search ───────────────────────────────────────────────────────

    @Query(
        """
        SELECT s.* FROM save_items s
        JOIN save_items_fts ON save_items_fts.rowid = s.id
        WHERE save_items_fts MATCH :query
        ORDER BY s.created_at DESC
        LIMIT 200
        """
    )
    suspend fun search(query: String): List<SaveItemEntity>
}

data class SourceCount(val source: String, val count: Int)
data class CategoryCount(val categoryId: String, val count: Int)
