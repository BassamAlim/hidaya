package bassamalim.hidaya.features.hijriDatePicker

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.first
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HijriDatePickerDomain @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val appStateRepository: AppStateRepository
) {

    private var selectedDate = HijrahDate.now()
    private val currentDate = HijrahDate.now()
    val minYear = currentDate.get(ChronoField.YEAR) - 100
    val maxYear = currentDate.get(ChronoField.YEAR) + 100

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    fun getMonthNames() = appStateRepository.getHijriMonthNames()

    fun getWeekDays() = appStateRepository.getWeekDayNames()

    fun getWeekDaysAbb(language: Language) = appStateRepository.getWeekDaysAbbreviations(language)

    fun getSelectedDate() = selectedDate

    fun setSelectedDate(year: Int, month: Int, day: Int) {
        selectedDate = HijrahDate.of(year, month, day)
    }

    fun getCurrentDate() = currentDate

}