package bassamalim.hidaya.core.enums

import android.os.Build

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM,
    DYNAMIC;  // Material You: wallpaper colors, follows system dark mode (Android 12+)

    fun isDark(isSystemDark: Boolean) = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM, DYNAMIC -> isSystemDark
    }

    companion object {
        val available =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) entries
            else entries - DYNAMIC
    }
}
