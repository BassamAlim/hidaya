package bassamalim.hidaya.features.quran.reader.versePlayer

import android.content.Context
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import bassamalim.hidaya.R
import bassamalim.hidaya.core.data.dataSources.room.entities.Verse
import bassamalim.hidaya.core.data.dataSources.room.entities.VerseRecitation
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.di.ApplicationScope
import bassamalim.hidaya.core.enums.VerseRepeatMode
import bassamalim.hidaya.core.helpers.ListeningTimeRecorder
import bassamalim.hidaya.core.helpers.buildAudioPlayer
import bassamalim.hidaya.core.helpers.mediaNotificationProvider
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.core.utils.LangUtils.withAppLocale
import bassamalim.hidaya.core.utils.report
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
import java.util.Locale
import javax.inject.Inject

/**
 * Plays Quran verses one after another. A controller sets one media item whose media id is the
 * verse id to start from. The player gets a short queue of the verses after it, topped up as it
 * advances, which ends early at the page or sura end if the user chose to stop there.
 */
@AndroidEntryPoint
@OptIn(UnstableApi::class)
class VersePlayerService : MediaSessionService() {

    @Inject lateinit var quranRepository: QuranRepository
    @Inject lateinit var recitationsRepository: RecitationsRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: MediaSession
    private lateinit var listeningTime: ListeningTimeRecorder
    private var data: Data? = null
    /** Times the current verse has finished and started over. */
    private var repeatsDone = 0
    private var isQueueing = false

    private class Data(
        val verses: List<Verse>,
        val recitations: List<VerseRecitation>,
        val reciterNames: List<String>,
        val suraNames: List<String>
    )

    companion object {
        private const val QUEUE_AHEAD = 5
    }

    // Notification text etc. in the app language (Services don't get it from AppCompat < API 33)
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate() {
        super.onCreate()

        setMediaNotificationProvider(
            mediaNotificationProvider(
                notificationId = 101,
                channelId = "verse_playback",
                channelName = R.string.recitations,
                oldChannelId = "AyaPlayer"
            )
        )

        val player = buildAudioPlayer(this)
        listeningTime = ListeningTimeRecorder(userRepository, appScope)
        player.addListener(listeningTime)
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) repeatsDone++
                else {
                    repeatsDone = 0
                    dropPlayedVerses()
                }

                serviceScope.launch {
                    applyRepeatMode()
                    topUpQueue()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    this@VersePlayerService,
                    R.string.error_fetching_data,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        session = MediaSession.Builder(this, player)
            .setCallback(callback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
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
            val verseId = mediaItems.first().mediaId.toIntOrNull()
                ?: return Futures.immediateFailedFuture(
                    IllegalArgumentException("Bad verse id: ${mediaItems.first().mediaId}")
                )

            val result = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    val loaded = data ?: loadData().also { data = it }
                    val startIdx = verseId - 1
                    val queue = listOf(verseItem(loaded, startIdx, currentRecitation(loaded))) +
                            versesAfter(loaded, startIdx, QUEUE_AHEAD)

                    result.set(MediaSession.MediaItemsWithStartPosition(queue, 0, 0))
                } catch (e: Exception) {
                    e.report()
                    result.setException(e)
                }
            }
            return result
        }
    }

    private suspend fun loadData() = Data(
        verses = quranRepository.getAllVerses(),
        recitations = recitationsRepository.getAllVerseRecitations(),
        reciterNames = recitationsRepository.getVerseReciterNames(),
        suraNames = quranRepository.getDecoratedSuraNames(language = LangUtils.getAppLanguage())
    )

    private suspend fun applyRepeatMode() {
        session.player.repeatMode =
            playerRepeatMode(recitationsRepository.getVerseRepeatMode().first(), repeatsDone)
    }

    private suspend fun topUpQueue() {
        val data = data ?: return
        if (isQueueing) return
        isQueueing = true

        try {
            val player = session.player
            if (player.mediaItemCount == 0) return
            val ahead = player.mediaItemCount - 1 - player.currentMediaItemIndex
            if (ahead >= QUEUE_AHEAD) return

            val lastIdx = player.getMediaItemAt(player.mediaItemCount - 1).mediaId.toInt() - 1
            player.addMediaItems(versesAfter(data, lastIdx, QUEUE_AHEAD - ahead))
        } catch (e: Exception) {
            e.report()
        } finally {
            isQueueing = false
        }
    }

    /** Keeps one played verse, so "previous" still has somewhere to go. */
    private fun dropPlayedVerses() {
        val player = session.player
        if (player.currentMediaItemIndex > 1)
            player.removeMediaItems(0, player.currentMediaItemIndex - 1)
    }

    private suspend fun versesAfter(data: Data, lastIdx: Int, count: Int): List<MediaItem> {
        val recitation = currentRecitation(data)

        return versesToQueue(
            verses = data.verses,
            lastIdx = lastIdx,
            count = count,
            stopOnPageEnd = recitationsRepository.getShouldStopOnPageEnd().first(),
            stopOnSuraEnd = recitationsRepository.getShouldStopOnSuraEnd().first()
        ).map { idx -> verseItem(data, idx, recitation) }
    }

    /** The chosen reciter's best-quality recitation. */
    private suspend fun currentRecitation(data: Data): VerseRecitation {
        val reciterId = recitationsRepository.getVerseReciterId().first()
        return data.recitations.filter { it.reciterId == reciterId }.maxBy { it.bitrate }
    }

    private fun verseItem(data: Data, verseIdx: Int, recitation: VerseRecitation): MediaItem {
        val verse = data.verses[verseIdx]
        val uri = "https://www.everyayah.com/data/${recitation.source}" +
                String.format(Locale.US, "%03d%03d.mp3", verse.suraNum, verse.num)

        return MediaItem.Builder()
            .setMediaId(verse.id.toString())
            .setUri(uri.toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(data.suraNames[verse.suraNum - 1])
                    .setArtist(data.reciterNames[recitation.reciterId])
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

}

/**
 * The player's repeat mode for a verse that has already started over [repeatsDone] times: it
 * loops until the verse's last play in [mode], then moves on.
 */
internal fun playerRepeatMode(mode: VerseRepeatMode, repeatsDone: Int): Int {
    val plays = when (mode) {
        VerseRepeatMode.NO_REPEAT -> 1
        VerseRepeatMode.TWO -> 2
        VerseRepeatMode.THREE -> 3
        VerseRepeatMode.FIVE -> 5
        VerseRepeatMode.INFINITE -> Int.MAX_VALUE
    }

    return if (repeatsDone + 1 < plays) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
}

/**
 * Indices of up to [count] verses following [lastIdx], stopping before one that starts another
 * page or sura when set to.
 */
internal fun versesToQueue(
    verses: List<Verse>,
    lastIdx: Int,
    count: Int,
    stopOnPageEnd: Boolean,
    stopOnSuraEnd: Boolean
): List<Int> {
    val indices = mutableListOf<Int>()
    var idx = lastIdx
    while (indices.size < count && idx + 1 < verses.size) {
        val current = verses[idx]
        val next = verses[idx + 1]
        if (stopOnSuraEnd && next.suraNum != current.suraNum) break
        if (stopOnPageEnd && next.pageNum != current.pageNum) break

        idx++
        indices += idx
    }
    return indices
}
