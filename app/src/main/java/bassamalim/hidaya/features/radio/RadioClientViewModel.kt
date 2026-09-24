package bassamalim.hidaya.features.radio

import android.app.Activity
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import bassamalim.hidaya.core.enums.PlaybackStatus
import bassamalim.hidaya.core.helpers.PlayerConnection
import bassamalim.hidaya.core.helpers.playbackStatus
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

    private var connection: PlayerConnection? = null

    fun onStart(activity: Activity) {
        _uiState.update { it.copy(btnState = PlaybackStatus.CONNECTING) }

        connection = PlayerConnection(
            context = activity.applicationContext,
            service = RadioService::class.java,
            // The service resolves the stream now, so pressing play starts without that wait
            onConnected = { controller -> if (controller.mediaItemCount == 0) loadStream(controller) },
            onChange = { controller ->
                _uiState.update { it.copy(btnState = controller.playbackStatus()) }
            }
        )

        activity.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    fun onStop() {
        connection?.release()
        connection = null
    }

    fun onPlayPauseClick() {
        val controller = connection?.controller ?: return

        when (controller.playbackStatus()) {
            PlaybackStatus.PLAYING -> controller.pause()
            PlaybackStatus.PAUSED, PlaybackStatus.STOPPED -> {
                controller.prepare()
                controller.play()
            }
            PlaybackStatus.ERROR -> {
                // The dynamic link may have expired, so resolve it again
                loadStream(controller)
                controller.prepare()
                controller.play()
            }
            PlaybackStatus.CONNECTING, PlaybackStatus.BUFFERING -> {}
        }
    }

    private fun loadStream(controller: MediaController) {
        controller.setMediaItem(MediaItem.Builder().setMediaId(domain.getUrl()).build())
    }

}
