package com.ghostgramlabs.pettibox.data.bookmarks

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Parser for the Netscape Bookmark File format — the de-facto interchange
 * format that browsers (Chrome, Firefox, Edge, Safari) and bookmark managers
 * (Raindrop.io, Instapaper, Pinboard, Linkwarden, Karakeep, old Pocket)
 * all export.
 *
 * The format is deliberately malformed HTML (unclosed `<DT>`/`<DD>`
 * everywhere), so we lean on jsoup's error-tolerant parser rather than
 * hand-rolling one. Structure per the format:
 *
 * ```
 * <DT><H3 ADD_DATE="...">Folder name</H3>
 * <DL><p>
 *     <DT><A HREF="..." ADD_DATE="..." TAGS="a,b">Title</A>
 *     <DD>Optional description
 * </DL><p>
 * ```
 */
object NetscapeBookmarkParser {

    // Wrapper folders browsers put everything inside. Filing 2,000 links
    // into a collection literally named "Bookmarks bar" helps nobody, so a
    // link whose nearest folder is one of these keeps climbing to an outer
    // (real) folder, or imports unfiled.
    private val rootContainers = setOf(
        "bookmarks", "bookmarks bar", "bookmarks menu", "bookmarks toolbar",
        "other bookmarks", "mobile bookmarks", "favorites", "favorites bar",
        "imported", "unsorted bookmarks", "reading list"
    )

    fun parse(html: String): List<ImportedBookmark> =
        Jsoup.parse(html).select("a[href]").mapNotNull { anchor ->
            val url = anchor.attr("href").trim()
            if (!isImportableUrl(url)) return@mapNotNull null
            ImportedBookmark(
                url = url,
                title = anchor.text().trim().ifBlank { url },
                folder = folderOf(anchor),
                tags = splitTags(anchor.attr("tags")),
                notes = descriptionOf(anchor),
                // Old Pocket HTML exports used TIME_ADDED instead of ADD_DATE.
                createdAt = parseEpoch(
                    anchor.attr("add_date").ifBlank { anchor.attr("time_added") }
                )
            )
        }

    /** Only real web links survive import — bookmarklets, `place:` smart folders, etc. don't. */
    private fun isImportableUrl(url: String): Boolean =
        url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)

    /**
     * Nearest enclosing folder that isn't a browser root container. Walks
     * up the ancestor `<dl>` lists; each list's name lives in the `<h3>`
     * jsoup leaves either as the list's previous sibling or as an earlier
     * child of the `<dt>` the list got nested into.
     */
    private fun folderOf(anchor: Element): String? {
        var el: Element? = anchor.parent()
        while (el != null) {
            if (el.tagName() == "dl") {
                val name = headingFor(el)?.text()?.trim()
                if (!name.isNullOrBlank() && name.lowercase() !in rootContainers) return name
            }
            el = el.parent()
        }
        return null
    }

    private fun headingFor(dl: Element): Element? {
        dl.previousElementSibling()?.takeIf { it.tagName() == "h3" }?.let { return it }
        val parent = dl.parent() ?: return null
        if (parent.tagName() == "dt") {
            return parent.children().firstOrNull { it.tagName() == "h3" }
        }
        return null
    }

    /**
     * The `<DD>` description following the link, if any. `<dt>` auto-closes
     * when `<dd>` starts, so after parsing the `<dd>` sits as the next
     * sibling of the anchor's `<dt>`.
     */
    private fun descriptionOf(anchor: Element): String? {
        val dt = anchor.parent()?.takeIf { it.tagName() == "dt" } ?: return null
        val dd = dt.nextElementSibling()?.takeIf { it.tagName() == "dd" } ?: return null
        return dd.ownText().trim().ifBlank { null }
    }

    private fun splitTags(raw: String): List<String> =
        raw.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    /**
     * ADD_DATE is epoch seconds in every mainstream export, but a few tools
     * write millis (13+ digits) or microseconds (16+). Out-of-range values
     * (year outside 1990–2100) are dropped rather than imported as garbage
     * timestamps that would sort the item into the wrong century.
     */
    internal fun parseEpoch(raw: String): Long? {
        val digits = raw.trim()
        if (digits.isEmpty() || !digits.all { it.isDigit() }) return null
        val value = digits.toLongOrNull() ?: return null
        val millis = when {
            digits.length >= 16 -> value / 1000
            digits.length >= 12 -> value
            else -> value * 1000
        }
        return millis.takeIf { it in MIN_PLAUSIBLE_MILLIS..MAX_PLAUSIBLE_MILLIS }
    }

    private const val MIN_PLAUSIBLE_MILLIS = 631_152_000_000L   // 1990-01-01
    private const val MAX_PLAUSIBLE_MILLIS = 4_102_444_800_000L // 2100-01-01
}
