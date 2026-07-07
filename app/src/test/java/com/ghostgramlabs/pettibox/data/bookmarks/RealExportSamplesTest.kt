package com.ghostgramlabs.pettibox.data.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses genuine export files instead of synthetic snippets:
 *
 * - `pocket_ril_sample_export.html` / `pocket_sample_flat.html` — real
 *   Pocket ("Read It Later") HTML exports, fetched from the
 *   llbbl/raindrop-import-scripts repo on GitHub.
 * - `raindrop_export_sample.csv` — built to Raindrop.io's documented
 *   export schema (`id,title,note,excerpt,url,folder,tags,created,cover,
 *   highlights,favorite`, ISO 8601 dates, `Parent/Child` nested folders).
 */
class RealExportSamplesTest {

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("bookmarks/$name")) {
            "missing test resource $name"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `real pocket ril export parses its single unread link`() {
        val result = BookmarkFileParser.parse(resource("pocket_ril_sample_export.html"), "ril_export.html")
        assertEquals(1, result.size)
        val item = result.single()
        assertEquals("http://getpocket.com/example", item.url)
        // Pocket used the URL as the anchor text for untitled saves.
        assertEquals("http://getpocket.com/example", item.title)
        assertEquals(1_389_234_760_000L, item.createdAt)
        assertTrue(item.tags.isEmpty())
        assertNull(item.folder)
    }

    @Test
    fun `flat pocket sample keeps time_added and tags per link`() {
        val result = BookmarkFileParser.parse(resource("pocket_sample_flat.html"), "pocket.html")
        assertEquals(3, result.size)
        assertEquals(listOf("sample", "test"), result[0].tags)
        assertEquals(1_609_459_200_000L, result[0].createdAt)
        assertEquals("Sample Article 2", result[1].title)
        assertTrue(result[2].tags.isEmpty())
    }

    @Test
    fun `raindrop export csv maps notes excerpts folders and favorites`() {
        val result = BookmarkFileParser.parse(resource("raindrop_export_sample.csv"), "raindrop-export.csv")
        assertEquals(3, result.size)

        val pasta = result[0]
        assertEquals("Best pasta in town", pasta.title)
        assertEquals("Recipes", pasta.folder)
        assertEquals(listOf("cooking", "italian"), pasta.tags)
        // User note beats the auto-scraped excerpt.
        assertEquals("try this weekend", pasta.notes)
        assertTrue(pasta.isFavorite)
        assertEquals(1_730_571_730_000L, pasta.createdAt) // 2024-11-02T18:22:10Z

        val paper = result[1]
        // Nested "Tech/AI" files into the leaf collection.
        assertEquals("AI", paper.folder)
        // No note — the excerpt is the fallback.
        assertTrue(paper.notes!!.startsWith("Abstract:"))
        assertFalse(paper.isFavorite)

        val plain = result[2]
        assertNull(plain.folder)
        assertNull(plain.notes)
        assertTrue(plain.tags.isEmpty())
    }
}
