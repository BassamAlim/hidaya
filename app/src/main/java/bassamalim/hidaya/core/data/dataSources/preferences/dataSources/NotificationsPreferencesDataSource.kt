package bassamalim.hidaya.core.data.dataSources.preferences.dataSources

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import bassamalim.hidaya.core.data.dataSources.preferences.objects.NotificationsPreferences
import bassamalim.hidaya.core.enums.NotificationType
import bassamalim.hidaya.core.enums.Reminder
import bassamalim.hidaya.core.models.TimeOfDay
import bassamalim.hidaya.core.utils.report
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class NotificationsPreferencesDataSource(
    private val dataStore: DataStore<NotificationsPreferences>
) {

    private val flow: Flow<NotificationsPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                exception.report()
                emit(NotificationsPreferences())
            }
            else throw exception
        }

    fun getNotificationTypes() = flow.map { it.notificationTypes }
    suspend fun updateNotificationTypes(types: PersistentMap<Reminder.Prayer, NotificationType>) {
        dataStore.updateData { preferences ->
            preferences.copy(notificationTypes = types)
        }
    }

    fun getDevotionalReminderEnabledStatuses() = flow.map {
        it.devotionalReminderEnabledStatuses
    }
    suspend fun updateDevotionalReminderEnabledStatuses(
        statuses: PersistentMap<Reminder.Devotional, Boolean>
    ) {
        dataStore.updateData { preferences ->
            preferences.copy(devotionalReminderEnabledStatuses = statuses)
        }
    }

    fun getDevotionalReminderTimes() = flow.map { it.devotionalReminderTimes }
    suspend fun updateDevotionalReminderTimes(
        times: PersistentMap<Reminder.Devotional, TimeOfDay>
    ) {
        dataStore.updateData { preferences ->
            preferences.copy(devotionalReminderTimes = times)
        }
    }

    fun getPrayerExtraReminderTimeOffsets() = flow.map { it.prayerExtraReminderTimeOffsets }
    suspend fun updatePrayerExtraReminderTimeOffsets(offsets: PersistentMap<Reminder.PrayerExtra, Int>) {
        dataStore.updateData { preferences ->
            preferences.copy(prayerExtraReminderTimeOffsets = offsets)
        }
    }

    fun getLastNotificationDates() = flow.map { it.lastNotificationDates }
    suspend fun updateLastNotificationDates(dates: PersistentMap<Reminder, Int>) {
        dataStore.updateData { preferences ->
            preferences.copy(lastNotificationDates = dates)
        }
    }

}