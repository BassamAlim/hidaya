package bassamalim.hidaya.features.recitations.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Activity
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.di.ApplicationScope
import bassamalim.hidaya.core.enums.StartAction
import bassamalim.hidaya.core.helpers.ListeningTimeRecorder
import bassamalim.hidaya.core.helpers.buildAudioPlayer
import bassamalim.hidaya.core.helpers.mediaNotificationProvider
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.core.utils.LangUtils.withAppLocale
import bassamalim.hidaya.core.utils.report
import bassamalim.hidaya.features.recitations.RecitationMediaId
import bassamalim.hidaya.features.recitations.recitersMenu.LastPlayedMedia
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Plays a narration's suras. A controller sets one media item whose media id is a
 * [RecitationMediaId]; the service expands it into the narration's whole playlist, starting at
 * that sura, so next/previous, shuffle and repeat are the player's own.
 */
@AndroidEntryPoint
@OptIn(UnstableApi::class)
class RecitationPlayerService : MediaSessionService() {

    @Inject lateinit var quranRepository: QuranRepository
    @Inject lateinit var recitationsRepository: RecitationsRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: MediaSession
    private lateinit var listeningTime: ListeningTimeRecorder

    // Notification text etc. in the app language (Services don't get it from AppCompat < API 33)
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate() {
        super.onCreate()

        setMediaNotificationProvider(
            mediaNotificationProvider(
                notificationId = 333,
                channelId = "recitations_playback",
                channelName = R.string.recitations,
                oldChannelId = "Recitations"
            )
        )

        val player = buildAudioPlayer(this)
        listeningTime = ListeningTimeRecorder(userRepository, appScope)
        player.addListener(listeningTime)
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) saveForLater()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                session.setSessionActivity(contentIntent(mediaItem?.mediaId))
            }
        })

        session = MediaSession.Builder(this, player)
            .setSessionActivity(contentIntent(mediaId = null))
            .setCallback(callback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        saveForLater()
        listeningTime.flush()
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
            val mediaId = mediaItems.first().mediaId
            val parts = RecitationMediaId.decode(mediaId)
                ?: return Futures.immediateFailedFuture(
                    IllegalArgumentException("Bad media id: $mediaId")
                )

            val result = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    val playlist = buildPlaylist(parts.reciterId, parts.narrationId)

                    val player = mediaSession.player
                    player.repeatMode = recitationsRepository.getRepeatMode().first()
                    player.shuffleModeEnabled = recitationsRepository.getShuffleMode().first() != 0

                    result.set(
                        MediaSession.MediaItemsWithStartPosition(
                            playlist,
                            playlist.indexOfFirst { it.mediaId == mediaId }.coerceAtLeast(0),
                            startPositionMs
                        )
                    )
                } catch (e: Exception) {
                    e.report()
                    result.setException(e)
                }
            }
            return result
        }
    }

    private suspend fun buildPlaylist(reciterId: Int, narrationId: Int): List<MediaItem> {
        val language = LangUtils.getAppLanguage()
        val suraNames = quranRepository.getDecoratedSuraNames(language)
        val reciterName = recitationsRepository.getSuraReciterName(reciterId, language)
        val narration = recitationsRepository.getNarration(reciterId, narrationId, language)
        val downloadDir =
            "${getExternalFilesDir(null)}${recitationsRepository.prefix}$reciterId/$narrationId/"

        return narration.availableSuras.sorted().map { suraNum ->
            val suraIdx = suraNum - 1
            val downloaded = File("$downloadDir$suraIdx.mp3")

            MediaItem.Builder()
                .setMediaId(RecitationMediaId.encode(reciterId, narrationId, suraIdx))
                .setUri(
                    if (downloaded.exists()) downloaded.toUri()
                    else String.format(Locale.US, "%s/%03d.mp3", narration.server, suraNum).toUri()
                )
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(suraNames[suraIdx])
                        .setArtist(reciterName)
                        .setAlbumTitle(narration.name)
                        .setTrackNumber(suraNum)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
    }

    private fun saveForLater() {
        val player = session.player
        val mediaId = player.currentMediaItem?.mediaId ?: return

        recitationsRepository.setLastPlayedMedia(
            LastPlayedMedia(mediaId = mediaId, progress = player.currentPosition)
        )
    }

    private fun contentIntent(mediaId: String?): PendingIntent {
        val intent = Intent(this, Activity::class.java).apply {
            action = StartAction.GO_TO_RECITATION.name
            putExtra("media_id", mediaId)
        }

        return PendingIntent.getActivity(
            this,
            36,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}
