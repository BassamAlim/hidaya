package bassamalim.hidaya.features.remembrances.remembrancesMenu

import bassamalim.hidaya.core.data.repositories.AnalyticsRepository
import bassamalim.hidaya.core.data.repositories.RemembrancesRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.MenuType
import bassamalim.hidaya.core.helpers.Searcher
import bassamalim.hidaya.core.models.AnalyticsEvent
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemembrancesMenuDomain @Inject constructor(
    private val remembrancesRepository: RemembrancesRepository,
    private val analyticsRepository: AnalyticsRepository
) {

    private val searcher = Searcher<RemembrancesItem>()

    fun getLanguage() = LangUtils.getAppLanguage()

    fun getRemembrances(menuType: MenuType, categoryId: Int): Flow<List<RemembrancesItem>> {
        val remembrancesFlow = when (menuType) {
            MenuType.ALL, MenuType.DOWNLOADED -> remembrancesRepository.observeAllRemembrances()
            MenuType.FAVORITES -> remembrancesRepository.observeFavorites()
            MenuType.CUSTOM -> remembrancesRepository.observeCategoryRemembrances(categoryId)
        }

        val language = getLanguage()
        return remembrancesFlow.map { remembrances ->
            remembrances.map { remembrance ->
                RemembrancesItem(
                    id = remembrance.id,
                    categoryId = remembrance.categoryId,
                    name = when (language) {
                        Language.ARABIC -> remembrance.nameAr!!
                        Language.ENGLISH -> remembrance.nameEn!!
                    },
                    isFavorite = remembrance.isFavorite == 1
                )
            }
        }
    }

    suspend fun getRemembrancePassages(remembrancesId: Int) =
        remembrancesRepository.getRemembrancePassages(remembrancesId)

    suspend fun getCategoryTitle(categoryId: Int, language: Language) =
        remembrancesRepository.getRemembranceCategoryName(categoryId, language)

    fun setFavoriteStatus(id: Int, value: Boolean) {
        remembrancesRepository.setFavorite(id, value)
    }

    fun getSearchResults(query: String, items: List<RemembrancesItem>) =
        searcher.containsSearch(
            items = items,
            query = query,
            keySelector = { remembrance -> remembrance.name }
        )

    fun trackRemembranceViewed(remembranceId: Int, remembranceName: String) {
        analyticsRepository.trackEvent(
            AnalyticsEvent.RemembranceViewed(
                remembranceId = remembranceId,
                remembranceName = remembranceName
            )
        )
    }

}