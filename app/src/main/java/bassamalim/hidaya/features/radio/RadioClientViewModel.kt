package bassamalim.hidaya.features.radio

import android.app.Activity
import android.content.ComponentName
import android.media.AudioManager
import android.os.Build
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import bassamalim.hidaya.core.Globals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class RadioClientViewModel @Inject constructor(
    private val domain: RadioDomain
): ViewModel() {

    private val _uiState = MutableStateFlow(RadioClientUiState())
    val uiState = _uiState.asStateFlow()

    private var pendingActivity: Activity? = null
    private var mediaBrowser: MediaBrowserCompat? = null
    private var controller: MediaControllerCompat? = null
    private var tc: MediaControllerCompat.TransportControls? = null

    private val connectionCallbacks: MediaBrowserCompat.ConnectionCallback =
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                val activity = pendingActivity ?: return

                // Get the token for the MediaSession
                val token = mediaBrowser!!.sessionToken

                // Create a MediaControllerCompat
                val mediaController = MediaControllerCompat(activity, token)

                // Save the controller
                MediaControllerCompat.setMediaController(activity, mediaController)
                controller = MediaControllerCompat.getMediaController(activity)
                tc = controller!!.transportControls

                // just sending the static url to extract the dynamic url
                val url = domain.getUrl()
                tc?.playFromMediaId(url, null)

                // Finish building the UI
                buildTransportControls()
            }

            override fun onConnectionSuspended() {
                Log.e(Globals.TAG, "Connection suspended in RadioClient")
                // The Service has crashed.
                // Disable transport controls until it automatically reconnects
                updatePbState(PlaybackStateCompat.STATE_NONE)
            }

            override fun onConnectionFailed() {
                Log.e(Globals.TAG, "Connection failed in RadioClient")
                // The Service has refused our connection
                updatePbState(PlaybackStateCompat.STATE_NONE)
            }
        }

    fun onStart(activity: Activity) {
        pendingActivity = activity

        updatePbState(PlaybackStateCompat.STATE_CONNECTING)

        mediaBrowser = MediaBrowserCompat(
            activity,
            ComponentName(activity, RadioService::class.java),
            connectionCallbacks,
            null
        )
        mediaBrowser?.connect()

        activity.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    fun onStop(activity: Activity) {
        MediaControllerCompat.getMediaController(activity)
            ?.unregisterCallback(controllerCallback)

        mediaBrowser?.disconnect()
        pendingActivity = null
    }

    private var controllerCallback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {}

            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                // To change the playback state inside the app when the user changes it
                // from the notification
                updatePbState(state.state)
            }
        }

    private fun buildTransportControls() {
        // Display the initial state
        updatePbState(controller?.playbackState?.state ?: PlaybackStateCompat.STATE_NONE)

        // Register a Callback to stay in sync
        controller?.registerCallback(controllerCallback)
    }

    private fun updatePbState(state: Int) {
        when (state) {
            PlaybackStateCompat.STATE_PLAYING,
            PlaybackStateCompat.STATE_STOPPED,
            PlaybackStateCompat.STATE_PAUSED,
            PlaybackStateCompat.STATE_CONNECTING,
            PlaybackStateCompat.STATE_BUFFERING,
            PlaybackStateCompat.STATE_ERROR ->
                _uiState.update { it.copy(
                    btnState = state
                )}
            else -> {}
        }
    }

    fun onPlayPauseClick() {
        when (controller?.playbackState?.state) {
            PlaybackStateCompat.STATE_PLAYING -> {
                tc?.pause()
                updatePbState(PlaybackStateCompat.STATE_STOPPED)
            }
            PlaybackStateCompat.STATE_STOPPED,
            PlaybackStateCompat.STATE_PAUSED -> {
                tc?.play()
                updatePbState(PlaybackStateCompat.STATE_PLAYING)
            }
            PlaybackStateCompat.STATE_ERROR -> {
                // Same request as on first connect; the service re-resolves the stream if needed
                updatePbState(PlaybackStateCompat.STATE_CONNECTING)
                tc?.playFromMediaId(domain.getUrl(), null)
            }
            else -> {}
        }
    }

}
