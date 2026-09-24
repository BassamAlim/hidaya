package bassamalim.hidaya.core.utils

import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PrayerTimeFormatTest {

    private fun at(hour: Int, minute: Int) = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }

    private fun format(
        hour: Int,
        minute: Int,
        timeFormat: TimeFormat,
        language: Language = Language.ENGLISH,
        numerals: Language = Language.ENGLISH
    ) = PrayerTimeUtils.formatPrayerTime(at(hour, minute), language, numerals, timeFormat)

    @Test
    fun `24 hour times keep their zeros, including past midnight`() {
        assertEquals("00:30", format(0, 30, TimeFormat.TWENTY_FOUR))
        assertEquals("00:05", format(0, 5, TimeFormat.TWENTY_FOUR))
        assertEquals("05:07", format(5, 7, TimeFormat.TWENTY_FOUR))
        assertEquals("19:45", format(19, 45, TimeFormat.TWENTY_FOUR))
    }

    @Test
    fun `12 hour times use the app language's suffix`() {
        assertEquals("12:30 am", format(0, 30, TimeFormat.TWELVE))
        assertEquals("7:18 pm", format(19, 18, TimeFormat.TWELVE))
        assertEquals("7:18 م", format(19, 18, TimeFormat.TWELVE, language = Language.ARABIC))
    }

    @Test
    fun `numerals follow the numerals language`() {
        assertEquals(
            "٠٠:٣٠",
            format(0, 30, TimeFormat.TWENTY_FOUR, numerals = Language.ARABIC)
        )
        assertEquals(
            "٧:١٨ م",
            format(19, 18, TimeFormat.TWELVE, language = Language.ARABIC, numerals = Language.ARABIC)
        )
    }

}
