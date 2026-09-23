package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MySurface(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(vertical = 6.dp, horizontal = 8.dp),
    cornerRadius: Dp = 10.dp,
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = elevation,
        tonalElevation = elevation
    ) {
        content()
    }
}

@Composable
fun MyClickableSurface(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(vertical = 3.dp, horizontal = 8.dp),
    cornerRadius: Dp = 10.dp,
    elevation: Dp = 2.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.padding(padding),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = elevation,
        tonalElevation = elevation
    ) {
        Box(
            modifier = Modifier.clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
