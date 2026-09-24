package bassamalim.hidaya.features.recitations.player

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.PlaybackStatus
import bassamalim.hidaya.core.helpers.PlayerConnection
import bassamalim.hidaya.core.helpers.ReceiverWrapper
import bassamalim.hidaya.core.helpers.playbackStatus
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.features.recitations.RecitationMediaId
import bassamalim.hidaya.features.recitations.recitersMenu.Recitation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(UnstableApi::class)
@HiltViewModel
class RecitationPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: RecitationPlayerDomain,
    private val navigator: Navigator
): ViewModel() {

    private val action = savedStateHandle.get<String>("action") ?: ""
    private val mediaId = savedStateHandle.get<String>("media_id") ?: ""

    private lateinit var language: Language
    // Every source (suras menu, notification, last played) is validated upstream
    private val mediaIdParts = checkNotNull(RecitationMediaId.decode(mediaId)) { "Bad media id: $mediaId" }
    var reciterId = mediaIdParts.reciterId
    private var narrationId = mediaIdParts.narrationId
    private var suraIdx = mediaIdParts.suraIdx
    private lateinit var narration: Recitation.Narration
    private lateinit var suraNames: List<String>
    var duration = 0L
    var progress = 0L
    private var isSeeking = false

    private val _uiState = MutableStateFlow(RecitationPlayerUiState())
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getRepeatMode(),
        domain.isShuffleOn()
    ) { state, repeatMode, isShuffleOn ->
        state.copy(
            repeatMode = repeatMode,
            isShuffleOn = isShuffleOn
        )
    }.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = RecitationPlayerUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            language = domain.getLanguage()
            suraNames = domain.getSuraNames(language)

            _uiState.update { it.copy(
                isLoading = false,
                reciterName = domain.getReciterName(id = reciterId, language = language)
            )}

            updateTrackState()
        }
    }

    private var connection: PlayerConnection? = null
    private var downloadReceiver: ReceiverWrapper? = null
    private var progressTicker: Job? = null

    fun onStart(activity: Activity) {
        downloadReceiver = ReceiverWrapper(
            context = activity.applicationContext,
            intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    _uiState.update { it.copy(
                        downloadState = domain.checkDownload()
                    )}
                }
            }
        ).also { it.register() }

        connection = PlayerConnection(
            context = activity.applicationContext,
            service = RecitationPlayerService::class.java,
            onConnected = ::onConnected,
            onChange = ::onPlayerChange
        )

        // The player doesn't report position changes, so poll it while playing
        progressTicker = viewModelScope.launch {
            while (true) {
                delay(500)
                connection?.controller?.let { if (it.isPlaying) updateProgress(it) }
            }
        }

        activity.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    fun onStop() {
        downloadReceiver?.unregister()
        progressTicker?.cancel()
        connection?.release()
        connection = null
    }

    private fun onConnected(controller: MediaController) {
        _uiState.update { it.copy(controlsEnabled = true) }

        // Coming back to what is already loaded (e.g. from the notification) keeps it as it is
        if (action == "back") return
        if (controller.currentMediaItem?.mediaId == mediaId
            && controller.playbackStatus() != PlaybackStatus.STOPPED) return

        viewModelScope.launch {
            val startPosition = if (action == "continue") domain.getLastPlayedProgress() else 0L
            controller.setMediaItem(MediaItem.Builder().setMediaId(mediaId).build(), startPosition)
            controller.prepare()
            controller.play()
        }
    }

    private fun onPlayerChange(controller: MediaController) {
        val item = controller.currentMediaItem
        val parts = item?.let { RecitationMediaId.decode(it.mediaId) }
        // Until this screen's request is applied, the player may still hold another narration
        if (parts != null && parts.reciterId == reciterId && parts.narrationId == narrationId) {
            suraIdx = parts.suraIdx
            duration = controller.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            domain.setPath(reciterId = reciterId, narrationId = narrationId)

            _uiState.update { it.copy(
                suraName = item.mediaMetadata.title?.toString() ?: it.suraName,
                duration = formatPlaybackTime(duration),
                downloadState = domain.checkDownload()
            )}
        }

        updateProgress(controller)
        _uiState.update { it.copy(btnState = controller.playbackStatus()) }
    }

    private fun updateProgress(controller: Player) {
        if (isSeeking) return

        progress = controller.currentPosition
        _uiState.update { it.copy(
            progress = formatPlaybackTime(progress),
            secondaryProgress = controller.bufferedPosition
        )}
    }

    private suspend fun updateTrackState() {
        narration = domain.getNarration(reciterId, narrationId, language)

        _uiState.update { it.copy(
            suraName = suraNames[suraIdx],
            narrationName = narration.name,
            reciterName = domain.getReciterName(id = reciterId, language = language),
            downloadState = domain.checkDownload()
        )}
    }

    fun onBackPressed(activity: Activity) {
        if (activity.isTaskRoot) {
            navigator.navigate(
                Screen.RecitationSurasMenu(
                    reciterId = reciterId.toString(),
                    narrationId = narrationId.toString()
                )
            ) {
                popUpTo(Screen.RecitationPlayer(action, mediaId).route) {
                    inclusive = true
                }
            }
        }
        else
            (activity as AppCompatActivity).onBackPressedDispatcher.onBackPressed()
    }

    fun onPlayPauseClick() {
        // Prepares after an error and restarts after the end, as well as toggling
        connection?.controller?.let { Util.handlePlayPauseButtonAction(it) }
    }

    fun onPreviousTrackClick() {
        connection?.controller?.seekToPreviousMediaItem()
    }

    fun onNextTrackClick() {
        connection?.controller?.seekToNextMediaItem()
    }

    fun onSliderChange(progress: Float) {
        isSeeking = true
        this.progress = progress.toLong()
        _uiState.update { it.copy(
            progress = formatPlaybackTime(progress.toLong())
        )}
    }

    fun onSliderChangeFinished() {
        connection?.controller?.seekTo(progress)
        isSeeking = false
    }

    fun onRepeatClick(oldMode: Int) {
        val newMode =
            if (oldMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE
            else Player.REPEAT_MODE_OFF
        connection?.controller?.repeatMode = newMode

        viewModelScope.launch {
            domain.setRepeatMode(newMode)
        }
    }

    fun onShuffleClick() {
        val isOn = !uiState.value.isShuffleOn
        connection?.controller?.shuffleModeEnabled = isOn

        viewModelScope.launch {
            domain.setShuffleOn(isOn)
        }
    }

    fun onDownloadClick() {
        if (_uiState.value.downloadState == DownloadState.NOT_DOWNLOADED) {
            _uiState.update { it.copy(
                downloadState = DownloadState.DOWNLOADING
            )}

            domain.downloadRecitation(
                narration = narration,
                suraIdx = suraIdx,
                suraName = suraNames[suraIdx]
            )
        }
        else {
            _uiState.update { it.copy(
                downloadState = DownloadState.NOT_DOWNLOADED
            )}

            domain.deleteRecitation()
        }
    }

}

/** "mm:ss" under an hour, else "h:mm:ss". */
internal fun formatPlaybackTime(timeInMillis: Long): String {
    val hours = timeInMillis / (60 * 60 * 1000) % 24
    val minutes = timeInMillis / (60 * 1000) % 60
    val seconds = timeInMillis / 1000 % 60
    var hms = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    if (hms.startsWith("0")) {
        hms = hms.substring(1)
        if (hms.startsWith("0")) hms = hms.substring(2)
    }
    return hms
}
