package com.ghostgramlabs.pettibox.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations for the PettiBox Room database.
 *
 * v1 → v2 adds the archive flag and the reminder timestamp on save_items.
 * Indices match the ones declared on [SaveItemEntity] so Room's schema
 * validation succeeds — name format `index_<table>_<column>` is what
 * Room auto-generates when no explicit name is set on @Index.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE save_items ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE save_items ADD COLUMN remind_at INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_save_items_is_archived ON save_items(is_archived)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_save_items_remind_at ON save_items(remind_at)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
