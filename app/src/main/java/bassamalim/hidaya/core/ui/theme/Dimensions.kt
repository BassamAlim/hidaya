package bassamalim.hidaya.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Semantic design tokens. Prefer these over raw `.dp` literals so spacing/sizing
 * stays consistent and is centrally tweakable. Most values follow a 4dp grid.
 *
 * Access via `MaterialTheme.dimensions.<token>` from any @Composable.
 */
data class AppDimensions(
    // Spacing — use for padding, margins, gaps between elements.
    val spaceXs: Dp = 4.dp,
    val spaceSm: Dp = 8.dp,
    val spaceMd: Dp = 12.dp,
    val spaceLg: Dp = 16.dp,
    val spaceXl: Dp = 24.dp,
    val spaceXxl: Dp = 32.dp,

    // Screen-level padding for the outermost content container.
    val screenPaddingHorizontal: Dp = 16.dp,
    val screenPaddingVertical: Dp = 16.dp,

    // Icon sizes.
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 24.dp,
    val iconLg: Dp = 32.dp,
    val iconXl: Dp = 40.dp,

    // Component sizes.
    val minTouchTarget: Dp = 48.dp,
    val buttonHeight: Dp = 48.dp,
    val topBarHeight: Dp = 56.dp,
    val bottomBarHeight: Dp = 80.dp,
    val listItemHeight: Dp = 56.dp,

    // Corner radii (kept in sync with `shapes`).
    val radiusSm: Dp = 8.dp,
    val radiusMd: Dp = 12.dp,
    val radiusLg: Dp = 20.dp,
    val radiusXl: Dp = 28.dp,

    // Elevation. Kept low: surfaces separate mainly by color, shadows only soften the edge.
    val elevationSm: Dp = 1.dp,
    val elevationMd: Dp = 4.dp,
    val elevationLg: Dp = 8.dp,

    // Misc.
    val dividerThickness: Dp = 1.dp,
    val borderThin: Dp = 1.dp,
    val borderThick: Dp = 2.dp,
)

internal val LocalDimensions = staticCompositionLocalOf { AppDimensions() }

val MaterialTheme.dimensions: AppDimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalDimensions.current
