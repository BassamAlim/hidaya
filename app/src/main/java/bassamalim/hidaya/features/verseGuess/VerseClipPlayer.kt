package bassamalim.hidaya.features.verseGuess

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import bassamalim.hidaya.core.enums.PlaybackStatus
import bassamalim.hidaya.core.helpers.buildAudioPlayer
import bassamalim.hidaya.core.helpers.playbackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClipPlayback(
    val status: PlaybackStatus = PlaybackStatus.STOPPED,
    /** Which of the clip's verses is loaded */
    val verseIndex: Int = 0
)

/**
 * Plays a round's verses back to back, in the app, with no media session: the clip is part of
 * the game screen, so it pauses with it rather than continuing in the background.
 */
class VerseClipPlayer(context: Context) {

    private val player = buildAudioPlayer(context)

    private val _playback = MutableStateFlow(ClipPlayback())
    val playback = _playback.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                _playback.value = ClipPlayback(
                    status = player.playbackStatus(),
                    verseIndex = player.currentMediaItemIndex
                )
            }
        })
    }

    fun play(urls: List<String>) {
        player.setMediaItems(urls.map(MediaItem::fromUri))
        player.prepare()
        player.play()
    }

    /** Plays from the start once the clip has ended, and retries after an error. */
    fun togglePlayPause() {
        when {
            player.playerError != null -> replay()
            player.playbackState == Player.STATE_ENDED -> replay()
            player.isPlaying -> player.pause()
            else -> player.play()
        }
    }

    fun replay() {
        if (player.playerError != null) player.prepare()
        player.seekTo(0, 0)
        player.play()
    }

    /** Plays from the start of the clip's verse at [index]. */
    fun playVerse(index: Int) {
        if (player.playerError != null) player.prepare()
        player.seekTo(index, 0)
        player.play()
    }

    /** How far into the loaded verse playback is, from 0 to 1. Not observable; poll it. */
    fun verseProgress(): Float {
        val duration = player.duration
        if (duration == C.TIME_UNSET || duration <= 0) return 0f
        return (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    }

    fun pause() = player.pause()

    fun stop() {
        player.stop()
        player.clearMediaItems()
    }

    fun release() = player.release()

}
