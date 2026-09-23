package bassamalim.hidaya.core.startup

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.ui.theme.getColorScheme
import bassamalim.hidaya.core.ui.theme.isSystemDark
import javax.inject.Inject

class ThemeApplier @Inject constructor() {

    fun apply(activity: ComponentActivity, theme: Theme) {
        val colorScheme = getColorScheme(theme, activity)
        activity.window.decorView.setBackgroundColor(colorScheme.surface.toArgb())

        activity.enableEdgeToEdge(
            statusBarStyle =
                if (theme.isDark(activity.isSystemDark()))
                    SystemBarStyle.dark(scrim = Color.Transparent.toArgb())
                else SystemBarStyle.light(
                    scrim = Color.Transparent.toArgb(),
                    darkScrim = Color.White.toArgb()
                )
        )
    }

}
