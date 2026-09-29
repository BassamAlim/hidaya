package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

// Height of Tajawal's visual middle above the baseline, in em: the centre of its x-height and of
// the main body of Arabic letters (both about 0.45em tall). The font's line box is centred
// lower (about 0.14em), so centring an icon on the text's box leaves the text looking high.
private const val TAJAWAL_VISUAL_MIDDLE_EM = 0.23f

@Composable
fun MyRow(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
    ) {
        content()
    }
}

/**
 * For an icon in a [Row] next to Tajawal text: centres the icon on the text's visual middle
 * instead of its line box. Give the text `Modifier.alignByBaseline()`.
 */
@Composable
fun RowScope.alignIconWithText(fontSize: TextUnit): Modifier {
    val middle = with(LocalDensity.current) { (fontSize * TAJAWAL_VISUAL_MIDDLE_EM).toPx() }
    return Modifier.alignBy { icon -> icon.measuredHeight / 2 + middle.roundToInt() }
}
