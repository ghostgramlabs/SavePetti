package com.ghostgramlabs.pettibox.ui.theme

import androidx.compose.ui.graphics.Color

// "Muji notebook" palette. Aim is quiet and considered — almost-grayscale
// paper base, a single warm accent, and washed pastel categories that feel
// gathered (one collection, slightly faded) rather than nine separate
// product mascots screaming at the user.
//
// Deliberately avoids both the Material 3 You "purple-on-grey" default AND
// the now-stock indie-app combo of Airbnb-coral-on-cream + Tailwind-500
// jewel tones, which read as "Figma kit" the moment you see them.

// ── Paper base ────────────────────────────────────────────────────────────
val Bone = Color(0xFFF0EBE2)        // background — warm off-white, not pure
val PaperOff = Color(0xFFF8F4EC)    // surface — slightly lifted from Bone
val PaperEdge = Color(0xFFE6DFD2)   // outline / hairlines on light paper

// ── Ink ───────────────────────────────────────────────────────────────────
val Soot = Color(0xFF1F1D1A)        // primary text — warm near-black
val Gravel = Color(0xFF6B6660)      // secondary text — soft warm gray
val Pebble = Color(0xFF9A938A)      // tertiary / placeholder

// ── Accent ────────────────────────────────────────────────────────────────
// Single warm accent. Persimmon = a real pigment name; sits between rust and
// orange. Distinct from any famous brand color (Airbnb coral, Stripe purple,
// etc.) because it's slightly muddied — you'd never pick it as a defaults pick.
val Persimmon = Color(0xFFD85A36)
val PersimmonDeep = Color(0xFFB54223)
val PersimmonWash = Color(0xFFF1CFC1) // tonal container, not pure peach

// ── Category accents ──────────────────────────────────────────────────────
// Washed pastels, all roughly the same value/saturation so they read as a
// set, not a starter-pack rainbow. Each name describes the actual hue (no
// "TravelTeal" branding) so future-you remembers what it looks like.
val Apricot = Color(0xFFE8A176)     // Recipes
val SeaGlass = Color(0xFF8FB5A8)    // Travel
val StoneBlue = Color(0xFF7E94AE)   // Fitness
val Ochre = Color(0xFFC49A5C)       // Style
val Mauve = Color(0xFFA38AA0)       // Home
val Petal = Color(0xFFD4A4AE)       // Beauty
val SkyStone = Color(0xFF98ADBF)    // Read Later
val Sage = Color(0xFF9DAE83)        // Finance
val Rust = Color(0xFFB07358)        // Music

// ── Backwards-compat aliases ──────────────────────────────────────────────
// The old constants are referenced from CategoryPalette, CollectionColorSeeds,
// Theme.kt, etc. Re-export under the old names so we only have to touch one
// file. Names are now misleading (RecipeOrange isn't really orange) — that's
// fine; if anything ever needs renaming we can do it on a quiet day.
val Cream = Bone
val CreamDeep = PaperEdge
val Ink = Soot
val InkSoft = Gravel
val InkMuted = Pebble
val Coral = Persimmon
val CoralDeep = PersimmonDeep
val Peach = PersimmonWash
val Butter = Color(0xFFE8DCC2)      // muted dijon for secondary container
val Surface = PaperOff
val SurfaceTint = Color(0xFFEFE6D6) // subtle tonal lift
val Outline = PaperEdge

val RecipeOrange = Apricot
val TravelTeal = SeaGlass
val FitnessIndigo = StoneBlue
val StyleAmber = Ochre
val HomePurple = Mauve
val BeautyPink = Petal
val ReadLaterSky = SkyStone
val FinanceEmerald = Sage
val MusicCoral = Rust

val ShoppingOrange = Apricot
val RecipeTomato = Apricot
val IdeasMustard = Ochre
val WatchPlum = Mauve
val WorkIndigo = StoneBlue
val PersonalPink = Petal
val ReadingForest = Sage
val FavoritesGold = Ochre

// ── Dark variants ─────────────────────────────────────────────────────────
// Matched to the same restrained feel — warm charcoal, not pure black.
val InkDark = Color(0xFF1A1815)         // background
val SurfaceDark = Color(0xFF24211D)     // raised surface
val SurfaceTintDark = Color(0xFF2F2C26) // outline-ish container
val OutlineDark = Color(0xFF3F3B34)
val CreamOnDark = Color(0xFFE9E2D2)     // body text on dark
