package bassamalim.hidaya.features.books.bookReader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.data.dataSources.preferences.objects.BookReadingPosition
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookReaderViewModel @Inject constructor(
    private val domain: BookReaderDomain,
    private val navigator: Navigator,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val bookId = savedStateHandle.get<Int>("book_id") ?: 0
    private val chapterId = savedStateHandle.get<Int>("chapter_id") ?: 0

    private val _uiState = MutableStateFlow(BookReaderUiState())
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getTextSize()
    ) { state, textSize -> state.copy(
        textSize = textSize
    )}.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = BookReaderUiState()
    )

    private suspend fun initializeData() {
        val chapterTitles = domain.getChapterTitles(bookId)
        // Only resume mid-chapter if this is the chapter the user stopped in
        val savedPosition = domain.getReadingPosition(bookId).first()
            ?.takeIf { it.chapterId == chapterId }

        _uiState.update { it.copy(
            isLoading = false,
            bookTitle = domain.getBookTitle(bookId, domain.getLanguage()),
            chapterTitle = chapterTitles.getOrElse(chapterId) { "" },
            doors = domain.getDoors(bookId, chapterId),
            initialDoorIndex = savedPosition?.doorIndex ?: 0,
            initialScrollOffset = savedPosition?.scrollOffset ?: 0,
            previousChapterTitle = chapterTitles.getOrNull(chapterId - 1),
            nextChapterTitle = chapterTitles.getOrNull(chapterId + 1)
        )}
    }

    fun onReadingPositionChange(doorIndex: Int, scrollOffset: Int) {
        viewModelScope.launch {
            domain.setReadingPosition(
                bookId = bookId,
                position = BookReadingPosition(
                    chapterId = chapterId,
                    doorIndex = doorIndex,
                    scrollOffset = scrollOffset
                )
            )
        }
    }

    fun onPreviousChapterClick() {
        openChapter(chapterId - 1)
    }

    fun onNextChapterClick() {
        openChapter(chapterId + 1)
    }

    private fun openChapter(targetChapterId: Int) {
        navigator.navigate(
            Screen.BookReader(bookId = bookId.toString(), chapterId = targetChapterId.toString())
        ) {
            // Replace this chapter instead of stacking one reader per chapter on the back stack
            popUpTo(Screen.BookReader("{book_id}", "{chapter_id}").route) { inclusive = true }
        }
    }

    fun onDoorsClick() {
        _uiState.update { it.copy(isDoorsSheetShown = true) }
    }

    fun onDoorsSheetDismiss() {
        _uiState.update { it.copy(isDoorsSheetShown = false) }
    }

    fun onTextSizeChange(textSize: Float) {
        viewModelScope.launch {
            domain.setTextSize(textSize)
        }
    }

}
