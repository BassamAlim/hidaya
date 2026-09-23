package bassamalim.hidaya.features.home

import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.TimeOfDay

data class HomeUiState(
    val isLoading: Boolean = true,
    val pendingPermissions: List<PendingPermission> = emptyList(),
    val previousPrayerName: String = "",
    val previousPrayerTimeText: String = "",
    val passed: String = "",
    val nextPrayerName: String = "",
    val nextPrayerTimeText: String = "",
    val remaining: String = "",
    val previousPrayerTime: TimeOfDay? = null,
    val nextPrayerTime: TimeOfDay? = null,
    val todayPrayers: List<TodayPrayer> = emptyList(),
    val isMorning: Boolean = true,
    val werdPage: String = "",
    val isWerdDone: Boolean = false,
    val quranRecord: String = "",
    val recitationsRecord: String = "",
    val isLeaderboardEnabled: Boolean = false,
    val language: Language = Language.ARABIC,
    val numeralsLanguage: Language = Language.ARABIC
)

data class TodayPrayer(val name: String, val timeText: String, val status: Status) {
    enum class Status { PASSED, NEXT, UPCOMING }
}
