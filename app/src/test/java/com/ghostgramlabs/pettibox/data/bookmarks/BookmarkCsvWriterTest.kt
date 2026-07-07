package com.ghostgramlabs.pettibox.data.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkCsvWriterTest {

    private val awkward = ImportedBookmark(
        url = "https://food.example/pasta?a=1&b=2",
        title = "Pasta, \"the best\" recipe",
        folder = "Recipes",
        tags = listOf("cooking", "dinner"),
        notes = "line one\nline two",
        createdAt = 1_747_312_496_000L, // 2025-05-15T12:34:56Z
        isFavorite = true,
        isArchived = false
    )

    private val bare = ImportedBookmark(
        url = "https://plain.example/",
        title = "Plain",
        isArchived = true
    )

    @Test
    fun `header matches the import contract`() {
        val csv = BookmarkCsvWriter.write(emptyList())
        assertEquals("title,url,folder,tags,note,created,favorite,status", csv.lineSequence().first())
    }

    @Test
    fun `commas quotes and newlines survive a full round-trip through our own parser`() {
        val csv = BookmarkCsvWriter.write(listOf(awkward, bare))
        val parsed = BookmarkFileParser.parse(csv, "pettibox-links.csv")

        assertEquals(2, parsed.size)
        val pasta = parsed.first()
        assertEquals(awkward.url, pasta.url)
        assertEquals(awkward.title, pasta.title)
        assertEquals(awkward.folder, pasta.folder)
        assertEquals(awkward.tags, pasta.tags)
        assertEquals(awkward.notes, pasta.notes)
        assertEquals(awkward.createdAt, pasta.createdAt)
        assertTrue(pasta.isFavorite)
        assertFalse(pasta.isArchived)

        val plain = parsed[1]
        assertTrue(plain.isArchived)
        assertFalse(plain.isFavorite)
        assertEquals(null, plain.folder)
        assertTrue(plain.tags.isEmpty())
    }

    @Test
    fun `iso created stamp is utc`() {
        val csv = BookmarkCsvWriter.write(listOf(awkward))
        assertTrue(csv.contains("2025-05-15T12:34:56Z"))
    }
}
