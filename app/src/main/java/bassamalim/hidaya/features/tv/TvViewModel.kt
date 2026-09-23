package bassamalim.hidaya.features.tv

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.utils.report
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvViewModel @Inject constructor(
    app: Application,
    private val domain: TvDomain
): ViewModel() {

    private val _uiState = MutableStateFlow(TvUiState())
    val uiState = _uiState.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(app)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        .build()

    private var loadJob: Job? = null
    private var hasRetried = false
    private var isInForeground = true
    private var shouldResumeOnStart = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) hasRetried = false
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(Globals.TAG, "TV playback error: ${error.errorCodeName}", error)

            // usually the signed url expired or playback fell behind the live window,
            // so retry once with a fresh url
            if (!hasRetried) {
                hasRetried = true
                loadChannel(_uiState.value.selectedChannel)
            }
            else onPlaybackFailed()
        }
    }

    init {
        player.addListener(playerListener)
        loadChannel(TvChannel.QURAN)
    }

    fun onStart() {
        isInForeground = true

        if (shouldResumeOnStart) {
            shouldResumeOnStart = false
            player.seekToDefaultPosition()
            player.play()
        }
    }

    fun onStop() {
        isInForeground = false

        shouldResumeOnStart = player.playWhenReady
        player.pause()
    }

    fun onQuranChannelClick() {
        onChannelClick(TvChannel.QURAN)
    }

    fun onSunnahChannelClick() {
        onChannelClick(TvChannel.SUNNAH)
    }

    fun onFullscreenButtonClick(isFullscreen: Boolean) {
        _uiState.update { it.copy(
            isFullscreen = isFullscreen
        )}
    }

    fun onBackPressedInFullscreen() {
        _uiState.update { it.copy(
            isFullscreen = false
        )}
    }

    fun onPlaybackFailedShown() {
        _uiState.update { it.copy(
            isPlaybackFailed = false
        )}
    }

    private fun onChannelClick(channel: TvChannel) {
        hasRetried = false
        loadChannel(channel)

        domain.trackTvChannelViewed(channel)
    }

    private fun loadChannel(channel: TvChannel) {
        loadJob?.cancel()
        _uiState.update { it.copy(
            selectedChannel = channel,
            isLoading = true
        )}

        loadJob = viewModelScope.launch {
            try {
                val url = domain.getStreamUrl(channel)

                player.setMediaItem(
                    MediaItem.Builder()
                        .setUri(url)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .build()
                )
                player.prepare()
                if (isInForeground) player.play()
                else shouldResumeOnStart = true

                _uiState.update { it.copy(
                    isLoading = false
                )}
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.report()
                Log.e(Globals.TAG, "Failed to get TV stream url for $channel", e)
                onPlaybackFailed()
            }
        }
    }

    private fun onPlaybackFailed() {
        player.stop()

        _uiState.update { it.copy(
            isLoading = false,
            isPlaybackFailed = true
        )}
    }

    override fun onCleared() {
        super.onCleared()

        loadJob?.cancel()
        player.removeListener(playerListener)
        player.release()
    }

}
