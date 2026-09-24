package bassamalim.hidaya.features.prayers.board

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.data.repositories.NotificationsRepository
import bassamalim.hidaya.core.data.repositories.PrayerTimesReport
import bassamalim.hidaya.core.data.repositories.PrayerTimesReportRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.models.PrayerTimeCalculatorSettings
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.core.utils.PrayerTimeUtils
import bassamalim.hidaya.features.prayers.notificationSettings.PrayerNotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayersBoardDomain @Inject constructor(
    private val prayersRepository: PrayersRepository,
    private val locationRepository: LocationRepository,
    private val notificationsRepository: NotificationsRepository,
    private val appStateRepository: AppStateRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val prayerTimesReportRepository: PrayerTimesReportRepository
) {

    suspend fun submitReport(report: PrayerTimesReport) =
        prayerTimesReportRepository.submitReport(report)

    fun getMethodName(ordinal: Int) =
        prayerTimesReportRepository.getMethodName(ordinal)

    fun getLocation() = locationRepository.getLocation()

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    fun getHijriMonths() = appStateRepository.getHijriMonthNames()

    suspend fun getCountryName(countryId: Int, language: Language) =
        locationRepository.getCountryName(countryId = countryId, language = language)

    suspend fun getCityName(cityId: Int, language: Language) =
        locationRepository.getCityName(cityId = cityId, language = language)

    fun getPrayerNames() = prayersRepository.getPrayerNames()

    suspend fun getShouldShowTutorial() =
        prayersRepository.getShouldShowBoardTutorial().first()

    fun setTutorialSeen() {
        prayersRepository.setShouldShowBoardTutorial(false)
    }

    fun getPrayerSettings(): Flow<Map<Prayer, PrayerNotificationSettings>> {
        return combine(
            getNotificationTypes(),
            getReminderOffsets()
        ) { notificationTypes, reminderOffsets ->
            val prayers = listOf(
                Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHAA
            )
            prayers.associateWith { prayer ->
                PrayerNotificationSettings(
                    notificationType = notificationTypes[prayer.toReminder()]!!,
                    reminderOffset = reminderOffsets[prayer.toExtraReminder()]!!
                )
            }
        }
    }

    private fun getNotificationTypes() = notificationsRepository.getNotificationTypes()

    private fun getReminderOffsets() = notificationsRepository.getPrayerExtraReminderTimeOffsets()

    fun getPrayerTimesCalculatorSettings() =
        prayersRepository.getPrayerTimesCalculatorSettings()

    suspend fun getTimes(
        location: Location,
        date: Calendar,
        prayerTimesCalculatorSettings: PrayerTimeCalculatorSettings
    ): SortedMap<Prayer, String> {
        val prayerTimes = PrayerTimeUtils.getPrayerTimes(
            settings = prayerTimesCalculatorSettings,
            selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
            location = location,
            calendar = date
        )

        return PrayerTimeUtils.formatPrayerTimes(
            prayerTimes = prayerTimes,
            language = getLanguage(),
            timeFormat = appSettingsRepository.getTimeFormat().first(),
            numeralsLanguage = appSettingsRepository.getNumeralsLanguage().first(),
        )
    }

    /** The earliest of [candidates] still ahead today, or null once they've all passed. */
    suspend fun getNextPrayer(
        location: Location,
        prayerTimesCalculatorSettings: PrayerTimeCalculatorSettings,
        candidates: Set<Prayer>
    ): Prayer? {
        val now = System.currentTimeMillis()
        return PrayerTimeUtils.getPrayerTimes(
            settings = prayerTimesCalculatorSettings,
            selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
            location = location,
            calendar = Calendar.getInstance()
        ).mapNotNull { (prayer, time) ->
            // Times can be null for prayers that don't occur at high latitudes
            time?.timeInMillis?.takeIf { prayer in candidates && it > now }?.let { prayer to it }
        }.minByOrNull { it.second }?.first
    }

}
