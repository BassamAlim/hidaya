package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun <V> HorizontalRadioGroup(
    selection: V,
    items: List<V>,
    entries: Array<String>,
    onSelect: (V) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        entries.forEachIndexed { index, text ->
            val item = items[index]

            MySquareButton(
                text = text,
                fontSize = 20.sp,
                textColor =
                    if (item == selection) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                innerPadding = PaddingValues(vertical = 1.dp),
                modifier = Modifier
                    .weight(1F)
                    .padding(horizontal = 5.dp),
                isSelected = item == selection,
                onClick = { onSelect(items[index]) }
            )
        }
    }
}