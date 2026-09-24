package bassamalim.hidaya.features.remembrances.reader

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.RemembrancesRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemembranceReaderDomain @Inject constructor(
    private val remembrancesRepository: RemembrancesRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    suspend fun getTitle(id: Int, language: Language) =
        remembrancesRepository.getRemembranceName(id, language)

    suspend fun getRemembrancePassages(id: Int) = remembrancesRepository.getRemembrancePassages(id)

    fun getTextSize() = remembrancesRepository.getTextSize()

    suspend fun setTextSize(textSize: Float) {
        remembrancesRepository.setTextSize(textSize)
    }

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

}