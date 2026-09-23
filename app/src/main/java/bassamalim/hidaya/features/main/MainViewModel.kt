package bassamalim.hidaya.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val domain: MainDomain,
    private val navigator: Navigator
): ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getDateOffset(),
        domain.getNumeralsLanguage()
    ) { state, dateOffset, numeralsLanguage -> state.copy(
        hijriDate = getHijriDate(dateOffset, numeralsLanguage),
        gregorianDate = getGregorianDate(numeralsLanguage)
    )}.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = MainUiState()
    )

    fun onDateClick() {
        navigator.navigate(Screen.DateEditor)
    }

    private fun getHijriDate(dateOffset: Int, numeralsLanguage: Language): String {
        val hijri = domain.getHijriDateCalendar(dateOffset)
        val hijriNoOffset = domain.getHijriDateCalendar(dateOffset = 0)

        val hDayName = domain.getWeekDays()[hijriNoOffset[Calendar.DAY_OF_WEEK] - 1]
        val hMonth = domain.getHijriMonths()[hijri[Calendar.MONTH]]
        val hijriStr = "$hDayName ${hijri[Calendar.DATE]} $hMonth ${hijri[Calendar.YEAR]}"
        return translateNums(numeralsLanguage = numeralsLanguage, string = hijriStr)
    }

    private fun getGregorianDate(numeralsLanguage: Language): String {
        val gregorian = domain.getGregorianDateCalendar()

        val mMonth = domain.getGregorianMonths()[gregorian[Calendar.MONTH]]
        val gregorianStr = "${gregorian[Calendar.DATE]} $mMonth ${gregorian[Calendar.YEAR]}"
        return translateNums(
            numeralsLanguage = numeralsLanguage,
            string = gregorianStr
        )
    }

}