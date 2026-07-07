package com.ghostgramlabs.pettibox.data.bookmarks

/**
 * Minimal RFC 4180 CSV reader: quoted fields, `""` escapes, commas and
 * newlines inside quotes, CRLF or LF line endings, optional UTF-8 BOM.
 * Export files are at most a few megabytes even for power users, so it
 * parses from an in-memory string rather than streaming.
 */
object CsvParser {

    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false

        fun endField() {
            row.add(field.toString())
            field.clear()
        }

        fun endRow() {
            endField()
            // A trailing newline (or blank separator lines) shouldn't
            // produce phantom single-empty-cell rows.
            if (row.size > 1 || row[0].isNotBlank()) rows.add(row)
            row = mutableListOf()
        }

        var i = if (text.startsWith('\uFEFF')) 1 else 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        field.append('"'); i++
                    }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> endField()
                c == '\r' -> if (i + 1 >= text.length || text[i + 1] != '\n') endRow()
                c == '\n' -> endRow()
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}
