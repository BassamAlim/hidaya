package bassamalim.hidaya.core.utils

import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.LocationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.enums.TimeFormat
import bassamalim.hidaya.core.helpers.PrayerTimeCalculator
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.models.PrayerTimeCalculatorSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import java.util.SortedMap
import java.util.TimeZone

object PrayerTimeUtils {

    /**
     * Prayer times on [date] at [location], as moments in [zone]. Their wall-clock times are the
     * location's, placed on [zone]'s clock (the device's).
     *
     * So for a manual location in another zone than the device's, athans sound when the device's
     * clock shows the location's prayer time. That's intended: it's right for users in that city
     * whose phone has the wrong zone but a clock set to local time by hand (common after
     * short-notice DST changes the phone's zone data doesn't know about). Firing at the
     * location's real moments instead would only help users following a city they aren't in,
     * who mainly want to see its times, and it would break those users. The mismatch is pointed
     * out to the user instead ([hasZoneMismatch]).
     */
    fun getPrayerTimes(
        settings: PrayerTimeCalculatorSettings,
        selectedTimeZoneId: String,
        location: Location,
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): SortedMap<Prayer, ZonedDateTime?> {
        val locationZone = getLocationZone(location, selectedTimeZoneId)
        // The offset at midday of the date, not at the time of the call: the daily update runs
        // at midnight, and DST switches happen at night, so a midnight sample would apply the
        // pre-switch offset to the whole day, putting every time an hour off on those days
        val utcOffset = date.atTime(LocalTime.NOON).atZone(locationZone).offset

        return PrayerTimeCalculator(settings)
            .getPrayerTimes(location.coordinates, date, utcOffset)
            .mapValuesTo(sortedMapOf<Prayer, ZonedDateTime?>()) { (_, time) ->
                // A time repeated when clocks go back is the later one, as the offset used is
                // midday's, the post-switch one
                time?.let { ZonedDateTime.of(date, it, zone).withLaterOffsetAtOverlap() }
            }
    }

    /**
     * Whether a manual location's UTC offset differs from the device's at [now]. Its times then
     * follow the device's clock (see [getPrayerTimes]), which is only right if the user set that
     * clock to the location's time.
     */
    fun hasZoneMismatch(location: Location, selectedTimeZoneId: String, now: Instant): Boolean {
        if (location.type != LocationType.MANUAL) return false
        val locationOffset = getLocationZone(location, selectedTimeZoneId).rules.getOffset(now)
        return locationOffset != ZoneId.systemDefault().rules.getOffset(now)
    }

    private fun getLocationZone(location: Location, selectedTimeZoneId: String): ZoneId =
        when (location.type) {
            LocationType.AUTO -> ZoneId.systemDefault()
            // Through TimeZone, which falls back to GMT for an unknown id instead of throwing
            LocationType.MANUAL -> ZoneId.of(TimeZone.getTimeZone(selectedTimeZoneId).id)
            LocationType.NONE -> ZoneOffset.UTC
        }

    fun formatPrayerTimes(
        prayerTimes: Map<Prayer, ZonedDateTime?>,
        language: Language,
        numeralsLanguage: Language,
        timeFormat: TimeFormat
    ): SortedMap<Prayer, String> = sortedMapOf<Prayer, String>().apply {
        prayerTimes.forEach { (prayer, time) ->
            put(
                key = prayer,
                value = formatPrayerTime(
                    time = time,
                    language = language,
                    numeralsLanguage = numeralsLanguage,
                    timeFormat = timeFormat
                )
            )
        }
    }

    fun formatPrayerTime(
        time: ZonedDateTime?,
        language: Language,
        numeralsLanguage: Language,
        timeFormat: TimeFormat
    ): String {
        if (time == null) return ""

        val formattedTime = when (timeFormat) {
            TimeFormat.TWENTY_FOUR -> {
                val hour = String.format(Locale.ENGLISH, "%02d", time.hour)
                val minute = String.format(Locale.ENGLISH, "%02d", time.minute)
                "$hour:$minute"
            }
            TimeFormat.TWELVE -> {
                var hour = time.hour
                val suffix = when (language) {
                    Language.ENGLISH -> { if (hour >= 12) "pm" else "am" }
                    Language.ARABIC -> { if (hour >= 12) "م" else "ص" }
                }
                hour = (hour + 12 - 1) % 12 + 1
                val minute = time.minute

                val formattedMinute = String.format(locale = Locale.ENGLISH, "%02d", minute)
                "$hour:$formattedMinute $suffix"
            }
        }

        // Zero-stripping is for durations: it turns a 24-hour "00:30" into "30"
        return LangUtils.translateTimeNums(
            language = language,
            numeralsLanguage = numeralsLanguage,
            string = formattedTime,
            removeLeadingZeros = false
        )
    }

}