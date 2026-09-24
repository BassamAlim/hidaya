package bassamalim.hidaya.features.quran.reader

import android.app.Activity
import android.media.AudioManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.PlaybackStatus
import bassamalim.hidaya.core.enums.QuranViewType
import bassamalim.hidaya.core.helpers.PlayerConnection
import bassamalim.hidaya.core.helpers.playbackStatus
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import bassamalim.hidaya.features.quran.reader.versePlayer.VersePlayerService
import bassamalim.hidaya.features.quran.surasMenu.BookmarkItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import bassamalim.hidaya.core.data.dataSources.room.entities.Verse as VerseEntity

@HiltViewModel
class QuranReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: QuranReaderDomain,
    private val navigator: Navigator
): ViewModel() {

    private val targetType = QuranTarget.valueOf(
        savedStateHandle.get<String>("target_type") ?: QuranTarget.SURA.name
    )
    private var targetValue = savedStateHandle.get<Int>("target_value") ?: 0

    lateinit var language: Language
    lateinit var numeralsLanguage: Language
    private lateinit var suraNames: List<String>
    private lateinit var allVerses: List<VerseEntity>
    lateinit var pageBuilder: QuranPageBuilder
        private set
    var pageNum = 0
        private set
    private var suraId = 0
    var scrollTo = -1F
        private set
    private val versePositions = mutableMapOf<Int, Float>()
    private var pendingVerseSelectionId: Int? = null
    private var pressedVerseId: Int? = null
    private var longPressJob: Job? = null

    private val _uiState = MutableStateFlow(QuranReaderUiState())
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getViewType(),
        domain.getFillPage(),
        domain.getTextSize(),
        domain.getKeepScreenOn()
    ) { state, viewType, fillPage, textSize, keepScreenOn ->
        if (state.isLoading) return@combine state

        state.copy(
            pageNum = translateNums(
                string = pageNum.toString(),
                numeralsLanguage = numeralsLanguage
            ),
            viewType = if (language == Language.ARABIC) viewType else QuranViewType.LIST,
            fillPage = fillPage,
            textSize = textSize,
            keepScreenOn = keepScreenOn
        )
    }.combine(domain.getBookmarks()) { state, bookmarks ->
        // Verses and names load with the rest of the data, so wait for it
        if (state.isLoading) return@combine state

        state.copy(
            bookmarks = listOf(
                bookmarks.bookmark1VerseId,
                bookmarks.bookmark2VerseId,
                bookmarks.bookmark3VerseId,
                bookmarks.bookmark4VerseId
            ).mapIndexedNotNull { index, verseId ->
                val verse = allVerses.firstOrNull { it.id == verseId }
                    ?: return@mapIndexedNotNull null
                BookmarkItem(
                    index = index,
                    verseId = verse.id,
                    suraName = suraNames[verse.suraNum - 1],
                    verseNumText = translateNums(
                        string = verse.num.toString(),
                        numeralsLanguage = numeralsLanguage
                    )
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = QuranReaderUiState()
    )

    init {
        // must run once per instance, not on every re-subscription, otherwise going back to the
        // screen after the subscription times out would reset the reader to the navigation target
        initializeData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            language = domain.getLanguage()
            numeralsLanguage = domain.getNumeralsLanguage()

            allVerses = domain.getAllVerses()
            suraNames = domain.getSuraNames(language)
            pageBuilder = QuranPageBuilder(allVerses, suraNames, language)

            pageNum = when (targetType) {
                QuranTarget.PAGE -> targetValue
                QuranTarget.SURA -> domain.getSuraPageNum(targetValue)
                QuranTarget.VERSE -> {
                    pendingVerseSelectionId = targetValue
                    domain.getVersePageNum(targetValue)
                }
            }

            domain.setPageNumCallback { pageNum }

            _uiState.update { it.copy(
                isLoading = false,
                isTutorialActive = domain.getShouldShowTutorial()
            )}
        }
    }

    private var connection: PlayerConnection? = null

    fun onStop() {
        connection?.release()
        connection = null

        domain.stopHandler()
    }

    fun onPageChange(currentPageIdx: Int, pageIdx: Int) {
        if (currentPageIdx != pageIdx) return

        pageNum = pageIdx+1

        val pageVerses = pageBuilder.getPageVerses(pageNum)
        val firstVerse = allVerses.firstOrNull { verse -> verse.pageNum == pageNum }
        if (firstVerse != null) suraId = firstVerse.suraNum - 1

        // the pending verse is only selectable once its page is the one being shown
        val pendingVerse = pendingVerseSelectionId?.let { verseId ->
            pageVerses.firstOrNull { verse -> verse.id == verseId }
        }
        if (pendingVerse != null) pendingVerseSelectionId = null

        _uiState.update { it.copy(
            pageNum = translateNums(
                string = pageNum.toString(),
                numeralsLanguage = numeralsLanguage
            ),
            suraName = suraNames[suraId],
            juzNum =
                if (firstVerse == null) it.juzNum
                else translateNums(
                    string = firstVerse.juzNum.toString(),
                    numeralsLanguage = numeralsLanguage
                ),
            pageVerses = pageVerses,
            selectedVerse = pendingVerse ?: it.selectedVerse
        )}

        domain.handlePageChange(pageNum)

        domain.trackPageViewed(pageNum)
    }

    fun onBookmarksClick() {
        _uiState.update { it.copy(isBookmarksSheetShown = true) }
    }

    fun onBookmarksSheetDismiss() {
        _uiState.update { it.copy(isBookmarksSheetShown = false) }
    }

    fun onBookmarkClick(verseId: Int) {
        _uiState.update { it.copy(isBookmarksSheetShown = false) }

        viewModelScope.launch {
            val targetPageNum = domain.getVersePageNum(verseId)
            // the bookmarked verse is usually on another page, so it must be looked up in that
            // page's verses instead of the currently displayed ones
            val targetVerse = pageBuilder.getPageVerses(targetPageNum).find { verse -> verse.id == verseId }
            _uiState.update { it.copy(
                navigateToPage = targetPageNum - 1,
                selectedVerse = targetVerse ?: it.selectedVerse
            )}
        }
    }

    fun onPlayPauseClick(activity: Activity) {
        if (connection == null) {
            _uiState.update { it.copy(playerState = PlaybackStatus.CONNECTING) }

            connection = PlayerConnection(
                context = activity.applicationContext,
                service = VersePlayerService::class.java,
                onConnected = ::togglePlayback,
                onChange = ::onPlayerChange
            )

            activity.volumeControlStream = AudioManager.STREAM_MUSIC
        }
        else connection?.controller?.let { togglePlayback(it) }
    }

    private fun togglePlayback(controller: MediaController) {
        val selectedVerse = _uiState.value.selectedVerse

        when (controller.playbackStatus()) {
            PlaybackStatus.PLAYING -> controller.pause()
            PlaybackStatus.PAUSED ->
                if (selectedVerse == null) controller.play()
                else play(controller, selectedVerse.id)
            PlaybackStatus.STOPPED, PlaybackStatus.ERROR -> {
                val verse = selectedVerse ?: _uiState.value.pageVerses.firstOrNull() ?: return
                play(controller, verse.id)
            }
            PlaybackStatus.CONNECTING, PlaybackStatus.BUFFERING -> {}
        }
    }

    private fun play(controller: MediaController, verseId: Int) {
        controller.setMediaItem(MediaItem.Builder().setMediaId(verseId.toString()).build())
        controller.prepare()
        controller.play()

        _uiState.update { it.copy(selectedVerse = null) }
    }

    fun onPreviousVerseClick() {
        skipVerses(-1)
    }

    fun onNextVerseClick() {
        skipVerses(1)
    }

    private fun skipVerses(count: Int) {
        val controller = connection?.controller ?: return
        val verseId = controller.currentMediaItem?.mediaId?.toIntOrNull() ?: return

        val targetId = verseId + count
        if (targetId in 1..allVerses.size) play(controller, targetId)
    }

    private fun onPlayerChange(controller: MediaController) {
        val status = controller.playbackStatus()
        val verseId = controller.currentMediaItem?.mediaId?.toIntOrNull()
        val trackedVerseId =
            if (verseId == null || status == PlaybackStatus.STOPPED || status == PlaybackStatus.ERROR) -1
            else verseId
        val isNewVerse = trackedVerseId != -1 && trackedVerseId != _uiState.value.trackedVerseId

        _uiState.update { it.copy(
            playerState = status,
            trackedVerseId = trackedVerseId
        )}

        if (!isNewVerse) return

        if (uiState.value.viewType == QuranViewType.LIST) {
            versePositions[trackedVerseId]?.let { position ->
                _uiState.update { it.copy(scrollToVersePosition = position) }
            }
        }

        val versePageNum = allVerses[trackedVerseId - 1].pageNum
        if (versePageNum != pageNum) {
            _uiState.update { it.copy(navigateToPage = versePageNum - 1) }
        }
    }

    fun onSettingsClick() {
        navigator.navigate(Screen.QuranSettings)
    }

    fun onVersePointerInput(
        pointerInputScope: PointerInputScope,
        layoutResult: TextLayoutResult?,
        annotatedString: AnnotatedString
    ) {
        val swipeThreshold = 10f

        viewModelScope.launch {
            pointerInputScope.awaitPointerEventScope {
                var initialPosition: Offset? = null
                var verseId: Int? = null
                var isLongPressDetected = false

                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val offset = event.changes[0].position

                    when (event.type) {
                        PointerEventType.Press -> {
                            longPressJob?.cancel()
                            isLongPressDetected = false

                            initialPosition = offset
                            val position = layoutResult?.getOffsetForPosition(offset)
                            verseId = position?.let {
                                annotatedString.getStringAnnotations(
                                    start = position,
                                    end = position
                                ).firstOrNull()
                                    ?.tag?.toInt()
                            }

                            verseId?.let { onVersePressed(it) }
                        }
                        PointerEventType.Release -> {
                            val movementDistance = initialPosition?.let { start ->
                                (offset - start).getDistance()
                            } ?: 0f

                            if (movementDistance <= swipeThreshold && !isLongPressDetected)
                                verseId?.let { onVerseReleased() }

                            initialPosition = null
                            verseId = null
                            isLongPressDetected = false
                        }
                        PointerEventType.Move -> {
                            val movementDistance = initialPosition?.let { start ->
                                (offset - start).getDistance()
                            } ?: 0f

                            if (movementDistance > swipeThreshold) {
                                isLongPressDetected = true
                                longPressJob?.cancel()
                                pressedVerseId = null
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onVersePressed(verseId: Int) {
        pressedVerseId = verseId

        longPressJob = viewModelScope.launch {
            delay(500)
            if (pressedVerseId == verseId) onVerseHold(verseId)
        }
    }

    private fun onVerseReleased() {
        pressedVerseId?.let { onVerseClick(it) }
        pressedVerseId = null
        longPressJob?.cancel()
    }

    private fun onVerseClick(verseId: Int) {
        _uiState.value.pageVerses.find { it.id == verseId }?.let { verse ->
            _uiState.update { it.copy(
                selectedVerse = if (_uiState.value.selectedVerse?.id == verse.id) null else verse
            )}
        }
    }

    private fun onVerseHold(verseId: Int) {
        navigator.navigate(Screen.VerseInfo(verseId.toString()))
        pressedVerseId = null
        longPressJob?.cancel()
    }

    fun onSuraHeaderGloballyPositioned(
        suraNum: Int,
        isCurrentPage: Boolean,
        layoutCoordinates: LayoutCoordinates
    ) {
        if (
            isCurrentPage
            && scrollTo == -1F
            && targetType == QuranTarget.SURA
            && suraNum == targetValue+1
            ) {
            scrollTo = layoutCoordinates.positionInParent().y - 13
        }
    }

    fun onVerseGloballyPositioned(
        verseId: Int,
        isCurrentPage: Boolean,
        layoutCoordinates: LayoutCoordinates
    ) {
        val screenHeight = domain.getScreenHeight()
        if (isCurrentPage && _uiState.value.viewType == QuranViewType.LIST)
            versePositions[verseId] = layoutCoordinates.positionInParent().y - screenHeight / 3f
    }

    fun onScrolled() {
        scrollTo = 0F
    }

    fun onNavigateToPageConsumed() {
        _uiState.update { it.copy(navigateToPage = null) }
    }

    fun onScrollToVerseConsumed() {
        _uiState.update { it.copy(scrollToVersePosition = null) }
    }

    fun onTutorialFinished() {
        _uiState.update { it.copy(
            isTutorialActive = false
        )}

        viewModelScope.launch {
            domain.setDoNotShowTutorial()
        }
    }

}
