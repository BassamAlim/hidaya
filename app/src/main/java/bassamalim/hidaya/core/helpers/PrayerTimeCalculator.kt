package bassamalim.hidaya.core.helpers

import bassamalim.hidaya.core.enums.HighLatitudesAdjustmentMethod
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.enums.Prayer.ASR
import bassamalim.hidaya.core.enums.Prayer.DHUHR
import bassamalim.hidaya.core.enums.Prayer.FAJR
import bassamalim.hidaya.core.enums.Prayer.ISHAA
import bassamalim.hidaya.core.enums.Prayer.MAGHRIB
import bassamalim.hidaya.core.enums.Prayer.SUNRISE
import bassamalim.hidaya.core.enums.Prayer.SUNSET
import bassamalim.hidaya.core.enums.PrayerTimeCalculationMethod
import bassamalim.hidaya.core.enums.PrayerTimeJuristicMethod
import bassamalim.hidaya.core.models.Coordinates
import bassamalim.hidaya.core.models.PrayerTimeCalculatorSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.SortedMap
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class PrayerTimeCalculator(private val settings: PrayerTimeCalculatorSettings) {

    private enum class RoundingMode { NEAREST, FLOOR, CEIL }

    private var asrJuristic =
        if (settings.juristicMethod == PrayerTimeJuristicMethod.HANAFI) 1
        else 0
    private var dhuhrMinutes = 0 // minutes after midday for Dhuhr
    private var numIterations = 1 // number of iterations needed to compute times
    private var horizonDip = 0.0 // degrees of horizon depression from observer elevation

    private val methodParams = hashMapOf(
        PrayerTimeCalculationMethod.MECCA to doubleArrayOf(18.5, 1.0, 0.0, 1.0, 90.0),
        PrayerTimeCalculationMethod.MWL to doubleArrayOf(18.0, 1.0, 0.0, 0.0, 17.0),
        PrayerTimeCalculationMethod.ISNA to doubleArrayOf(15.0, 1.0, 0.0, 0.0, 15.0),
        PrayerTimeCalculationMethod.KARACHI to doubleArrayOf(18.0, 1.0, 0.0, 0.0, 18.0),
        PrayerTimeCalculationMethod.EGYPT to doubleArrayOf(19.5, 1.0, 0.0, 0.0, 17.5),
        PrayerTimeCalculationMethod.TAHRAN to doubleArrayOf(17.7, 0.0, 4.5, 0.0, 14.0),
        PrayerTimeCalculationMethod.JORDAN to doubleArrayOf(18.0, 1.0, 5.0, 0.0, 18.0)
    )

    // -------------------- Interface Functions --------------------
    /**
     * Wall-clock prayer times on [date] at a place [utcOffset] ahead of UTC. A time past midnight
     * wraps to the small hours of the same [date]. Null where the sun never reaches the angle.
     */
    fun getPrayerTimes(
        coordinates: Coordinates,
        date: LocalDate,
        utcOffset: ZoneOffset
    ): SortedMap<Prayer, LocalTime?> {
        // Horizon dip from observer elevation: ~0.0347 * sqrt(h_m) degrees.
        // Applied to sunrise only — matches awqaf.gov.jo, which displays an elevation-shifted
        // sunrise but uses geometric sunset for the Maghrib offset.
        horizonDip =
            if (coordinates.elevation > 0.0) 0.0347 * sqrt(coordinates.elevation)
            else 0.0

        val times = computeDayTimes(
            coordinates = coordinates,
            utcOffset = utcOffset.totalSeconds / 3600.0,
            jDate = getJulianDate(date = date, longitude = coordinates.longitude)
        )

        return sortedMapOf(
            FAJR to toLocalTime(times[FAJR]!!, roundingFor(FAJR)),
            SUNRISE to toLocalTime(times[SUNRISE]!!, roundingFor(SUNRISE)),
            DHUHR to toLocalTime(times[DHUHR]!!, roundingFor(DHUHR)),
            ASR to toLocalTime(times[ASR]!!, roundingFor(ASR)),
            // skipping sunset time
            MAGHRIB to toLocalTime(times[MAGHRIB]!!, roundingFor(MAGHRIB)),
            ISHAA to toLocalTime(times[ISHAA]!!, roundingFor(ISHAA))
        )
    }

    // Awqaf-style safety rounding for Jordan: Fajr/Sunrise floor (earlier end-of-Fajr safety),
    // Asr/Maghrib/Isha ceil (wait-for-actual-event safety). Other methods stay round-to-nearest.
    private fun roundingFor(prayer: Prayer): RoundingMode {
        if (settings.calculationMethod != PrayerTimeCalculationMethod.JORDAN)
            return RoundingMode.NEAREST
        return when (prayer) {
            FAJR, SUNRISE -> RoundingMode.FLOOR
            ASR, MAGHRIB, ISHAA -> RoundingMode.CEIL
            else -> RoundingMode.NEAREST
        }
    }

    private fun toLocalTime(time: Double, mode: RoundingMode): LocalTime? {
        if (time.isNaN()) return null
        val rawMinutes = fixHour(time) * 60.0
        val totalMinutes = when (mode) {
            RoundingMode.NEAREST -> round(rawMinutes).toInt()
            RoundingMode.FLOOR -> floor(rawMinutes).toInt()
            RoundingMode.CEIL -> ceil(rawMinutes).toInt()
        }
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60

        return LocalTime.of(hours, minutes)
    }

    // ---------------------- Julian Date Functions -----------------------
    // calculate julian date from a calendar date
    private fun getJulianDate(date: LocalDate, longitude: Double): Double {
        var year = date.year
        var month = date.monthValue
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val day = date.dayOfMonth

        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)
        var jDate = (
                floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + day + b
                ) - 1524.5
        jDate -= longitude / (15.0 * 24.0)
        return jDate
    }

    // compute prayer times at given julian date
    private fun computeDayTimes(
        coordinates: Coordinates,
        utcOffset: Double,
        jDate: Double
    ): SortedMap<Prayer, Double> {
        var times = sortedMapOf(
            FAJR to 5.0,
            SUNRISE to 6.0,
            DHUHR to 12.0,
            ASR to 13.0,
            SUNSET to 18.0,
            MAGHRIB to 18.0,
            ISHAA to 18.0
        )  // default times
        for (i in 1..numIterations)
            times = computeTimes(times = times, latitude = coordinates.latitude, jDate = jDate)
        times = adjustTimes(times = times, longitude = coordinates.longitude, utcOffset = utcOffset)
        return times
    }

    // compute prayer times at given julian date
    private fun computeTimes(
        times: SortedMap<Prayer, Double>,
        latitude: Double,
        jDate: Double
    ): SortedMap<Prayer, Double> {
        val t = dayPortion(times)
        return sortedMapOf(
            FAJR to computeTime(
                g = 180 - (methodParams[settings.calculationMethod]!![0]),
                t = t[FAJR]!!,
                latitude = latitude,
                jDate = jDate
            ),
            SUNRISE to computeTime(
                g = 180 - (0.833 + horizonDip),
                t = t[SUNRISE]!!,
                latitude = latitude,
                jDate = jDate
            ),
            DHUHR to computeMidDay(gT = t[DHUHR]!!, jDate = jDate),
            ASR to computeAsr(
                step = (1 + asrJuristic).toDouble(),
                t = t[ASR]!!,
                latitude = latitude,
                jDate = jDate
            ),
            SUNSET to computeTime(
                // Sunrise gets the elevation dip; sunset doesn't. Empirically, awqaf.gov.jo's
                // Maghrib lines up with geometric sunset + the documented 5-min offset, with
                // no extra dip — applying the dip on this side double-counts and pushes
                // Maghrib several minutes late.
                g = 0.833,
                t = t[SUNSET]!!,
                latitude = latitude,
                jDate = jDate
            ),
            MAGHRIB to computeTime(
                g = methodParams[settings.calculationMethod]!![2],
                t = t[MAGHRIB]!!,
                latitude = latitude,
                jDate = jDate
            ),
            ISHAA to computeTime(
                g = methodParams[settings.calculationMethod]!![4],
                t = t[ISHAA]!!,
                latitude = latitude,
                jDate = jDate
            )
        )
    }

    // compute the time of Asr
    // Shafii: step=1, Hanafi: step=2
    private fun computeAsr(step: Double, t: Double, latitude: Double, jDate: Double): Double {
        val d = sunDeclination(jDate + t)
        val g = -degreeArcCot(step + degreeTan(abs(latitude - d)))
        return computeTime(g, t, latitude, jDate)
    }

    // compute time for a given angle G
    // Error here in some cases such as poland (v = NaN)
    private fun computeTime(g: Double, t: Double, latitude: Double, jDate: Double): Double {
        val d = sunDeclination(jDate + t)
        val z = computeMidDay(gT = t, jDate = jDate)
        val beg = -degreeSin(g) - degreeSin(d) * degreeSin(latitude)
        val mid = degreeCos(d) * degreeCos(latitude)
        val v = degreeArcCos(beg / mid) / 15.0
        return z + if (g > 90) -v else v
    }

    // compute declination angle of sun
    private fun sunDeclination(jd: Double) = sunPosition(jd)[0]

    // compute mid-day (Dhuhr, Zawal) time
    private fun computeMidDay(gT: Double, jDate: Double): Double {
        val t = equationOfTime(jDate + gT)
        return fixHour(12 - t)
    }

    // compute equation of time
    private fun equationOfTime(jd: Double) = sunPosition(jd)[1]

    // compute declination angle of sun and equation of time
    private fun sunPosition(jd: Double): DoubleArray {
        val dd = jd - 2451545
        val g = fixAngle(357.529 + 0.98560028 * dd)
        val q = fixAngle(280.459 + 0.98564736 * dd)
        val l = fixAngle(q + 1.915 * degreeSin(g) + 0.020 * degreeSin(2 * g))

        // double R = 1.00014 - 0.01671 * [self dCos:g] - 0.00014 * [self dCos:
        // (2*g)];
        val e = 23.439 - 0.00000036 * dd
        val d = degreeArcSin(degreeSin(e) * degreeSin(l))
        var ra = degreeArcTan2(degreeCos(e) * degreeSin(l), degreeCos(l)) / 15.0
        ra = fixHour(ra)
        val eqT = q / 15.0 - ra
        val sPosition = DoubleArray(2)
        sPosition[0] = d
        sPosition[1] = eqT
        return sPosition
    }

    // adjust times in a prayer time array
    private fun adjustTimes(
        times: SortedMap<Prayer, Double>,
        longitude: Double,
        utcOffset: Double
    ): SortedMap<Prayer, Double> {
        val adjustedTimes = sortedMapOf<Prayer, Double>().apply {
            for ((point, time) in times) {
                put(point, time + utcOffset - longitude / 15)
            }
        }

        adjustedTimes[DHUHR] = adjustedTimes[DHUHR]!! + dhuhrMinutes / 60.0 // Dhuhr
        if (methodParams[settings.calculationMethod]?.get(1)?.toInt() == 1) // Maghrib
            adjustedTimes[MAGHRIB] = adjustedTimes[SUNSET]!! +
                    (methodParams[settings.calculationMethod]?.get(2)!!) / 60
        if (methodParams[settings.calculationMethod]?.get(3)?.toInt() == 1) // ishaa
            adjustedTimes[ISHAA] = adjustedTimes[MAGHRIB]!! +
                    (methodParams[settings.calculationMethod]?.get(4)!!) / 60
        if (settings.highLatitudesAdjustmentMethod != HighLatitudesAdjustmentMethod.NONE)
            adjustHighLatTimes(adjustedTimes)

        return adjustedTimes
    }

    // adjust Fajr, ishaa and Maghrib for locations in higher latitudes
    private fun adjustHighLatTimes(times: SortedMap<Prayer, Double>): SortedMap<Prayer, Double> {
        val nightTime = timeDiff(times[SUNSET]!!, times[SUNRISE]!!) // sunset to sunrise

        // Adjust Fajr
        val fajrDiff = nightPortion(methodParams[settings.calculationMethod]?.get(0)!!) * nightTime
        if (java.lang.Double.isNaN(times[FAJR]!!)
            || timeDiff(times[FAJR]!!, times[SUNRISE]!!) > fajrDiff)
            times[FAJR] = times[SUNRISE]!! - fajrDiff

        // Adjust ishaa
        val ishaaAngle =
            if (methodParams[settings.calculationMethod]?.get(3)?.toInt() == 0)
                methodParams[settings.calculationMethod]?.get(4)!!.toDouble()
            else 18.0

        val ishaaDiff = nightPortion(ishaaAngle) * nightTime
        if (java.lang.Double.isNaN(times[ISHAA]!!)
            || timeDiff(times[SUNSET]!!, times[ISHAA]!!) > ishaaDiff)
            times[ISHAA] = times[SUNSET]!! + ishaaDiff

        // Adjust Maghrib
        val maghribAngle =
            if (methodParams[settings.calculationMethod]?.get(1)?.toInt() == 0)
                methodParams[settings.calculationMethod]?.get(2)!!.toDouble()
            else 4.0

        val maghribDiff = nightPortion(maghribAngle) * nightTime
        if (java.lang.Double.isNaN(times[MAGHRIB]!!)
            || timeDiff(times[SUNSET]!!, times[MAGHRIB]!!) > maghribDiff)
            times[MAGHRIB] = times[SUNSET]!! + maghribDiff

        return times
    }

    // compute the difference between two times
    private fun timeDiff(time1: Double, time2: Double) =
        fixHour(time2 - time1)

    // convert hours to day portions
    private fun dayPortion(times: SortedMap<Prayer, Double>) =
        sortedMapOf<Prayer, Double>().apply {
            for ((point, time) in times) {
                put(point, time / 24.0)
            }
        }

    // the night portion used for adjusting times in higher latitudes
    private fun nightPortion(angle: Double): Double {
        return when (settings.highLatitudesAdjustmentMethod) {
            HighLatitudesAdjustmentMethod.MIDNIGHT -> 0.5
            HighLatitudesAdjustmentMethod.ONE_SEVENTH -> 0.14286
            HighLatitudesAdjustmentMethod.ANGLE_BASED -> angle / 60.0
            else -> 0.0
        }
    }

    // ---------------------- Trigonometric Functions -----------------------

    // range reduce angle in degrees.
    private fun fixAngle(gA: Double): Double {
        var a = gA
        a -= 360 * floor(a / 360.0)
        a = if (a < 0) a + 360 else a
        return a
    }

    // range reduce hours to 0..23
    private fun fixHour(gA: Double): Double {
        var a = gA
        a -= 24.0 * floor(a / 24.0)
        a = if (a < 0) a + 24 else a
        return a
    }

    private fun degreeSin(d: Double) = sin(degreesToRadians(d))

    private fun degreeCos(d: Double) = cos(degreesToRadians(d))

    private fun degreeTan(d: Double) = tan(degreesToRadians(d))

    private fun degreeArcSin(x: Double) = radiansToDegrees(asin(x))

    private fun degreeArcCos(x: Double) = radiansToDegrees(acos(x))

    private fun degreeArcTan2(y: Double, x: Double) = radiansToDegrees(atan2(y, x))

    private fun degreeArcCot(x: Double) = radiansToDegrees(atan2(1.0, x))

    private fun radiansToDegrees(alpha: Double) = alpha * 180.0 / Math.PI

    private fun degreesToRadians(alpha: Double) = alpha * Math.PI / 180.0

}