package bassamalim.hidaya.core.helpers

import bassamalim.hidaya.core.enums.HighLatitudesAdjustmentMethod
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.enums.PrayerTimeCalculationMethod
import bassamalim.hidaya.core.enums.PrayerTimeJuristicMethod
import bassamalim.hidaya.core.models.Coordinates
import bassamalim.hidaya.core.models.PrayerTimeCalculatorSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.SortedMap

/**
 * Expected times are characterization values generated from a line-by-line port of the
 * algorithm, sanity-checked against real-world sources (Umm al-Qura calendar for Mecca,
 * observed sunrise/sunset for Oslo on the June solstice).
 */
class PrayerTimeCalculatorTest {

    private val mecca = Coordinates(latitude = 21.4225, longitude = 39.8262, elevation = 0.0)
    private val amman = Coordinates(latitude = 31.9539, longitude = 35.9106, elevation = 900.0)
    private val cairo = Coordinates(latitude = 30.0444, longitude = 31.2357, elevation = 0.0)
    private val oslo = Coordinates(latitude = 59.9139, longitude = 10.7522, elevation = 0.0)

    private fun calculator(
        method: PrayerTimeCalculationMethod = PrayerTimeCalculationMethod.MECCA,
        juristic: PrayerTimeJuristicMethod = PrayerTimeJuristicMethod.SHAFII,
        highLatAdjustment: HighLatitudesAdjustmentMethod = HighLatitudesAdjustmentMethod.NONE
    ) = PrayerTimeCalculator(
        PrayerTimeCalculatorSettings(
            calculationMethod = method,
            juristicMethod = juristic,
            highLatitudesAdjustmentMethod = highLatAdjustment
        )
    )

    private data class Day(val date: LocalDate, val utcOffset: ZoneOffset)

    private fun dayAt(year: Int, month: Int, day: Int, utcOffsetHours: Int) =
        Day(LocalDate.of(year, month, day), ZoneOffset.ofHours(utcOffsetHours))

    private fun PrayerTimeCalculator.getPrayerTimes(coordinates: Coordinates, day: Day) =
        getPrayerTimes(coordinates, day.date, day.utcOffset)

    private fun format(time: LocalTime?): String {
        assertNotNull(time)
        return time.toString()
    }

    private fun assertTimes(
        expected: Map<Prayer, String>,
        actual: SortedMap<Prayer, LocalTime?>
    ) {
        for ((prayer, time) in expected)
            assertEquals("$prayer", time, format(actual[prayer]))
    }

    @Test
    fun `mecca method produces expected times for mecca on the equinox`() {
        val times = calculator().getPrayerTimes(mecca, dayAt(2024, 3, 20, 3))

        assertTimes(
            expected = mapOf(
                Prayer.FAJR to "05:08",
                Prayer.SUNRISE to "06:25",
                Prayer.DHUHR to "12:28",
                Prayer.ASR to "15:53",
                Prayer.MAGHRIB to "18:32",
                Prayer.ISHAA to "20:02"
            ),
            actual = times
        )
    }

    @Test
    fun `mecca method sets ishaa to ninety minutes after maghrib`() {
        val times = calculator().getPrayerTimes(mecca, dayAt(2024, 3, 20, 3))

        assertEquals(
            Duration.ofMinutes(90),
            Duration.between(times[Prayer.MAGHRIB], times[Prayer.ISHAA])
        )
    }

    @Test
    fun `hanafi asr is later than shafii asr and other times are unchanged`() {
        val date = dayAt(2024, 3, 20, 3)
        val shafii = calculator(juristic = PrayerTimeJuristicMethod.SHAFII)
            .getPrayerTimes(mecca, date)
        val hanafi = calculator(juristic = PrayerTimeJuristicMethod.HANAFI)
            .getPrayerTimes(mecca, date)

        assertEquals("15:53", format(shafii[Prayer.ASR]))
        assertEquals("16:50", format(hanafi[Prayer.ASR]))
        for (prayer in listOf(
            Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.MAGHRIB, Prayer.ISHAA
        ))
            assertEquals("$prayer", format(shafii[prayer]), format(hanafi[prayer]))
    }

    @Test
    fun `jordan method produces expected times for amman in summer`() {
        val times = calculator(method = PrayerTimeCalculationMethod.JORDAN)
            .getPrayerTimes(amman, dayAt(2024, 7, 15, 3))

        assertTimes(
            expected = mapOf(
                Prayer.FAJR to "04:04",
                Prayer.SUNRISE to "05:35",
                Prayer.DHUHR to "12:42",
                Prayer.ASR to "16:24",
                Prayer.MAGHRIB to "19:49",
                Prayer.ISHAA to "21:21"
            ),
            actual = times
        )
    }

    @Test
    fun `observer elevation shifts sunrise earlier and leaves other times unchanged`() {
        val date = dayAt(2024, 3, 20, 3)
        val atSeaLevel = calculator().getPrayerTimes(mecca, date)
        val elevated = calculator()
            .getPrayerTimes(mecca.copy(elevation = 300.0), date)

        assertEquals("06:25", format(atSeaLevel[Prayer.SUNRISE]))
        assertEquals("06:22", format(elevated[Prayer.SUNRISE]))
        for (prayer in listOf(
            Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHAA
        ))
            assertEquals("$prayer", format(atSeaLevel[prayer]), format(elevated[prayer]))
    }

    @Test
    fun `fajr and ishaa are null at high latitudes in midsummer without adjustment`() {
        val times = calculator(method = PrayerTimeCalculationMethod.MWL)
            .getPrayerTimes(oslo, dayAt(2024, 6, 21, 2))

        assertNull(times[Prayer.FAJR])
        assertNull(times[Prayer.ISHAA])
        assertEquals("03:54", format(times[Prayer.SUNRISE]))
        assertEquals("13:19", format(times[Prayer.DHUHR]))
        assertEquals("18:01", format(times[Prayer.ASR]))
        assertEquals("22:44", format(times[Prayer.MAGHRIB]))
    }

    @Test
    fun `high latitude adjustment produces times when twilight never ends`() {
        val date = dayAt(2024, 6, 21, 2)
        val expected = mapOf(
            // Ishaa crosses midnight and wraps to the small hours of the same calendar date
            HighLatitudesAdjustmentMethod.MIDNIGHT to Pair("01:19", "01:19"),
            HighLatitudesAdjustmentMethod.ONE_SEVENTH to Pair("03:10", "23:28"),
            HighLatitudesAdjustmentMethod.ANGLE_BASED to Pair("02:21", "00:12")
        )

        for ((method, fajrAndIshaa) in expected) {
            val times = calculator(
                method = PrayerTimeCalculationMethod.MWL,
                highLatAdjustment = method
            ).getPrayerTimes(oslo, date)

            assertEquals("$method fajr", fajrAndIshaa.first, format(times[Prayer.FAJR]))
            assertEquals("$method ishaa", fajrAndIshaa.second, format(times[Prayer.ISHAA]))
        }
    }

    @Test
    fun `fajr angle differences across methods produce expected fajr and ishaa times`() {
        val date = dayAt(2024, 10, 5, 2)
        val expected = mapOf(
            PrayerTimeCalculationMethod.MECCA to Pair("04:29", "19:05"),
            PrayerTimeCalculationMethod.MWL to Pair("04:31", "18:50"),
            PrayerTimeCalculationMethod.ISNA to Pair("04:45", "18:41"),
            PrayerTimeCalculationMethod.KARACHI to Pair("04:31", "18:55"),
            PrayerTimeCalculationMethod.EGYPT to Pair("04:25", "18:52"),
            PrayerTimeCalculationMethod.TAHRAN to Pair("04:33", "18:36"),
            PrayerTimeCalculationMethod.JORDAN to Pair("04:31", "18:55")
        )

        for ((method, fajrAndIshaa) in expected) {
            val times = calculator(method = method).getPrayerTimes(cairo, date)

            assertEquals("$method fajr", fajrAndIshaa.first, format(times[Prayer.FAJR]))
            assertEquals("$method ishaa", fajrAndIshaa.second, format(times[Prayer.ISHAA]))
        }
    }

    @Test
    fun `prayer times are in chronological order for all calculation methods`() {
        val date = dayAt(2024, 10, 5, 2)
        val order = listOf(
            Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHAA
        )

        for (method in PrayerTimeCalculationMethod.entries) {
            val times = calculator(method = method).getPrayerTimes(cairo, date)

            order.zipWithNext().forEach { (earlier, later) ->
                assertTrue(
                    "$method: $earlier should be before $later",
                    times[earlier]!! < times[later]!!
                )
            }
        }
    }

    @Test
    fun `returned times are whole minutes`() {
        val times = calculator().getPrayerTimes(mecca, dayAt(2024, 3, 20, 3))

        for ((prayer, time) in times) {
            assertNotNull("$prayer", time)
            assertEquals("$prayer second", 0, time!!.second)
            assertEquals("$prayer nano", 0, time.nano)
        }
    }

}
