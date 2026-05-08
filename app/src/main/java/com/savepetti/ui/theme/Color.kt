package com.savepetti.ui.theme

import androidx.compose.ui.graphics.Color

// Hand-picked warm palette. Cream paper, coral ink, jewel-tone categories.
// Avoid the default Material 3 You "purple-on-grey" look.

val Cream = Color(0xFFFFF8F1)
val CreamDeep = Color(0xFFF6ECDF)
val Ink = Color(0xFF1B1A17)
val InkSoft = Color(0xFF4A463E)
val InkMuted = Color(0xFF8C8579)

val Coral = Color(0xFFFF5A5F)
val CoralDeep = Color(0xFFE43F44)
val Peach = Color(0xFFFFD8A8)
val Butter = Color(0xFFFFE8B0)

val Surface = Color(0xFFFFFBF5)
val SurfaceTint = Color(0xFFFFE5D9)
val Outline = Color(0xFFE8DDCC)

// Category accents — each saturated, distinct, recognizable across thumbnails.
// Mapped to the reference app's category set: Recipes / Travel / Fitness /
// Style / Home / Beauty / Read Later / Finance / Music.
val RecipeOrange = Color(0xFFF97316)   // Recipes 🍳
val TravelTeal = Color(0xFF14B8A6)      // Travel ✈️
val FitnessIndigo = Color(0xFF6366F1)   // Fitness 💪
val StyleAmber = Color(0xFFF59E0B)      // Style 👗
val HomePurple = Color(0xFFA855F7)      // Home 🏡
val BeautyPink = Color(0xFFEC4899)      // Beauty 💄
val ReadLaterSky = Color(0xFF0EA5E9)    // Read Later 📖
val FinanceEmerald = Color(0xFF10B981)  // Finance 💰
val MusicCoral = Color(0xFFFB923C)      // Music 🎵 (warm orange-coral)

// Aliases retained for backwards-compat with theme references elsewhere.
val ShoppingOrange = RecipeOrange
val RecipeTomato = RecipeOrange
val IdeasMustard = StyleAmber
val WatchPlum = HomePurple
val WorkIndigo = FitnessIndigo
val PersonalPink = BeautyPink
val ReadingForest = FinanceEmerald
val FavoritesGold = StyleAmber

// Dark variants — bumped outline so borders don't disappear, lifted surface
// tint so cards have visible edges against background.
val InkDark = Color(0xFF14130F)
val SurfaceDark = Color(0xFF22201A)
val SurfaceTintDark = Color(0xFF2F2C22)
val OutlineDark = Color(0xFF54503F)
val CreamOnDark = Color(0xFFF1E7D4)
