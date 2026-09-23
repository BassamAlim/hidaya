package bassamalim.hidaya.core.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.enums.ThemeColor

@Composable
fun AppTheme(
    theme: Theme = Theme.SYSTEM,
    direction: LayoutDirection = LayoutDirection.Rtl,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides direction,
        LocalDimensions provides AppDimensions(),
        LocalTypography provides AppTypography(),
    ) {
        MaterialTheme(
            colorScheme = getColorScheme(theme, LocalContext.current, isSystemInDarkTheme()),
            shapes = shapes,
        ) {
            content()
        }
    }
}

fun Context.isSystemDark() =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

fun getColorScheme(
    theme: Theme,
    context: Context,
    isSystemDark: Boolean = context.isSystemDark()
): ColorScheme {
    val isDark = theme.isDark(isSystemDark)
    return if (theme == Theme.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    else if (isDark) darkColorScheme
    else lightColorScheme
}

fun getThemeColor(color: ThemeColor, theme: Theme, context: Context): Color {
    val colorScheme = getColorScheme(theme, context)

    return when (color) {
        ThemeColor.PRIMARY -> colorScheme.primary
        ThemeColor.ON_PRIMARY -> colorScheme.onPrimary
        ThemeColor.PRIMARY_CONTAINER -> colorScheme.primaryContainer
        ThemeColor.ON_PRIMARY_CONTAINER -> colorScheme.onPrimaryContainer
        ThemeColor.INVERSE_PRIMARY -> colorScheme.inversePrimary
        ThemeColor.SECONDARY -> colorScheme.secondary
        ThemeColor.ON_SECONDARY -> colorScheme.onSecondary
        ThemeColor.SECONDARY_CONTAINER -> colorScheme.secondaryContainer
        ThemeColor.ON_SECONDARY_CONTAINER -> colorScheme.onSecondaryContainer
        ThemeColor.TERTIARY -> colorScheme.tertiary
        ThemeColor.ON_TERTIARY -> colorScheme.onTertiary
        ThemeColor.TERTIARY_CONTAINER -> colorScheme.tertiaryContainer
        ThemeColor.ON_TERTIARY_CONTAINER -> colorScheme.onTertiaryContainer
        ThemeColor.BACKGROUND -> colorScheme.background
        ThemeColor.ON_BACKGROUND -> colorScheme.onBackground
        ThemeColor.SURFACE -> colorScheme.surface
        ThemeColor.ON_SURFACE -> colorScheme.onSurface
        ThemeColor.SURFACE_VARIANT -> colorScheme.surfaceVariant
        ThemeColor.ON_SURFACE_VARIANT -> colorScheme.onSurfaceVariant
        ThemeColor.SURFACE_TINT -> colorScheme.surfaceTint
        ThemeColor.INVERSE_SURFACE -> colorScheme.inverseSurface
        ThemeColor.INVERSE_ON_SURFACE -> colorScheme.inverseOnSurface
        ThemeColor.ERROR -> colorScheme.error
        ThemeColor.ON_ERROR -> colorScheme.onError
        ThemeColor.ERROR_CONTAINER -> colorScheme.errorContainer
        ThemeColor.ON_ERROR_CONTAINER -> colorScheme.onErrorContainer
        ThemeColor.OUTLINE -> colorScheme.outline
        ThemeColor.OUTLINE_VARIANT -> colorScheme.outlineVariant
        ThemeColor.SCRIM -> colorScheme.scrim
        ThemeColor.SURFACE_CONTAINER_HIGHEST -> colorScheme.surfaceContainerHighest
        ThemeColor.SURFACE_CONTAINER_HIGH -> colorScheme.surfaceContainerHigh
        ThemeColor.SURFACE_CONTAINER -> colorScheme.surfaceContainer
        ThemeColor.SURFACE_CONTAINER_LOW -> colorScheme.surfaceContainerLow
        ThemeColor.SURFACE_CONTAINER_LOWEST -> colorScheme.surfaceContainerLowest
        ThemeColor.SURFACE_BRIGHT -> colorScheme.surfaceBright
        ThemeColor.SURFACE_DIM -> colorScheme.surfaceDim
    }
}