package bassamalim.hidaya.features.recitations.recitersMenu

import android.support.v4.media.session.PlaybackStateCompat
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.features.quran.surasMenu.RecitationInfo

data class RecitationRecitersMenuUiState(
    val isLoading: Boolean = true,
    val numeralsLanguage: Language = Language.ARABIC,
    val playbackRecitationInfo: RecitationInfo? = null,
    val playbackState: Int = PlaybackStateCompat.STATE_NONE,
    val searchText: String = "",
    val isFiltered: Boolean = false,
    val expandedReciterIds: Set<Int> = emptySet()
)