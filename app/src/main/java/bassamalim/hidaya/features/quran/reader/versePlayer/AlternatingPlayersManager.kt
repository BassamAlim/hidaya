package bassamalim.hidaya.features.quran.reader.versePlayer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnErrorListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.PowerManager
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.dataSources.room.entities.Verse
import bassamalim.hidaya.core.data.dataSources.room.entities.VerseRecitation
import bassamalim.hidaya.core.enums.VerseRepeatMode
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

@UnstableApi
class AlternatingPlayersManager(
    private val context: Context,
    private val allVerses: List<Verse>,
    private val recitationFlow: Flow<VerseRecitation>,
    private val repeatModeFlow: Flow<VerseRepeatMode>,
    private val stopOnPageEndFlow: Flow<Boolean>,
    private val stopOnSuraEndFlow: Flow<Boolean>,
    private val callback: PlayerCallback,
    private val scope: CoroutineScope
) : OnPreparedListener, OnCompletionListener, OnErrorListener {

    private val numOfPlayers = 2
    private var audioAttributes: AudioAttributes? = null
    private var volume = 1F
    private val aps = Array(numOfPlayers) { AlternatePlayer(createPlayer()) }
    private var playerIdx = 0
    var verseIdx = -1
    private var isPaused = false
    private var isReleased = false

    private fun createPlayer() = MediaPlayer().apply {
        setOnPreparedListener(this@AlternatingPlayersManager)
        setOnCompletionListener(this@AlternatingPlayersManager)
        setOnErrorListener(this@AlternatingPlayersManager)

        setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)

        audioAttributes?.let { setAudioAttributes(it) }
        setVolume(volume, volume)
    }

    override fun onPrepared(mp: MediaPlayer) {
        val currentPlayerIdx = idx(mp)
        val prvPlayerIdx = prvIdx(mp)
        val nxtPlayerIdx = nxtIdx(mp)
        Log.d(
            Globals.TAG,
            "in onPrepared with playerIdx: $currentPlayerIdx " +
                    "and verseIdx: ${aps[currentPlayerIdx].verseIdx}"
        )

        aps[currentPlayerIdx].state = PlayerState.PREPARED

        if (aps[prvPlayerIdx].state == PlayerState.NONE
            || aps[prvPlayerIdx].state == PlayerState.COMPLETED) {
            // check
            if (!isOtherPlayerPlaying())
                play(playerIdx = currentPlayerIdx, verseIdx = aps[currentPlayerIdx].verseIdx)
        }

        scope.launch {
            if (isReleased) return@launch

            val shouldStop = checkShouldStop(
                currentVerse = aps[currentPlayerIdx].verseIdx,
                shouldStopOnPageEnd = stopOnPageEndFlow.first(),
                shouldStopOnSuraEnd = stopOnSuraEndFlow.first()
            )
            if (shouldStop) aps[nxtPlayerIdx].state = PlayerState.STOPPED
            else {
                if (!isOtherPlayerPreparing()
                    && (aps[nxtPlayerIdx].state == PlayerState.NONE
                            || aps[nxtPlayerIdx].state == PlayerState.COMPLETED)) {
                    prepare(  // prepare next
                        playerIdx = nxt(currentPlayerIdx),
                        verseIdx = aps[currentPlayerIdx].verseIdx + 1
                    )
                }
            }
        }
    }

    override fun onCompletion(mp: MediaPlayer) {
        val currentPlayerIdx = idx(mp)
        val nxtPlayerIdx = nxtIdx(mp)
        Log.d(
            Globals.TAG,
            "in onCompletion with playerIdx: $currentPlayerIdx" +
                " and verseIdx: ${aps[currentPlayerIdx].verseIdx}"
        )

        aps[currentPlayerIdx].state = PlayerState.COMPLETED
        aps[currentPlayerIdx].repeated++

        scope.launch {
            if (isReleased) return@launch

            val shouldRepeat = checkShouldRepeat(currentPlayerIdx, repeatModeFlow.first())
            if (shouldRepeat)
                aps[currentPlayerIdx].mp.start()
            else {
                when (aps[nxtPlayerIdx].state) {
                    PlayerState.PREPARED -> {
                        if (!isOtherPlayerPlaying())
                            play(playerIdx = nxtPlayerIdx, verseIdx = aps[nxtPlayerIdx].verseIdx)
                    }
                    PlayerState.STOPPED -> {  // finished
                        callback.updatePbState(PlaybackStateCompat.STATE_STOPPED)
                    }
                    else -> {}
                }

                val shouldStop = checkShouldStop(
                    currentVerse = aps[currentPlayerIdx].verseIdx,
                    shouldStopOnPageEnd = stopOnPageEndFlow.first(),
                    shouldStopOnSuraEnd = stopOnSuraEndFlow.first()
                )
                if (shouldStop) aps[currentPlayerIdx].state = PlayerState.STOPPED
                else {
                    if (!isOtherPlayerPreparing()) {
                        prepare(
                            playerIdx = currentPlayerIdx,
                            verseIdx = aps[currentPlayerIdx].verseIdx + numOfPlayers
                        )
                    }
                }
            }
        }
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        Log.d(Globals.TAG, "in onError")

        Toast.makeText(
            context,
            context.getString(R.string.error_fetching_data),
            Toast.LENGTH_SHORT
        ).show()

        reset()

        callback.updatePbState(PlaybackStateCompat.STATE_STOPPED)

        return true
    }

    fun playFromMediaId(verseIdx: Int) {
        Log.d(
            Globals.TAG,
            "in playFromMediaId in AlternatingPlayersManager with verseIdx: $verseIdx"
        )

        if (isReleased || verseIdx != this.verseIdx) playNew(verseIdx)
        else if (isPaused) resume()

        callback.updatePbState(PlaybackStateCompat.STATE_PLAYING)
    }

    fun resume() {
        if (isReleased) {  // the players were torn down, restart the current verse
            if (verseIdx < 0) return
            playNew(verseIdx)
        }
        else {
            isPaused = false
            aps[playerIdx].mp.start()
        }

        callback.updatePbState(PlaybackStateCompat.STATE_PLAYING)
    }

    fun pause() {
        if (isReleased) return

        aps[playerIdx].mp.pause()

        isPaused = true
        callback.updatePbState(PlaybackStateCompat.STATE_PAUSED)
    }

    fun seekTo(pos: Long) {
        if (isReleased) return

        aps[playerIdx].mp.seekTo(pos.toInt())
    }

    fun previousVerse() {
        if (verseIdx > 0)
            playNew(verseIdx - 1)
    }

    fun nextVerse() {
        if (verseIdx < allVerses.size - 1)
            playNew(verseIdx + 1)
    }

    fun getCurrentPosition() = if (isReleased) 0 else aps[playerIdx].mp.currentPosition

    fun getDuration() = if (isReleased) 0 else aps[playerIdx].mp.duration

    private fun reset() {
        if (isReleased) return

        aps.map { ap ->
            ap.mp.reset()
            ap.state = PlayerState.NONE
        }
    }

    fun isNotInitialized(): Boolean {
        return !aps.map { ap ->
            ap.state == PlayerState.NONE
        }.contains(false)
    }

    fun release() {
        if (isReleased) return
        isReleased = true

        aps.map { ap ->
            // release() stops playback on its own and, unlike stop(), is valid in every state
            try {
                ap.mp.release()
            } catch (e: Exception) {
                Log.e(Globals.TAG, "Failed to release a verse player", e)
            }
            ap.state = PlayerState.NONE
        }

        callback.updatePbState(PlaybackStateCompat.STATE_STOPPED)
    }

    /**
     * Replaces the released players with fresh ones so playback can start again
     * on a service instance that was stopped but not destroyed.
     */
    private fun recreatePlayers() {
        aps.map { ap ->
            ap.mp = createPlayer()
            ap.state = PlayerState.NONE
            ap.repeated = 0
        }

        isReleased = false
    }

    fun setAudioAttributes(audioAttributes: AudioAttributes) {
        this.audioAttributes = audioAttributes

        if (isReleased) return
        aps.map { ap ->
            ap.mp.setAudioAttributes(audioAttributes)
        }
    }

    fun setVolume(volume: Float) {
        this.volume = volume

        if (isReleased) return
        aps.map { ap ->
            ap.mp.setVolume(volume, volume)
        }
    }

    private fun playNew(verseIdx: Int) {
        this.verseIdx = verseIdx
        isPaused = false

        if (isReleased) recreatePlayers()
        else reset()

        prepare(playerIdx = 0, verseIdx = verseIdx)  // prepare first
    }

    private fun play(playerIdx: Int, verseIdx: Int) {
        aps[playerIdx].state = PlayerState.PLAYING
        this.playerIdx = playerIdx
        this.verseIdx = verseIdx
        aps[playerIdx].mp.start()
        scope.launch {
            callback.track(verseId = verseIdx+1)
        }
    }

    private fun prepare(playerIdx: Int, verseIdx: Int) {
        aps[playerIdx].state = PlayerState.PREPARING
        aps[playerIdx].verseIdx = verseIdx
        aps[playerIdx].repeated = 0

        scope.launch {
            if (isReleased) return@launch

            val uri: Uri
            try {
                aps[playerIdx].mp.reset()
                uri = getUri(allVerses[verseIdx], recitation = recitationFlow.first())
                aps[playerIdx].mp.setDataSource(context, uri)
                aps[playerIdx].mp.prepareAsync()
            } catch (e: Exception) {
                e.report()
                e.printStackTrace()
                Log.e(Globals.TAG, "Reciter not found in verse recitations")
            }
        }
    }

    private fun checkShouldRepeat(playerIdx: Int, repeatMode: VerseRepeatMode): Boolean {
        Log.d(Globals.TAG, "in shouldRepeat with playerIdx: $playerIdx, repeatMode: $repeatMode")

        return when (repeatMode) {
            VerseRepeatMode.NO_REPEAT -> false
            VerseRepeatMode.TWO -> aps[playerIdx].repeated < 2
            VerseRepeatMode.THREE -> aps[playerIdx].repeated < 3
            VerseRepeatMode.FIVE -> aps[playerIdx].repeated < 5
            VerseRepeatMode.INFINITE -> true
        }
    }

    private fun isOtherPlayerPlaying(): Boolean {
        return aps.map { ap ->
            ap.state == PlayerState.PLAYING
        }.contains(true)
    }

    private fun isOtherPlayerPreparing(): Boolean {
        return aps.map { ap ->
            ap.state == PlayerState.PREPARING
        }.contains(true)
    }

    private fun checkShouldStop(
        currentVerse: Int,
        shouldStopOnPageEnd: Boolean,
        shouldStopOnSuraEnd: Boolean
    ): Boolean {
        val targetVerse = currentVerse + numOfPlayers
        return targetVerse >= allVerses.size
                || (shouldStopOnSuraEnd
                && allVerses[currentVerse].suraNum != allVerses[targetVerse].suraNum)
                || (shouldStopOnPageEnd
                && allVerses[currentVerse].pageNum != allVerses[targetVerse].pageNum)
    }

    private fun getUri(verse: Verse, recitation: VerseRecitation): Uri {
        Log.d(Globals.TAG, "In getUri with verse: $verse, recitation: $recitation")

        var uri = "https://www.everyayah.com/data/"
        uri += recitation.source
        uri += String.format(Locale.US, "%03d%03d.mp3", verse.suraNum, verse.num)
        return uri.toUri()
    }

    private fun prv(current: Int) = if (current == 0) numOfPlayers - 1 else current - 1
    private fun nxt(current: Int) = (current + 1) % numOfPlayers
    private fun idx(mp: MediaPlayer) = aps.indexOf(aps.find { ap -> ap.mp == mp })
    private fun prvIdx(mp: MediaPlayer) = prv(idx(mp))
    private fun nxtIdx(mp: MediaPlayer) = nxt(idx(mp))

}