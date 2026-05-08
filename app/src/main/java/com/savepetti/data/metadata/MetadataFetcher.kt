package com.savepetti.data.metadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataFetcher @Inject constructor() {

    /**
     * Pulls Open Graph / standard meta tags. Best-effort with a hard timeout —
     * the save flow must never hang on a slow page. Caller can fall back to
     * the URL's host as a title if this returns null.
     */
    suspend fun fetch(url: String): LinkMetadata? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(5_000L) {
            runCatching {
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Android) SavePetti/1.0")
                    .timeout(4_000)
                    .followRedirects(true)
                    .get()
                LinkMetadata(
                    title = doc.metaContent("og:title")
                        ?: doc.metaContent("twitter:title")
                        ?: doc.title().takeIf { it.isNotBlank() },
                    description = doc.metaContent("og:description")
                        ?: doc.metaContent("description"),
                    imageUrl = doc.metaContent("og:image")
                        ?: doc.metaContent("twitter:image"),
                    siteName = doc.metaContent("og:site_name")
                )
            }.getOrNull()
        }
    }

    private fun org.jsoup.nodes.Document.metaContent(prop: String): String? {
        val byProp = select("meta[property=$prop]").attr("content")
        if (byProp.isNotBlank()) return byProp
        val byName = select("meta[name=$prop]").attr("content")
        return byName.ifBlank { null }
    }
}
