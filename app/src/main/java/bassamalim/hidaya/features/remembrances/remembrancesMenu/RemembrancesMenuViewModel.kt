package bassamalim.hidaya.features.remembrances.remembrancesMenu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.MenuType
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.navigation.toRoute

@HiltViewModel
class RemembrancesMenuViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: RemembrancesMenuDomain,
    private val navigator: Navigator
): ViewModel() {

    private lateinit var language: Language

    private val menuType = savedStateHandle.toRoute<Screen.RemembrancesMenu>().type
    private val categoryId = savedStateHandle.toRoute<Screen.RemembrancesMenu>().categoryId

    private val _uiState = MutableStateFlow(RemembrancesMenuUiState(
        menuType = menuType
    ))
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getRemembrances(menuType, categoryId)
    ) { state, remembrances ->
        if (state.isLoading) return@combine state
        state.copy(remembrances = getFilteredRemembrances(remembrances))
    }.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = RemembrancesMenuUiState()
    )

    fun initializeData() {
        viewModelScope.launch {
            language = domain.getLanguage()

            _uiState.update { it.copy(
                isLoading = false,
                categoryTitle = if (menuType == MenuType.CUSTOM)
                    domain.getCategoryTitle(categoryId, language)
                else ""
            )}
        }
    }

    private suspend fun getFilteredRemembrances(
        remembrances: List<RemembrancesItem>
    ): List<RemembrancesItem> {
        val items = remembrances.filter { remembrance ->
            !(language == Language.ENGLISH && !hasEn(remembrance.id))
        }
        return domain.getSearchResults(
            query = _uiState.value.searchText,
            items = items
        )
    }

    private suspend fun hasEn(remembranceId: Int): Boolean {
        val remembrancePassages = domain.getRemembrancePassages(remembranceId)
        return remembrancePassages.any { it.textEn != null && it.textEn.length > 1 }
    }

    fun onFavoriteCLick(item: RemembrancesItem) {
        viewModelScope.launch {
            domain.setFavoriteStatus(item.id, !item.isFavorite)
        }
    }

    fun onItemClick(item: RemembrancesItem) {
        navigator.navigate(Screen.RemembranceReader(id = item.id))

        domain.trackRemembranceViewed(remembranceId = item.id, remembranceName = item.name)
    }

    fun onSearchTextChange(text: String) {
        _uiState.update { it.copy(
            searchText = text
        )}
    }

}