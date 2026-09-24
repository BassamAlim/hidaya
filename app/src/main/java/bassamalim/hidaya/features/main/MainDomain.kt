package bassamalim.hidaya.features.main

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainDomain @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    private val millisInDay = 1000 * 60 * 60 * 24

    fun getDateOffset() = appSettingsRepository.getDateOffset()

    fun getHijriDateCalendar(dateOffset: Int) =
        UmmalquraCalendar().apply {
            timeInMillis += dateOffset * millisInDay
        }

    fun getGregorianDateCalendar(): Calendar = Calendar.getInstance()

    fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage()

    fun getWeekDays() = appStateRepository.getWeekDayNames()

    fun getHijriMonths() = appStateRepository.getHijriMonthNames()

    fun getGregorianMonths() = appStateRepository.getGregorianMonthNames()

}