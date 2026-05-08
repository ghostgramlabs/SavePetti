package com.savepetti.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "save_items",
    indices = [
        Index("category_id"),
        Index("created_at"),
        Index("source_app"),
        Index("is_favorite"),
        Index("is_pinned")
    ]
)
data class SaveItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "url") val url: String? = null,
    @ColumnInfo(name = "local_uri") val localUri: String? = null,
    @ColumnInfo(name = "thumbnail_uri") val thumbnailUri: String? = null,
    @ColumnInfo(name = "content_type") val contentType: String,
    @ColumnInfo(name = "source_app") val sourceApp: String,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "ocr_text") val ocrText: String? = null,
    @ColumnInfo(name = "tags") val tags: String? = null,
    @ColumnInfo(name = "metadata_json") val metadataJson: String? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "opened_at") val openedAt: Long? = null
)
