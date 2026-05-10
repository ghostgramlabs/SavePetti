package com.ghostgramlabs.pettibox.data.util

import java.util.regex.Pattern

object TextUtils {

    private val URL = Pattern.compile(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    )

    fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val m = URL.matcher(text)
        return if (m.find()) m.group(1) else null
    }

    fun smartTitle(text: String?, fallback: String): String {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return fallback
        val firstLine = t.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        if (firstLine.length <= 80) return firstLine.ifBlank { fallback }
        return firstLine.take(77) + "…"
    }

    fun hostOf(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            java.net.URI(url).host?.removePrefix("www.")
        }.getOrNull()
    }
}
