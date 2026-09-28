package bassamalim.hidaya.core.utils

import bassamalim.hidaya.core.enums.LocationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.enums.PrayerTimeCalculationMethod
import bassamalim.hidaya.core.models.Coordinates
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.models.LocationIds
import bassamalim.hidaya.core.models.PrayerTimeCalculatorSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Covers the timezone-offset handling around the calculator, in particular the DST
 * transition-day regression: the daily update runs at midnight, before the 02:00-03:00
 * transition, and used to compute the whole day with the pre-transition offset, putting
 * every prayer time and athan alarm an hour off on those days.
 */
class PrayerTimeUtilsTest {

    private val berlinZone = "Europe/Berlin"
    private val berlin = Location(
        type = LocationType.MANUAL,
        coordinates = Coordinates(latitude = 52.52, longitude = 13.405, elevation = 0.0),
        ids = LocationIds(countryId = 1, cityId = 1)
    )
    private val settings =
        PrayerTimeCalculatorSettings(calculationMethod = PrayerTimeCalculationMethod.MWL)

    private fun timesOn(year: Int, month: Int, day: Int) = PrayerTimeUtils.getPrayerTimes(
        settings = settings,
        selectedTimeZoneId = berlinZone,
        location = berlin,
        date = LocalDate.of(year, month, day),
        zone = ZoneId.of(berlinZone)
    )

    private fun format(time: ZonedDateTime?): String {
        assertNotNull(time)
        return String.format(Locale.US, "%02d:%02d", time!!.hour, time.minute)
    }

    @Test
    fun `spring forward day uses the post-transition offset`() {
        // CEST starts 2024-03-31 at 02:00 (+1h -> +2h); midnight is still on +1h
        val times = timesOn(2024, 3, 31)

        // Pre-fix, these came out an hour early (Dhuhr 12:10)
        assertEquals("04:41", format(times[Prayer.FAJR]))
        assertEquals("13:10", format(times[Prayer.DHUHR]))
        assertEquals("19:40", format(times[Prayer.MAGHRIB]))
    }

    @Test
    fun `fall back day uses the post-transition offset`() {
        // CEST ends 2024-10-27 at 03:00 (+2h -> +1h); midnight is still on +2h
        val times = timesOn(2024, 10, 27)

        // Pre-fix, these came out an hour late (Dhuhr 12:50)
        assertEquals("04:59", format(times[Prayer.FAJR]))
        assertEquals("11:50", format(times[Prayer.DHUHR]))
        assertEquals("16:45", format(times[Prayer.MAGHRIB]))
    }

    @Test
    fun `returned times' instants match their wall-clock times in the real zone`() {
        // What AlarmManager fires on is the instant; it must agree with the displayed time
        val times = timesOn(2024, 3, 31)

        for ((prayer, time) in times) {
            assertNotNull("$prayer", time)
            val rendered = time!!.toInstant().atZone(ZoneId.of(berlinZone))
            assertEquals("$prayer", format(time), format(rendered))
        }
    }

}
