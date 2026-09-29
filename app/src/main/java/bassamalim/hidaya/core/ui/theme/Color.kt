package bassamalim.hidaya.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Status colors are shared by both themes, so each sits between the two backgrounds
val Positive = Color(0xFF2F8F46)
val Negative = Color(0xFFD2483E)
val Gold = Color(0xFFC5A600)
val Silver = Color(0xFFA8A8A8)
val Bronze = Color(0xFFB9732D)
val Bookmark1Color = Color(0xFF2778FF)
val Bookmark2Color = Color(0xFF35C0AD)
val Bookmark3Color = Color(0xFF683DDE)
val Bookmark4Color = Color(0xFFB2A133)

/*
 * Palette: deep teal as the main color, muted gold as the accent, on warm "paper" neutrals.
 * Every text/container pair here was checked for at least 5:1 contrast.
 *
 * Roles the app leans on:
 *  - primary: actions, highlights, the selected verse
 *  - primaryContainer: hero cards (next prayer) and icon badges
 *  - secondary: calm sage for chips and selected rows
 *  - tertiary: gold accent (tracked verse while reciting, "moderate" states, notices)
 *  - onSurfaceVariant: secondary/supporting text only, never main reading text
 *  - surfaceContainerLow: cards. In light mode containers go from white (lowest) toward
 *    darker (highest); in dark mode they go from darkest (lowest) to lightest (highest)
 */

val lightColorScheme = lightColorScheme(
    primary = Color(0xFF0B6B5D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEBE2),
    onPrimaryContainer = Color(0xFF00372F),
    inversePrimary = Color(0xFF6FD8C1),
    secondary = Color(0xFF4A635D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE7E2),
    onSecondaryContainer = Color(0xFF1C302B),
    tertiary = Color(0xFF7D6112),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF7E4AE),
    onTertiaryContainer = Color(0xFF3D2E00),
    background = Color(0xFFF4F2EC),
    onBackground = Color(0xFF1B1D1B),
    surface = Color(0xFFF4F2EC),
    onSurface = Color(0xFF1B1D1B),
    surfaceVariant = Color(0xFFE1E5E0),
    onSurfaceVariant = Color(0xFF575E5A),
    surfaceTint = Color(0xFFF4F2EC),  // No tonal tint: surfaces keep their exact color
    inverseSurface = Color(0xFF2F3230),
    inverseOnSurface = Color(0xFFF0F1EC),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF737A76),
    outlineVariant = Color(0xFFC6CBC7),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEECE5),
    surfaceContainerHigh = Color(0xFFE8E6DF),
    surfaceContainerHighest = Color(0xFFE2E0D9),
    surfaceBright = Color(0xFFFBFAF6),
    surfaceDim = Color(0xFFDAD8D1)
)

val darkColorScheme = darkColorScheme(
    primary = Color(0xFF6FD8C1),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF11493F),
    onPrimaryContainer = Color(0xFFBFF0E4),
    inversePrimary = Color(0xFF0B6B5D),
    secondary = Color(0xFFB2CCC5),
    onSecondary = Color(0xFF1D3530),
    secondaryContainer = Color(0xFF2A3B37),
    onSecondaryContainer = Color(0xFFCFE6DF),
    tertiary = Color(0xFFE3C36B),
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF4A3B08),
    onTertiaryContainer = Color(0xFFFBE4A2),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFE2E5E1),
    surface = Color(0xFF0E1513),
    onSurface = Color(0xFFE2E5E1),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFA9B2AD),
    surfaceTint = Color(0xFF0E1513),  // No tonal tint: surfaces keep their exact color
    inverseSurface = Color(0xFFE2E5E1),
    inverseOnSurface = Color(0xFF2C322F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8B938F),
    outlineVariant = Color(0xFF3F4945),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF090F0D),
    surfaceContainerLow = Color(0xFF18211E),
    surfaceContainer = Color(0xFF1B2522),
    surfaceContainerHigh = Color(0xFF232E2B),
    surfaceContainerHighest = Color(0xFF2D3935),
    surfaceBright = Color(0xFF333D3A),
    surfaceDim = Color(0xFF0E1513)
)
