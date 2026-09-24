package bassamalim.hidaya.features.recitations.recitersMenu

import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.PlaybackStatus
import bassamalim.hidaya.features.quran.surasMenu.RecitationInfo

data class RecitationRecitersMenuUiState(
    val isLoading: Boolean = true,
    val numeralsLanguage: Language = Language.ARABIC,
    val playbackRecitationInfo: RecitationInfo? = null,
    val playbackState: PlaybackStatus = PlaybackStatus.CONNECTING,
    val searchText: String = "",
    val isFiltered: Boolean = false,
    val expandedReciterIds: Set<Int> = emptySet()
)