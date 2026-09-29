package bassamalim.hidaya.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import bassamalim.hidaya.R

val tajwal = FontFamily(
    Font(R.font.tajawal_regular, weight = FontWeight.Normal),
    Font(R.font.tajawal_extra_bold, weight = FontWeight.ExtraBold),
    Font(R.font.tajawal_bold, weight = FontWeight.Bold),
    Font(R.font.tajawal_medium, weight = FontWeight.Medium),
    Font(R.font.tajawal_light, weight = FontWeight.Light),
    Font(R.font.tajawal_extra_light, weight = FontWeight.ExtraLight),
    Font(R.font.tajawal_black, weight = FontWeight.Black),
)

val hafs_smart = FontFamily(Font(R.font.hafs_smart_8))
val uthmanic_hafs = FontFamily(Font(R.font.uthmanic_hafs_v22))

/**
 * App-wide text styles. Prefer these over ad-hoc `TextStyle(...)` so type ramps
 * stay consistent across screens.
 *
 * Access via `MaterialTheme.appTypography.<style>` from any @Composable.
 *
 * Styles are ordered from largest to smallest:
 *   display (28) > h1 (24) > headline (20) > title (18) > body/subtitle/button (16) > label (14) > caption (12)
 */
data class AppTypography(
    val display: TextStyle = TextStyle(
        fontFamily = tajwal,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    val h1: TextStyle = TextStyle(
        fontFamily = tajwal,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = tajwal,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    ),
    val title: TextStyle = TextStyle(
        fontFamily = tajwal,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    val subtitle: TextStyle = TextStyle(
        fontFamily = tajwal,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = tajwal,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    val button: TextStyle = TextStyle(
        fontFamily = tajwal,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    val label: TextStyle = TextStyle(
        fontFamily = tajwal,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = tajwal,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
)

internal val LocalTypography = staticCompositionLocalOf { AppTypography() }

val MaterialTheme.appTypography: AppTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalTypography.current
