package com.ghostgramlabs.pettibox.data.bookmarks

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Entry point for "bring my bookmarks from another app". Sniffs the format
 * and dispatches:
 *
 * - **Netscape HTML** — Chrome/Firefox/Edge/Safari browser exports, plus
 *   Raindrop.io, Instapaper, Pinboard, Linkwarden, Karakeep.
 * - **CSV** — Raindrop.io (`id,title,note,excerpt,url,folder,tags,created,…`),
 *   Pocket's farewell export (`title,url,time_added,tags,status`), and any
 *   spreadsheet with a recognizable URL column.
 * - **Plain text** — one URL per line (Raindrop TXT export).
 *
 * Detection is content-first with the file name as a tiebreaker, because SAF
 * pickers routinely hand over generic MIME types and renamed files.
 */
object BookmarkFileParser {

    fun parse(content: String, fileName: String? = null): List<ImportedBookmark> {
        val name = fileName.orEmpty().lowercase()
        val head = content.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        return when {
            head.startsWith("<") || name.endsWith(".html") || name.endsWith(".htm") ->
                NetscapeBookmarkParser.parse(content)
            name.endsWith(".csv") || looksLikeBookmarkCsv(head) ->
                parseCsv(content)
            else -> parsePlainUrlList(content)
        }
    }

    // ── CSV ───────────────────────────────────────────────────────────────

    private val urlColumns = setOf("url", "href", "link")
    private val titleColumns = setOf("title", "name")
    private val folderColumns = setOf("folder", "collection", "category")
    private val noteColumns = setOf("note", "notes", "description")
    private val createdColumns = setOf(
        "created", "time_added", "created_at", "date_added", "add_date", "date"
    )
    private val favoriteColumns = setOf("favorite", "favourite", "starred")

    private fun looksLikeBookmarkCsv(head: String): Boolean {
        val firstLine = head.lineSequence().firstOrNull() ?: return false
        if (',' !in firstLine) return false
        val cells = firstLine.split(',').map { it.trim().trim('"').lowercase() }
        return cells.any { it in urlColumns }
    }

    private fun parseCsv(content: String): List<ImportedBookmark> {
        val rows = CsvParser.parse(content)
        if (rows.size < 2) return emptyList()
        val header = rows.first().map { it.trim().lowercase() }
        fun col(names: Set<String>): Int = header.indexOfFirst { it in names }

        val urlIdx = col(urlColumns)
        if (urlIdx < 0) return emptyList()
        val titleIdx = col(titleColumns)
        val folderIdx = col(folderColumns)
        val tagsIdx = col(setOf("tags"))
        val noteIdx = col(noteColumns)
        val excerptIdx = col(setOf("excerpt"))
        val createdIdx = col(createdColumns)
        val favoriteIdx = col(favoriteColumns)
        val statusIdx = col(setOf("status"))

        return rows.drop(1).mapNotNull { row ->
            fun cell(idx: Int): String =
                if (idx >= 0) row.getOrNull(idx)?.trim().orEmpty() else ""

            val url = cell(urlIdx)
            if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
                return@mapNotNull null
            }
            // Raindrop puts the user's own note in `note` and the page's
            // auto-scraped summary in `excerpt` — prefer what the user wrote.
            val notes = cell(noteIdx).ifBlank { cell(excerptIdx) }
            ImportedBookmark(
                url = url,
                title = cell(titleIdx).ifBlank { url },
                folder = cell(folderIdx).ifBlank { null },
                tags = splitTags(cell(tagsIdx)),
                notes = notes.ifBlank { null },
                createdAt = parseCreated(cell(createdIdx)),
                isFavorite = cell(favoriteIdx).lowercase() in setOf("true", "1", "yes"),
                isArchived = cell(statusIdx).lowercase() in setOf("archive", "archived")
            )
        }
    }

    /** Raindrop separates tags with commas, Pocket with pipes — accept both. */
    private fun splitTags(raw: String): List<String> =
        raw.split(',', '|', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    /**
     * Created stamps arrive as epoch seconds (Pocket), epoch millis, or
     * ISO 8601 (Raindrop, e.g. `2025-05-15T12:34:56.789Z`). SimpleDateFormat
     * instead of java.time because minSdk 24 has no java.time without
     * desugaring.
     */
    internal fun parseCreated(raw: String): Long? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        if (value.all { it.isDigit() }) return NetscapeBookmarkParser.parseEpoch(value)
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)
            }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return null
    }

    // ── Plain URL list ────────────────────────────────────────────────────

    private fun parsePlainUrlList(content: String): List<ImportedBookmark> =
        content.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("http://", true) || it.startsWith("https://", true) }
            .map { ImportedBookmark(url = it, title = it) }
            .toList()
}
