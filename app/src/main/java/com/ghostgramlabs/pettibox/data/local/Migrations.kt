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

/**
 * v2 → v3: adds is_pending_delete. Used as the staging flag for the
 * "Delete with Undo" flow so an in-flight delete is hidden from every
 * listing (including Archive). The Undo window is short (~5 s); on
 * app cold start anything still pending-delete is swept by
 * [com.ghostgramlabs.pettibox.PettiBoxApp] under the assumption that
 * the user (or a force-stop) committed to the delete.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE save_items ADD COLUMN is_pending_delete INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_save_items_is_pending_delete ON save_items(is_pending_delete)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
