package bassamalim.hidaya.features.main

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainDomain @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    fun getDateOffset() = appSettingsRepository.getDateOffset()

    fun getHijriDate(dateOffset: Int): HijrahDate =
        HijrahDate.now().plus(dateOffset.toLong(), ChronoUnit.DAYS)

    fun getGregorianDate(): LocalDate = LocalDate.now()

    fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage()

    fun getWeekDays() = appStateRepository.getWeekDayNames()

    fun getHijriMonths() = appStateRepository.getHijriMonthNames()

    fun getGregorianMonths() = appStateRepository.getGregorianMonthNames()

}