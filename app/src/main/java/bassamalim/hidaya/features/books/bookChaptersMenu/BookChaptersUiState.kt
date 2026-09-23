package bassamalim.hidaya.features.books.bookChaptersMenu

data class BookChaptersUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val searchText: String = "",
    /** The chapter the user last stopped in, if they've started this book */
    val continueReading: ContinueReading? = null
)

data class ContinueReading(val chapterId: Int, val chapterTitle: String)