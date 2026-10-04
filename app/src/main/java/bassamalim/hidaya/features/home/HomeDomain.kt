package bassamalim.hidaya.features.home

import android.app.Application
import bassamalim.hidaya.core.data.repositories.AnalyticsRepository
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.data.repositories.QuizRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.models.AnalyticsEvent
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.core.utils.OsUtils
import bassamalim.hidaya.core.utils.PrayerTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeDomain @Inject constructor(
    app: Application,
    private val prayersRepository: PrayersRepository,
    private val locationRepository: LocationRepository,
    private val quranRepository: QuranRepository,
    private val quizRepository: QuizRepository,
    private val userRepository: UserRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val analyticsRepository: AnalyticsRepository
) {

    private val deviceId = OsUtils.getDeviceId(app)

    suspend fun getPrayerTimeMap(location: Location) =
        PrayerTimeUtils.getPrayerTimes(
            settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
            selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
            location = location,
            date = LocalDate.now()
        )

    suspend fun getStrPrayerTimeMap(location: Location) =
        PrayerTimeUtils.formatPrayerTimes(
            prayerTimes = getPrayerTimeMap(location),
            timeFormat = appSettingsRepository.getTimeFormat().first(),
            language = getLanguage(),
            numeralsLanguage = getNumeralsLanguage().first()
        )

    suspend fun getYesterdayIshaa(location: Location) =
        PrayerTimeUtils.getPrayerTimes(
            settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
            selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
            location = location,
            date = LocalDate.now().minusDays(1)
        )[Prayer.ISHAA]

    suspend fun getStrYesterdayIshaa(location: Location) =
        PrayerTimeUtils.formatPrayerTime(
            time = getYesterdayIshaa(location),
            language = getLanguage(),
            numeralsLanguage = appSettingsRepository.getNumeralsLanguage().first(),
            timeFormat = appSettingsRepository.getTimeFormat().first()
        )

    suspend fun getTomorrowFajr(location: Location) =
        PrayerTimeUtils.getPrayerTimes(
            settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
            selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
            location = location,
            date = LocalDate.now().plusDays(1)
        )[Prayer.FAJR]

    suspend fun getStrTomorrowFajr(location: Location) =
        PrayerTimeUtils.formatPrayerTime(
            time = getTomorrowFajr(location),
            language = getLanguage(),
            numeralsLanguage = appSettingsRepository.getNumeralsLanguage().first(),
            timeFormat = appSettingsRepository.getTimeFormat().first()
        )

    // Times can be null for prayers that don't occur at high latitudes, so they're skipped.
    fun getPreviousPrayer(times: Map<Prayer, ZonedDateTime?>): Prayer? {
        val currentMillis = System.currentTimeMillis()
        for ((prayer, time) in times.entries.reversed()) {
            if (time != null && time.toInstant().toEpochMilli() < currentMillis) return prayer
        }
        return null
    }

    fun getNextPrayer(times: Map<Prayer, ZonedDateTime?>): Prayer? {
        val currentMillis = System.currentTimeMillis()
        for ((prayer, time) in times.entries) {
            if (time != null && time.toInstant().toEpochMilli() > currentMillis) return prayer
        }
        return null
    }

    fun getLanguage() = LangUtils.getAppLanguage()

    fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage()

    fun getWerdPage() = quranRepository.getWerdPageNum()

    fun isWerdDone() = quranRepository.isWerdDone()

    fun getLocalRecord() = userRepository.getLocalRecord()

    fun getQuizQuestionCount() = flow { emit(quizRepository.getQuestionCount()) }

    fun getLocation() = locationRepository.getLocation()

    fun getPrayerNames() = prayersRepository.getPrayerNames()

    suspend fun syncRecords() = userRepository.syncRecords(deviceId)

    fun trackDailyWerdViewed() {
        analyticsRepository.trackEvent(AnalyticsEvent.DailyWerdViewed)
    }

}