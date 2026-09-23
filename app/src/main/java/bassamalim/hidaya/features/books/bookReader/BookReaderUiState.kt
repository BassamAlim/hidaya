package bassamalim.hidaya.features.books.bookReader

import bassamalim.hidaya.core.models.BookContent

data class BookReaderUiState(
    val isLoading: Boolean = true,
    val bookTitle: String = "",
    val chapterTitle: String = "",
    val doors: List<BookContent.Chapter.Door> = emptyList(),
    val textSize: Float = 15f,
    /** Where to open the chapter: the saved position if the user last stopped here, else the top */
    val initialDoorIndex: Int = 0,
    val initialScrollOffset: Int = 0,
    val previousChapterTitle: String? = null,
    val nextChapterTitle: String? = null,
    val isDoorsSheetShown: Boolean = false
)
