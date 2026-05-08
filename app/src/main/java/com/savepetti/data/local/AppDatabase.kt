package com.savepetti.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema v1 — clean baseline. No migrations needed yet. When schema bumps
 * happen, add a real [androidx.room.migration.Migration] in [Migrations] and
 * register it in the Hilt module — never re-enable
 * `fallbackToDestructiveMigration` in production.
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
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun saveDao(): SaveDao
    abstract fun categoryDao(): CategoryDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun tagDao(): TagDao
}
