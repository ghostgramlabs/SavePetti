package com.ghostgramlabs.pettibox.data.metadata

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
                    .userAgent("Mozilla/5.0 (Android) PettiBox/1.0")
                    .timeout(4_000)
                    .followRedirects(true)
                    .get()
                val imageUrl = doc.metaContent("og:image")
                    ?: doc.metaContent("twitter:image")
                    ?: youtubeThumbnail(url)

                LinkMetadata(
                    title = doc.metaContent("og:title")
                        ?: doc.metaContent("twitter:title")
                        ?: doc.title().takeIf { it.isNotBlank() },
                    description = doc.metaContent("og:description")
                        ?: doc.metaContent("description"),
                    imageUrl = imageUrl,
                    siteName = doc.metaContent("og:site_name")
                )
            }.getOrNull() ?: youtubeThumbnail(url)?.let { thumbnail ->
                LinkMetadata(
                    title = null,
                    description = null,
                    imageUrl = thumbnail,
                    siteName = "YouTube"
                )
            }
        } ?: youtubeThumbnail(url)?.let { thumbnail ->
            LinkMetadata(
                title = null,
                description = null,
                imageUrl = thumbnail,
                siteName = "YouTube"
            )
        }
    }

    private fun org.jsoup.nodes.Document.metaContent(prop: String): String? {
        val byProp = select("meta[property=$prop]").attr("content")
        if (byProp.isNotBlank()) return byProp
        val byName = select("meta[name=$prop]").attr("content")
        return byName.ifBlank { null }
    }

    private fun youtubeThumbnail(url: String): String? {
        val id = Regex("""(?:youtube\.com/(?:watch\?v=|shorts/|embed/)|youtu\.be/)([A-Za-z0-9_-]{11})""")
            .find(url)
            ?.groupValues
            ?.getOrNull(1)
            ?: return null
        return "https://img.youtube.com/vi/$id/hqdefault.jpg"
    }
}
