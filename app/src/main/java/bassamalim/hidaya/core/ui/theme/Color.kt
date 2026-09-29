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
 * Palette: deep blue as the main color, muted gold as the accent, on warm "paper" neutrals.
 * Every text/container pair here was checked for at least 5:1 contrast.
 *
 * Roles the app leans on:
 *  - primary: actions, highlights, the selected verse
 *  - primaryContainer: hero cards (next prayer) and icon badges
 *  - secondary: soft slate blue for chips and selected rows
 *  - tertiary: gold accent (tracked verse while reciting, "moderate" states, notices)
 *  - onSurfaceVariant: secondary/supporting text only, never main reading text
 *  - surfaceContainerLow: cards. In light mode containers go from white (lowest) toward
 *    darker (highest); in dark mode they go from darkest (lowest) to lightest (highest)
 */

val lightColorScheme = lightColorScheme(
    primary = Color(0xFF1F5A96),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3F7),
    onPrimaryContainer = Color(0xFF0A2F55),
    inversePrimary = Color(0xFF9CC4F2),
    secondary = Color(0xFF4F5E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEE5EE),
    onSecondaryContainer = Color(0xFF1B2838),
    tertiary = Color(0xFF7D6112),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF7E4AE),
    onTertiaryContainer = Color(0xFF3D2E00),
    background = Color(0xFFF4F2EC),
    onBackground = Color(0xFF1B1C1E),
    surface = Color(0xFFF4F2EC),
    onSurface = Color(0xFF1B1C1E),
    surfaceVariant = Color(0xFFE1E4EA),
    onSurfaceVariant = Color(0xFF5A5F66),
    surfaceTint = Color(0xFFF4F2EC),  // No tonal tint: surfaces keep their exact color
    inverseSurface = Color(0xFF2F3136),
    inverseOnSurface = Color(0xFFF1F1F4),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF74787F),
    outlineVariant = Color(0xFFC8CAD0),
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
    primary = Color(0xFF9CC4F2),
    onPrimary = Color(0xFF0A2F55),
    primaryContainer = Color(0xFF1A3E66),
    onPrimaryContainer = Color(0xFFD3E3F7),
    inversePrimary = Color(0xFF1F5A96),
    secondary = Color(0xFFB7C7DA),
    onSecondary = Color(0xFF213243),
    secondaryContainer = Color(0xFF2B3847),
    onSecondaryContainer = Color(0xFFD7E3F1),
    tertiary = Color(0xFFE3C36B),
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF4A3B08),
    onTertiaryContainer = Color(0xFFFBE4A2),
    background = Color(0xFF0E131A),
    onBackground = Color(0xFFE2E5EA),
    surface = Color(0xFF0E131A),
    onSurface = Color(0xFFE2E5EA),
    surfaceVariant = Color(0xFF3F4753),
    onSurfaceVariant = Color(0xFFA9B1BC),
    surfaceTint = Color(0xFF0E131A),  // No tonal tint: surfaces keep their exact color
    inverseSurface = Color(0xFFE2E5EA),
    inverseOnSurface = Color(0xFF2C3138),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8B929C),
    outlineVariant = Color(0xFF3F4753),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF090D12),
    surfaceContainerLow = Color(0xFF161C24),
    surfaceContainer = Color(0xFF1A212A),
    surfaceContainerHigh = Color(0xFF222A34),
    surfaceContainerHighest = Color(0xFF2C3540),
    surfaceBright = Color(0xFF333C47),
    surfaceDim = Color(0xFF0E131A)
)
