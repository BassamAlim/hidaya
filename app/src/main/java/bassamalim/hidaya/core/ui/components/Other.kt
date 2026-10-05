package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MyFloatingActionButton(
    imageVector: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FloatingActionButton(onClick = onClick, modifier = modifier) {
        Icon(imageVector = imageVector, contentDescription = description)
    }
}

@Composable
fun MyHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    padding: PaddingValues = PaddingValues(vertical = 5.dp)
) {
    HorizontalDivider(
        modifier = modifier.padding(padding),
        thickness = thickness,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2F)
    )
}

@Composable
fun MyCheckbox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
    isEnabled: Boolean = true
) {
    Checkbox(checked = isChecked, onCheckedChange = onCheckedChange, enabled = isEnabled)
}

@Composable
fun MultiDrawableImage(
    drawables: List<Pair<Int, Color>>,
    modifier: Modifier = Modifier,
    innerModifier: Modifier = Modifier,
    contentDescription: String? = ""
) {
    Box(modifier) {
        drawables.forEach { (resId, color) ->
            Image(
                painter = painterResource(resId),
                contentDescription = contentDescription,
                modifier = innerModifier,
                colorFilter = ColorFilter.tint(color)
            )
        }
    }
}