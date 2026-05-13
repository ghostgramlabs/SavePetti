package com.ghostgramlabs.pettibox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema v2 — adds is_archived + remind_at columns on save_items for the
 * archive and reminders features. See [MIGRATION_1_2] for the migration.
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
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun saveDao(): SaveDao
    abstract fun categoryDao(): CategoryDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun tagDao(): TagDao
}
