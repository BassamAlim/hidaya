package bassamalim.hidaya.features.dateConverter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import javax.inject.Inject

@HiltViewModel
class DateConverterViewModel @Inject constructor(
    private val domain: DateConverterDomain,
    private val navigator: Navigator
): ViewModel() {

    private var hijriDate = HijrahDate.now()
    private var gregorianDate = LocalDate.now()
    private val hijriMonth = domain.getHijriMonths()
    private val gregorianMonths = domain.getGregorianMonths()
    private var numeralsLanguage: Language? = null

    private val _uiState = MutableStateFlow(DateConverterUiState())
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = DateConverterUiState()
    )

    init {
        navigator.results("selected_date").onEach { result ->
            val date = HijrahChronology.INSTANCE.dateEpochDay(result.getLong("selected_date"))

            hijriDate = date
            gregorianDate = domain.hijriToGregorian(date)

            updateDates()
        }.launchIn(viewModelScope)
    }

    private fun initializeData() {
        viewModelScope.launch {
            numeralsLanguage = domain.getNumeralsLanguage()

            _uiState.update { it.copy(
                gregorianDatePickerMillis = toDatePickerMillis(gregorianDate)
            )}
        }
    }

    fun onPickGregorianClick() {
        _uiState.update { it.copy(
            isGregorianDatePickerShown = true
        )}
    }

    fun onGregorianDatePicked(millis: Long?) {
        if (millis == null) return

        // The date picker works in UTC; reading it in the local zone gave the previous day west
        // of UTC
        val pickedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

        gregorianDate = pickedDate

        hijriDate = domain.gregorianToHijri(pickedDate)

        updateDates()

        _uiState.update { it.copy(
            gregorianDatePickerMillis = toDatePickerMillis(pickedDate),
            isGregorianDatePickerShown = false
        )}
    }

    fun onGregorianDatePickerDismiss() {
        _uiState.update { it.copy(
            isGregorianDatePickerShown = false
        )}
    }

    fun onPickHijriClick() {
        val dateStr = "${hijriDate.get(ChronoField.YEAR)}" +
                "-${hijriDate.get(ChronoField.MONTH_OF_YEAR)}" +
                "-${hijriDate.get(ChronoField.DAY_OF_MONTH)}"

        navigator.navigate(Screen.HijriDatePicker(initialDate = dateStr))
    }

    private fun updateDates() {
        val numeralsLanguage = numeralsLanguage ?: return

        _uiState.update { it.copy(
            hijriDate = Date(
                year = translateNums(
                    numeralsLanguage = numeralsLanguage,
                    string = hijriDate.get(ChronoField.YEAR).toString()
                ),
                month = hijriMonth[hijriDate.get(ChronoField.MONTH_OF_YEAR) - 1],
                day = translateNums(
                    numeralsLanguage = numeralsLanguage,
                    string = hijriDate.get(ChronoField.DAY_OF_MONTH).toString()
                )
            ),
            gregorianDate = Date(
                year = translateNums(
                    numeralsLanguage = numeralsLanguage,
                    string = gregorianDate.year.toString()
                ),
                month = gregorianMonths[gregorianDate.monthValue - 1],
                day = translateNums(
                    numeralsLanguage = numeralsLanguage,
                    string = gregorianDate.dayOfMonth.toString()
                )
            )
        )}
    }

    private fun toDatePickerMillis(date: LocalDate) =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

}