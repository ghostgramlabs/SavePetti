package com.savepetti.data.local

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

    // ── Hot, capped browses (Home) ───────────────────────────────────────

    @Query("SELECT * FROM save_items ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<SaveItemEntity>>

    @Query("SELECT * FROM save_items WHERE is_pinned = 1 ORDER BY updated_at DESC LIMIT :limit")
    fun observePinned(limit: Int = 12): Flow<List<SaveItemEntity>>

    @Query("SELECT * FROM save_items WHERE is_favorite = 1 ORDER BY created_at DESC LIMIT :limit")
    fun observeFavorites(limit: Int = 12): Flow<List<SaveItemEntity>>

    // ── Paged browses (Categories drill-in, future "all saves") ──────────

    @Query(
        """
        SELECT * FROM save_items
        WHERE category_id = :categoryId
        ORDER BY is_pinned DESC, created_at DESC
        """
    )
    fun pagedByCategory(categoryId: String): PagingSource<Int, SaveItemEntity>

    @Query(
        """
        SELECT * FROM save_items
        ORDER BY is_pinned DESC, created_at DESC
        """
    )
    fun pagedAll(): PagingSource<Int, SaveItemEntity>

    @Query(
        """
        SELECT * FROM save_items
        WHERE source_app = :sourceApp
        ORDER BY is_pinned DESC, created_at DESC
        """
    )
    fun pagedBySource(sourceApp: String): PagingSource<Int, SaveItemEntity>

    // ── Aggregate queries: avoid loading rows just to count ───────────────

    @Query(
        """
        SELECT source_app AS source, COUNT(*) AS count
        FROM save_items
        GROUP BY source_app
        ORDER BY count DESC
        """
    )
    fun observeSourceCounts(): Flow<List<SourceCount>>

    @Query(
        """
        SELECT category_id AS categoryId, COUNT(*) AS count
        FROM save_items
        WHERE category_id IS NOT NULL
        GROUP BY category_id
        """
    )
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM save_items")
    fun observeTotal(): Flow<Int>

    // ── Mutations ────────────────────────────────────────────────────────

    @Query("UPDATE save_items SET is_favorite = :fav, updated_at = :ts WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET is_pinned = :pin, updated_at = :ts WHERE id = :id")
    suspend fun setPinned(id: Long, pin: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET opened_at = :ts WHERE id = :id")
    suspend fun touchOpened(id: Long, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET ocr_text = :text, updated_at = :ts WHERE id = :id")
    suspend fun setOcrText(id: Long, text: String, ts: Long = System.currentTimeMillis())

    // ── FTS search ───────────────────────────────────────────────────────

    @Query(
        """
        SELECT s.* FROM save_items s
        JOIN save_items_fts f ON f.rowid = s.id
        WHERE save_items_fts MATCH :query
        ORDER BY s.created_at DESC
        LIMIT 200
        """
    )
    suspend fun search(query: String): List<SaveItemEntity>
}

data class SourceCount(val source: String, val count: Int)
data class CategoryCount(val categoryId: String, val count: Int)
