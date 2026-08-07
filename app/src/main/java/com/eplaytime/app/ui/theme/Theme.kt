package com.eplaytime.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Defined in Type.kt
// val OutfitFontFamily ...
// val Typography ...

/**
 * OLED Dark Color Scheme - Always Dark, Never Light
 * This is the flagship "Rich & Soft" theme with Soft Gold accent
 */
private val OLEDDarkColorScheme = darkColorScheme(
    // Background colors - True OLED Black
    background = BlackBackground,
    surface = BlackSurface,
    surfaceVariant = BlackCard,

    // Primary - Soft Gold accent
    primary = SoftGold,
    onPrimary = Color.Black,
    primaryContainer = SoftGoldDark,
    onPrimaryContainer = TextPrimary,

    // Secondary - Subtle white
    secondary = GlassWhite,
    onSecondary = TextPrimary,
    secondaryContainer = GlassBorder,
    onSecondaryContainer = TextSecondary,

    // Tertiary - Glass highlight
    tertiary = GlassHighlight,
    onTertiary = TextPrimary,

    // Error
    error = ErrorRed,
    onError = TextPrimary,

    // Text colors
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,

    // Outline
    outline = GlassBorder,
    outlineVariant = GlassWhite
)

@Composable
fun PlayTimeTheme(
    darkTheme: Boolean = true, // ALWAYS TRUE - OLED Dark only
    content: @Composable () -> Unit
) {
    // FORCE dark theme regardless of system setting
    val colorScheme = OLEDDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Make system bars TRANSPARENT for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Make icons light (for dark theme)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Uses Type.kt Typography
        content = content
    )
}
