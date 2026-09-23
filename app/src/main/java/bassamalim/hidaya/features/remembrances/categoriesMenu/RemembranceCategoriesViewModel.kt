package bassamalim.hidaya.features.remembrances.categoriesMenu

import androidx.lifecycle.ViewModel
import bassamalim.hidaya.core.enums.MenuType
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RemembranceCategoriesViewModel @Inject constructor(
    private val navigator: Navigator
): ViewModel() {

    fun onAllRemembrancesClick() {
        navigator.navigate(
            Screen.RemembrancesMenu(MenuType.ALL.name)
        )
    }

    fun onFavoriteRemembrancesClick() {
        navigator.navigate(
            Screen.RemembrancesMenu(MenuType.FAVORITES.name)
        )
    }

    /** Opens a remembrance directly; 0 and 1 are morning and evening (same ids the reminders use) */
    fun onRemembranceClick(remembranceId: Int) {
        navigator.navigate(Screen.RemembranceReader(remembranceId.toString()))
    }

    fun onCategoryClick(categoryId: Int) {
        navigator.navigate(
            Screen.RemembrancesMenu(
                type = MenuType.CUSTOM.name,
                categoryId = categoryId.toString()
            )
        )
    }

}