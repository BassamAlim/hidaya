package bassamalim.hidaya.core.utils

import bassamalim.hidaya.core.enums.HighLatitudesAdjustmentMethod
import bassamalim.hidaya.core.enums.LocationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.enums.PrayerTimeCalculationMethod
import bassamalim.hidaya.core.enums.PrayerTimeJuristicMethod
import bassamalim.hidaya.core.models.Coordinates
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.models.LocationIds
import bassamalim.hidaya.core.models.PrayerTimeCalculatorSettings
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.zone.ZoneRulesProvider
import java.util.Locale
import java.util.TimeZone

/**
 * Golden master for prayer times: runs [PrayerTimeUtils.getPrayerTimes], the entry point every
 * screen, widget and alarm uses, over a large grid of cities, time zones, dates and settings,
 * and requires every result to match a recorded baseline exactly. Each time is recorded both as
 * the wall-clock time users see and as the instant the athan alarm fires at.
 *
 * It proves times are unchanged, not that they're correct: the baseline is whatever the code
 * produced when it was recorded.
 *
 * The baseline is [GOLDEN_PATH]. When it's missing, this test records it from the current code
 * and fails once so the recording can't go unnoticed; commit it. To accept an intended change in
 * times, delete the file, re-run, and review the diff before committing.
 *
 * Results depend on the JDK's time zone rules (tzdata). If only zones whose rules changed in a
 * tzdata update differ, re-record rather than change code; the header records the version.
 */
class PrayerTimesGoldenTest {

    private val originalZone: TimeZone = TimeZone.getDefault()

    @After
    fun restoreDefaultZone() {
        TimeZone.setDefault(originalZone)
    }

    private data class City(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val elevation: Double,
        val zone: String
    )

    private data class Scenario(
        val name: String,
        val city: City,
        val deviceZone: String,
        val locationType: LocationType,
        val settings: PrayerTimeCalculatorSettings
    )

    private val mecca = City("mecca", 21.4225, 39.8262, 277.0, "Asia/Riyadh")
    private val cairo = City("cairo", 30.0444, 31.2357, 23.0, "Africa/Cairo")
    private val amman = City("amman", 31.9539, 35.9106, 773.0, "Asia/Amman")
    private val istanbul = City("istanbul", 41.0082, 28.9784, 39.0, "Europe/Istanbul")
    private val tehran = City("tehran", 35.6892, 51.3890, 1189.0, "Asia/Tehran")  // +03:30
    private val karachi = City("karachi", 24.8607, 67.0011, 8.0, "Asia/Karachi")
    private val delhi = City("delhi", 28.6139, 77.2090, 216.0, "Asia/Kolkata")  // +05:30
    private val kathmandu = City("kathmandu", 27.7172, 85.3240, 1400.0, "Asia/Kathmandu")
    private val jakarta = City("jakarta", -6.2088, 106.8456, 8.0, "Asia/Jakarta")
    private val london = City("london", 51.5074, -0.1278, 11.0, "Europe/London")
    private val berlin = City("berlin", 52.52, 13.405, 34.0, "Europe/Berlin")
    private val newYork = City("new_york", 40.7128, -74.0060, 10.0, "America/New_York")
    // DST switches at midnight
    private val santiago = City("santiago", -33.4489, -70.6693, 570.0, "America/Santiago")
    private val sydney = City("sydney", -33.8688, 151.2093, 58.0, "Australia/Sydney")
    private val reykjavik = City("reykjavik", 64.1466, -21.9426, 0.0, "Atlantic/Reykjavik")

    private fun settings(
        method: PrayerTimeCalculationMethod,
        juristic: PrayerTimeJuristicMethod = PrayerTimeJuristicMethod.SHAFII,
        highLatitudes: HighLatitudesAdjustmentMethod = HighLatitudesAdjustmentMethod.NONE
    ) = PrayerTimeCalculatorSettings(method, juristic, highLatitudes)

    private fun auto(city: City, settings: PrayerTimeCalculatorSettings) =
        Scenario(city.name, city, city.zone, LocationType.AUTO, settings)

    private val meccaMethod = settings(PrayerTimeCalculationMethod.MECCA)
    private val mwl = settings(PrayerTimeCalculationMethod.MWL)
    private val mwlAngleBased = settings(
        PrayerTimeCalculationMethod.MWL,
        highLatitudes = HighLatitudesAdjustmentMethod.ANGLE_BASED
    )
    private val karachiHanafi =
        settings(PrayerTimeCalculationMethod.KARACHI, PrayerTimeJuristicMethod.HANAFI)
    private val isna = settings(PrayerTimeCalculationMethod.ISNA)

    /** Every day of [YEAR], so every DST transition in every zone is covered. */
    private val dailyScenarios = listOf(
        auto(mecca, meccaMethod),
        auto(cairo, settings(PrayerTimeCalculationMethod.EGYPT)),
        auto(amman, settings(PrayerTimeCalculationMethod.JORDAN)),
        auto(istanbul, mwl),
        auto(tehran, settings(PrayerTimeCalculationMethod.TAHRAN)),
        auto(karachi, karachiHanafi),
        auto(delhi, karachiHanafi),
        auto(kathmandu, settings(PrayerTimeCalculationMethod.KARACHI)),
        auto(jakarta, mwl),
        auto(london, mwlAngleBased),
        auto(berlin, mwlAngleBased),
        auto(newYork, isna),
        auto(santiago, isna),
        auto(sydney, mwl),
        auto(reykjavik, settings(
            PrayerTimeCalculationMethod.MWL,
            highLatitudes = HighLatitudesAdjustmentMethod.MIDNIGHT
        )),
        // Manual location in the device's own zone
        Scenario("mecca_manual", mecca, mecca.zone, LocationType.MANUAL, meccaMethod),
        Scenario("london_manual", london, london.zone, LocationType.MANUAL, mwlAngleBased),
        // Manual location in another zone than the device's. Intended behavior: times are the
        // location's wall-clock times on the device's clock, so athans follow the clock the
        // user sees (see PrayerTimeUtils.getPrayerTimes for why), and the prayers board points
        // the mismatch out. Don't "fix" these to the location's real moments.
        Scenario("mecca_on_berlin_device", mecca, berlin.zone, LocationType.MANUAL, meccaMethod),
        Scenario(
            "london_on_riyadh_device", london, mecca.zone, LocationType.MANUAL, mwlAngleBased
        ),
        Scenario("new_york_on_london_device", newYork, london.zone, LocationType.MANUAL, isna)
    )

    /** Every combination of settings, on the equinoxes and solstices. */
    private val gridCities = listOf(mecca, jakarta, tehran, newYork, london, reykjavik)
    private val gridDates = listOf(
        LocalDate.of(YEAR, 3, 20),
        LocalDate.of(YEAR, 6, 21),
        LocalDate.of(YEAR, 9, 22),
        LocalDate.of(YEAR, 12, 21)
    )

    @Test
    fun `prayer times match the recorded baseline`() {
        val actual = generate()
        val file = File(GOLDEN_PATH)

        if (!file.exists()) {
            file.parentFile!!.mkdirs()
            file.writeText(render(actual))
            fail("Recorded ${actual.size} results to ${file.absolutePath} from the current " +
                    "code. Review and commit it, then re-run.")
        }

        val expectedLines = file.readLines()
        val expected = parse(expectedLines)
        val problems = mutableListOf<String>()
        for ((key, value) in expected) {
            val actualValue = actual[key]
            if (actualValue == null) problems += "missing  $key"
            else if (actualValue != value)
                problems += "changed  $key\n  expected $value\n  actual   $actualValue"
        }
        for (key in actual.keys - expected.keys) problems += "new      $key: ${actual[key]}"

        if (problems.isNotEmpty()) {
            val actualFile = File(ACTUAL_PATH)
            actualFile.parentFile!!.mkdirs()
            actualFile.writeText(render(actual))
            val recordedTzdata = expectedLines.firstOrNull { it.startsWith(TZDATA_PREFIX) }
            fail(buildString {
                appendLine("${problems.size} prayer time results differ from the baseline.")
                appendLine("Baseline $recordedTzdata; current ${tzdataLine()}")
                appendLine("Full current results: ${actualFile.absolutePath}")
                problems.take(MAX_REPORTED).forEach { appendLine(it) }
                if (problems.size > MAX_REPORTED)
                    appendLine("... and ${problems.size - MAX_REPORTED} more")
            })
        }
    }

    /** Result key ("scenario date") to its formatted times, in a stable order. */
    private fun generate(): LinkedHashMap<String, String> {
        val results = LinkedHashMap<String, String>()

        for (scenario in dailyScenarios) {
            var date = LocalDate.of(YEAR, 1, 1)
            while (date.year == YEAR) {
                results["${scenario.name} $date"] = compute(scenario, date)
                date = date.plusDays(1)
            }
        }

        for (city in gridCities) {
            for (method in PrayerTimeCalculationMethod.entries)
                for (juristic in PrayerTimeJuristicMethod.entries)
                    for (highLatitudes in HighLatitudesAdjustmentMethod.entries) {
                        val name = "grid:${city.name}:$method:$juristic:$highLatitudes"
                        val scenario = auto(city, settings(method, juristic, highLatitudes))
                        for (date in gridDates)
                            results["$name $date"] = compute(scenario, date)
                    }
        }

        return results
    }

    private fun compute(scenario: Scenario, date: LocalDate): String {
        // The device's zone, which an automatic location's times are computed in
        TimeZone.setDefault(TimeZone.getTimeZone(scenario.deviceZone))
        val city = scenario.city
        val times = PrayerTimeUtils.getPrayerTimes(
            settings = scenario.settings,
            selectedTimeZoneId = city.zone,
            location = Location(
                type = scenario.locationType,
                coordinates = Coordinates(city.latitude, city.longitude, city.elevation),
                ids = LocationIds(countryId = 1, cityId = 1)
            ),
            date = date,
            zone = ZoneId.of(scenario.deviceZone)
        )
        return times.entries.joinToString(" ") { (prayer, time) ->
            "${abbreviation(prayer)}=${format(date, time)}"
        }
    }

    /**
     * "HH:mm" as shown, then "@HH:mmZ", the UTC time the alarm fires at. A day shift from [date]
     * is appended to either as "(+1d)" when there is one.
     */
    private fun format(date: LocalDate, time: ZonedDateTime?): String {
        if (time == null) return "--"

        val utc = time.toInstant().atZone(ZoneOffset.UTC)
        return buildString {
            append("%02d:%02d".format(Locale.US, time.hour, time.minute))
            append(dayShift(date, time.toLocalDate()))
            append("@%02d:%02d".format(Locale.US, utc.hour, utc.minute))
            if (utc.second != 0 || utc.nano != 0)
                append(":%02d.%03d".format(Locale.US, utc.second, utc.nano / 1_000_000))
            append('Z')
            append(dayShift(date, utc.toLocalDate()))
        }
    }

    private fun dayShift(from: LocalDate, to: LocalDate): String {
        val days = ChronoUnit.DAYS.between(from, to)
        return if (days == 0L) "" else "(%+dd)".format(Locale.US, days)
    }

    private fun abbreviation(prayer: Prayer) = when (prayer) {
        Prayer.FAJR -> "Fa"
        Prayer.SUNRISE -> "Sr"
        Prayer.DHUHR -> "Dh"
        Prayer.ASR -> "As"
        Prayer.MAGHRIB -> "Ma"
        Prayer.ISHAA -> "Is"
        Prayer.SUNSET -> "Ss"
    }

    private fun render(results: Map<String, String>) = buildString {
        appendLine("# Prayer times golden master, see PrayerTimesGoldenTest. Don't edit by hand.")
        appendLine("# Per prayer: shown time @ alarm time in UTC; -- means no time")
        appendLine(tzdataLine())
        for ((key, value) in results) appendLine("$key: $value")
    }

    private fun parse(lines: List<String>) = lines
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .associate { it.substringBefore(": ") to it.substringAfter(": ") }

    private fun tzdataLine() =
        TZDATA_PREFIX + ZoneRulesProvider.getVersions("Europe/Berlin").lastKey()

    private companion object {
        const val YEAR = 2026
        // Relative to the module, which is the working directory of unit tests
        const val GOLDEN_PATH = "src/test/resources/golden/prayer_times.txt"
        const val ACTUAL_PATH = "build/golden/prayer_times.actual.txt"
        const val TZDATA_PREFIX = "# tzdata: "
        const val MAX_REPORTED = 30
    }

}
