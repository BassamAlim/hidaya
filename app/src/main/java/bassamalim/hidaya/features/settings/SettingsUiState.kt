package bassamalim.hidaya.features.settings

import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.Reminder
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.enums.TimeFormat
import bassamalim.hidaya.core.models.TimeOfDay

data class SettingsUiState(
    val language: Language = Language.ARABIC,
    val numeralsLanguage: Language = Language.ARABIC,
    val timeFormat: TimeFormat = TimeFormat.TWELVE,
    val theme: Theme = Theme.SYSTEM,
    val devotionalReminderEnabledStatuses: Map<Reminder.Devotional, Boolean> = emptyMap(),
    val devotionalReminderTimes: Map<Reminder.Devotional, TimeOfDay> = emptyMap(),
    val devotionalReminderSummaries: Map<Reminder.Devotional, String> = emptyMap(),
    val isTimePickerShown: Boolean = false,
    val morningAndEveningRemembrancesEnabled: Boolean = false,
    val athanAudioId: Int = 0,
)