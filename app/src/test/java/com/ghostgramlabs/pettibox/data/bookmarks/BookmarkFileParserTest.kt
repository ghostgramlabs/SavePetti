package com.ghostgramlabs.pettibox.data.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkFileParserTest {

    // ── Raindrop.io CSV ───────────────────────────────────────────────────

    private val raindropCsv = """
        id,title,note,excerpt,url,folder,tags,created,cover,highlights,favorite
        1,"Pasta, the best","my note","site excerpt",https://food.example/pasta,Recipes,"cooking, dinner",2025-05-15T12:34:56.789Z,,,true
        2,Plain link,,,https://plain.example/,,,2025-01-02T00:00:00.000Z,,,false
    """.trimIndent()

    @Test
    fun `raindrop csv maps every field`() {
        val result = BookmarkFileParser.parse(raindropCsv, "raindrop-export.csv")
        assertEquals(2, result.size)
        val pasta = result.first()
        assertEquals("Pasta, the best", pasta.title)
        assertEquals("https://food.example/pasta", pasta.url)
        assertEquals("Recipes", pasta.folder)
        assertEquals(listOf("cooking", "dinner"), pasta.tags)
        // User's own note wins over the auto-scraped excerpt.
        assertEquals("my note", pasta.notes)
        assertTrue(pasta.isFavorite)
        assertEquals(1_747_312_496_789L, pasta.createdAt)
    }

    @Test
    fun `raindrop excerpt is the fallback note`() {
        val csv = """
            url,note,excerpt
            https://a.example/,,auto summary
        """.trimIndent()
        assertEquals("auto summary", BookmarkFileParser.parse(csv, "x.csv").single().notes)
    }

    // ── Pocket CSV ────────────────────────────────────────────────────────

    @Test
    fun `pocket farewell csv maps time_added pipes and archive status`() {
        val csv = """
            title,url,time_added,tags,status
            Example,https://example.com/,1700000000,tech|news,archive
            Fresh,https://fresh.example/,1710000000,,unread
        """.trimIndent()
        val result = BookmarkFileParser.parse(csv, "part_000000.csv")
        assertEquals(2, result.size)
        val archived = result.first()
        assertEquals(1_700_000_000_000L, archived.createdAt)
        assertEquals(listOf("tech", "news"), archived.tags)
        assertTrue(archived.isArchived)
        assertFalse(result[1].isArchived)
        assertNull(result[1].folder)
    }

    // ── CSV mechanics ─────────────────────────────────────────────────────

    @Test
    fun `quoted fields survive commas newlines and escaped quotes`() {
        val csv = "url,title,note\n" +
            "https://a.example/,\"Title, with comma\",\"line one\nline two with \"\"quotes\"\"\""
        val result = BookmarkFileParser.parse(csv, "weird.csv")
        val item = result.single()
        assertEquals("Title, with comma", item.title)
        assertEquals("line one\nline two with \"quotes\"", item.notes)
    }

    @Test
    fun `rows without a usable url are dropped`() {
        val csv = """
            url,title
            ,Empty row
            notaurl,Bad row
            https://good.example/,Good row
        """.trimIndent()
        val result = BookmarkFileParser.parse(csv, "x.csv")
        assertEquals(listOf("https://good.example/"), result.map { it.url })
    }

    @Test
    fun `csv without a url column yields nothing`() {
        val csv = "name,age\nalice,30"
        assertTrue(BookmarkFileParser.parse(csv, "people.csv").isEmpty())
    }

    // ── Format detection ──────────────────────────────────────────────────

    @Test
    fun `html content is detected without a file name`() {
        val html = """<DL><DT><A HREF="https://a.example/">A</A></DL>"""
        val result = BookmarkFileParser.parse(html, fileName = null)
        assertEquals("https://a.example/", result.single().url)
    }

    @Test
    fun `csv content is detected by its header without a csv extension`() {
        val csv = "title,url\nA,https://a.example/"
        val result = BookmarkFileParser.parse(csv, "export.txt")
        assertEquals("A", result.single().title)
    }

    @Test
    fun `plain url list imports one bookmark per line`() {
        val txt = """
            https://one.example/

            https://two.example/
            not a url line
        """.trimIndent()
        val result = BookmarkFileParser.parse(txt, "bookmarks.txt")
        assertEquals(listOf("https://one.example/", "https://two.example/"), result.map { it.url })
    }

    // ── Date parsing ──────────────────────────────────────────────────────

    @Test
    fun `created accepts iso with and without millis plus date only`() {
        assertEquals(1_747_312_496_789L, BookmarkFileParser.parseCreated("2025-05-15T12:34:56.789Z"))
        assertEquals(1_747_312_496_000L, BookmarkFileParser.parseCreated("2025-05-15T12:34:56Z"))
        assertEquals(1_747_267_200_000L, BookmarkFileParser.parseCreated("2025-05-15"))
        assertNull(BookmarkFileParser.parseCreated("yesterday"))
        assertNull(BookmarkFileParser.parseCreated(""))
    }
}
