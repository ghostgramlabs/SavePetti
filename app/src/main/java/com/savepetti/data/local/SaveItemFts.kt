package com.savepetti.data.local

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS index for the user-visible text fields on a save. Tags are NOT indexed
 * here — they're a separate normalized table joined at query time, which is
 * faster and lets us avoid LIKE %x% scans for tag filtering.
 */
@Entity(tableName = "save_items_fts")
@Fts4(contentEntity = SaveItemEntity::class)
data class SaveItemFts(
    val title: String,
    val url: String?,
    val notes: String?,
    val ocr_text: String?,
    val source_app: String
)
