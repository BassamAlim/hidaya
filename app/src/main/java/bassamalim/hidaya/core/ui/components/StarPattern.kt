package bassamalim.hidaya.core.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

// The union of two squares rotated 45° apart: its inner corners sit at this fraction of the outer
private const val STAR_INNER_RATIO = 0.7654f

/**
 * Draws a faint lattice of eight-pointed stars (Rub el Hizb) behind the content, with small
 * diamonds between them. It fades from invisible at the start edge to [color] at the end edge,
 * so text (which sits at the start) stays clean while the decoration shows beside it.
 *
 * Meant for hero surfaces only; pass a low-alpha [color].
 */
fun Modifier.starPattern(
    color: Color,
    cellSize: Dp = 44.dp,
    strokeWidth: Dp = 1.dp
): Modifier = drawWithCache {
    val cell = cellSize.toPx()
    val starRadius = cell * 0.34f
    val diamondRadius = cell * 0.1f

    val path = Path()
    val rows = ceil(size.height / cell).toInt()
    val cols = ceil(size.width / cell).toInt()
    for (row in 0..rows) {
        for (col in 0..cols) {
            path.addStar(Offset(col * cell, row * cell), starRadius)
            path.addDiamond(Offset((col + 0.5f) * cell, (row + 0.5f) * cell), diamondRadius)
        }
    }

    val isRtl = layoutDirection == LayoutDirection.Rtl
    val brush = Brush.horizontalGradient(
        colors =
            if (isRtl) listOf(color, color.copy(alpha = 0f))
            else listOf(color.copy(alpha = 0f), color)
    )
    val stroke = Stroke(width = strokeWidth.toPx())

    onDrawBehind {
        drawPath(path = path, brush = brush, style = stroke)
    }
}

private fun Path.addStar(center: Offset, radius: Float) {
    val innerRadius = radius * STAR_INNER_RATIO
    for (i in 0 until 16) {
        val angle = (i * PI / 8 - PI / 2).toFloat()
        val r = if (i % 2 == 0) radius else innerRadius
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun Path.addDiamond(center: Offset, radius: Float) {
    moveTo(center.x, center.y - radius)
    lineTo(center.x + radius, center.y)
    lineTo(center.x, center.y + radius)
    lineTo(center.x - radius, center.y)
    close()
}
