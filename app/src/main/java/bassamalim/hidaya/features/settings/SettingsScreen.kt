package bassamalim.hidaya.features.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.Reminder
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.enums.TimeFormat
import bassamalim.hidaya.core.ui.components.MyHorizontalDivider
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.TimePickerDialog
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.core.utils.LangUtils.translateNums

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current!!

    MyScaffold(title = stringResource(R.string.settings)) { padding ->
        // Everything visible at once: there are few enough settings that folding them only
        // costs taps
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = MaterialTheme.dimensions.screenPaddingHorizontal,
                    vertical = MaterialTheme.dimensions.spaceSm
                )
        ) {
            SettingsSection(title = stringResource(R.string.appearance)) {
                AppearanceSettings(
                    selectedLanguage = state.language,
                    onLanguageChange = {
                        viewModel.onLanguageChange(newLanguage = it, activity = activity)
                    },
                    selectedNumeralsLanguage = state.numeralsLanguage,
                    onNumeralsLanguageChange = viewModel::onNumeralsLanguageChange,
                    selectedTimeFormat = state.timeFormat,
                    onTimeFormatChange = viewModel::onTimeFormatChange,
                    selectedTheme = state.theme,
                    onThemeChange = viewModel::onThemeChange,
                    numeralsLanguage = state.numeralsLanguage
                )
            }

            SettingsSection(title = stringResource(R.string.extra_notifications)) {
                DevotionReminderSettings(
                    devotionReminderEnabledStatuses = state.devotionalReminderEnabledStatuses,
                    devotionReminderSummaries = state.devotionalReminderSummaries,
                    onDevotionReminderSwitch = viewModel::onDevotionReminderSwitch,
                    morningAndEveningRemembrancesEnabled =
                        state.morningAndEveningRemembrancesEnabled
                )
            }

            SettingsSection(title = stringResource(R.string.athan_settings)) {
                AthanSettings(
                    athanAudioId = state.athanAudioId,
                    onAthanAudioIdChange = viewModel::onAthanAudioIdChange
                )
            }
        }
    }

    if (state.isTimePickerShown) {
        TimePickerDialog(
            initialHour = viewModel.timePickerInitialHour,
            initialMinute = viewModel.timePickerInitialMinute,
            onConfirm = viewModel::onTimePicked,
            onDismiss = viewModel::onTimePickerDismiss
        )
    }
}

@Composable
fun AppearanceSettings(
    selectedLanguage: Language,
    onLanguageChange: (Language) -> Unit,
    selectedNumeralsLanguage: Language,
    onNumeralsLanguageChange: (Language) -> Unit,
    selectedTimeFormat: TimeFormat,
    onTimeFormatChange: (TimeFormat) -> Unit,
    selectedTheme: Theme,
    onThemeChange: (Theme) -> Unit,
    numeralsLanguage: Language
) {
    Column(Modifier.padding(bottom = 10.dp)) {
        // Language
        MenuSetting(
            selection = selectedLanguage,
            items = Language.entries.toTypedArray(),
            entries = stringArrayResource(R.array.language_entries),
            title = stringResource(R.string.language),
            icon = R.drawable.ic_translation,
            onSelection = onLanguageChange
        )

        MyHorizontalDivider(Modifier.padding(horizontal = 16.dp))

        // Numerals language
        MenuSetting(
            selection = selectedNumeralsLanguage,
            items = Language.entries.toTypedArray(),
            entries = stringArrayResource(R.array.numerals_language_entries),
            title = stringResource(R.string.numerals_language),
            icon = Icons.Default.Numbers,
            onSelection = onNumeralsLanguageChange
        )

        MyHorizontalDivider(Modifier.padding(horizontal = 16.dp))

        // Time format
        MenuSetting(
            selection = selectedTimeFormat,
            items = TimeFormat.entries.toTypedArray(),
            entries = stringArrayResource(R.array.time_format_entries).map {
                translateNums(numeralsLanguage = numeralsLanguage, string = it)
            }.toTypedArray(),
            title = stringResource(R.string.time_format),
            icon = R.drawable.ic_time_format,
            onSelection = onTimeFormatChange
        )

        MyHorizontalDivider(Modifier.padding(horizontal = 16.dp))

        // Theme
        val themeNames = stringArrayResource(R.array.themes_entries)
        MenuSetting(
            // DYNAMIC can be restored from a backup onto an older device, where it acts as SYSTEM
            selection = if (selectedTheme in Theme.available) selectedTheme else Theme.SYSTEM,
            items = Theme.available.toTypedArray(),
            entries = Theme.available.map { themeNames[it.ordinal] }.toTypedArray(),
            title = stringResource(R.string.theme),
            icon = Icons.Default.Contrast,
            onSelection = onThemeChange
        )
    }
}

@Composable
private fun DevotionReminderSettings(
    devotionReminderEnabledStatuses: Map<Reminder.Devotional, Boolean>,
    devotionReminderSummaries: Map<Reminder.Devotional, String>,
    onDevotionReminderSwitch: (Reminder.Devotional, Boolean) -> Unit,
    morningAndEveningRemembrancesEnabled: Boolean
) {
    // The maps are empty until the settings load, so rows fall back to off / no summary
    Column(Modifier.padding(bottom = 10.dp)) {
        SwitchSetting(
            value =
                devotionReminderEnabledStatuses[Reminder.Devotional.MorningRemembrances] ?: false,
            title = stringResource(R.string.morning_remembrance_title),
            summary = stringResource(R.string.thirty_minutes_after_fajr),
            enabled = morningAndEveningRemembrancesEnabled,
            onSwitch = { onDevotionReminderSwitch(Reminder.Devotional.MorningRemembrances, it) }
        )

        MyHorizontalDivider(Modifier.padding(horizontal = 16.dp))

        SwitchSetting(
            value =
                devotionReminderEnabledStatuses[Reminder.Devotional.EveningRemembrances] ?: false,
            title = stringResource(R.string.evening_remembrance_title),
            summary = stringResource(R.string.thirty_minutes_after_asr),
            enabled = morningAndEveningRemembrancesEnabled,
            onSwitch = { onDevotionReminderSwitch(Reminder.Devotional.EveningRemembrances, it) }
        )

        MyHorizontalDivider(Modifier.padding(horizontal = 16.dp))

        SwitchSetting(
            value = devotionReminderEnabledStatuses[Reminder.Devotional.DailyWerd] ?: false,
            title = stringResource(R.string.daily_werd_title),
            summary = devotionReminderSummaries[Reminder.Devotional.DailyWerd].orEmpty(),
            onSwitch = { onDevotionReminderSwitch(Reminder.Devotional.DailyWerd, it) }
        )

        MyHorizontalDivider(Modifier.padding(horizontal = 16.dp))

        SwitchSetting(
            value = devotionReminderEnabledStatuses[Reminder.Devotional.FridayKahf] ?: false,
            title = stringResource(R.string.friday_kahf_title),
            summary = devotionReminderSummaries[Reminder.Devotional.FridayKahf].orEmpty(),
            onSwitch = { onDevotionReminderSwitch(Reminder.Devotional.FridayKahf, it) }
        )
    }
}

@Composable
private fun AthanSettings(athanAudioId: Int, onAthanAudioIdChange: (Int) -> Unit) {
    Column(
        Modifier.padding(bottom = 10.dp)
    ) {
        MenuSetting(
            selection = athanAudioId,
            items = stringArrayResource(R.array.athan_voices_entries)
                .mapIndexed { i, _ -> i+1 }.toTypedArray(),
            entries = stringArrayResource(R.array.athan_voices_entries),
            title = stringResource(R.string.athan_voice),
            icon = Icons.Default.Campaign,
            onSelection = onAthanAudioIdChange
        )
    }
}