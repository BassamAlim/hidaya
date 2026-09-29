package bassamalim.hidaya.core.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sqrt

/** An eight-pointed star (Rub el Hizb): two squares, one turned 45°, with corners at [radius]. */
fun DrawScope.drawEightPointStar(color: Color, center: Offset, radius: Float, style: DrawStyle) {
    val side = radius * sqrt(2f)
    repeat(2) { i ->
        rotate(45f * i, pivot = center) {
            drawRect(
                color = color,
                topLeft = center - Offset(side / 2, side / 2),
                size = Size(side, side),
                style = style
            )
        }
    }
}
