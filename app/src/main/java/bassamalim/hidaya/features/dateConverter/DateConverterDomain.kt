package bassamalim.hidaya.features.dateConverter

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.chrono.HijrahDate
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

    // Supported from 1882 to 2174; the Gregorian date picker is limited to 1900-2100
    fun gregorianToHijri(gregorian: LocalDate): HijrahDate = HijrahDate.from(gregorian)

    fun hijriToGregorian(hijri: HijrahDate): LocalDate = LocalDate.from(hijri)

}