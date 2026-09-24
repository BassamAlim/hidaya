package bassamalim.hidaya.core.utils

import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.LocationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.enums.TimeFormat
import bassamalim.hidaya.core.helpers.PrayerTimeCalculator
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.models.PrayerTimeCalculatorSettings
import java.util.Calendar
import java.util.Locale
import java.util.SortedMap
import java.util.TimeZone

object PrayerTimeUtils {

    fun getPrayerTimes(
        settings: PrayerTimeCalculatorSettings,
        selectedTimeZoneId: String = "",
        location: Location,
        calendar: Calendar = Calendar.getInstance()
    ): SortedMap<Prayer, Calendar?> {
        // Sample the UTC offset at midday of the target date, not at the calendar's own
        // time-of-day: the daily update runs at midnight, and DST transitions happen between
        // 00:00 and 04:00, so a midnight sample would apply the pre-transition offset to the
        // whole day, putting every prayer time and athan alarm off by an hour on those days.
        val midday = (calendar.clone() as Calendar).apply {
            this[Calendar.HOUR_OF_DAY] = 12
            this[Calendar.MINUTE] = 0
        }
        calendar[Calendar.ZONE_OFFSET] = getZoneOffset(
            locationType = location.type,
            date = midday.timeInMillis,
            selectedTimeZone = selectedTimeZoneId
        )
        return PrayerTimeCalculator(settings).getPrayerTimes(location.coordinates, calendar)
    }

    fun formatPrayerTimes(
        prayerTimes: SortedMap<Prayer, Calendar?>,
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

    private fun getZoneOffset(
        locationType: LocationType,
        date: Long,
        selectedTimeZone: String = ""
    ) = when (locationType) {
        LocationType.AUTO -> TimeZone.getDefault().getOffset(date)
        LocationType.MANUAL -> TimeZone.getTimeZone(selectedTimeZone).getOffset(date)
        LocationType.NONE -> 0
    }

    fun formatPrayerTime(
        time: Calendar?,
        language: Language,
        numeralsLanguage: Language,
        timeFormat: TimeFormat
    ): String {
        if (time == null) return ""

        val formattedTime = when (timeFormat) {
            TimeFormat.TWENTY_FOUR -> {
                val hour = String.format(Locale.ENGLISH, "%02d", time[Calendar.HOUR_OF_DAY])
                val minute = String.format(Locale.ENGLISH, "%02d", time[Calendar.MINUTE])
                "$hour:$minute"
            }
            TimeFormat.TWELVE -> {
                var hour = time[Calendar.HOUR_OF_DAY]
                val suffix = when (language) {
                    Language.ENGLISH -> { if (hour >= 12) "pm" else "am" }
                    Language.ARABIC -> { if (hour >= 12) "م" else "ص" }
                }
                hour = (hour + 12 - 1) % 12 + 1
                val minute = time[Calendar.MINUTE]

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