package bassamalim.hidaya.features.recitations.player

import android.app.Application
import android.app.DownloadManager
import androidx.core.net.toUri
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.utils.FileUtils
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.features.recitations.recitersMenu.Recitation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecitationPlayerDomain @Inject constructor(
    private val app: Application,
    private val recitationsRepository: RecitationsRepository,
    private val quranRepository: QuranRepository
) {

    private var path = ""

    fun setPath(reciterId: Int, narrationId: Int) {
        this.path = "${recitationsRepository.prefix}${reciterId}/$narrationId/"
    }

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getSuraNames(language: Language) = quranRepository.getDecoratedSuraNames(language)

    suspend fun getReciterName(id: Int, language: Language) =
        recitationsRepository.getSuraReciterName(id, language)

    suspend fun getNarration(reciterId: Int, narrationId: Int, language: Language) =
        recitationsRepository.getNarration(reciterId, narrationId, language).let {
            Recitation.Narration(
                id = it.id,
                name = it.name,
                server = it.server,
                availableSuras = it.availableSuras,
                downloadState = recitationsRepository.getNarrationDownloadState(
                    reciterId,
                    narrationId
                )
            )
        }

    fun getRepeatMode() = recitationsRepository.getRepeatMode()

    suspend fun setRepeatMode(mode: Int) {
        recitationsRepository.setRepeatMode(mode)
    }

    fun isShuffleOn() = recitationsRepository.getShuffleMode().map { it != 0 }

    suspend fun setShuffleOn(isOn: Boolean) {
        recitationsRepository.setShuffleMode(if (isOn) 1 else 0)
    }

    suspend fun getLastPlayedProgress() =
        recitationsRepository.getLastPlayedMedia().first()?.progress ?: 0L

    fun downloadRecitation(narration: Recitation.Narration, suraIdx: Int, suraName: String) {
        val server = narration.server
        val link = String.Companion.format(Locale.US, "%s/%03d.mp3", server, suraIdx+1)
        val uri = link.toUri()

        val request = DownloadManager.Request(uri)
        request.setTitle(suraName)
        FileUtils.createDir(app, path)
        request.setDestinationInExternalFilesDir(app, path, "${suraIdx}.mp3")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        recitationsRepository.enqueueDownload(request)
    }

    fun deleteRecitation() {
        FileUtils.deleteFile(app, path)
    }

    fun checkDownload(): DownloadState {
        return if (File("${app.getExternalFilesDir(null)}$path").exists())
            DownloadState.DOWNLOADED
        else
            DownloadState.NOT_DOWNLOADED
    }

}
