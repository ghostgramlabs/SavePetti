package com.ghostgramlabs.pettibox.data.bookmarks

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Writes saves out as bookmark CSV — the mirror of [BookmarkFileParser].
 * Column names are chosen so the file imports cleanly into Raindrop.io,
 * Karakeep, spreadsheets, and PettiBox itself (a full round-trip through
 * our own parser is unit-tested):
 *
 * `title,url,folder,tags,note,created,favorite,status`
 *
 * `created` is ISO 8601 UTC; `status` uses Pocket's `archive`/`unread`
 * convention; tags are comma-separated inside one quoted field.
 */
object BookmarkCsvWriter {

    fun write(bookmarks: List<ImportedBookmark>): String = buildString {
        append("title,url,folder,tags,note,created,favorite,status\r\n")
        bookmarks.forEach { b ->
            append(escape(b.title)).append(',')
            append(escape(b.url)).append(',')
            append(escape(b.folder.orEmpty())).append(',')
            append(escape(b.tags.joinToString(","))).append(',')
            append(escape(b.notes.orEmpty())).append(',')
            append(escape(b.createdAt?.let(::iso8601).orEmpty())).append(',')
            append(if (b.isFavorite) "true" else "false").append(',')
            append(if (b.isArchived) "archive" else "unread")
            append("\r\n")
        }
    }

    /** RFC 4180: quote when the value contains a comma, quote, or newline; double inner quotes. */
    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun iso8601(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(millis))
}
