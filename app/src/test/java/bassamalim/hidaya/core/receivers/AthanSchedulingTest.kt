package bassamalim.hidaya.core.receivers

import bassamalim.hidaya.core.Globals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * The daily update re-arms every athan alarm, so a wrong next-update time or a wrong
 * "already updated today" answer silently costs users a day of athans.
 */
class AthanSchedulingTest {

    private val riyadh = TimeZone.getTimeZone("Asia/Riyadh")
    // Lebanon springs forward at midnight, so the 00:10 update time doesn't exist that day
    private val beirut = TimeZone.getTimeZone("Asia/Beirut")

    private fun at(zone: TimeZone, year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0) =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }

    private fun updateTimeOn(zone: TimeZone, year: Int, month: Int, day: Int) =
        at(zone, year, month, day, Globals.DAILY_UPDATE_HOUR, Globals.DAILY_UPDATE_MINUTE)

    // nextDailyUpdateTime

    @Test
    fun `next update is tomorrow at the update time`() {
        val next = nextDailyUpdateTime(updateTimeOn(riyadh, 2026, 9, 23))
        assertEquals(updateTimeOn(riyadh, 2026, 9, 24).timeInMillis, next.timeInMillis)
    }

    @Test
    fun `a late run still schedules tomorrow, not the day after`() {
        val next = nextDailyUpdateTime(at(riyadh, 2026, 9, 23, hour = 23, minute = 59))
        assertEquals(updateTimeOn(riyadh, 2026, 9, 24).timeInMillis, next.timeInMillis)
    }

    @Test
    fun `rolls over month, year and leap day`() {
        assertEquals(
            updateTimeOn(riyadh, 2026, 10, 1).timeInMillis,
            nextDailyUpdateTime(at(riyadh, 2026, 9, 30, hour = 12)).timeInMillis
        )
        assertEquals(
            updateTimeOn(riyadh, 2027, 1, 1).timeInMillis,
            nextDailyUpdateTime(at(riyadh, 2026, 12, 31, hour = 12)).timeInMillis
        )
        assertEquals(
            updateTimeOn(riyadh, 2028, 2, 29).timeInMillis,
            nextDailyUpdateTime(at(riyadh, 2028, 2, 28, hour = 12)).timeInMillis
        )
    }

    @Test
    fun `lands on the next date when DST skips the update time`() {
        val now = updateTimeOn(beirut, 2026, 3, 28)
        val next = nextDailyUpdateTime(now)

        assertEquals(29, next[Calendar.DAY_OF_MONTH])
        assertTrue(next.after(now))
    }

    // isSameDay (guards the "already updated today" check)

    @Test
    fun `same date at different times is the same day`() {
        assertTrue(isSameDay(at(riyadh, 2026, 9, 23, hour = 0, minute = 10), at(riyadh, 2026, 9, 23, hour = 23)))
    }

    @Test
    fun `either side of midnight is not the same day`() {
        assertFalse(isSameDay(at(riyadh, 2026, 9, 23, hour = 23, minute = 59), at(riyadh, 2026, 9, 24)))
    }

    @Test
    fun `same day of month in another month is not the same day`() {
        assertFalse(isSameDay(at(riyadh, 2026, 8, 23), at(riyadh, 2026, 9, 23)))
    }

    @Test
    fun `same day of year in another year is not the same day`() {
        assertFalse(isSameDay(at(riyadh, 2025, 9, 23), at(riyadh, 2026, 9, 23)))
    }

    // isOnTime (the receiver drops stale alarms)

    private val prayerTime = at(riyadh, 2026, 9, 23, hour = 12).timeInMillis

    @Test
    fun `alarm on time or up to 2 minutes late plays`() {
        assertTrue(isOnTime(prayerTime, now = prayerTime))
        assertTrue(isOnTime(prayerTime, now = prayerTime + 120_000))
    }

    @Test
    fun `alarm more than 2 minutes late is dropped`() {
        assertFalse(isOnTime(prayerTime, now = prayerTime + 120_001))
    }

}
