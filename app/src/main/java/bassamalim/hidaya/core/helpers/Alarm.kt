package bassamalim.hidaya.core.helpers

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.data.repositories.NotificationsRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.enums.NotificationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.enums.Reminder
import bassamalim.hidaya.core.receivers.NotificationReceiver
import bassamalim.hidaya.core.utils.PrayerTimeUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.SortedMap

class Alarm(
    private val app: Application,
    private val prayersRepository: PrayersRepository,
    private val notificationsRepository: NotificationsRepository,
    private val locationRepository: LocationRepository
) {

    suspend fun setAll(prayerTimes: SortedMap<Prayer, Calendar?>) {
        val reminderTimes = prayerTimes.map { (prayer, time) ->
            prayer.toReminder() to time
        }.toMap()

        val plan = planPrayerAlarms(
            prayerTimes = reminderTimes,
            notificationTypes = reminderTimes.keys.associateWith {
                notificationsRepository.getNotificationType(it).first()
            },
            extraReminderOffsets = notificationsRepository.getPrayerExtraReminderTimeOffsets().first(),
            now = System.currentTimeMillis()
        )
        for ((reminder, millis) in plan) schedule(reminder, millis)

        setDevotionalAlarms()
    }

    suspend fun setAlarm(reminder: Reminder) {
        when (reminder) {
            is Reminder.Prayer -> {
                val location = locationRepository.getLocation().first() ?: return
                val prayerTimes = PrayerTimeUtils.getPrayerTimes(
                    settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
                    selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
                    location = location,
                    calendar = Calendar.getInstance()
                ).map { (prayer, time) -> prayer.toReminder() to time }.toMap()

                val time = prayerTimes[reminder] ?: return
                scheduleIfAhead(reminder, time.timeInMillis)
            }
            is Reminder.PrayerExtra -> {
                val location = locationRepository.getLocation().first() ?: return
                val prayerTime = PrayerTimeUtils.getPrayerTimes(
                    settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
                    selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
                    location = location,
                    calendar = Calendar.getInstance()
                )[reminder.toPrayer()] ?: return

                val offset =
                    notificationsRepository.getPrayerExtraReminderTimeOffsets().first()[reminder] ?: 0
                scheduleIfAhead(reminder, extraReminderMillis(prayerTime.timeInMillis, offset))
            }
            is Reminder.Devotional -> {
                setDevotionalAlarm(reminder)
            }
        }
    }

    // An alarm set in the past fires immediately and is then dropped by the receiver's on-time
    // check, so there is nothing to schedule; the daily update sets tomorrow's.
    private fun scheduleIfAhead(reminder: Reminder, millis: Long) {
        if (millis >= System.currentTimeMillis()) schedule(reminder, millis)
        else Log.i(Globals.TAG, "$reminder Passed")
    }

    private fun schedule(reminder: Reminder, millis: Long) {
        val intent = Intent(app, NotificationReceiver::class.java).apply {
            action = getAction(reminder)
            putExtra("id", reminder.id)
            putExtra("time", millis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            app, reminder.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)

        Log.i(Globals.TAG, "alarm $reminder set")
    }

    private suspend fun setDevotionalAlarms() {
        Log.i(Globals.TAG, "in Alarm.setDevotionalAlarms")

        val today = Calendar.getInstance()

        val devotionAlarmEnabledMap =
            notificationsRepository.getDevotionalReminderEnabledMap().first()

        for ((devotion, enabled) in devotionAlarmEnabledMap) {
            if (enabled) {
                if (devotion is Reminder.Devotional.FridayKahf) {
                    if (today[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY)
                        setDevotionalAlarm(devotion)
                }
                else setDevotionalAlarm(devotion)
            }
        }
    }

    private suspend fun setDevotionalAlarm(devotion: Reminder.Devotional) {
        Log.i(Globals.TAG, "in Alarm.setDevotionalAlarm")

        scheduleIfAhead(devotion, getDevotionalReminderTime(devotion).timeInMillis)
    }

    suspend fun getDevotionalReminderTime(devotion: Reminder.Devotional): Calendar {
        val time = when (devotion) {
            Reminder.Devotional.MorningRemembrances, Reminder.Devotional.EveningRemembrances -> {
                val referencePrayer =
                    if (devotion == Reminder.Devotional.MorningRemembrances) Prayer.FAJR
                    else Prayer.ASR
                val prayerTime = getPrayerTime(referencePrayer)
                    ?: return Calendar.getInstance()
                Calendar.getInstance().apply {
                    timeInMillis = prayerTime.timeInMillis
                    add(Calendar.MINUTE, 30)
                }
            }
            Reminder.Devotional.DailyWerd, Reminder.Devotional.FridayKahf -> {
                val timeOfDay =
                    notificationsRepository.getDevotionalReminderTimes().first()[devotion]!!
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timeOfDay.hour)
                    set(Calendar.MINUTE, timeOfDay.minute)
                }
            }
        }

        time[Calendar.SECOND] = 0
        time[Calendar.MILLISECOND] = 0

        return time
    }

    fun cancelAlarm(reminder: Reminder) {
        // AlarmManager matches alarms by PendingIntent, and PendingIntents by request code plus
        // Intent.filterEquals(), so the intent has to carry the same component and action as the
        // one the alarm was scheduled with. A bare Intent() matches nothing and cancels nothing.
        val intent = Intent(app, NotificationReceiver::class.java).apply {
            action = getAction(reminder)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            app, reminder.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        Log.i(Globals.TAG, "Canceled $reminder Alarm")
    }

    /**
     * The action the alarm of [reminder] is scheduled with. Scheduling and canceling have to agree
     * on it, otherwise the canceling PendingIntent doesn't match the scheduled one.
     */
    private fun getAction(reminder: Reminder) = when (reminder) {
        Reminder.Prayer.Sunrise -> "devotion"
        is Reminder.Prayer -> "prayer"
        is Reminder.PrayerExtra -> "prayer_extra"
        is Reminder.Devotional -> "devotion"
    }

    suspend fun getPrayerTime(prayer: Prayer): Calendar? {
        val location = locationRepository.getLocation().first()!!

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

}

/**
 * The alarms [Alarm.setAll] schedules, as epoch millis: each prayer whose notifications aren't
 * off, and each extra reminder with a non-zero offset (independent of its prayer's setting).
 * Times already behind [now] are left out; tomorrow's daily update sets them.
 */
internal fun planPrayerAlarms(
    prayerTimes: Map<Reminder.Prayer, Calendar?>,
    notificationTypes: Map<Reminder.Prayer, NotificationType>,
    extraReminderOffsets: Map<Reminder.PrayerExtra, Int>,
    now: Long
): Map<Reminder, Long> {
    val plan = mutableMapOf<Reminder, Long>()
    for ((prayer, time) in prayerTimes) {
        val millis = time?.timeInMillis ?: continue

        val type = notificationTypes[prayer]
        if (type != null && type != NotificationType.OFF && millis >= now) plan[prayer] = millis

        val extra = prayer.toPrayerExtra()
        val offset = extraReminderOffsets[extra] ?: 0
        val extraMillis = extraReminderMillis(millis, offset)
        if (offset != 0 && extraMillis >= now) plan[extra] = extraMillis
    }
    return plan
}

/** [offsetMinutes] is negative for a reminder before the prayer. */
internal fun extraReminderMillis(prayerMillis: Long, offsetMinutes: Int) =
    prayerMillis + offsetMinutes * 60_000L
