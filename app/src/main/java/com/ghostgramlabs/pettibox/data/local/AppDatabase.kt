package com.ghostgramlabs.pettibox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema v3 — adds is_pending_delete on save_items so the "Delete with
 * Undo" flow can hide the row from every listing during the Undo
 * window without lying about its location (it's no longer staged in
 * Archive). See [MIGRATION_2_3].
 */
@Database(
    entities = [
        SaveItemEntity::class,
        SaveItemFts::class,
        CategoryEntity::class,
        AttachmentEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun saveDao(): SaveDao
    abstract fun categoryDao(): CategoryDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun tagDao(): TagDao
}
