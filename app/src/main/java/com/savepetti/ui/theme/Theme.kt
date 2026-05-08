package com.savepetti.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Cream,
    primaryContainer = Peach,
    onPrimaryContainer = Ink,
    secondary = WatchPlum,
    onSecondary = Cream,
    secondaryContainer = Butter,
    onSecondaryContainer = Ink,
    tertiary = TravelTeal,
    onTertiary = Cream,
    background = Cream,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = InkSoft,
    outline = Outline,
    outlineVariant = CreamDeep,
    error = CoralDeep,
    onError = Cream
)

// Used only inside the dark scheme; declared here so we don't pollute Color.kt
private val Color_InkMutedDark = androidx.compose.ui.graphics.Color(0xFFC2B79F)

private val DarkColors = darkColorScheme(
    primary = Coral,
    onPrimary = InkDark,
    primaryContainer = CoralDeep,
    onPrimaryContainer = CreamOnDark,
    secondary = WatchPlum,
    onSecondary = CreamOnDark,
    tertiary = TravelTeal,
    onTertiary = InkDark,
    background = InkDark,
    onBackground = CreamOnDark,
    surface = SurfaceDark,
    onSurface = CreamOnDark,
    surfaceVariant = SurfaceTintDark,
    onSurfaceVariant = Color_InkMutedDark,
    outline = OutlineDark,
    outlineVariant = SurfaceTintDark,
    error = CoralDeep,
    onError = CreamOnDark
)

@Composable
fun SavePettiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Deliberately NOT using dynamicColor — we want our hand-picked palette,
    // not the device-wallpaper-derived Material You scheme.
    val colors = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = SavePettiTypography,
        shapes = SavePettiShapes,
        content = content
    )
}

