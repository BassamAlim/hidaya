package bassamalim.hidaya.features.recitations.player

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ONE
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_NONE
import android.support.v4.media.session.PlaybackStateCompat.STATE_NONE
import android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.helpers.ReceiverWrapper
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.features.recitations.RecitationMediaId
import bassamalim.hidaya.features.recitations.recitersMenu.Recitation
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _uiState = MutableStateFlow(RecitationPlayerUiState())
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getRepeatMode(),
        domain.getShuffleMode()
    ) { state, repeatMode, shuffleMode ->
        state.copy(
            repeatMode = repeatMode,
            shuffleMode = shuffleMode
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

    private var pendingActivity: Activity? = null
    private var mediaBrowser: MediaBrowserCompat? = null
    private var controller: MediaControllerCompat? = null
    private var tc: MediaControllerCompat.TransportControls? = null
    private lateinit var downloadReceiver: ReceiverWrapper

    fun onStart(activity: Activity) {
        Log.i(Globals.TAG, "in onStart of RecitationsPlayerViewModel")

        pendingActivity = activity

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
        )

        mediaBrowser = MediaBrowserCompat(
            activity,
            ComponentName(activity, RecitationPlayerService::class.java),
            connectionCallbacks,
            null
        )
        mediaBrowser?.connect()

        activity.volumeControlStream = AudioManager.STREAM_MUSIC

        downloadReceiver.register()
    }

    fun onStop(activity: Activity) {
        Log.i(Globals.TAG, "in onStop of RecitationsPlayerViewModel")

        downloadReceiver.unregister()

        MediaControllerCompat.getMediaController(activity)
            ?.unregisterCallback(controllerCallback)

        mediaBrowser?.disconnect()
        pendingActivity = null
    }

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.i(Globals.TAG, "onConnected in RecitationsPlayerViewModel")

            val activity = pendingActivity ?: return

            Log.d(Globals.TAG, "in initializeController of RecitationPlayerViewModel")

            // Get the token for the MediaSession
            val token = mediaBrowser!!.sessionToken

            // Create a MediaControllerCompat
            val mediaController = MediaControllerCompat(activity, token)

            // Save the controller
            MediaControllerCompat.setMediaController(activity, mediaController)

            controller = MediaControllerCompat.getMediaController(activity)
            tc = controller!!.transportControls

            // Register a Callback to stay in sync
            controller?.registerCallback(controllerCallback)

            // Finish building the UI
            buildTransportControls()

            if (action != "back" &&
                (controller?.playbackState?.state == STATE_NONE ||
                        mediaId != controller?.metadata
                            ?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))
            ) {
                // Pass media data
                val bundle = Bundle()
                bundle.putString("play_type", action)
                bundle.putString("reciter_name", _uiState.value.reciterName)
                bundle.putSerializable("narration", narration)

                // Start Playback
                tc?.playFromMediaId(mediaId, bundle)
            }
        }

        override fun onConnectionSuspended() {
            Log.e(Globals.TAG, "Connection suspended in RecitationsPlayerViewModel")
            // The Service has crashed.
            // Disable transport controls until it automatically reconnects
            disableControls()
        }

        override fun onConnectionFailed() {
            Log.e(Globals.TAG, "Connection failed in RecitationsPlayerViewModel")
            // The Service has refused our connection
            disableControls()
        }
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

    private fun enableControls() {
        _uiState.update { it.copy(
            btnState = STATE_PLAYING,
            controlsEnabled = true
        )}
    }

    private fun disableControls() {
        _uiState.update { it.copy(
            controlsEnabled = false
        )}
    }

    private fun buildTransportControls() {
        enableControls()

        // Display the initial state
        controller?.metadata?.let { updateMetadata(it) }
        controller?.playbackState?.let { updatePlaybackState(it) }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            // To change the metadata inside the app when the user changes it from the notification
            updateMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            // To change the playback state inside the app when the user changes it
            // from the notification
            updatePlaybackState(state)
        }

        override fun onSessionDestroyed() {
            mediaBrowser?.disconnect()
        }
    }

    private fun updateMetadata(metadata: MediaMetadataCompat) {
        suraIdx = metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER).toInt()
        duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)

        domain.setPath(reciterId = reciterId, narrationId = narrationId)

        _uiState.update { it.copy(
            suraName = suraNames[suraIdx],
            duration = formatTime(duration),
            downloadState = domain.checkDownload()
        )}
    }

    private fun updatePlaybackState(state: PlaybackStateCompat) {
        progress = state.position

        _uiState.update { it.copy(
            btnState = state.state,
            progress = formatTime(progress),
            secondaryProgress = state.bufferedPosition
        )}
    }

    private fun formatTime(timeInMillis: Long): String {
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
        if (_uiState.value.btnState == STATE_NONE)
            return

        if (controller?.playbackState?.state == STATE_PLAYING) {
            tc?.pause()
            _uiState.update { it.copy(
                btnState = STATE_PAUSED
            )}
        }
        else {
            tc?.play()
            _uiState.update { it.copy(
                btnState = STATE_PLAYING
            )}
        }
    }

    fun onPreviousTrackClick() {
        tc?.skipToPrevious()
    }

    fun onNextTrackClick() {
        tc?.skipToNext()
    }

    fun onSliderChange(progress: Float) {
        this.progress = progress.toLong()
        _uiState.update { it.copy(
            progress = formatTime(progress.toLong())
        )}
    }

    fun onSliderChangeFinished() {
        tc?.seekTo(progress)
    }

    fun onRepeatClick(oldMode: Int) {
        viewModelScope.launch {
            val newMode = if (oldMode == REPEAT_MODE_NONE) REPEAT_MODE_ONE else REPEAT_MODE_NONE
            tc?.setRepeatMode(newMode)
            domain.setRepeatMode(newMode)
        }
    }

    fun onShuffleClick(oldMode: Int) {
        viewModelScope.launch {
            val newMode = if (oldMode == SHUFFLE_MODE_NONE) SHUFFLE_MODE_ALL else SHUFFLE_MODE_NONE
            tc?.setShuffleMode(newMode)
            domain.setShuffleMode(newMode)
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
