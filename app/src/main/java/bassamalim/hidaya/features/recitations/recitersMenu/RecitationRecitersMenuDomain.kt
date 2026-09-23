package bassamalim.hidaya.features.recitations.recitersMenu

import android.app.Activity
import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.helpers.ReceiverWrapper
import bassamalim.hidaya.core.helpers.Searcher
import bassamalim.hidaya.core.utils.FileUtils
import bassamalim.hidaya.features.recitations.RecitationMediaId
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.features.quran.surasMenu.RecitationInfo
import bassamalim.hidaya.features.recitations.player.RecitationPlayerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Locale
import javax.inject.Inject

class RecitationRecitersMenuDomain @Inject constructor(
    private val app: Application,
    private val recitationsRepository: RecitationsRepository,
    private val quranRepository: QuranRepository
) {

    private lateinit var activity: Activity
    private var mediaBrowser: MediaBrowserCompat? = null
    private lateinit var controller: MediaControllerCompat
    private lateinit var tc: MediaControllerCompat.TransportControls
    private val searcher = Searcher<Recitation>()
    private val downloadReceiver = ReceiverWrapper(
        context = app,
        intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                try {
                    recitationsRepository.popFromDownloading(downloadId)
                } catch (_: RuntimeException) {}
            }
        }
    )

    suspend fun observeRecitersWithNarrations(language: Language): Flow<Map<Int, Recitation>> {
        val allReciters = recitationsRepository.observeAllSuraReciters(language)
        val allNarrations = recitationsRepository.getAllNarrations(language)
        val downloadStates = recitationsRepository.getNarrationDownloadStates(
            ids = allReciters.first().associate { reciter ->
                reciter.id to allNarrations.filter { narration ->
                    narration.reciterId == reciter.id
                }.map { narration ->
                    narration.id
                }
            }
        )

        return allReciters.map {
            it.associate { reciter ->
                reciter.id to Recitation(
                    reciterId = reciter.id,
                    reciterName = reciter.name,
                    isFavoriteReciter = reciter.isFavorite,
                    narrations = allNarrations.filter { narration ->
                        narration.reciterId == reciter.id
                    }.associate { narration ->
                        narration.id to Recitation.Narration(
                            id = narration.id,
                            name = narration.name,
                            server = narration.server,
                            availableSuras = narration.availableSuras,
                            downloadState = downloadStates[reciter.id]?.get(narration.id)!!
                        )
                    }
                )
            }
        }
    }

    fun registerDownloadReceiver() {
        downloadReceiver.register()
    }

    fun unregisterDownloadReceiver() {
        downloadReceiver.unregister()
    }

    fun cleanFiles() {
        val mainDir = File(recitationsRepository.dir)
        FileUtils.deleteDirRecursive(mainDir)
    }

    suspend fun downloadNarration(
        reciterId: Int,
        narration: Recitation.Narration,
        suraNames: List<String>,
        suraString: String
    ) {
        val language = getLanguage()
        val reciterNames = recitationsRepository.getSuraReciterNames(language)
        Thread {
            var request: DownloadManager.Request
            var posted = false
            for (i in 0..113) {
                if (narration.availableSuras.contains(i+1)) {
                    val link = String.format(
                        Locale.US,
                        "%s/%03d.mp3",
                        narration.server,
                        i + 1
                    )
                    val uri = link.toUri()

                    request = DownloadManager.Request(uri)
                    request.setTitle(
                        "${reciterNames[reciterId]} ${narration.name}" +
                                " $suraString ${suraNames[i]}"
                    )
                    val suffix = "${recitationsRepository.prefix}$reciterId/${narration.id}"
                    FileUtils.createDir(app, suffix)
                    request.setDestinationInExternalFilesDir(app, suffix, "$i.mp3")
                    request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )

                    // All suras share the same path, so if one is refused the rest will be too
                    val downloadId = recitationsRepository.enqueueDownload(request) ?: break
                    if (!posted) {
                        recitationsRepository.addToDownloading(downloadId, reciterId, narration.id)
                        posted = true
                    }
                }
            }
        }.start()
    }

    fun deleteNarration(reciterId: Int, narration: Recitation.Narration) {
        FileUtils.deleteFile(
            context = app,
            path = "${recitationsRepository.prefix}$reciterId/${narration.id}"
        )
    }

    fun getLanguage() = LangUtils.getAppLanguage()

    fun setFavorite(reciterId: Int, value: Boolean) {
        recitationsRepository.setReciterFavorite(
            reciterId = reciterId,
            isFavorite = value
        )
    }

    suspend fun getLastPlayedMedia(mediaId: String): RecitationInfo? {
        val (reciterId, narrationId, suraId) = RecitationMediaId.decode(mediaId) ?: return null
        Log.d(
            "RecitationsRecitersMenuViewModel",
            "reciterId: $reciterId, narrationId: $narrationId, suraIndex: $suraId"
        )

        val language = getLanguage()
        return RecitationInfo(
            reciterName = getReciterName(reciterId, language),
            narrationName = getNarration(reciterId, narrationId, language).name,
            suraName = getSuraNames(language)[suraId]
        )
    }

    suspend fun getNarration(reciterId: Int, narrationId: Int, language: Language) =
        recitationsRepository.getNarration(reciterId, narrationId, language)

    suspend fun getReciterName(reciterId: Int, language: Language) =
        recitationsRepository.getSuraReciterName(reciterId, language)

    suspend fun getSuraNames(language: Language) = quranRepository.getDecoratedSuraNames(language)

    suspend fun getNarrationSelections(language: Language) =
        recitationsRepository.getNarrationSelections(language)

    fun getLastPlayed() = recitationsRepository.getLastPlayedMedia()

    fun getSearchResults(searchText: String, recitations: List<Recitation>) =
        searcher.containsSearch(
            items = recitations,
            query = searchText,
            keySelector = { recitation -> recitation.reciterName }
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    fun connect(
        activity: Activity,
        connectionCallbacks: MediaBrowserCompat.ConnectionCallback
    ) {
        this.activity = activity

        mediaBrowser = MediaBrowserCompat(
            activity,
            ComponentName(activity, RecitationPlayerService::class.java),
            connectionCallbacks,
            null
        )
        mediaBrowser?.connect()

        activity.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    fun isConnected() = mediaBrowser?.isConnected == true

    fun initializeController(controllerCallback: MediaControllerCompat.Callback) {
        Log.d(Globals.TAG, "in initializeController of RecitationPlayerDomain")

        // Get the token for the MediaSession
        val token = mediaBrowser!!.sessionToken

        // Create a MediaControllerCompat
        val mediaController = MediaControllerCompat(activity, token)

        // Save the controller
        MediaControllerCompat.setMediaController(activity, mediaController)

        controller = MediaControllerCompat.getMediaController(activity)
        tc = controller.transportControls

        // Register a Callback to stay in sync
        controller.registerCallback(controllerCallback)
    }

    fun pause() = tc.pause()

    fun resume() = tc.play()

    fun getState() = controller.playbackState.state

    fun getMetadata(): MediaMetadataCompat = controller.metadata

    fun getPlaybackState(): PlaybackStateCompat = controller.playbackState

    fun disconnectMediaBrowser() {
        mediaBrowser?.disconnect()
    }

    fun stopMediaBrowser(controllerCallback: MediaControllerCompat.Callback) {
        downloadReceiver.unregister()

        MediaControllerCompat.getMediaController(activity)
            ?.unregisterCallback(controllerCallback)

        disconnectMediaBrowser()
    }

}