package bassamalim.hidaya.features.quran.verseInfo

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.ui.theme.uthmanic_hafs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.navigation.toRoute
import bassamalim.hidaya.core.nav.Screen

@HiltViewModel
class VerseInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: VerseInfoDomain,
    private val navigator: Navigator
): ViewModel() {

    private val verseId = savedStateHandle.toRoute<Screen.VerseInfo>().verseId

    private val _uiState = MutableStateFlow(VerseInfoUiState())
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getBookmarks()
    ) { uiState, bookmarks ->
        uiState.copy(bookmarks = bookmarks)
    }.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = VerseInfoUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            val verse = domain.getVerse(verseId)

            _uiState.update { it.copy(
                isLoading = false,
                verseId = verseId,
                verseText = verse.decoratedText,
                interpretation = annotateInterpretation(verse.interpretation)
            )}
        }
    }

    fun onBookmarkClick(index: Int, bookmarkVerseId: Int?) {
        viewModelScope.launch {
            domain.setBookmarkVerseId(
                index = index,
                verseId = if (bookmarkVerseId == verseId) null else verseId
            )
        }
    }

    private fun annotateInterpretation(interpretation: String): AnnotatedString {
        return buildAnnotatedString {
            val regex = Regex("(<aya>(.*?)</aya>)")
            var lastIndex = 0

            regex.findAll(interpretation).forEach { matchResult ->
                val ayaStartIndex = matchResult.range.first
                val ayaEndIndex = matchResult.range.last + 1

                append(interpretation.substring(lastIndex, ayaStartIndex))

                pushStyle(SpanStyle(fontFamily = uthmanic_hafs))
                append(matchResult.groups[2]?.value.orEmpty()) // Groups[2] contains the text within the tags
                pop()

                lastIndex = ayaEndIndex
            }

            if (lastIndex < interpretation.length) {
                append(interpretation.substring(lastIndex))
            }
        }
    }

    fun onDismiss() {
        navigator.popBackStack()
    }

}