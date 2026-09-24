package bassamalim.hidaya.features.quran.settings

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.enums.VerseRepeatMode
import bassamalim.hidaya.core.enums.QuranViewType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranSettingsDomain @Inject constructor(
    private val quranRepository: QuranRepository,
    private val recitationsRepository: RecitationsRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    suspend fun getReciterNames() = recitationsRepository.getVerseReciterNames()

    fun getViewType() = quranRepository.getViewType()

    suspend fun setViewType(viewType: QuranViewType) {
        quranRepository.setViewType(viewType)
    }

    fun getFillPage() = quranRepository.getFillPage()

    suspend fun setFillPage(fillPage: Boolean) {
        quranRepository.setFillPage(fillPage)
    }

    fun getTextSize() = quranRepository.getTextSize()

    suspend fun setTextSize(size: Float) {
        quranRepository.setTextSize(size)
    }

    fun getKeepScreenOn() = quranRepository.getKeepScreenOn()

    suspend fun setKeepScreenOn(keepScreenOn: Boolean) {
        quranRepository.setKeepScreenOn(keepScreenOn)
    }

    fun getReciterId() = recitationsRepository.getVerseReciterId()

    fun setReciterId(reciterId: Int) = recitationsRepository.setVerseReciterId(reciterId)

    fun getRepeatMode() = recitationsRepository.getVerseRepeatMode()

    fun setRepeatMode(repeatMode: VerseRepeatMode) =
        recitationsRepository.setVerseRepeatMode(repeatMode)

    fun getShouldStopOnSuraEnd() = recitationsRepository.getShouldStopOnSuraEnd()

    fun setShouldStopOnSuraEnd(shouldStop: Boolean) =
        recitationsRepository.setShouldStopOnSuraEnd(shouldStop)

    fun getShouldStopOnPageEnd() = recitationsRepository.getShouldStopOnPageEnd()

    fun setShouldStopOnPageEnd(shouldStop: Boolean) =
        recitationsRepository.setShouldStopOnPageEnd(shouldStop)

}