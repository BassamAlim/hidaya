package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.theme.dimensions

@Composable
fun MyDialog(
    shown: Boolean,
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit
) {
    if (shown) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                shape = RoundedCornerShape(MaterialTheme.dimensions.radiusXl),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                content()
            }
        }
    }
}

@Composable
fun FullScreenDialog(
    shown: Boolean,
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit
) {
    if (shown) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        shape = RoundedCornerShape(MaterialTheme.dimensions.radiusXl),
                        color = MaterialTheme.colorScheme.surface
                    )
            ) {
                content()
            }
        }
    }
}

@Composable
fun DialogTitle(title: String) {
    MyText(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun DialogSubmitButton(text: String = stringResource(R.string.select), onSubmit: () -> Unit) {
    TextButton(onClick = onSubmit) {
        MyText(text = text, modifier = Modifier.padding(horizontal = 6.dp))
    }
}

@Composable
fun DialogDismissButton(text: String = stringResource(R.string.cancel), onDismiss: () -> Unit) {
    TextButton(onClick = onDismiss) {
        MyText(text = text, modifier = Modifier.padding(horizontal = 6.dp))
    }
}