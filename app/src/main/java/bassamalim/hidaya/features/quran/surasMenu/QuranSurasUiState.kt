package bassamalim.hidaya.features.quran.surasMenu

data class QuranSurasUiState(
    val isLoading: Boolean = true,
    val bookmarks: List<BookmarkItem> = emptyList(),
    val isTutorialActive: Boolean = false,
)

/** [index] is the bookmark slot (0..3), which decides its color. */
data class BookmarkItem(
    val index: Int,
    val verseId: Int,
    val suraName: String,
    val verseNumText: String
)

data class SuraItem(
    val id: Int,
    val name: String,
    val numberText: String,
    val isMeccan: Boolean,
    val startPageText: String,
    val isFavorite: Boolean
)
