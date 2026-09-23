package bassamalim.hidaya.core.data.repositories

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import bassamalim.hidaya.R
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.RecitationsPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.room.daos.RecitationNarrationsDao
import bassamalim.hidaya.core.data.dataSources.room.daos.SuraRecitersDao
import bassamalim.hidaya.core.data.dataSources.room.daos.VerseRecitationsDao
import bassamalim.hidaya.core.data.dataSources.room.daos.VerseRecitersDao
import bassamalim.hidaya.core.di.ApplicationScope
import bassamalim.hidaya.core.di.DefaultDispatcher
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.VerseRepeatMode
import bassamalim.hidaya.core.models.DownloadingRecitation
import bassamalim.hidaya.core.models.Narration
import bassamalim.hidaya.core.models.Recitation
import bassamalim.hidaya.core.models.Reciter
import bassamalim.hidaya.core.utils.FileUtils
import bassamalim.hidaya.core.utils.LangUtils.withAppLocale
import bassamalim.hidaya.core.utils.report
import bassamalim.hidaya.features.recitations.RecitationMediaId
import bassamalim.hidaya.features.recitations.recitersMenu.LastPlayedMedia
import com.google.gson.Gson
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import androidx.core.net.toUri

class RecitationsRepository @Inject constructor(
    private val app: Application,
    private val recitationsPreferencesDataSource: RecitationsPreferencesDataSource,
    private val suraRecitersDao: SuraRecitersDao,
    private val verseRecitationsDao: VerseRecitationsDao,
    private val verseRecitersDao: VerseRecitersDao,
    private val recitationNarrationsDao: RecitationNarrationsDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
    private val gson: Gson
) {

    val prefix = "/Telawat/"
    val dir = "${app.getExternalFilesDir(null)}$prefix"
    private val downloading = mutableMapOf<Long, DownloadingRecitation>()

    fun downloadSura(
        reciterId: Int,
        narrationId: Int,
        suraId: Int,
        suraSearchName: String,
        server: String
    ) {
        Thread {
            val link = String.format(Locale.US, "%s/%03d.mp3", server, suraId+1)
            val uri = link.toUri()

            val request = DownloadManager.Request(uri)
            request.setTitle(suraSearchName)
            FileUtils.createDir(app, prefix)
            request.setDestinationInExternalFilesDir(app, "$prefix/$reciterId/$narrationId/", "$suraId.mp3")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

            val downloadId = enqueueDownload(request) ?: return@Thread
            addToDownloading(downloadId, narrationId, suraId)
        }.start()
    }

    /** Null if the system refuses (some ROMs reject the app's own external files path). */
    fun enqueueDownload(request: DownloadManager.Request): Long? =
        try {
            (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        } catch (e: SecurityException) {
            e.report()
            // Callers enqueue from background threads
            Handler(Looper.getMainLooper()).post {
                val context = app.withAppLocale()
                Toast.makeText(context, context.getString(R.string.download_failed), Toast.LENGTH_LONG)
                    .show()
            }
            null
        }

    fun deleteSura(reciterId: Int, narrationId: Int, suraId: Int) {
        FileUtils.deleteFile(context = app, path = "$prefix$reciterId/$narrationId/$suraId.mp3")
    }

    fun observeAllSuraReciters(language: Language) =
        suraRecitersDao.observeAll().map {
            it.map { reciter ->
                Reciter(
                    id = reciter.id,
                    name = when (language) {
                        Language.ARABIC -> reciter.nameAr
                        Language.ENGLISH -> reciter.nameEn
                    },
                    isFavorite = reciter.isFavorite != 0
                )
            }
        }

    private fun getReciterFavoriteStatuses() = suraRecitersDao.observeFavoriteStatuses().map {
        it.map { (id, isFavorite) -> id to (isFavorite == 1) }.toMap()
    }

    fun setReciterFavorite(reciterId: Int, isFavorite: Boolean) {
        scope.launch {
            withContext(dispatcher) {
                suraRecitersDao.setFavoriteStatus(id = reciterId, value = if (isFavorite) 1 else 0)
                updateReciterFavoritesBackup(getReciterFavoriteStatuses().first())
            }
        }
    }

    fun setReciterFavorites(favorites: Map<Int, Boolean>) {
        scope.launch {
            favorites.map { (id, isFavorite) ->
                setReciterFavorite(id, isFavorite)
            }
        }
    }

    suspend fun getSuraReciterName(id: Int, language: Language) = withContext(dispatcher) {
        when (language) {
            Language.ARABIC -> suraRecitersDao.getNameAr(id)
            Language.ENGLISH -> suraRecitersDao.getNameEn(id)
        }
    }

    suspend fun getSuraReciterNames(language: Language) = withContext(dispatcher) {
        when (language) {
            Language.ARABIC -> suraRecitersDao.getNamesAr()
            Language.ENGLISH -> suraRecitersDao.getNamesEn()
        }
    }

    fun getReciterFavoritesBackup() = recitationsPreferencesDataSource.getReciterFavorites()

    private suspend fun updateReciterFavoritesBackup(favorites: Map<Int, Boolean>) {
        recitationsPreferencesDataSource.updateReciterFavorites(favorites.toPersistentMap())
    }

    suspend fun getAllNarrations(language: Language) = withContext(dispatcher) {
        recitationNarrationsDao.getAll().map {
            Narration(
                id = it.id,
                reciterId = it.reciterId,
                name = when (language) {
                    Language.ARABIC -> it.nameAr
                    Language.ENGLISH -> it.nameEn
                },
                server = it.url,
                availableSuras = gson.fromJson(it.availableSuras, IntArray::class.java)
            )
        }
    }

    suspend fun getReciterNarrations(reciterId: Int, language: Language) = withContext(dispatcher) {
        recitationNarrationsDao.getReciterNarrations(reciterId).let { narrations ->
            narrations.map {
                Recitation.Narration(
                    id = it.id,
                    name = when (language) {
                        Language.ARABIC -> it.nameAr
                        Language.ENGLISH -> it.nameEn
                    },
                    server = it.url,
                    availableSuras = gson.fromJson(it.availableSuras, IntArray::class.java)
                )
            }
        }
    }

    suspend fun getNarrationSelections(language: Language): Flow<Map<String, Boolean>> {
        val narrations = getAllNarrations(language).distinct()
        return recitationsPreferencesDataSource.getNarrationSelections().map { selections ->
            val mutableSelections = selections.toMutableMap()
            var added = false
            for (narration in narrations) {
                if (!mutableSelections.containsKey(narration.name)) {
                    mutableSelections[narration.name] = true
                    added = true
                }
            }
            if (added) setNarrationSelections(mutableSelections)

            mutableSelections
        }
    }

    suspend fun setNarrationSelections(selections: Map<String, Boolean>) {
        recitationsPreferencesDataSource.updateNarrationSelections(selections.toPersistentMap())
    }

    fun getNarrationDownloadState(reciterId: Int, narrationId: Int): DownloadState {
        val fileExists = File("${dir}${reciterId}/${narrationId}").exists()
        return if (fileExists) {
            if (isNarrationDownloading(narrationId)) DownloadState.DOWNLOADING
            else DownloadState.DOWNLOADED
        }
        else DownloadState.NOT_DOWNLOADED
    }

    fun getSuraDownloadState(reciterId: Int, narrationId: Int, suraId: Int): DownloadState {
        val fileExists = File("${dir}${reciterId}/${narrationId}/${suraId}.mp3").exists()
        return if (fileExists) {
            if (isSuraDownloading(narrationId, suraId)) DownloadState.DOWNLOADING
            else DownloadState.DOWNLOADED
        }
        else DownloadState.NOT_DOWNLOADED
    }

    fun getNarrationDownloadStates(ids: Map<Int, List<Int>>): Map<Int, Map<Int, DownloadState>> {
        return ids.map { (reciterId, narrationIds) ->
            reciterId to narrationIds.associateWith { narrationId ->
                getNarrationDownloadState(reciterId, narrationId)
            }
        }.toMap()
    }

    private fun isNarrationDownloading(narrationId: Int) =
        downloading.values.any { it.narrationId == narrationId }

    private fun isSuraDownloading(narrationId: Int, suraId: Int) =
        downloading.values.any { it.narrationId == narrationId && it.suraId == suraId }

    fun addToDownloading(downloadId: Long, narrationId: Int, suraId: Int) {
        downloading[downloadId] = DownloadingRecitation(narrationId, suraId)
    }

    fun popFromDownloading(downloadId: Long) = downloading.remove(downloadId)

    fun getRepeatMode() = recitationsPreferencesDataSource.getRepeatMode()

    suspend fun setRepeatMode(mode: Int) {
        recitationsPreferencesDataSource.updateRepeatMode(mode)
    }

    fun getShuffleMode() = recitationsPreferencesDataSource.getShuffleMode()

    suspend fun setShuffleMode(mode: Int) {
        recitationsPreferencesDataSource.updateShuffleMode(mode)
    }

    fun getLastPlayedMedia(): Flow<LastPlayedMedia?> {
        return recitationsPreferencesDataSource.getLastPlayedMedia().map {
            // Drop ids an older version saved corrupted, so nothing downstream has to cope
            if (it != null && RecitationMediaId.decode(it.mediaId) == null) {
                setLastPlayedMedia(null)
                null
            }
            else it
        }
    }

    fun setLastPlayedMedia(lastPlayed: LastPlayedMedia?) {
        scope.launch {
            recitationsPreferencesDataSource.updateLastPlayedMedia(lastPlayed)
        }
    }

    fun getVerseReciterId() = recitationsPreferencesDataSource.getVerseReciterId()

    fun setVerseReciterId(verseReciterId: Int) {
        scope.launch {
            recitationsPreferencesDataSource.updateVerseReciterId(verseReciterId)
        }
    }

    fun getVerseRepeatMode() = recitationsPreferencesDataSource.getVerseRepeatMode()

    fun setVerseRepeatMode(verseRepeatMode: VerseRepeatMode) {
        scope.launch {
            recitationsPreferencesDataSource.updateVerseRepeatMode(verseRepeatMode)
        }
    }

    fun getShouldStopOnSuraEnd() = recitationsPreferencesDataSource.getShouldStopOnSuraEnd()

    fun setShouldStopOnSuraEnd(shouldStopOnSuraEnd: Boolean) {
        scope.launch {
            recitationsPreferencesDataSource.updateShouldStopOnSuraEnd(shouldStopOnSuraEnd)
        }
    }

    fun getShouldStopOnPageEnd() = recitationsPreferencesDataSource.getShouldStopOnPageEnd()

    fun setShouldStopOnPageEnd(shouldStopOnPageEnd: Boolean) {
        scope.launch {
            recitationsPreferencesDataSource.updateShouldStopOnPageEnd(shouldStopOnPageEnd)
        }
    }

    suspend fun getVerseReciterNames() = withContext(dispatcher) {
        verseRecitersDao.getNames()
    }

    suspend fun getNarration(reciterId: Int, narrationId: Int, language: Language) =
        withContext(dispatcher) {
            recitationNarrationsDao.getNarration(reciterId, narrationId).let {
                Recitation.Narration(
                    id = it.id,
                    name = when (language) {
                        Language.ARABIC -> it.nameAr
                        Language.ENGLISH -> it.nameEn
                    },
                    server = it.url,
                    availableSuras = gson.fromJson(it.availableSuras, IntArray::class.java)
                )
            }
        }

    suspend fun getAllVerseRecitations() = withContext(dispatcher) {
        verseRecitationsDao.getAll()
    }

}