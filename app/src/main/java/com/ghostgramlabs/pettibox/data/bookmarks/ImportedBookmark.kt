package com.ghostgramlabs.pettibox.data.bookmarks

/**
 * One bookmark lifted out of another app's export file, normalized to the
 * fields PettiBox can actually keep. Every parser (Netscape HTML, CSV, plain
 * URL list) produces these; [com.ghostgramlabs.pettibox.data.repository.SaveRepository]
 * turns them into save rows.
 */
data class ImportedBookmark(
    val url: String,
    val title: String,
    /** Source folder/collection name — mapped to a collection by name on import. */
    val folder: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    /** Epoch millis the source app recorded, or null to stamp import time. */
    val createdAt: Long? = null,
    val isFavorite: Boolean = false,
    /** Pocket "archive" status maps here so read items don't flood Home. */
    val isArchived: Boolean = false
)
