package bassamalim.hidaya.features.prayers.extraReminderSettings

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.NotificationsRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.helpers.Alarm
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerExtraReminderSettingsDomain @Inject constructor(
    private val prayersRepository: PrayersRepository,
    private val notificationsRepository: NotificationsRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val alarm: Alarm
) {

    val offsetMin = 30f

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    suspend fun getOffset(prayer: Prayer) =
        notificationsRepository.getPrayerExtraReminderTimeOffsets()
            .first()[prayer.toExtraReminder()]!!

    fun setOffset(prayer: Prayer, offset: Int) {
        notificationsRepository.setPrayerExtraReminderOffset(prayer.toExtraReminder(), offset)
    }

    fun getPrayerName(prayer: Prayer) = prayersRepository.getPrayerName(prayer)

    suspend fun updatePrayerTimeAlarms(prayer: Prayer) {
        alarm.setAlarm(prayer.toExtraReminder())
    }

}