package com.savepetti.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: introduced user-created collections (parentId, userCreated) and
 * the attachments table for multi-photo bundling. SaveItemFts is unchanged
 * structurally — Room's content-entity FTS keeps itself in sync, so we don't
 * need to rebuild it here.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN parentId TEXT")
        db.execSQL("ALTER TABLE categories ADD COLUMN userCreated INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS attachments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                item_id INTEGER NOT NULL,
                uri TEXT NOT NULL,
                kind TEXT NOT NULL,
                ocr_text TEXT,
                sort_order INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(item_id) REFERENCES save_items(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_item_id ON attachments(item_id)")
    }
}
