package com.savepetti.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM save_items ORDER BY is_pinned DESC, created_at DESC")
    fun observeAll(): Flow<List<SaveItemEntity>>

    @Query("SELECT * FROM save_items WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun observeFavorites(): Flow<List<SaveItemEntity>>

    @Query("SELECT * FROM save_items WHERE is_pinned = 1 ORDER BY updated_at DESC")
    fun observePinned(): Flow<List<SaveItemEntity>>

    @Query("SELECT * FROM save_items WHERE category_id = :categoryId ORDER BY created_at DESC")
    fun observeByCategory(categoryId: String): Flow<List<SaveItemEntity>>

    @Query("SELECT * FROM save_items ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 12): Flow<List<SaveItemEntity>>

    @Query("SELECT * FROM save_items WHERE opened_at IS NOT NULL ORDER BY opened_at DESC LIMIT :limit")
    fun observeRecentlyOpened(limit: Int = 8): Flow<List<SaveItemEntity>>

    @Query("UPDATE save_items SET is_favorite = :fav, updated_at = :ts WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET is_pinned = :pin, updated_at = :ts WHERE id = :id")
    suspend fun setPinned(id: Long, pin: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET opened_at = :ts WHERE id = :id")
    suspend fun touchOpened(id: Long, ts: Long = System.currentTimeMillis())

    @Query("UPDATE save_items SET ocr_text = :text, updated_at = :ts WHERE id = :id")
    suspend fun setOcrText(id: Long, text: String, ts: Long = System.currentTimeMillis())

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

    @Query("SELECT COUNT(*) FROM save_items")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM save_items WHERE category_id = :categoryId")
    fun observeCountByCategory(categoryId: String): Flow<Int>
}
