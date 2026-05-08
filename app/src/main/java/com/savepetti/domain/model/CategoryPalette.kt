package com.savepetti.domain.model

import androidx.compose.ui.graphics.Color
import com.savepetti.ui.theme.BeautyPink
import com.savepetti.ui.theme.FinanceEmerald
import com.savepetti.ui.theme.FitnessIndigo
import com.savepetti.ui.theme.HomePurple
import com.savepetti.ui.theme.MusicCoral
import com.savepetti.ui.theme.ReadLaterSky
import com.savepetti.ui.theme.RecipeOrange
import com.savepetti.ui.theme.StyleAmber
import com.savepetti.ui.theme.TravelTeal

data class CategoryPreset(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: Long
)

object CategoryPalette {
    // Mirrors the reference app's category strip exactly.
    val Defaults: List<CategoryPreset> = listOf(
        CategoryPreset("recipes",    "Recipes",    "\uD83C\uDF73", RecipeOrange.toLongHex()),
        CategoryPreset("travel",     "Travel",     "\u2708\uFE0F", TravelTeal.toLongHex()),
        CategoryPreset("fitness",    "Fitness",    "\uD83D\uDCAA", FitnessIndigo.toLongHex()),
        CategoryPreset("style",      "Style",      "\uD83D\uDC57", StyleAmber.toLongHex()),
        CategoryPreset("home",       "Home",       "\uD83C\uDFE1", HomePurple.toLongHex()),
        CategoryPreset("beauty",     "Beauty",     "\uD83D\uDC84", BeautyPink.toLongHex()),
        CategoryPreset("read_later", "Read Later", "\uD83D\uDCD6", ReadLaterSky.toLongHex()),
        CategoryPreset("finance",    "Finance",    "\uD83D\uDCB0", FinanceEmerald.toLongHex()),
        CategoryPreset("music",      "Music",      "\uD83C\uDFB5", MusicCoral.toLongHex())
    )

    fun colorOf(hex: Long): Color = Color(hex)
}

private fun Color.toLongHex(): Long {
    val a = (alpha * 255).toInt() and 0xFF
    val r = (red * 255).toInt() and 0xFF
    val g = (green * 255).toInt() and 0xFF
    val b = (blue * 255).toInt() and 0xFF
    return ((a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
}
