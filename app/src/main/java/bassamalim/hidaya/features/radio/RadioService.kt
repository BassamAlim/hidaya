package bassamalim.hidaya.features.radio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Activity
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.helpers.buildAudioPlayer
import bassamalim.hidaya.core.helpers.mediaNotificationProvider
import bassamalim.hidaya.core.utils.LangUtils.withAppLocale
import bassamalim.hidaya.core.utils.report
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Plays the live Quran radio. A controller sets one media item whose media id is the static
 * stream link; the service resolves it to the current dynamic link before playing.
 */
@OptIn(UnstableApi::class)
class RadioService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: MediaSession

    // Notification text etc. in the app language (Services don't get it from AppCompat < API 33)
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate() {
        super.onCreate()

        setMediaNotificationProvider(
            mediaNotificationProvider(
                notificationId = 444,
                channelId = "quran_radio_playback",
                channelName = R.string.quran_radio,
                oldChannelId = "QuranRadio"
            )
        )

        val player = buildAudioPlayer(this)
        player.addListener(object : Player.Listener {
            // A live stream paused for long falls out of the stream's window; rejoin at live
            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                }
            }
        })

        session = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    37,
                    Intent(this, Activity::class.java).setAction("back"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setCallback(callback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        serviceScope.cancel()
        session.player.release()
        session.release()
        super.onDestroy()
    }

    private val callback = object : MediaSession.Callback {
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val staticUrl = mediaItems.first().mediaId
            val result = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()

            serviceScope.launch {
                // On failure play the static link anyway, so the player reports the error and
                // the screen can offer a retry, which re-resolves
                val url = resolveDynamicUrl(staticUrl) ?: staticUrl
                val item = MediaItem.Builder()
                    .setMediaId(staticUrl)
                    .setUri(url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(getString(R.string.quran_radio))
                            .setIsPlayable(true)
                            .build()
                    )
                    .build()
                result.set(MediaSession.MediaItemsWithStartPosition(listOf(item), 0, 0))
            }

            return result
        }
    }

    // Other Links:
    // https://www.aloula.sa/83c0bda5-18e7-4c80-9c0a-21e764537d47
    // https://m.live.net.sa:1935/live/quransa/playlist.m3u8

    /**
     * Follows the static link's redirect to get the final dynamic link. The redirect points to
     * plain http, which the app may not load, so it is upgraded to https.
     */
    private suspend fun resolveDynamicUrl(staticUrl: String): String? =
        try {
            withContext(Dispatchers.IO) {
                val connection = URL(staticUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                URL(connection.getHeaderField("Location")).toString()
                    .replaceFirst("http:".toRegex(), "https:")
            }.also { Log.i(Globals.TAG, "Dynamic Quran Radio URL: $it") }
        } catch (e: IOException) {
            e.report()
            Log.e(Globals.TAG, "Problem resolving the Quran radio link", e)
            null
        }

}
