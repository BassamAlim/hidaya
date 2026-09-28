package bassamalim.hidaya.features.prayers.board

import bassamalim.hidaya.core.enums.Prayer
import java.util.SortedMap

data class PrayersBoardUiState(
    val loading: Boolean = true,
    val locationAvailable: Boolean = false,
    val locationName: String = "",
    val hasZoneMismatch: Boolean = false,
    val prayersData: SortedMap<Prayer, PrayerCardData> = sortedMapOf(),
    val noDateOffset: Boolean = true,
    val dateText: String = "",
    val settingsDialogShown: Boolean = false,
    val timeCalculationSettingsDialogShown: Boolean = false,
    val isTutorialActive: Boolean = false,
    val report: ReportUiState = ReportUiState(),
)

data class ReportUiState(
    val dialogShown: Boolean = false,
    val step: ReportStep = ReportStep.CHECKS,
    val currentMethodName: String = "",
    val isAutoLocation: Boolean = false,
    val prayerNames: Map<Prayer, String> = emptyMap(),
    val computedTimes: Map<Prayer, String> = emptyMap(),
    val wrongPrayers: Set<Prayer> = emptySet(),
    val correctTimes: Map<Prayer, String> = emptyMap(),
    val notes: String = "",
    val timePickerTarget: Prayer? = null,
    val submitting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
)
