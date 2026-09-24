package bassamalim.hidaya.features.dateConverter

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateConverterDomain @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val appStateRepository: AppStateRepository
) {

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    fun getHijriMonths() = appStateRepository.getNumberedHijriMonthNames()

    fun getGregorianMonths() = appStateRepository.getNumberedGregorianMonthNames()

    fun gregorianToHijri(gregorian: Calendar): Calendar {
        val hijri = UmmalquraCalendar()
        hijri.time = gregorian.time
        return hijri
    }

    fun hijriToGregorian(hijri: Calendar): Calendar {
        val gregorian = Calendar.getInstance()
        gregorian.time = hijri.time
        return gregorian
    }

}