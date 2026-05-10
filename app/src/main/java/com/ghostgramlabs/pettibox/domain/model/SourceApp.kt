package com.ghostgramlabs.pettibox.domain.model

enum class SourceApp(val displayName: String, val emoji: String) {
    INSTAGRAM("Instagram", "\uD83D\uDCF7"),
    REDDIT("Reddit", "\uD83D\uDC7D"),
    YOUTUBE("YouTube", "\u25B6"),
    CHROME("Chrome", "\uD83C\uDF10"),
    MAPS("Maps", "\uD83D\uDCCD"),
    WHATSAPP("WhatsApp", "\uD83D\uDCAC"),
    TWITTER("X / Twitter", "\uD83D\uDC26"),
    PINTEREST("Pinterest", "\uD83D\uDCCC"),
    SPOTIFY("Spotify", "\uD83C\uDFB5"),
    AMAZON("Amazon", "\uD83D\uDCE6"),
    FILES("Files", "\uD83D\uDCC1"),
    UNKNOWN("Other", "\u2728");

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
