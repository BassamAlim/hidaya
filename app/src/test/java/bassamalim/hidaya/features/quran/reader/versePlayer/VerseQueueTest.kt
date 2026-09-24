package bassamalim.hidaya.features.quran.reader.versePlayer

import androidx.media3.common.Player
import bassamalim.hidaya.core.data.dataSources.room.entities.Verse
import bassamalim.hidaya.core.enums.VerseRepeatMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which verses the verse player queues, and how often it repeats each. */
class VerseQueueTest {

    private fun verse(idx: Int, suraNum: Int, pageNum: Int) = Verse(
        id = idx + 1, num = 1, juzNum = 1, suraNum = suraNum, pageNum = pageNum,
        decoratedText = "", plainText = "", startLineNum = 1, endLineNum = 1,
        translationEn = "", interpretation = ""
    )

    // Sura 1 on page 1 (idx 0-2), sura 2 on page 2 (idx 3-4) and page 3 (idx 5-6)
    private val verses = listOf(
        verse(0, suraNum = 1, pageNum = 1),
        verse(1, suraNum = 1, pageNum = 1),
        verse(2, suraNum = 1, pageNum = 1),
        verse(3, suraNum = 2, pageNum = 2),
        verse(4, suraNum = 2, pageNum = 2),
        verse(5, suraNum = 2, pageNum = 3),
        verse(6, suraNum = 2, pageNum = 3)
    )

    private fun queue(lastIdx: Int, count: Int = 10, page: Boolean = false, sura: Boolean = false) =
        versesToQueue(verses, lastIdx, count, stopOnPageEnd = page, stopOnSuraEnd = sura)

    @Test
    fun `queues the following verses up to count`() {
        assertEquals(listOf(1, 2, 3), queue(lastIdx = 0, count = 3))
    }

    @Test
    fun `stops at the last verse`() {
        assertEquals(listOf(5, 6), queue(lastIdx = 4))
        assertEquals(emptyList<Int>(), queue(lastIdx = 6))
    }

    @Test
    fun `stop on sura end keeps to the current sura`() {
        assertEquals(listOf(1, 2), queue(lastIdx = 0, sura = true))
        assertEquals(listOf(4, 5, 6), queue(lastIdx = 3, sura = true))
    }

    @Test
    fun `stop on page end keeps to the current page`() {
        assertEquals(listOf(4), queue(lastIdx = 3, page = true))
        assertEquals(emptyList<Int>(), queue(lastIdx = 4, page = true))
    }

    @Test
    fun `no repeat never loops`() {
        assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(VerseRepeatMode.NO_REPEAT, 0))
    }

    @Test
    fun `counted repeats loop until the last play`() {
        // THREE plays: loops after the 1st and 2nd, moves on after the 3rd
        assertEquals(Player.REPEAT_MODE_ONE, playerRepeatMode(VerseRepeatMode.THREE, 0))
        assertEquals(Player.REPEAT_MODE_ONE, playerRepeatMode(VerseRepeatMode.THREE, 1))
        assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(VerseRepeatMode.THREE, 2))

        assertEquals(Player.REPEAT_MODE_ONE, playerRepeatMode(VerseRepeatMode.TWO, 0))
        assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(VerseRepeatMode.TWO, 1))

        assertEquals(Player.REPEAT_MODE_ONE, playerRepeatMode(VerseRepeatMode.FIVE, 3))
        assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(VerseRepeatMode.FIVE, 4))
    }

    @Test
    fun `infinite always loops`() {
        assertEquals(Player.REPEAT_MODE_ONE, playerRepeatMode(VerseRepeatMode.INFINITE, 1_000))
    }

}
