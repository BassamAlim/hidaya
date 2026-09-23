package bassamalim.hidaya.features.prayers.notificationSettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.NotificationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.nav.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrayerNotificationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: PrayerNotificationSettingsDomain,
    private val navigator: Navigator
): ViewModel() {

    private val prayer = Prayer.valueOf(savedStateHandle.get<String>("prayer_name") ?: "")

    private val _uiState = MutableStateFlow(PrayerNotificationSettingsUiState(
        prayer = prayer,
        prayerName = domain.getPrayerName(prayer)
    ))
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = PrayerNotificationSettingsUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                notificationType = domain.getNotificationType(prayer)
            )}
        }
    }

    /** A single choice, so picking an option applies it right away (no separate Save) */
    fun onNotificationTypeChange(notificationType: NotificationType) {
        _uiState.update { it.copy(
            notificationType = notificationType
        )}

        save()
    }

    private fun save() {
        viewModelScope.launch {
            domain.setNotificationType(_uiState.value.notificationType, prayer)
            domain.updateAlarm(_uiState.value.notificationType, prayer)

            navigator.popBackStack()
        }
    }

    fun onDismiss() {
        navigator.popBackStack()
    }

}