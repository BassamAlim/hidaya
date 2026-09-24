package bassamalim.hidaya.core.helpers

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import bassamalim.hidaya.R
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.enums.PlaybackStatus
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

/**
 * Player for the audio services. ExoPlayer handles audio focus (ducking included), pausing when
 * headphones are unplugged, and the wake and wifi locks.
 */
@OptIn(UnstableApi::class)
fun buildAudioPlayer(context: Context): ExoPlayer =
    ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

/**
 * Media3 posts and updates the notification itself. [oldChannelId] is the channel the pre-Media3
 * service used; it was created at default importance, so reusing it could make the notification
 * alert. Media3 creates [channelId] as low importance.
 */
@OptIn(UnstableApi::class)
fun MediaSessionService.setUpMediaNotification(
    notificationId: Int,
    channelId: String,
    @StringRes channelName: Int,
    oldChannelId: String
) {
    NotificationManagerCompat.from(this).deleteNotificationChannel(oldChannelId)

    setMediaNotificationProvider(
        DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(notificationId)
            .setChannelId(channelId)
            .setChannelName(channelName)
            .build()
            .apply { setSmallIcon(R.drawable.small_launcher_foreground) }
    )
}

fun Player.playbackStatus() = when {
    playerError != null -> PlaybackStatus.ERROR
    isPlaying -> PlaybackStatus.PLAYING
    playbackState == Player.STATE_BUFFERING && playWhenReady -> PlaybackStatus.BUFFERING
    playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED ->
        PlaybackStatus.STOPPED
    else -> PlaybackStatus.PAUSED
}

/**
 * A [MediaController] connected to a player [service]. [onConnected] runs once, then [onChange]
 * runs on connect and after every player change. Both run on the main thread.
 */
class PlayerConnection(
    context: Context,
    service: Class<out MediaSessionService>,
    onConnected: (MediaController) -> Unit = {},
    onChange: (MediaController) -> Unit
) {

    private val future = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, service))
    ).buildAsync()
    private var isReleased = false

    /** Null until connected, and after [release]. */
    var controller: MediaController? = null
        private set

    init {
        future.addListener(
            {
                if (isReleased) return@addListener

                val controller = try {
                    future.get()
                } catch (_: CancellationException) {
                    return@addListener
                } catch (e: ExecutionException) {
                    e.report()
                    return@addListener
                }

                this.controller = controller
                controller.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) =
                        onChange(controller)
                })
                onConnected(controller)
                onChange(controller)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun release() {
        isReleased = true
        controller = null
        MediaController.releaseFuture(future)
    }

}

/**
 * Adds the time a player spends playing to the user's recitations record. [scope] must outlive
 * the service, since the last stretch is flushed from onDestroy.
 */
class ListeningTimeRecorder(
    private val userRepository: UserRepository,
    private val scope: CoroutineScope
) : Player.Listener {

    // ponytail: a stretch is recorded when playback stops, so a process killed mid-play loses it;
    // flush on a timer if that turns out to matter
    private var playingSince: Long? = null

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) playingSince = SystemClock.elapsedRealtime()
        else flush()
    }

    fun flush() {
        val since = playingSince ?: return
        playingSince = null
        val elapsed = SystemClock.elapsedRealtime() - since

        scope.launch {
            val old = userRepository.getRecitationsRecord().first()
            userRepository.setRecitationsRecord(old + elapsed)
        }
    }

}
