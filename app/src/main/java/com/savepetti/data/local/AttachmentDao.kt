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

    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AttachmentEntity?

    @Query("SELECT * FROM attachments ORDER BY item_id ASC, sort_order ASC")
    suspend fun allForExport(): List<AttachmentEntity>

    @Query(
        """
        SELECT * FROM attachments
        WHERE kind = 'IMAGE'
            AND (ocr_text IS NULL OR ocr_text = '')
        ORDER BY created_at DESC
        """
    )
    suspend fun imageAttachmentsNeedingOcr(): List<AttachmentEntity>

    @Query(
        """
        SELECT a.* FROM attachments a
        JOIN save_items s ON s.id = a.item_id
        WHERE a.kind = 'PDF'
            AND (s.ocr_text IS NULL OR s.ocr_text = '')
        ORDER BY a.created_at DESC
        """
    )
    suspend fun pdfAttachmentsNeedingOcr(): List<AttachmentEntity>

    /**
     * Returns the URIs of attachments belonging to an item — used pre-delete
     * so the repository can clean up the underlying files in filesDir/ once
     * the cascade fires.
     */
    @Query(
        """
        SELECT a.uri FROM attachments a WHERE a.item_id = :itemId
        UNION
        SELECT s.local_uri FROM save_items s WHERE s.id = :itemId AND s.local_uri IS NOT NULL
        """
    )
    suspend fun urisForItem(itemId: Long): List<String>

    @Query("UPDATE attachments SET ocr_text = :text WHERE id = :id")
    suspend fun setOcrText(id: Long, text: String)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: Long)
}
