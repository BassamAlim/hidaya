package bassamalim.hidaya.features.locationPicker

import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.helpers.Searcher
import bassamalim.hidaya.core.utils.LangUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationPickerDomain @Inject constructor(
    private val locationRepository: LocationRepository
) {

    private val searcher = Searcher<LocationPickerItem>()
    private var countryId = -1

    fun setCountryId(id: Int) { countryId = id }

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getCountries(language: Language) =
        locationRepository.getCountries(language = language)

    suspend fun getCities(language: Language) =
        locationRepository.getCities(
            countryId = countryId,
            language = language
        )

    fun getSearchResults(query: String, items: List<LocationPickerItem>): List<LocationPickerItem> {
        return searcher.containsSearch(
            items = items,
            query = query,
            keySelector = { it.name }
        )
    }

}