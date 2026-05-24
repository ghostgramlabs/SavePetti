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

// ── Paper base (light) ─────────────────────────────────────────────────────
// A three-tier warm surface ladder rather than a flat sheet. Surfaces separate
// by LUMINANCE (deeper paper bg → soft-ivory card → brightest cream for docks)
// with the light-mode card shadow doing the lifting — but the borders are kept
// a warm, PRESENT pencil-edge, not a faint wash: defined enough to read as
// hand-cut paper, never a cold wireframe. The bg also lets the PaperTexture
// grain show through (see MainActivity) so it reads as real paper, not vector
// color — that grain is what stops the daytime palette feeling machine-made.
val Bone = Color(0xFFF1E9DC)        // background — warm oatmeal paper, sits a touch deeper so cards lift
val PaperOff = Color(0xFFFBF6EE)    // primary surface / cards — soft ivory
val PaperBright = Color(0xFFFFFDF8) // elevated cream — bottom-nav dock, dialogs, brightest tier
val PaperEdge = Color(0xFFD9C9B1)   // outline / hairlines — a warm, present pencil-edge (defined, not cold)

// ── Ink ───────────────────────────────────────────────────────────────────
val Soot = Color(0xFF2A231D)        // primary text — warm charcoal, softer than the old near-black
val Gravel = Color(0xFF5C5046)      // secondary text — deep warm brown, legible not washed
val Pebble = Color(0xFF9A938A)      // tertiary / placeholder (also dark-mode secondary text — leave as-is)

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
val SurfaceTint = Color(0xFFECE3D4) // recessed warm tonal — inset fills (search) & soft dividers
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
