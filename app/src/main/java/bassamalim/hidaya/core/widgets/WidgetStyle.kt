package bassamalim.hidaya.core.widgets

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProviders as GlanceColors
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.layout.fillMaxSize
import androidx.glance.material3.ColorProviders
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.ui.theme.darkColorScheme
import bassamalim.hidaya.core.ui.theme.lightColorScheme

/**
 * The app's theme choice as widget colors. System and Material You follow the launcher's
 * day/night, like the app follows the system's.
 */
fun widgetColors(theme: Theme): GlanceColors = when {
    theme == Theme.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        DynamicThemeColorProviders
    theme == Theme.LIGHT -> ColorProviders(scheme = lightColorScheme)
    theme == Theme.DARK -> ColorProviders(scheme = darkColorScheme)
    else -> ColorProviders(light = lightColorScheme, dark = darkColorScheme)
}

/**
 * A whole widget's background: Material's widget color, marked as the background for the
 * launcher, with the launcher's corner radius on Android 12+ (older ones don't round widgets).
 */
@Composable
fun GlanceModifier.widgetBackground(): GlanceModifier {
    val modifier = fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
        .appWidgetBackground()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        modifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    else modifier
}
