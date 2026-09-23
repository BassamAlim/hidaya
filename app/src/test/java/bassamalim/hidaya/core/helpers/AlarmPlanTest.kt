package bassamalim.hidaya.core.helpers

import bassamalim.hidaya.core.enums.NotificationType
import bassamalim.hidaya.core.enums.Reminder
import bassamalim.hidaya.core.enums.Reminder.Prayer.Asr
import bassamalim.hidaya.core.enums.Reminder.Prayer.Dhuhr
import bassamalim.hidaya.core.enums.Reminder.Prayer.Fajr
import bassamalim.hidaya.core.enums.Reminder.Prayer.Ishaa
import bassamalim.hidaya.core.enums.Reminder.Prayer.Maghrib
import bassamalim.hidaya.core.enums.Reminder.Prayer.Sunrise
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** When [Alarm] schedules prayer, extra and remembrance reminders. */
class AlarmPlanTest {

    private val zone = TimeZone.getTimeZone("Asia/Riyadh")

    private fun at(hour: Int, minute: Int = 0) = Calendar.getInstance(zone).apply {
        clear()
        set(2026, Calendar.SEPTEMBER, 23, hour, minute)
    }

    private fun millis(hour: Int, minute: Int = 0) = at(hour, minute).timeInMillis

    private val prayerTimes: Map<Reminder.Prayer, Calendar?> = mapOf(
        Fajr to at(4, 30),
        Sunrise to at(5, 50),
        Dhuhr to at(12, 0),
        Asr to at(15, 20),
        Maghrib to at(18, 0),
        Ishaa to at(19, 30)
    )

    private val allOn = prayerTimes.keys.associateWith { NotificationType.ATHAN }
    private val noExtras = emptyMap<Reminder.PrayerExtra, Int>()

    private fun plan(
        now: Long,
        times: Map<Reminder.Prayer, Calendar?> = prayerTimes,
        types: Map<Reminder.Prayer, NotificationType> = allOn,
        offsets: Map<Reminder.PrayerExtra, Int> = noExtras
    ) = planPrayerAlarms(times, types, offsets, now)

    @Test
    fun `at the start of the day every prayer is scheduled at its time`() {
        assertEquals(
            prayerTimes.mapValues { it.value!!.timeInMillis },
            plan(now = millis(0, 10))
        )
    }

    @Test
    fun `prayers already passed are skipped`() {
        assertEquals(
            setOf(Asr, Maghrib, Ishaa),
            plan(now = millis(13, 0)).keys
        )
    }

    @Test
    fun `a prayer exactly now is still scheduled`() {
        assertEquals(millis(15, 20), plan(now = millis(15, 20))[Asr])
    }

    @Test
    fun `prayers with notifications off are skipped, other types are scheduled`() {
        val types = allOn + mapOf(
            Dhuhr to NotificationType.OFF,
            Asr to NotificationType.SILENT,
            Maghrib to NotificationType.NOTIFICATION
        )
        assertEquals(
            setOf(Fajr, Sunrise, Asr, Maghrib, Ishaa),
            plan(now = millis(0, 10), types = types).keys
        )
    }

    @Test
    fun `a prayer without a time is skipped`() {
        // High-latitude days can have no Ishaa
        val times = prayerTimes + (Ishaa to null)
        assertEquals(null, plan(now = millis(0, 10), times = times)[Ishaa])
    }

    @Test
    fun `extra reminders are offset from their prayer, before or after`() {
        val offsets = mapOf(
            Reminder.PrayerExtra.Fajr to -15,
            Reminder.PrayerExtra.Ishaa to 30
        )
        val plan = plan(now = millis(0, 10), offsets = offsets)

        assertEquals(millis(4, 15), plan[Reminder.PrayerExtra.Fajr])
        assertEquals(millis(20, 0), plan[Reminder.PrayerExtra.Ishaa])
    }

    @Test
    fun `a zero offset means no extra reminder`() {
        val offsets = mapOf(Reminder.PrayerExtra.Dhuhr to 0)
        assertEquals(null, plan(now = millis(0, 10), offsets = offsets)[Reminder.PrayerExtra.Dhuhr])
    }

    @Test
    fun `an extra reminder already passed is skipped even if its prayer is ahead`() {
        // 12:50 now: Asr at 15:20 is ahead, but its 3-hours-before reminder (12:20) passed
        val offsets = mapOf(Reminder.PrayerExtra.Asr to -180)
        val plan = plan(now = millis(12, 50), offsets = offsets)

        assertEquals(millis(15, 20), plan[Asr])
        assertEquals(null, plan[Reminder.PrayerExtra.Asr])
    }

    @Test
    fun `extra reminders don't depend on the prayer's own notification`() {
        val types = allOn + (Maghrib to NotificationType.OFF)
        val offsets = mapOf(Reminder.PrayerExtra.Maghrib to -10)
        val plan = plan(now = millis(0, 10), types = types, offsets = offsets)

        assertEquals(null, plan[Maghrib])
        assertEquals(millis(17, 50), plan[Reminder.PrayerExtra.Maghrib])
    }

    // nextTimeAfterPrayer (morning/evening remembrances, 30 minutes after Fajr/Asr)

    private val fajrToday = at(4, 30)
    private val fajrTomorrow = (at(4, 31)).apply { add(Calendar.DATE, 1) }

    private fun remembrance(now: Long, today: Calendar? = fajrToday) =
        nextTimeAfterPrayer(today, fajrTomorrow, minutesAfter = 30, now = now)?.timeInMillis

    @Test
    fun `before the prayer, the reminder is 30 minutes after today's prayer`() {
        assertEquals(millis(5, 0), remembrance(now = millis(0, 10)))
    }

    @Test
    fun `between the prayer and the reminder, today's reminder is kept`() {
        // Used to roll over to tomorrow here, because it checked the prayer time, not the reminder's
        assertEquals(millis(5, 0), remembrance(now = millis(4, 45)))
        assertEquals(millis(5, 0), remembrance(now = millis(5, 0)))
    }

    @Test
    fun `after today's reminder, it's 30 minutes after tomorrow's prayer`() {
        val expected = (at(5, 1)).apply { add(Calendar.DATE, 1) }.timeInMillis
        assertEquals(expected, remembrance(now = millis(5, 1)))
    }

    @Test
    fun `no prayer time today falls back to tomorrow's`() {
        val expected = (at(5, 1)).apply { add(Calendar.DATE, 1) }.timeInMillis
        assertEquals(expected, remembrance(now = millis(0, 10), today = null))
    }

}
