package bassamalim.hidaya.features.books.booksMenu

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BooksMenuViewModel @Inject constructor(
    private val domain: BooksMenuDomain,
    private val navigator: Navigator
): ViewModel() {

    private lateinit var language: Language

    private val _uiState = MutableStateFlow(BooksMenuUiState())
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = BooksMenuUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            language = domain.getLanguage()

            _uiState.update { it.copy(
                isLoading = false,
                books = domain.getBooks(language),
                isTutorialActive = domain.getShowTutorial().first()
            )}
        }
    }

    fun onItemClick(id: Int, book: Book) {
        when (book.downloadState) {
            DownloadState.NOT_DOWNLOADED -> {
                _uiState.update { it.copy(
                    books = it.books.toMutableMap().apply {
                        this[id] = book.copy(downloadState = DownloadState.DOWNLOADING)
                    }
                )}

                domain.downloadBook(
                    bookId = id,
                    onDownloadedCallback = {
                        _uiState.update { it.copy(
                            books = it.books.toMutableMap().apply {
                                this[id] = book.copy(downloadState = DownloadState.DOWNLOADED)
                            }
                        )}
                    },
                    onFailedCallback = {
                        _uiState.update { it.copy(
                            books = it.books.toMutableMap().apply {
                                this[id] = book.copy(downloadState = DownloadState.NOT_DOWNLOADED)
                            }
                        )}
                    }
                )
            }
            DownloadState.DOWNLOADED -> {
                navigator.navigate(Screen.BookChaptersMenu(bookId = id))

                domain.trackBookOpened(id)
            }
            DownloadState.DOWNLOADING -> {
                _uiState.update { it.copy(
                    shouldShowWait = it.shouldShowWait + 1
                )}
            }
        }
    }

    fun onDownloadButtonClick(id: Int, book: Book) {
        when (book.downloadState) {
            DownloadState.NOT_DOWNLOADED -> {
                _uiState.update { it.copy(
                    books = it.books.toMutableMap().apply {
                        this[id] = book.copy(downloadState = DownloadState.DOWNLOADING)
                    }
                )}

                domain.downloadBook(
                    bookId = id,
                    onDownloadedCallback = {
                        _uiState.update { it.copy(
                            books = it.books.toMutableMap().apply {
                                this[id] = book.copy(downloadState = DownloadState.DOWNLOADED)
                            }
                        )}
                    },
                    onFailedCallback = {
                        _uiState.update { it.copy(
                            books = it.books.toMutableMap().apply {
                                this[id] = book.copy(downloadState = DownloadState.NOT_DOWNLOADED)
                            }
                        )}
                    }
                )
            }
            DownloadState.DOWNLOADED -> {
                _uiState.update { it.copy(
                    books = it.books.toMutableMap().apply {
                        this[id] = book.copy(downloadState = DownloadState.NOT_DOWNLOADED)
                    }
                )}

                domain.deleteBook(id)
            }
            DownloadState.DOWNLOADING -> {
                _uiState.update { it.copy(
                    shouldShowWait = it.shouldShowWait + 1
                )}
            }
        }
    }

    fun onSearcherClick(snackBarHostState: SnackbarHostState, message: String) {
        val noDownloadedBooks = !uiState.value.books.values.any {
            it.downloadState == DownloadState.DOWNLOADED
        }
        if (noDownloadedBooks) {
            viewModelScope.launch {
                snackBarHostState.showSnackbar(message)
            }
        }
        else navigator.navigate(Screen.BookSearcher)
    }

    fun onTutorialFinished() {
        _uiState.update { it.copy(
            isTutorialActive = false
        )}

        domain.setTutorialSeen()
    }

}