package bassamalim.hidaya.features.quran.reader

import bassamalim.hidaya.core.enums.PlaybackStatus
import bassamalim.hidaya.core.enums.QuranViewType
import bassamalim.hidaya.core.models.Verse
import bassamalim.hidaya.features.quran.surasMenu.BookmarkItem

data class QuranReaderUiState(
    val isLoading: Boolean = true,
    val pageNum: String = "",
    val juzNum: String = "",
    val suraName: String = "",
    val pageVerses: List<Verse> = emptyList(),
    val trackedVerseId: Int = -1,
    val selectedVerse: Verse? = null,
    val viewType: QuranViewType = QuranViewType.PAGE,
    val fillPage: Boolean = false,
    val textSize: Float = 15f,
    val keepScreenOn: Boolean = false,
    val playerState: PlaybackStatus = PlaybackStatus.STOPPED,
    val bookmarks: List<BookmarkItem> = emptyList(),
    val isBookmarksSheetShown: Boolean = false,
    val isTutorialActive: Boolean = false,
    val navigateToPage: Int? = null,
    val scrollToVersePosition: Float? = null
)