package com.savepetti.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AttachmentEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AttachmentEntity): Long

    @Query("SELECT * FROM attachments WHERE item_id = :itemId ORDER BY sort_order ASC")
    fun observeForItem(itemId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE item_id = :itemId ORDER BY sort_order ASC")
    suspend fun forItem(itemId: Long): List<AttachmentEntity>

    @Query("UPDATE attachments SET ocr_text = :text WHERE id = :id")
    suspend fun setOcrText(id: Long, text: String)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: Long)
}
