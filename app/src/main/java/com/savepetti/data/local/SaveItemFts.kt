package com.savepetti.data.local

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "save_items_fts")
@Fts4(contentEntity = SaveItemEntity::class)
data class SaveItemFts(
    val title: String,
    val url: String?,
    val notes: String?,
    val ocr_text: String?,
    val tags: String?,
    val source_app: String
)
