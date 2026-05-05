package com.activespark.gen7.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Active Spark Gen 7 — Futuristic Dark Color Scheme
 * Primary: Neon Cyan (#00F5FF)
 * Secondary: Neon Lime Green (#39FF14)
 * Tertiary: Neon Pink / Magenta (#FF1B8D)
 * Background: Deep dark (#0A0A0F)
 */
private val ActiveSparkDarkColorScheme = darkColorScheme(
    // Primary
    primary = NeonCyan,
    onPrimary = Background,
    primaryContainer = NeonCyanSubtle,
    onPrimaryContainer = NeonCyan,

    // Secondary
    secondary = NeonGreen,
    onSecondary = Background,
    secondaryContainer = NeonGreenSubtle,
    onSecondaryContainer = NeonGreen,

    // Tertiary
    tertiary = NeonPink,
    onTertiary = Background,
    tertiaryContainer = NeonPinkSubtle,
    onTertiaryContainer = NeonPink,

    // Error
    error = Error,
    onError = Background,
    errorContainer = NeonPinkSubtle,
    onErrorContainer = NeonPink,

    // Background
    background = Background,
    onBackground = TextPrimary,

    // Surface
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,

    // Outline
    outline = DividerColor,
    outlineVariant = TextDisabled,

    // Inverse
    inverseSurface = TextPrimary,
    inverseOnSurface = Background,
    inversePrimary = NeonCyanDim,

    // Scrim
    scrim = ScrimOverlay
)

/**
 * Main theme composable for Active Spark Gen 7.
 * Applies the dark futuristic color scheme, neon typography, and custom shapes.
 * Forces edge-to-edge with transparent system bars.
 */
@Composable
fun ActiveSparkTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent for edge-to-edge look
            window.statusBarColor = Background.toArgb()
            window.navigationBarColor = Background.toArgb()
            // Light icons = false since background is very dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = ActiveSparkDarkColorScheme,
        typography = ActiveSparkTypography,
        shapes = ActiveSparkShapes,
        content = content
    )
}
