package bassamalim.hidaya.features.recitations.recitersMenuFilter

import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecitersMenuFilterDomain @Inject constructor(
    private val recitationsRepository: RecitationsRepository
) {

    suspend fun getOptions(language: Language) =
        recitationsRepository.getNarrationSelections(language).first()

    suspend fun setOptions(options: Map<String, Boolean>) {
        recitationsRepository.setNarrationSelections(options)
    }

    fun getLanguage() = LangUtils.getAppLanguage()

}