package bassamalim.hidaya.features.quran.reader

import android.app.Activity
import android.content.ComponentName
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.QuranViewType
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.models.Verse
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import bassamalim.hidaya.core.utils.report
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
import java.util.concurrent.Executors
import javax.inject.Inject
import android.util.Log
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

    private var pendingActivity: Activity? = null
    private var mediaBrowser: MediaBrowserCompat? = null
    private var controller: MediaControllerCompat? = null
    private var tc: MediaControllerCompat.TransportControls? = null

    fun onStop(activity: Activity) {
        controllerCallback.let {
            MediaControllerCompat.getMediaController(activity)?.unregisterCallback(it)
        }

        mediaBrowser?.disconnect()
        mediaBrowser = null

        pendingActivity = null

        domain.stopHandler()
    }

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.i(Globals.TAG, "In onServiceConnected")

            if (mediaBrowser == null) return

            val activity = pendingActivity ?: return

            val mediaController: MediaControllerCompat?
            try {
                // Create a MediaControllerCompat
                mediaController = MediaControllerCompat(activity, mediaBrowser!!.sessionToken)
            } catch (e: IllegalStateException) {
                e.report()
                Log.e(Globals.TAG, "Error in QuranReader: ${e.message}")
                return
            }

            // Save the controller
            MediaControllerCompat.setMediaController(activity, mediaController)

            controller = MediaControllerCompat.getMediaController(activity)
            tc = controller!!.transportControls

            if (_uiState.value.selectedVerse == null) {
                _uiState.update { it.copy(
                    selectedVerse = it.pageVerses[0]
                )}
            }

            // Finish building the UI
            buildTransportControls()

            requestPlay(_uiState.value.selectedVerse!!.id)
        }

        override fun onConnectionSuspended() {
            Log.e(Globals.TAG, "Connection suspended in QuranReader")
            // The Service has crashed.
        }

        override fun onConnectionFailed() {
            Log.e(Globals.TAG, "Connection failed in QuranReader")
            // The Service has refused our connection
        }
    }

    private fun buildTransportControls() {
        // Register a Callback to stay in sync
        controller?.registerCallback(controllerCallback)
    }

    private fun requestPlay(ayaId: Int) {
        Executors.newSingleThreadExecutor().execute {
            tc?.playFromMediaId(ayaId.toString(), Bundle())

            _uiState.update { it.copy(selectedVerse = null) }
        }
    }

    fun onPageChange(currentPageIdx: Int, pageIdx: Int) {
        if (currentPageIdx != pageIdx) return

        pageNum = pageIdx+1

        val pageVerses = getPageVerses(pageNum)
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
            val targetVerse = getPageVerses(targetPageNum).find { verse -> verse.id == verseId }
            _uiState.update { it.copy(
                navigateToPage = targetPageNum - 1,
                selectedVerse = targetVerse ?: it.selectedVerse
            )}
        }
    }

    fun onPlayPauseClick(
        activity: Activity,
        snackbarHostState: SnackbarHostState,
        message: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            viewModelScope.launch {
                snackbarHostState.showSnackbar(message)
            }
            return
        }

        if (mediaBrowser == null || controller == null) {
            updateButton(PlaybackStateCompat.STATE_BUFFERING)

            pendingActivity = activity
            mediaBrowser = MediaBrowserCompat(
                activity,
                ComponentName(activity, VersePlayerService::class.java),
                connectionCallbacks,
                null
            )
            mediaBrowser?.connect()

            activity.volumeControlStream = AudioManager.STREAM_MUSIC
        }
        else {
            when (controller?.playbackState?.state ?: PlaybackStateCompat.STATE_NONE) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    updateButton(PlaybackStateCompat.STATE_PAUSED)
                    tc?.pause()
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    updateButton(PlaybackStateCompat.STATE_BUFFERING)

                    if (_uiState.value.selectedVerse == null) {
                        tc?.play()

                        _uiState.update { it.copy(
                            selectedVerse = null
                        )}
                    }
                    else
                        requestPlay(_uiState.value.selectedVerse!!.id)
                }
                PlaybackStateCompat.STATE_STOPPED -> {
                    updateButton(PlaybackStateCompat.STATE_BUFFERING)

                    if (_uiState.value.selectedVerse == null) {
                        _uiState.update { it.copy(
                            selectedVerse = it.pageVerses[0]
                        )}
                    }

                    requestPlay(_uiState.value.selectedVerse!!.id)
                }
                else -> {}
            }
        }
    }

    fun onPreviousVerseClick() {
        tc?.skipToPrevious()
    }

    fun onNextVerseClick() {
        tc?.skipToNext()
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

    private fun getPageVerses(pageNumber: Int) =
        allVerses.filter { it.pageNum == pageNumber }.map { verse ->
            Verse(
                id = verse.id,
                juzNum = verse.juzNum,
                suraNum = verse.suraNum,
                suraName = suraNames[verse.suraNum - 1],
                num = verse.num,
                text = "${verse.decoratedText} ",
                startLineNum = verse.startLineNum,
                endLineNum = verse.endLineNum,
                translation = verse.translationEn,
                interpretation = verse.interpretation
            )
        }

    fun buildPage(
        pageNumber: Int,
        selectedVerseId: Int?,
        trackedVerseId: Int,
        defaultVerseColor: Color,
        selectedVerseColor: Color,
        trackedVerseColor: Color
    ): List<Section> {
        val sections = mutableListOf<Section>()

        val tempVerses = mutableListOf<Verse>()
        // get page start
        var counter = allVerses.indexOfFirst { verse -> verse.pageNum == pageNumber }
        do {
            val verse = allVerses[counter]

            if (verse.num == 1) {
                if (tempVerses.isNotEmpty()) {
                    sections.add(
                        VersesSection(
                            suraNum = verse.suraNum,
                            annotatedString = versesToAnnotatedString(
                                verses = tempVerses.toList(),
                                selectedVerseId = selectedVerseId,
                                trackedVerseId = trackedVerseId,
                                defaultVerseColor = defaultVerseColor,
                                selectedVerseColor = selectedVerseColor,
                                trackedVerseColor = trackedVerseColor
                            ),  // toList() to make a copy
                            numOfLines =
                            tempVerses.last().endLineNum - tempVerses.first().startLineNum + 1
                        )
                    )
                    tempVerses.clear()
                }

                sections.add(
                    SuraHeaderSection(
                        suraNum = verse.suraNum,
                        suraName = suraNames[verse.suraNum - 1]
                    )
                )

                if (verse.suraNum != 1 && verse.suraNum != 9)
                    sections.add(BasmalahSection())
            }

            tempVerses.add(
                Verse(
                    id = verse.id,
                    juzNum = verse.juzNum,
                    suraNum = verse.suraNum,
                    suraName = suraNames[verse.suraNum - 1],
                    num = verse.num,
                    text = "${verse.decoratedText} ",
                    startLineNum = verse.startLineNum,
                    endLineNum = verse.endLineNum,
                    translation = verse.translationEn,
                    interpretation = verse.interpretation
                )
            )

            counter++
        } while (counter != Globals.NUM_OF_QURAN_VERSES && allVerses[counter].pageNum == pageNumber)

        if (tempVerses.isNotEmpty()) {
            sections.add(
                VersesSection(
                    suraNum = tempVerses.last().suraNum,
                    annotatedString = versesToAnnotatedString(
                        verses = tempVerses.toList(),
                        selectedVerseId = selectedVerseId,
                        trackedVerseId = trackedVerseId,
                        defaultVerseColor = defaultVerseColor,
                        selectedVerseColor = selectedVerseColor,
                        trackedVerseColor = trackedVerseColor
                    ),  // toList() to make a copy
                    numOfLines = tempVerses.last().endLineNum - tempVerses.first().startLineNum + 1
                )
            )
        }

        return sections
    }

    fun buildListPage(
        pageNumber: Int,
        selectedVerseId: Int?,
        trackedVerseId: Int,
        defaultVerseColor: Color,
        selectedVerseColor: Color,
        trackedVerseColor: Color
    ): List<Section> {
        val sections = mutableListOf<Section>()

        // get page start
        var counter = allVerses.indexOfFirst { verse -> verse.pageNum == pageNumber }
        do {
            val verse = allVerses[counter]

            if (verse.num == 1) {
                sections.add(
                    SuraHeaderSection(
                        suraNum = verse.suraNum,
                        suraName = suraNames[verse.suraNum - 1]
                    )
                )

                if (verse.suraNum != 1 && verse.suraNum != 9)
                    sections.add(BasmalahSection())
            }

            sections.add(
                ListVerse(
                    id = verse.id,
                    text = versesToAnnotatedString(
                        listOf(
                            Verse(
                                id = verse.id,
                                juzNum = verse.juzNum,
                                suraNum = verse.suraNum,
                                suraName = suraNames[verse.suraNum - 1],
                                num = verse.num,
                                text = "${verse.decoratedText} ",
                                startLineNum = verse.startLineNum,
                                endLineNum = verse.endLineNum,
                                translation = verse.translationEn,
                                interpretation = verse.interpretation
                            )
                        ),
                        selectedVerseId = selectedVerseId,
                        trackedVerseId = trackedVerseId,
                        defaultVerseColor = defaultVerseColor,
                        selectedVerseColor = selectedVerseColor,
                        trackedVerseColor = trackedVerseColor
                    ),
                    translation = verse.translationEn
                )
            )

            counter++
        } while (counter != Globals.NUM_OF_QURAN_VERSES && allVerses[counter].pageNum == pageNumber)

        return sections
    }

    private fun versesToAnnotatedString(
        verses: List<Verse>,
        selectedVerseId: Int?,
        trackedVerseId: Int,
        defaultVerseColor: Color,
        selectedVerseColor: Color,
        trackedVerseColor: Color
    ): AnnotatedString {
        return buildAnnotatedString {
            for (verse in verses) {
                val text = when (language) {
                    Language.ARABIC -> verse.text!!
                    Language.ENGLISH -> verse.text!!.reversed()
                }
                val color = when (verse.id) {
                    selectedVerseId -> selectedVerseColor
                    trackedVerseId -> trackedVerseColor
                    else -> defaultVerseColor
                }

                pushStringAnnotation(tag = verse.id.toString(), annotation = verse.id.toString())
                withStyle(style = SpanStyle(color = color)) {
                    append(text)
                }
                pop()
            }
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            _uiState.update { it.copy(
                trackedVerseId = metadata
                    .getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER).toInt()
            )}
            if (_uiState.value.viewType == QuranViewType.LIST) {
                versePositions[_uiState.value.trackedVerseId]?.let { position ->
                    _uiState.update { it.copy(scrollToVersePosition = position) }
                }
            }

            val newPageNum = metadata.getLong("page_num").toInt()
            if (newPageNum != pageNum) {
                _uiState.update { it.copy(navigateToPage = newPageNum - 1) }
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            // To change the playback state inside the app when the user changes it
            // from the notification
            updateButton(state.state)

            if (state.state == PlaybackStateCompat.STATE_STOPPED) {
                _uiState.update { it.copy(
                    trackedVerseId = -1
                )}
            }
        }

        override fun onSessionDestroyed() {
            mediaBrowser?.disconnect()
        }
    }

    private fun updateButton(state: Int) {
        when (state) {
            PlaybackStateCompat.STATE_NONE,
            PlaybackStateCompat.STATE_PAUSED,
            PlaybackStateCompat.STATE_STOPPED,
            PlaybackStateCompat.STATE_PLAYING,
            PlaybackStateCompat.STATE_BUFFERING -> {
                _uiState.update { it.copy(
                    playerState = state
                )}
            }
            else -> {}
        }
    }

}
