package bassamalim.hidaya.features.locator

data class LocatorUiState(
    /** First launch: the user can decline, and there's nowhere to go back to */
    val isInitial: Boolean = true
)
