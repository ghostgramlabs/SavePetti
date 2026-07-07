package com.ghostgramlabs.pettibox.data.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetscapeBookmarkParserTest {

    // Structure Chrome/Edge/Firefox actually emit: root container folder,
    // nested real folder, unclosed <DT>/<DD>, ICON noise.
    private val chromeStyleExport = """
        <!DOCTYPE NETSCAPE-Bookmark-file-1>
        <META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
        <TITLE>Bookmarks</TITLE>
        <H1>Bookmarks</H1>
        <DL><p>
            <DT><H3 ADD_DATE="1700000000" PERSONAL_TOOLBAR_FOLDER="true">Bookmarks bar</H3>
            <DL><p>
                <DT><A HREF="https://example.com/" ADD_DATE="1700000100">Example</A>
                <DT><H3 ADD_DATE="1700000000">Recipes</H3>
                <DL><p>
                    <DT><A HREF="https://food.example/pasta" ADD_DATE="1700000200" ICON="data:image/png;base64,AAAA">Pasta night</A>
                    <DD>Best pasta ever
                </DL><p>
            </DL><p>
        </DL><p>
    """.trimIndent()

    @Test
    fun `parses links with titles and add dates`() {
        val result = NetscapeBookmarkParser.parse(chromeStyleExport)
        assertEquals(2, result.size)
        val example = result.first { it.url == "https://example.com/" }
        assertEquals("Example", example.title)
        assertEquals(1_700_000_100_000L, example.createdAt)
    }

    @Test
    fun `root container folders import unfiled, real folders keep their name`() {
        val result = NetscapeBookmarkParser.parse(chromeStyleExport)
        val example = result.first { it.url == "https://example.com/" }
        assertNull(example.folder)
        val pasta = result.first { it.url == "https://food.example/pasta" }
        assertEquals("Recipes", pasta.folder)
    }

    @Test
    fun `dd description becomes the note`() {
        val pasta = NetscapeBookmarkParser.parse(chromeStyleExport)
            .first { it.url == "https://food.example/pasta" }
        assertEquals("Best pasta ever", pasta.notes)
    }

    @Test
    fun `tags attribute is parsed - pinboard and raindrop style`() {
        val html = """
            <DL><p>
                <DT><A HREF="https://a.example/" ADD_DATE="1700000000" TAGS="cooking, dinner">A</A>
            </DL><p>
        """.trimIndent()
        val result = NetscapeBookmarkParser.parse(html)
        assertEquals(listOf("cooking", "dinner"), result.single().tags)
    }

    @Test
    fun `old pocket html uses time_added and a flat list`() {
        val html = """
            <ul>
                <li><a href="https://x.example/post" time_added="1600000000" tags="news">Post</a></li>
            </ul>
        """.trimIndent()
        val result = NetscapeBookmarkParser.parse(html)
        assertEquals(1, result.size)
        assertEquals(1_600_000_000_000L, result.single().createdAt)
        assertEquals(listOf("news"), result.single().tags)
        assertNull(result.single().folder)
    }

    @Test
    fun `bookmarklets and firefox smart folders are skipped`() {
        val html = """
            <DL><p>
                <DT><A HREF="javascript:alert(1)">Bookmarklet</A>
                <DT><A HREF="place:type=6&sort=14">Recent Tags</A>
                <DT><A HREF="https://keep.example/">Keep me</A>
            </DL><p>
        """.trimIndent()
        val result = NetscapeBookmarkParser.parse(html)
        assertEquals(listOf("https://keep.example/"), result.map { it.url })
    }

    @Test
    fun `blank titles fall back to the url`() {
        val html = """<DL><DT><A HREF="https://untitled.example/"></A></DL>"""
        assertEquals("https://untitled.example/", NetscapeBookmarkParser.parse(html).single().title)
    }

    @Test
    fun `epoch parser handles seconds millis and garbage`() {
        assertEquals(1_700_000_000_000L, NetscapeBookmarkParser.parseEpoch("1700000000"))
        assertEquals(1_700_000_000_000L, NetscapeBookmarkParser.parseEpoch("1700000000000"))
        // Microseconds (some tools) collapse to the same instant.
        assertEquals(1_700_000_000_000L, NetscapeBookmarkParser.parseEpoch("1700000000000000"))
        assertNull(NetscapeBookmarkParser.parseEpoch(""))
        assertNull(NetscapeBookmarkParser.parseEpoch("0"))
        assertNull(NetscapeBookmarkParser.parseEpoch("not-a-date"))
    }

    @Test
    fun `deeply nested folders resolve to the nearest named folder`() {
        val html = """
            <DL><p>
                <DT><H3>Bookmarks Menu</H3>
                <DL><p>
                    <DT><H3>Work</H3>
                    <DL><p>
                        <DT><H3>Design</H3>
                        <DL><p>
                            <DT><A HREF="https://design.example/">Design tool</A>
                        </DL><p>
                    </DL><p>
                </DL><p>
            </DL><p>
        """.trimIndent()
        val result = NetscapeBookmarkParser.parse(html)
        assertEquals("Design", result.single().folder)
        assertTrue(result.single().tags.isEmpty())
    }
}
