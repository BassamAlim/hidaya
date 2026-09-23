package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    title: String = "",
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            MyText(text = title, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        },
        modifier = Modifier.safeDrawingPadding(),
        navigationIcon = {
            MyBackButton(onClick = onBack)
        },
        actions = actions
    )
}