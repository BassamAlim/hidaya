package bassamalim.hidaya.features.settings

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.data.repositories.NotificationsRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.enums.Reminder
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.enums.TimeFormat
import bassamalim.hidaya.core.helpers.Alarm
import bassamalim.hidaya.core.models.TimeOfDay
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.core.utils.PrayerTimeUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDomain @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val prayersRepository: PrayersRepository,
    private val notificationsRepository: NotificationsRepository,
    private val locationRepository: LocationRepository,
    private val alarm: Alarm
) {

    fun getLanguage() = LangUtils.getAppLanguage()

    fun setLanguage(language: Language) {
        val appLocale = LangUtils.languageToLocaleList(language)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage()

    fun setNumeralsLanguage(numeralsLanguage: Language) {
        appSettingsRepository.setNumeralsLanguage(numeralsLanguage)
    }

    fun getTimeFormat() = appSettingsRepository.getTimeFormat()

    fun setTimeFormat(timeFormat: TimeFormat) {
        appSettingsRepository.setTimeFormat(timeFormat)
    }

    fun getTheme() = appSettingsRepository.getTheme()

    fun setTheme(theme: Theme) {
        appSettingsRepository.setTheme(theme)
    }

    fun getDevotionReminderEnabledMap() =
        notificationsRepository.getDevotionalReminderEnabledMap()

    fun getDevotionReminderTimeOfDayMap() =
        notificationsRepository.getDevotionalReminderTimes()

    fun getAthanAudioId() = prayersRepository.getAthanAudioId()

    fun setAthanAudioId(athanAudioId: Int) {
        prayersRepository.setAthanAudioId(athanAudioId)
    }

    fun recreateActivity(activity: Activity) {
        activity.recreate()
    }

    fun getLocation() = locationRepository.getLocation()

    suspend fun getPrayerTime(prayer: Prayer): Calendar? {
        val location = locationRepository.getLocation().first() ?: return null

        var prayerTime = PrayerTimeUtils.getPrayerTimes(
            settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
            selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
            location = location,
            calendar = Calendar.getInstance()
        )[prayer] ?: return null

        // if prayer time passed
        if (prayerTime.timeInMillis < System.currentTimeMillis()) {
            prayerTime = PrayerTimeUtils.getPrayerTimes(
                settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
                selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
                location = location,
                calendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            )[prayer] ?: return null
        }

        return prayerTime
    }

    suspend fun setDevotionalReminder(reminder: Reminder.Devotional, hour: Int, minute: Int) {
        val adjustedTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        setDevotionReminderEnabled(reminder, true)
        setDevotionReminderTimeOfDay(
            reminder = reminder,
            timeOfDay = TimeOfDay(
                hour = adjustedTime.get(Calendar.HOUR_OF_DAY),
                minute = adjustedTime.get(Calendar.MINUTE)
            )
        )

        setAlarm(reminder)
    }

    fun cancelDevotionalReminder(reminder: Reminder.Devotional) {
        setDevotionReminderEnabled(reminder, false)
        cancelAlarm(reminder)
    }

    private fun setDevotionReminderEnabled(reminder: Reminder.Devotional, enabled: Boolean) {
        notificationsRepository.setDevotionalReminderEnabled(enabled, reminder)
    }

    private fun setDevotionReminderTimeOfDay(reminder: Reminder.Devotional, timeOfDay: TimeOfDay) {
        notificationsRepository.setDevotionalReminderTimes(timeOfDay, reminder)
    }

    private suspend fun setAlarm(reminder: Reminder) {
        alarm.setAlarm(reminder)
    }

    private fun cancelAlarm(reminder: Reminder) {
        alarm.cancelAlarm(reminder)
    }

}