package bassamalim.hidaya.features.recitations.player

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatPlaybackTimeTest {

    @Test
    fun `under an hour shows minutes and seconds`() {
        assertEquals("00:00", formatPlaybackTime(0))
        assertEquals("03:05", formatPlaybackTime((3 * 60 + 5) * 1000L))
        assertEquals("59:59", formatPlaybackTime((59 * 60 + 59) * 1000L))
    }

    @Test
    fun `from an hour on shows hours`() {
        assertEquals("1:00:00", formatPlaybackTime(60 * 60 * 1000L))
        assertEquals("2:03:04", formatPlaybackTime(((2 * 60 + 3) * 60 + 4) * 1000L))
    }

}
