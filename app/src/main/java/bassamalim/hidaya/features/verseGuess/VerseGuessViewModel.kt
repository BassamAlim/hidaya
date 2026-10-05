package bassamalim.hidaya.features.verseGuess

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.helpers.verseAudioUrl
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import bassamalim.hidaya.features.quran.reader.QuranTarget
import bassamalim.hidaya.features.verseGuess.map.VerseMapItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class VerseGuessViewModel @Inject constructor(
    app: Application,
    private val domain: VerseGuessDomain,
    private val navigator: Navigator
): ViewModel() {

    /** Set once loading completes, so callbacks never see a partial state. */
    private class LoadedData(
        val allItems: List<VerseMapItem>,
        /** Indexed by verse id - 1 */
        val pagePositions: DoubleArray,
        /** Indexed by verse id - 1 */
        val decoratedTexts: List<String>,
        /** Keyed by reciter id */
        val recitationSources: Map<Int, String>
    )

    private var data: LoadedData? = null

    /** Clip starts played in this scope, oldest first, so rounds don't come back too soon */
    private val recentClips = ArrayDeque<Int>()

    private val player = VerseClipPlayer(app)

    private val _uiState = MutableStateFlow(VerseGuessUiState())
    val uiState = combine(
        _uiState,
        player.playback,
        domain.observeProgress()
    ) { state, playback, progress ->
        state.copy(playback = playback, progress = progress)
    }.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = VerseGuessUiState()
    )

    private fun initializeData() {
        if (data != null) return

        viewModelScope.launch {
            val numeralsLanguage = domain.getNumeralsLanguage()
            val juzNumTexts = (1..JUZ_COUNT).map { translateNums(it.toString(), numeralsLanguage) }
            val verses = domain.getAllVerses()
            val items = verses.map {
                VerseMapItem(
                    id = it.id,
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
            val reciterNames = domain.getReciterNames()
            val reciterId = domain.getDefaultReciterId()
            data = LoadedData(
                allItems = items,
                pagePositions = pagePositions(items),
                decoratedTexts = verses.map { it.decoratedText },
                recitationSources = domain.getRecitationSources()
            )

            _uiState.update { it.copy(
                isLoading = false,
                numeralsLanguage = numeralsLanguage,
                suraNames = suraNames,
                juzNumTexts = juzNumTexts,
                reciterNames = reciterNames,
                reciterId = reciterId,
                items = items.inJuz(it.scopeJuz)
            )}
        }
    }

    fun onScopeChange(juz: Int) {
        val data = data ?: return
        recentClips.clear()
        _uiState.update { it.copy(scopeJuz = juz, items = data.allItems.inJuz(juz)) }
    }

    fun onReciterChange(reciterId: Int) {
        _uiState.update { it.copy(reciterId = reciterId) }
    }

    fun onStartClick() {
        _uiState.update { it.copy(sessionRounds = 0, sessionPoints = 0L) }
        startRound()
    }

    fun onPlayPauseClick() = player.togglePlayPause()

    fun onReplayClick() = player.replay()

    fun onClipVerseClick(index: Int) = player.playVerse(index)

    fun readVerseProgress() = player.verseProgress()

    fun onVerseSelect(index: Int) {
        if (_uiState.value.isRevealed) return
        _uiState.update { it.copy(selectedIndex = index) }
    }

    fun onSelectClick() {
        val data = data ?: return
        val state = _uiState.value
        val guess = state.selectedIndex ?: return
        val clipStart = state.clipStart ?: return
        if (state.isRevealed) return

        fun position(index: Int) = data.pagePositions[state.items[index].id - 1]
        val distance = distanceInPages(
            guess = position(guess),
            clip = (clipStart until clipStart + CLIP_LENGTH).map(::position)
        )
        val result = RoundResult(
            clipStart = clipStart,
            guessIndex = guess,
            distancePages = distance,
            points = roundPoints(distance, state.scopePages)
        )

        _uiState.update { it.copy(result = result, sessionPoints = it.sessionPoints + result.points) }
        viewModelScope.launch { domain.addRound(result) }
    }

    fun onNextClick() {
        if (_uiState.value.isRevealed) startRound()
    }

    fun onOpenInQuranClick(itemIndex: Int) {
        val item = _uiState.value.items.getOrNull(itemIndex) ?: return
        player.pause()
        navigator.navigate(Screen.QuranReader(targetType = QuranTarget.VERSE, targetValue = item.id))
    }

    fun onLeaderboardClick() {
        navigator.navigate(Screen.Leaderboard)
    }

    /** Leaving a game goes back to its setup rather than off the screen. */
    fun onBackPressed() {
        player.stop()
        _uiState.update { it.copy(phase = VerseGuessPhase.SETUP, selectedIndex = null, result = null) }
    }

    fun onStop() = player.pause()

    override fun onCleared() {
        player.release()
    }

    /**
     * Picks a fresh clip and plays it. The round and its verses change in one update, so the
     * screen never shows a round without its text.
     */
    private fun startRound() {
        val data = data ?: return
        val items = _uiState.value.items
        val clipStart = pickClipStart(items, Random.Default, recentClips) ?: return

        recentClips.addLast(clipStart)
        if (recentClips.size > RECENT_CLIPS_KEPT) recentClips.removeFirst()

        val clip = (clipStart until clipStart + CLIP_LENGTH).map { items[it] }
        _uiState.update { it.copy(
            phase = VerseGuessPhase.ROUND,
            sessionRounds = it.sessionRounds + 1,
            clipStart = clipStart,
            clipVerses = clip.map { item ->
                ClipVerse.fromDecoratedText(data.decoratedTexts[item.id - 1])
            },
            selectedIndex = null,
            result = null
        )}

        val source = data.recitationSources[_uiState.value.reciterId] ?: return
        player.play(clip.map { verseAudioUrl(source, it.suraNum, it.verseNum) })
    }

    private fun List<VerseMapItem>.inJuz(juz: Int) =
        if (juz == 0) this else filter { it.juzNum == juz }

    companion object {
        const val JUZ_COUNT = 30
        /**
         * The smallest juz, the 6th with 110 verses, holds about 36 clips that don't overlap, so
         * this spaces repeats out even there
         */
        private const val RECENT_CLIPS_KEPT = 30
    }

}
