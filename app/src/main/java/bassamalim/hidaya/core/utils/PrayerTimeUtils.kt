package bassamalim.hidaya.core.utils

import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.LocationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.enums.TimeFormat
import bassamalim.hidaya.core.helpers.PrayerTimeCalculator
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.models.PrayerTimeCalculatorSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
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
        val date = LocalDate.of(
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH] + 1,
            calendar[Calendar.DAY_OF_MONTH]
        )
        val zone = when (location.type) {
            LocationType.AUTO -> ZoneId.systemDefault()
            // Through TimeZone, which falls back to GMT for an unknown id instead of throwing
            LocationType.MANUAL -> ZoneId.of(TimeZone.getTimeZone(selectedTimeZoneId).id)
            LocationType.NONE -> ZoneOffset.UTC
        }
        // The offset at midday of the date, not at the calendar's time of day: the daily update
        // runs at midnight, and DST switches happen at night, so a midnight sample would apply
        // the pre-switch offset to the whole day, putting every time an hour off on those days
        val utcOffset = date.atTime(LocalTime.NOON).atZone(zone).offset

        val times = PrayerTimeCalculator(settings)
            .getPrayerTimes(location.coordinates, date, utcOffset)

        // As wall-clock times of the calendar's own zone
        val calendarZone = ZoneId.of(calendar.timeZone.id)
        return times.mapValuesTo(sortedMapOf<Prayer, Calendar?>()) { (_, time) ->
            time?.let {
                (calendar.clone() as Calendar).apply {
                    timeInMillis =
                        ZonedDateTime.of(date, it, calendarZone).toInstant().toEpochMilli()
                }
            }
        }
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