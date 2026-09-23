package bassamalim.hidaya.features.prayers.extraReminderSettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.DialogDismissButton
import bassamalim.hidaya.core.ui.components.DialogSubmitButton
import bassamalim.hidaya.core.ui.components.DialogTitle
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.components.MyValuedSlider

@Composable
fun PrayerExtraReminderSettingsDialog(viewModel: PrayerExtraReminderSettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return

    AlertDialog(
        onDismissRequest = viewModel::onDismiss,
        dismissButton = {
            DialogDismissButton { viewModel.onDismiss() }
        },
        confirmButton = {
            DialogSubmitButton(text = stringResource(R.string.save)) {
                viewModel.onSave()
            }
        },
        title = {
            DialogTitle(
                String.format(
                    stringResource(R.string.reminder_of),
                    // Arabic grammar, not a bug: the string ends in "ل", and "ل" + "الفجر"
                    // is written "للفجر", so the name's leading alef is dropped. English
                    // names don't start with "ا", so they pass through unchanged.
                    state.prayerName.removePrefix("ا")
                )
            )
        },
        text = {
            DialogContent(viewModel, state)
        }
    )
}

@Composable
private fun DialogContent(
    viewModel: PrayerExtraReminderSettingsViewModel,
    state: PrayerExtraReminderSettingsUiState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MyText(
            text = stringResource(R.string.reminder_time),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            textAlign = TextAlign.Start
        )

        MyValuedSlider(
            value = state.offset + viewModel.offsetMin,
            valueRange = 0F..60F,
            modifier = Modifier.fillMaxWidth(),
            progressMin = viewModel.offsetMin,
            valueFormatter = viewModel::formatSliderValue,
            onValueChange = { value -> viewModel.onOffsetChange(value.toInt()) }
        )
    }
}