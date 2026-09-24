package bassamalim.hidaya.features.prayers.notificationSettings

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.NotificationsRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.enums.NotificationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.helpers.Alarm
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerNotificationSettingsDomain @Inject constructor(
    private val prayersRepository: PrayersRepository,
    private val notificationsRepository: NotificationsRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val alarm: Alarm
) {

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    suspend fun getNotificationType(prayer: Prayer) =
        notificationsRepository.getNotificationType(prayer.toReminder()).first()

    fun setNotificationType(type: NotificationType, prayer: Prayer) =
        notificationsRepository.setNotificationType(type, prayer.toReminder())

    /**
     * Alarms are only scheduled for prayers whose notification is on, so turning one back on has
     * to schedule today's alarm; otherwise nothing fires until the next daily update at midnight.
     */
    suspend fun updateAlarm(type: NotificationType, prayer: Prayer) {
        val reminder = prayer.toReminder()
        if (type == NotificationType.OFF) alarm.cancelAlarm(reminder)
        else alarm.setAlarm(reminder)
    }

    fun getPrayerName(prayer: Prayer) = prayersRepository.getPrayerName(prayer)

}