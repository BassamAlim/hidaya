package bassamalim.hidaya.features.quran.surasMenu

import androidx.compose.ui.graphics.Color
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.data.dataSources.room.entities.Verse
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.MenuType
import bassamalim.hidaya.core.models.Sura
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import bassamalim.hidaya.features.quran.reader.QuranTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuranSurasViewModel @Inject constructor(
    private val domain: QuranSurasDomain,
    private val navigator: Navigator
): ViewModel() {

    /**
     * Holds all data loaded asynchronously in [initializeData]. Kept as a single nullable holder
     * set atomically once loading completes, so callbacks can never observe a partially
     * initialized state (previously the source of [UninitializedPropertyAccessException] crashes
     * when a callback fired before the loading coroutine finished).
     */
    private data class LoadedData(
        val numeralsLanguage: Language,
        val suraNames: List<String>,
        val allSurasFlow: Flow<List<Sura>>,
        val allSuras: List<Sura>,
        val allVerses: List<Verse>,
        /** Keyed by 0-based sura id */
        val suraStartPages: Map<Int, Int>
    )

    private var data: LoadedData? = null

    private val _uiState = MutableStateFlow(QuranSurasUiState())
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getBookmarks()
    ) { state, bookmarks ->
        val data = data
        if (state.isLoading || data == null) state
        else state.copy(
            bookmarks = listOf(
                bookmarks.bookmark1VerseId,
                bookmarks.bookmark2VerseId,
                bookmarks.bookmark3VerseId,
                bookmarks.bookmark4VerseId
            ).mapIndexedNotNull { index, verseId ->
                val verse = data.allVerses.firstOrNull { it.id == verseId }
                    ?: return@mapIndexedNotNull null
                BookmarkItem(
                    index = index,
                    verseId = verse.id,
                    suraName = data.suraNames[verse.suraNum - 1],
                    verseNumText = translateNums(
                        string = verse.num.toString(),
                        numeralsLanguage = data.numeralsLanguage
                    )
                )
            }
        )
    }.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = QuranSurasUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            val language = domain.getLanguage()
            val allSurasFlow = domain.getAllSuras(language)
            val allVerses = domain.getAllVerses()
            data = LoadedData(
                numeralsLanguage = domain.getNumeralsLanguage(),
                suraNames = domain.getSuraNames(language),
                allSurasFlow = allSurasFlow,
                allSuras = allSurasFlow.first(),
                allVerses = allVerses,
                suraStartPages = allVerses.groupBy { it.suraNum - 1 }
                    .mapValues { (_, verses) -> verses.minOf { it.pageNum } }
            )

            _uiState.update { it.copy(
                isLoading = false,
                isTutorialActive = domain.getShouldShowTutorial()
            )}
        }
    }

    fun onTutorialFinished() {
        _uiState.update { it.copy(isTutorialActive = false) }
        viewModelScope.launch {
            domain.setTutorialSeen()
        }
    }

    fun onSuraClick(suraId: Int) {
        navigator.navigate(
            Screen.QuranReader(
                targetType = QuranTarget.SURA.name,
                targetValue = suraId.toString()
            )
        )

        data?.let { domain.trackSuraViewed(it.suraNames[suraId]) }
    }

    fun onPageClick(pageNum: String) {
        navigator.navigate(
            Screen.QuranReader(
                targetType = QuranTarget.PAGE.name,
                targetValue = pageNum
            )
        )
    }

    fun onBookmarkClick(verseId: Int) {
        navigator.navigate(
            Screen.QuranReader(
                targetType = QuranTarget.VERSE.name,
                targetValue = verseId.toString()
            )
        )
    }

    fun onFavoriteClick(itemId: Int, oldState: Boolean) {
        viewModelScope.launch {
            domain.setFav(suraId = itemId, fav = !oldState)
        }
    }

    fun getItems(page: Int): Flow<List<SuraItem>> {
        val data = data ?: return flowOf(emptyList())
        val menuType = MenuType.entries[page]

        return data.allSurasFlow.map { suras ->
            suras.filter { sura ->
                !(menuType == MenuType.FAVORITES && !sura.isFavorite)
            }.map { sura ->
                SuraItem(
                    id = sura.id,
                    name = data.suraNames[sura.id],
                    numberText = translateNums(
                        string = (sura.id + 1).toString(),
                        numeralsLanguage = data.numeralsLanguage
                    ),
                    // Revelation 0 is Meccan, 1 is Medinan
                    isMeccan = sura.revelation == 0,
                    startPageText = translateNums(
                        string = data.suraStartPages.getValue(sura.id).toString(),
                        numeralsLanguage = data.numeralsLanguage
                    ),
                    isFavorite = sura.isFavorite
                )
            }
        }
    }

    fun searchSurasAndPages(query: String): List<SearchMatch> {
        val data = data ?: return emptyList()

        return if (query.isEmpty()) {
            data.allSuras.take(3).map { sura ->
                SuraMatch(
                    id = sura.id,
                    decoratedName = data.suraNames[sura.id],
                    plainName = sura.plainName,
                    isFavorite = sura.isFavorite
                )
            }
        }
        else if (query.isDigitsOnly()) {
            val num = query.toInt()
            if (num in 1..604) {
                val pageSuraId = data.allVerses.first { verse -> verse.pageNum == num }.suraNum - 1
                listOf(
                    PageMatch(
                        num = translateNums(
                            string = query,
                            numeralsLanguage = data.numeralsLanguage
                        ),
                        suraName = data.suraNames[pageSuraId]
                    )
                )
            }
            else emptyList()
        }
        else {
            domain.searchSuras(
                query = query,
                items = data.allSuras,
                limit = 3
            )
        }
    }

    fun searchVerses(query: String, highlightColor: Color): List<VerseMatch> {
        val data = data ?: return emptyList()

        return domain.searchVerses(
            query = query,
            items = data.allVerses,
            suraNames = data.suraNames,
            numeralsLanguage = data.numeralsLanguage,
            highlightColor = highlightColor
        )
    }

    fun onVerseClick(verseId: Int) {
        navigator.navigate(
            Screen.QuranReader(
                targetType = QuranTarget.VERSE.name,
                targetValue = verseId.toString()
            )
        )
    }

}