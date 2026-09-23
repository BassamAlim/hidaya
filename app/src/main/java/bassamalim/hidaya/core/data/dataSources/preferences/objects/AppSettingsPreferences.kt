package bassamalim.hidaya.core.data.dataSources.preferences.objects

import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.enums.TimeFormat
import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsPreferences(
    val numeralsLanguage: Language = Language.ARABIC,
    val theme: Theme = Theme.SYSTEM,
    val timeFormat: TimeFormat = TimeFormat.TWELVE,
    val dateOffset: Int = 0,
)