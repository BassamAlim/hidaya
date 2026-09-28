package bassamalim.hidaya.features.verseGuess

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class VerseGuessDomain @Inject constructor(
    private val quranRepository: QuranRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    suspend fun getSuraNames(language: Language) = quranRepository.getDecoratedSuraNames(language)

    suspend fun getAllVerses() = quranRepository.getAllVerses().sortedBy { it.id }

}
