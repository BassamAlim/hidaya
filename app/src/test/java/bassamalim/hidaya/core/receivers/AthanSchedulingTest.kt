package bassamalim.hidaya.core.receivers

import bassamalim.hidaya.core.Globals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The daily update re-arms every athan alarm, so a wrong next-update time or a wrong
 * "already updated today" answer silently costs users a day of athans.
 */
class AthanSchedulingTest {

    private val riyadh = ZoneId.of("Asia/Riyadh")
    // Lebanon springs forward at midnight, so the 00:10 update time doesn't exist that day
    private val beirut = ZoneId.of("Asia/Beirut")

    private fun zonedAt(
        zone: ZoneId, year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0
    ) = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)

    private fun ZonedDateTime.millis() = toInstant().toEpochMilli()

    private fun updateTimeOn(zone: ZoneId, year: Int, month: Int, day: Int) =
        zonedAt(zone, year, month, day, Globals.DAILY_UPDATE_HOUR, Globals.DAILY_UPDATE_MINUTE)

    // nextDailyUpdateTime

    @Test
    fun `next update is tomorrow at the update time`() {
        val next = nextDailyUpdateTime(updateTimeOn(riyadh, 2026, 9, 23))
        assertEquals(updateTimeOn(riyadh, 2026, 9, 24), next)
    }

    @Test
    fun `a late run still schedules tomorrow, not the day after`() {
        val next = nextDailyUpdateTime(zonedAt(riyadh, 2026, 9, 23, hour = 23, minute = 59))
        assertEquals(updateTimeOn(riyadh, 2026, 9, 24), next)
    }

    @Test
    fun `rolls over month, year and leap day`() {
        assertEquals(
            updateTimeOn(riyadh, 2026, 10, 1),
            nextDailyUpdateTime(zonedAt(riyadh, 2026, 9, 30, hour = 12))
        )
        assertEquals(
            updateTimeOn(riyadh, 2027, 1, 1),
            nextDailyUpdateTime(zonedAt(riyadh, 2026, 12, 31, hour = 12))
        )
        assertEquals(
            updateTimeOn(riyadh, 2028, 2, 29),
            nextDailyUpdateTime(zonedAt(riyadh, 2028, 2, 28, hour = 12))
        )
    }

    @Test
    fun `lands on the next date when DST skips the update time`() {
        val now = updateTimeOn(beirut, 2026, 3, 28)
        val next = nextDailyUpdateTime(now)

        assertEquals(29, next.dayOfMonth)
        assertTrue(next.isAfter(now))
    }

    // isOnDate (the "already updated today" check)

    @Test
    fun `an update earlier today is on today's date`() {
        val update = zonedAt(riyadh, 2026, 9, 23, hour = 0, minute = 10)
        assertTrue(isOnDate(update.millis(), LocalDate.of(2026, 9, 23), riyadh))
    }

    @Test
    fun `an update just before midnight is not on the next date`() {
        val update = zonedAt(riyadh, 2026, 9, 23, hour = 23, minute = 59)
        assertFalse(isOnDate(update.millis(), LocalDate.of(2026, 9, 24), riyadh))
    }

    @Test
    fun `an update on the same date a year earlier is not on today's date`() {
        val update = zonedAt(riyadh, 2025, 9, 23, hour = 12)
        assertFalse(isOnDate(update.millis(), LocalDate.of(2026, 9, 23), riyadh))
    }

    // isOnTime (the receiver drops stale alarms)

    private val prayerTime = zonedAt(riyadh, 2026, 9, 23, hour = 12).millis()

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
