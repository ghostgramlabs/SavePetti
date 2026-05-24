package com.ghostgramlabs.pettibox.data.repository

internal object SearchQuerySanitizer {
    /**
     * Sanitises raw user input for SQLite's FTS MATCH grammar. Strips control
     * chars, requires tokens of >=2 chars, appends a prefix wildcard so partial
     * words match. Returning a blank string short-circuits the search to an
     * empty result without ever touching the DB.
     */
    fun sanitizeFtsQuery(input: String): String {
        val cleaned = input.trim().replace("\"", " ").replace(Regex("[^\\p{L}\\p{N} ]+"), " ")
        val tokens = cleaned.split(Regex("\\s+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "$it*" }
    }
}
