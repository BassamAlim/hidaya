package bassamalim.hidaya.features.locator

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.nav.navResults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: LocatorDomain,
    private val navigator: Navigator
): ViewModel() {

    private val isInitialLocation = savedStateHandle.get<Boolean>("is_initial") == true

    private val _uiState = MutableStateFlow(LocatorUiState(
        isInitial = isInitialLocation
    ))
    val uiState = _uiState.asStateFlow()

    init {
        savedStateHandle.navResults().onEach { result ->
            domain.setManualLocation(result.getInt("city_id"))
            launch()
        }.launchIn(viewModelScope)
    }

    fun provide(
        locationRequestLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
        snackbarHostState: SnackbarHostState,
        snackbarMessage: String
    ) {
        domain.setLocationRequestLauncher(locationRequestLauncher)
        domain.setShowBackgroundLocationPermissionNeeded {
            viewModelScope.launch {
                snackbarHostState.showSnackbar(snackbarMessage)
            }
        }
        domain.setLaunch(::launch)
    }

    fun onLocateClick() {
        viewModelScope.launch {
            domain.locate()
        }
    }

    fun onSelectLocationClick() {
        navigator.navigate(Screen.LocationPicker)
    }

    fun onSkipLocationClick() {
        launch()
    }

    fun onLocationRequestResult(result: Map<String, Boolean>) {
        viewModelScope.launch {
            domain.handleLocationRequestResult(result)
        }
    }

    private fun launch() {
        if (isInitialLocation) {
            navigator.navigate(Screen.Main) {
                popUpTo(Screen.Locator(isInitial = "{is_initial}").route) {
                    inclusive = true
                }
            }
        }
        else navigator.popBackStack()
    }

}