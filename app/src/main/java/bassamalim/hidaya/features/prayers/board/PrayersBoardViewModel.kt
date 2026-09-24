package bassamalim.hidaya.features.prayers.board

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.R
import bassamalim.hidaya.core.data.repositories.PrayerTimesReport
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.LocationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import bassamalim.hidaya.features.prayers.notificationSettings.PrayerNotificationSettings
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.SortedMap
import javax.inject.Inject

@HiltViewModel
class PrayersBoardViewModel @Inject constructor(
    private val app: Application,
    private val domain: PrayersBoardDomain,
    private val navigator: Navigator
): ViewModel() {

    private lateinit var language: Language
    private lateinit var numeralsLanguage: Language
    private val location = domain.getLocation()
    private val prayerSettings = domain.getPrayerSettings()
    private val prayerTimesCalculatorSettings = domain.getPrayerTimesCalculatorSettings()
    private val currentDate = Calendar.getInstance()
    private val viewedDate = Calendar.getInstance()
    private val prayerNames = domain.getPrayerNames()
    // Re-evaluates which prayer is next while the board stays open. Without a location there's
    // nothing to highlight, so no timer runs (this also keeps virtual-time tests finite)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val minuteTicks = location.flatMapLatest { location ->
        if (location == null) flowOf(Unit)
        else flow {
            while (true) {
                emit(Unit)
                delay(60_000)
            }
        }
    }

    private val _uiState = MutableStateFlow(PrayersBoardUiState())
    val uiState = combine(
        _uiState.asStateFlow(),
        location,
        prayerSettings,
        prayerTimesCalculatorSettings,
        minuteTicks
    ) { state, location, prayerSettings, prayerTimesCalculatorSettings, _ ->
        if (location != null) {
            val prayerTimeMap = domain.getTimes(
                location = location,
                date = viewedDate,
                prayerTimesCalculatorSettings = prayerTimesCalculatorSettings
            )
            val nextPrayer =
                if (isSameDay(viewedDate, currentDate)) domain.getNextPrayer(
                    location = location,
                    prayerTimesCalculatorSettings = prayerTimesCalculatorSettings,
                    candidates = prayerNames.keys
                )
                else null
            state.copy(
                locationAvailable = true,
                prayersData = getPrayersData(
                    prayerTimeMap = prayerTimeMap,
                    prayerSettings = prayerSettings,
                    isToday = isSameDay(viewedDate, currentDate),
                    nextPrayer = nextPrayer
                ),
                locationName = getLocationName(location)
            )
        }
        else state.copy(
            locationAvailable = false,
            locationName = ""
        )
    }.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = PrayersBoardUiState()
    )

    private suspend fun initializeData() {
        language = domain.getLanguage()
        numeralsLanguage = domain.getNumeralsLanguage()

        _uiState.update { it.copy(
            loading = false,
            dateText = getDateText(viewedDate),
            isTutorialActive = domain.getShouldShowTutorial()
        )}
    }

    fun onTutorialFinished() {
        _uiState.update { it.copy(isTutorialActive = false) }
        domain.setTutorialSeen()
    }

    fun onLocatorClick() {
        navigator.navigate(Screen.Locator(isInitial = false.toString()))
    }

    fun onTimeCalculationSettingsClick() {
        navigator.navigate(Screen.PrayerTimeCalculationSettings)
    }

    fun onPrayerCardClick(prayer: Prayer) {
        navigator.navigate(Screen.PrayerSettings(prayerName = prayer.name))
    }

    fun onExtraReminderCardClick(prayer: Prayer) {
        navigator.navigate(Screen.PrayerExtraReminderSettings(prayerName = prayer.name))
    }

    fun onPreviousDayClick() {
        val newDate = (viewedDate.clone() as Calendar).apply { add(Calendar.DATE, -1) }
        updateDate(newDate)
    }

    fun onDateClick() {
        val newDate = (viewedDate.clone() as Calendar).apply { time = currentDate.time }
        updateDate(newDate)
    }

    fun onNextDayClick() {
        val newDate = (viewedDate.clone() as Calendar).apply { add(Calendar.DATE, 1) }
        updateDate(newDate)
    }

    fun onReportHelpClick() {
        viewModelScope.launch {
            val location = location.first()
            val settings = prayerTimesCalculatorSettings.first()
            val methodName = domain.getMethodName(settings.calculationMethod.ordinal)
            val computed = location?.let {
                domain.getTimes(
                    location = it,
                    date = currentDate,
                    prayerTimesCalculatorSettings = settings
                )
            } ?: sortedMapOf()

            _uiState.update { it.copy(
                report = ReportUiState(
                    dialogShown = true,
                    step = ReportStep.CHECKS,
                    currentMethodName = methodName,
                    isAutoLocation = location?.type == LocationType.AUTO,
                    prayerNames = prayerNames,
                    computedTimes = computed,
                    wrongPrayers = emptySet(),
                    correctTimes = emptyMap(),
                    notes = "",
                    submitting = false,
                    submitted = false,
                    error = null
                )
            )}
        }
    }

    fun onReportDismiss() {
        _uiState.update { it.copy(report = it.report.copy(dialogShown = false)) }
    }

    fun onReportNext() {
        _uiState.update {
            val next = when (it.report.step) {
                ReportStep.CHECKS -> ReportStep.FORM
                else -> it.report.step
            }
            it.copy(report = it.report.copy(step = next))
        }
    }

    fun onReportBack() {
        _uiState.update {
            val prev = when (it.report.step) {
                ReportStep.FORM -> ReportStep.CHECKS
                else -> it.report.step
            }
            it.copy(report = it.report.copy(step = prev))
        }
    }

    fun onReportTogglePrayer(prayer: Prayer) {
        _uiState.update {
            val newSet =
                if (prayer in it.report.wrongPrayers) it.report.wrongPrayers - prayer
                else it.report.wrongPrayers + prayer
            it.copy(report = it.report.copy(wrongPrayers = newSet))
        }
    }

    fun onCorrectTimePickerOpen(prayer: Prayer) {
        _uiState.update { it.copy(report = it.report.copy(timePickerTarget = prayer)) }
    }

    fun onCorrectTimePickerDismiss() {
        _uiState.update { it.copy(report = it.report.copy(timePickerTarget = null)) }
    }

    fun onCorrectTimePickerConfirm(hour: Int, minute: Int) {
        _uiState.update {
            val prayer = it.report.timePickerTarget ?: return@update it
            val value = "%02d:%02d".format(hour, minute)
            it.copy(
                report = it.report.copy(
                    correctTimes = it.report.correctTimes + (prayer to value),
                    timePickerTarget = null
                )
            )
        }
    }

    fun onReportNotesChange(value: String) {
        _uiState.update { it.copy(report = it.report.copy(notes = value)) }
    }

    fun onReportSubmit() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                report = it.report.copy(
                    step = ReportStep.RESULT,
                    submitting = true,
                    error = null
                )
            )}

            val location = location.first()
            val settings = prayerTimesCalculatorSettings.first()
            val state = _uiState.value

            val report = PrayerTimesReport(
                language = language,
                location = location,
                locationName = state.locationName,
                calculatorSettings = settings,
                computedTimes = state.report.computedTimes,
                wrongPrayers = state.report.wrongPrayers,
                correctTimes = state.report.correctTimes,
                notes = state.report.notes
            )

            val success = domain.submitReport(report)

            _uiState.update { it.copy(
                report = it.report.copy(
                    submitting = false,
                    submitted = success,
                    error =
                        if (success) null
                        else app.getString(R.string.report_submit_failed)
                )
            )}
        }
    }

    private fun updateDate(newDate: Calendar) {
        // Set first: the state update below makes uiState recompute the times for viewedDate
        viewedDate.time = newDate.time

        _uiState.update { it.copy(
            dateText = getDateText(newDate),
            noDateOffset = isSameDay(newDate, currentDate)
        )}
    }

    private fun isSameDay(a: Calendar, b: Calendar) =
        a[Calendar.YEAR] == b[Calendar.YEAR] && a[Calendar.DAY_OF_YEAR] == b[Calendar.DAY_OF_YEAR]

    private fun getPrayersData(
        prayerTimeMap: SortedMap<Prayer, String>,
        prayerSettings: Map<Prayer, PrayerNotificationSettings>,
        isToday: Boolean,
        nextPrayer: Prayer?
    ) = sortedMapOf<Prayer, PrayerCardData>().apply {
        prayerNames.forEach { (prayer, name) ->
            val settings = prayerSettings[prayer] ?: return@forEach
            this[prayer] = PrayerCardData(
                name = name,
                time = prayerTimeMap[prayer] ?: "",
                status = when {
                    !isToday -> PrayerCardData.Status.UPCOMING
                    prayer == nextPrayer -> PrayerCardData.Status.NEXT
                    // With no next prayer left, everything today has passed
                    nextPrayer == null || prayer < nextPrayer -> PrayerCardData.Status.PASSED
                    else -> PrayerCardData.Status.UPCOMING
                },
                notificationType = settings.notificationType,
                isExtraReminderOffsetSpecified = settings.reminderOffset != 0,
                extraReminderOffset = formatOffset(settings.reminderOffset)
            )
        }
    }

    private suspend fun getLocationName(location: Location): String {
        val countryName = domain.getCountryName(
            countryId = location.ids.countryId,
            language = language
        )
        val cityName = domain.getCityName(cityId = location.ids.cityId, language = language)
        return "$countryName, $cityName"
    }

    private fun getDateText(newDate: Calendar): String {
        val hijri = UmmalquraCalendar()
        hijri.time = newDate.time

        val year = translateNums(
            numeralsLanguage = numeralsLanguage,
            string = hijri[Calendar.YEAR].toString()
        )
        val month = domain.getHijriMonths()[hijri[Calendar.MONTH]]
        val day = translateNums(
            numeralsLanguage = numeralsLanguage,
            string = hijri[Calendar.DATE].toString()
        )

        return "$day $month $year"
    }

    private fun formatOffset(offset : Int): String {
        return if (offset < 0) translateNums(
            string = offset.toString(),
            numeralsLanguage = numeralsLanguage
        )
        else translateNums(string = "+$offset", numeralsLanguage = numeralsLanguage)
    }

}
