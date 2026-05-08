package com.savepetti.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tag dictionary. Names are unique case-insensitively (collated NOCASE) so
 * "Recipes", "recipes", and "RECIPES" all map to the same tag.
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE) val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * Many-to-many link between a save item and a tag. CASCADE on either side so
 * deleting an item cleans its links and deleting a tag (if we ever expose it)
 * does the same.
 */
@Entity(
    tableName = "item_tags",
    primaryKeys = ["item_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = SaveItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("item_id"),
        Index("tag_id")
    ]
)
data class ItemTagCrossRef(
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long
)
