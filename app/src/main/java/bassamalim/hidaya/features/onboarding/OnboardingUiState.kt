package bassamalim.hidaya.features.onboarding

import bassamalim.hidaya.core.enums.HighLatitudesAdjustmentMethod
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.PrayerTimeCalculationMethod
import bassamalim.hidaya.core.enums.PrayerTimeJuristicMethod
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.enums.TimeFormat

data class OnboardingUiState(
    val language: Language = Language.ARABIC,
    val numeralsLanguage: Language = Language.ARABIC,
    val timeFormat: TimeFormat = TimeFormat.TWELVE,
    val theme: Theme = Theme.SYSTEM,
    val calculationMethod: PrayerTimeCalculationMethod = PrayerTimeCalculationMethod.MECCA,
    val juristicMethod: PrayerTimeJuristicMethod = PrayerTimeJuristicMethod.SHAFII,
    val highLatitudesAdjustment: HighLatitudesAdjustmentMethod = HighLatitudesAdjustmentMethod.NONE
)