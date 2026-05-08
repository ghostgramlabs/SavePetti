package com.savepetti.domain.model

enum class SourceApp(val displayName: String, val emoji: String) {
    INSTAGRAM("Instagram", "📷"),
    REDDIT("Reddit", "👽"),
    YOUTUBE("YouTube", "▶"),
    CHROME("Chrome", "🌐"),
    MAPS("Maps", "📍"),
    WHATSAPP("WhatsApp", "💬"),
    TWITTER("X / Twitter", "🐦"),
    PINTEREST("Pinterest", "📌"),
    SPOTIFY("Spotify", "🎵"),
    AMAZON("Amazon", "📦"),
    FILES("Files", "📁"),
    UNKNOWN("Other", "✨");

    companion object {
        fun fromUrl(url: String?): SourceApp {
            if (url.isNullOrBlank()) return UNKNOWN
            val u = url.lowercase()
            return when {
                "instagram.com" in u -> INSTAGRAM
                "reddit.com" in u || "redd.it" in u -> REDDIT
                "youtube.com" in u || "youtu.be" in u -> YOUTUBE
                "twitter.com" in u || "x.com" in u -> TWITTER
                "pinterest." in u -> PINTEREST
                "spotify.com" in u -> SPOTIFY
                "amazon." in u -> AMAZON
                "google.com/maps" in u || "maps.app.goo.gl" in u || "goo.gl/maps" in u -> MAPS
                "wa.me" in u || "whatsapp.com" in u -> WHATSAPP
                u.startsWith("http") -> CHROME
                else -> UNKNOWN
            }
        }
    }
}
