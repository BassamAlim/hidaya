package bassamalim.hidaya.features.verseGuess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import bassamalim.hidaya.features.verseGuess.map.VerseMapItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerseGuessViewModel @Inject constructor(
    private val domain: VerseGuessDomain
): ViewModel() {

    /** Set once loading completes, so callbacks never see a partial state. */
    private var allItems: List<VerseMapItem>? = null

    private val _uiState = MutableStateFlow(VerseGuessUiState())
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = VerseGuessUiState()
    )

    private fun initializeData() {
        if (allItems != null) return

        viewModelScope.launch {
            val numeralsLanguage = domain.getNumeralsLanguage()
            val juzNumTexts = (1..JUZ_COUNT).map { translateNums(it.toString(), numeralsLanguage) }
            val items = domain.getAllVerses().map {
                VerseMapItem(
                    suraNum = it.suraNum,
                    verseNum = it.num,
                    pageNum = it.pageNum,
                    juzNum = it.juzNum,
                    textLength = it.plainText.length,
                    verseNumText = translateNums(it.num.toString(), numeralsLanguage),
                    pageNumText = translateNums(it.pageNum.toString(), numeralsLanguage),
                    juzNumText = juzNumTexts[it.juzNum - 1]
                )
            }
            val suraNames = domain.getSuraNames(domain.getLanguage())
            allItems = items

            _uiState.update { it.copy(
                isLoading = false,
                suraNames = suraNames,
                juzNumTexts = juzNumTexts,
                items = items.inJuz(it.scopeJuz)
            )}
        }
    }

    fun onScopeChange(juz: Int) {
        val allItems = allItems ?: return
        _uiState.update { it.copy(
            scopeJuz = juz,
            items = allItems.inJuz(juz),
            selectedIndex = null
        )}
    }

    fun onVerseSelect(index: Int) {
        _uiState.update { it.copy(selectedIndex = index) }
    }

    private fun List<VerseMapItem>.inJuz(juz: Int) =
        if (juz == 0) this else filter { it.juzNum == juz }

    companion object {
        const val JUZ_COUNT = 30
    }

}
