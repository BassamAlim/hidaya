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
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class Alarm(
    private val app: Application,
    private val prayersRepository: PrayersRepository,
    private val notificationsRepository: NotificationsRepository,
    private val locationRepository: LocationRepository
) {

    suspend fun setAll(prayerTimes: Map<Prayer, ZonedDateTime?>) {
        val reminderTimes = prayerTimes.map { (prayer, time) ->
            prayer.toReminder() to time
        }.toMap()

        val plan = planPrayerAlarms(
            prayerTimes = reminderTimes,
            notificationTypes = reminderTimes.keys.associateWith {
                notificationsRepository.getNotificationType(it).first()
            },
            extraReminderOffsets = notificationsRepository.getPrayerExtraReminderTimeOffsets().first(),
            now = Instant.now()
        )
        for ((reminder, time) in plan) schedule(reminder, time)

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
                    date = LocalDate.now()
                ).map { (prayer, time) -> prayer.toReminder() to time }.toMap()

                val time = prayerTimes[reminder] ?: return
                scheduleIfAhead(reminder, time.toInstant())
            }
            is Reminder.PrayerExtra -> {
                val location = locationRepository.getLocation().first() ?: return
                val prayerTime = PrayerTimeUtils.getPrayerTimes(
                    settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
                    selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
                    location = location,
                    date = LocalDate.now()
                )[reminder.toPrayer()] ?: return

                val offset =
                    notificationsRepository.getPrayerExtraReminderTimeOffsets().first()[reminder] ?: 0
                scheduleIfAhead(reminder, extraReminderTime(prayerTime.toInstant(), offset))
            }
            is Reminder.Devotional -> {
                setDevotionalAlarm(reminder)
            }
        }
    }

    // An alarm set in the past fires immediately and is then dropped by the receiver's on-time
    // check, so there is nothing to schedule; the daily update sets tomorrow's.
    private fun scheduleIfAhead(reminder: Reminder, time: Instant) {
        if (!time.isBefore(Instant.now())) schedule(reminder, time)
        else Log.i(Globals.TAG, "$reminder Passed")
    }

    private fun schedule(reminder: Reminder, time: Instant) {
        val millis = time.toEpochMilli()
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

        val today = LocalDate.now()

        val devotionAlarmEnabledMap =
            notificationsRepository.getDevotionalReminderEnabledMap().first()

        for ((devotion, enabled) in devotionAlarmEnabledMap) {
            if (enabled) {
                if (devotion is Reminder.Devotional.FridayKahf) {
                    if (today.dayOfWeek == DayOfWeek.FRIDAY)
                        setDevotionalAlarm(devotion)
                }
                else setDevotionalAlarm(devotion)
            }
        }
    }

    private suspend fun setDevotionalAlarm(devotion: Reminder.Devotional) {
        Log.i(Globals.TAG, "in Alarm.setDevotionalAlarm")

        scheduleIfAhead(devotion, getDevotionalReminderTime(devotion).toInstant())
    }

    suspend fun getDevotionalReminderTime(devotion: Reminder.Devotional): ZonedDateTime =
        when (devotion) {
            Reminder.Devotional.MorningRemembrances, Reminder.Devotional.EveningRemembrances -> {
                val referencePrayer =
                    if (devotion == Reminder.Devotional.MorningRemembrances) Prayer.FAJR
                    else Prayer.ASR
                nextTimeAfterPrayer(
                    todayPrayer = getPrayerTime(referencePrayer, dayOffset = 0),
                    tomorrowPrayer = getPrayerTime(referencePrayer, dayOffset = 1),
                    minutesAfter = 30,
                    now = Instant.now()
                ) ?: ZonedDateTime.now()
            }
            Reminder.Devotional.DailyWerd, Reminder.Devotional.FridayKahf -> {
                val timeOfDay =
                    notificationsRepository.getDevotionalReminderTimes().first()[devotion]!!
                LocalDate.now().atTime(timeOfDay.hour, timeOfDay.minute)
                    .atZone(ZoneId.systemDefault()).withLaterOffsetAtOverlap()
            }
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

    private suspend fun getPrayerTime(prayer: Prayer, dayOffset: Int): ZonedDateTime? {
        val location = locationRepository.getLocation().first() ?: return null
        return PrayerTimeUtils.getPrayerTimes(
            settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
            selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
            location = location,
            date = LocalDate.now().plusDays(dayOffset.toLong())
        )[prayer]
    }

}

/**
 * The alarms [Alarm.setAll] schedules: each prayer whose notifications aren't off, and each
 * extra reminder with a non-zero offset (independent of its prayer's setting). Times already
 * behind [now] are left out; tomorrow's daily update sets them.
 */
internal fun planPrayerAlarms(
    prayerTimes: Map<Reminder.Prayer, ZonedDateTime?>,
    notificationTypes: Map<Reminder.Prayer, NotificationType>,
    extraReminderOffsets: Map<Reminder.PrayerExtra, Int>,
    now: Instant
): Map<Reminder, Instant> {
    val plan = mutableMapOf<Reminder, Instant>()
    for ((prayer, zonedTime) in prayerTimes) {
        val time = zonedTime?.toInstant() ?: continue

        val type = notificationTypes[prayer]
        if (type != null && type != NotificationType.OFF && !time.isBefore(now)) plan[prayer] = time

        val extra = prayer.toPrayerExtra()
        val offset = extraReminderOffsets[extra] ?: 0
        val extraTime = extraReminderTime(time, offset)
        if (offset != 0 && !extraTime.isBefore(now)) plan[extra] = extraTime
    }
    return plan
}

/** [offsetMinutes] is negative for a reminder before the prayer. */
internal fun extraReminderTime(prayerTime: Instant, offsetMinutes: Int): Instant =
    prayerTime.plus(Duration.ofMinutes(offsetMinutes.toLong()))

/**
 * [minutesAfter] today's prayer, or after tomorrow's once today's has passed. Deciding by the
 * prayer time instead skipped today's reminder when set between the prayer and the reminder.
 */
internal fun nextTimeAfterPrayer(
    todayPrayer: ZonedDateTime?,
    tomorrowPrayer: ZonedDateTime?,
    minutesAfter: Int,
    now: Instant
): ZonedDateTime? {
    val today = todayPrayer?.plusMinutes(minutesAfter.toLong())
    return if (today != null && !today.toInstant().isBefore(now)) today
    else tomorrowPrayer?.plusMinutes(minutesAfter.toLong())
}
