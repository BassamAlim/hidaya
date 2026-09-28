package bassamalim.hidaya.features.onboarding

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.HighLatitudesAdjustmentMethod
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.PrayerTimeCalculationMethod
import bassamalim.hidaya.core.enums.PrayerTimeJuristicMethod
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.enums.TimeFormat
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val domain: OnboardingDomain,
    private val navigator: Navigator
): ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            language = LangUtils.getAppLanguage()
        )
    )
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getNumeralsLanguage(),
        domain.getTimeFormat(),
        domain.getTheme(),
        domain.getPrayerTimesCalculatorSettings()
    ) { state, numeralsLanguage, timeFormat, theme, prayerSettings ->
        state.copy(
            numeralsLanguage = numeralsLanguage,
            timeFormat = timeFormat,
            theme = theme,
            calculationMethod = prayerSettings.calculationMethod,
            juristicMethod = prayerSettings.juristicMethod,
            highLatitudesAdjustment = prayerSettings.highLatitudesAdjustmentMethod
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = OnboardingUiState()
    )

    fun onLanguageChange(language: Language, activity: Activity) {
        viewModelScope.launch {
            domain.setLanguage(language)
            _uiState.update { it.copy(
                language = language
            )}
            domain.recreateActivity(activity)
        }
    }

    fun onNumeralsLanguageChange(language: Language) {
        viewModelScope.launch {
            domain.setNumeralsLanguage(language)
        }
    }

    fun onTimeFormatChange(timeFormat: TimeFormat) {
        viewModelScope.launch {
            domain.setTimeFormat(timeFormat)
        }
    }

    fun onThemeChange(theme: Theme) {
        viewModelScope.launch {
            domain.setTheme(theme)
        }
    }

    fun onCalculationMethodChange(method: PrayerTimeCalculationMethod) {
        viewModelScope.launch {
            domain.setCalculationMethod(method)
        }
    }

    fun onJuristicMethodChange(method: PrayerTimeJuristicMethod) {
        viewModelScope.launch {
            domain.setJuristicMethod(method)
        }
    }

    fun onHighLatitudesAdjustmentChange(method: HighLatitudesAdjustmentMethod) {
        viewModelScope.launch {
            domain.setHighLatitudesAdjustment(method)
        }
    }

    fun onSaveClick() {
        navigator.navigate(Screen.Locator(isInitial = true)) {
            popUpTo<Screen.Onboarding> {
                inclusive = true
            }
        }

        viewModelScope.launch {
            domain.unsetFirstTime()
        }
    }

}