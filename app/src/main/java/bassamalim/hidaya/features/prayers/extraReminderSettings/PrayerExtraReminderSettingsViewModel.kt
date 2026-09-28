package bassamalim.hidaya.features.prayers.extraReminderSettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.utils.LangUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.navigation.toRoute
import bassamalim.hidaya.core.nav.Screen

@HiltViewModel
class PrayerExtraReminderSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: PrayerExtraReminderSettingsDomain,
    private val navigator: Navigator
): ViewModel() {

    private val prayer = savedStateHandle.toRoute<Screen.PrayerExtraReminderSettings>().prayer

    val offsetMin = domain.offsetMin
    private lateinit var numeralsLanguage: Language

    private val _uiState = MutableStateFlow(PrayerExtraReminderSettingsUiState(
        prayer = prayer,
        prayerName = domain.getPrayerName(prayer)
    ))
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = PrayerExtraReminderSettingsUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            numeralsLanguage = domain.getNumeralsLanguage()

            _uiState.update { it.copy(
                isLoading = false,
                offset = domain.getOffset(prayer)
            )}
        }
    }

    fun onOffsetChange(offset: Int) {
        _uiState.update { it.copy(
            offset = offset
        )}
    }

    fun onSave() {
        viewModelScope.launch {
            domain.setOffset(prayer = prayer, offset = uiState.value.offset)

            navigator.popBackStack()

            domain.updatePrayerTimeAlarms(prayer)
        }
    }

    fun onDismiss() {
        navigator.popBackStack()
    }

    fun formatSliderValue(value: String): String {
        return LangUtils.translateNums(
            string = value,
            numeralsLanguage = numeralsLanguage
        )
    }

}