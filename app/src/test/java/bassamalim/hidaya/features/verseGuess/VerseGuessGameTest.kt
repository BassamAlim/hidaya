package bassamalim.hidaya.features.verseGuess

import bassamalim.hidaya.features.verseGuess.map.VerseMapItem
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Picking clips and scoring guesses. */
class VerseGuessGameTest {

    private fun verse(sura: Int, page: Int) = VerseMapItem(
        id = 0,
        suraNum = sura,
        verseNum = 0,
        pageNum = page,
        juzNum = 1,
        textLength = 0,
        verseNumText = "",
        pageNumText = "",
        juzNumText = ""
    )

    @Test
    fun `a verse's position is its page plus how far into the page it is`() {
        val items = listOf(verse(1, 1), verse(1, 1), verse(1, 2), verse(1, 2), verse(1, 2), verse(1, 2))

        assertArrayEquals(
            doubleArrayOf(0.0, 0.5, 1.0, 1.25, 1.5, 1.75),
            pagePositions(items),
            1e-9
        )
    }

    // Suras of 3, 1, 5, 2 and 8 verses
    private val mixedSuras = listOf(3 to 1, 1 to 2, 5 to 3, 2 to 4, 8 to 5)
        .flatMap { (count, sura) -> List(count) { verse(sura, 1) } }

    @Test
    fun `clips stay inside one sura`() {
        repeat(50) { seed ->
            val start = pickClipStart(mixedSuras, Random(seed), recent = emptyList())!!

            assertTrue((start until start + CLIP_LENGTH).all { mixedSuras[it].suraNum == mixedSuras[start].suraNum })
        }
    }

    @Test
    fun `clips skip recent ones while fresh ones are left`() {
        // Clips start at 0, 4 to 6 and 11 to 16; these recents overlap all but 14 to 16
        val recent = listOf(0, 5, 11)

        repeat(50) { seed ->
            assertTrue(pickClipStart(mixedSuras, Random(seed), recent)!! in 14..16)
        }
    }

    @Test
    fun `with every clip recent, any clip will do`() {
        val items = List(3) { verse(108, 602) }

        assertEquals(0, pickClipStart(items, Random(0), recent = listOf(0)))
    }

    @Test
    fun `nothing is picked when no clip fits`() {
        assertEquals(null, pickClipStart(List(2) { verse(1, 1) }, Random(0), recent = emptyList()))
    }

    @Test
    fun `a guess on the clip is worth full points`() {
        val clip = listOf(10.2, 10.4, 10.6)

        assertEquals(0.0, distanceInPages(10.4, clip), 0.0)
        assertEquals(MAX_ROUND_POINTS, roundPoints(0.0, scopePages = 604))
    }

    @Test
    fun `distance is to the nearest clip verse`() {
        assertEquals(2.0, distanceInPages(12.6, listOf(10.2, 10.4, 10.6)), 1e-9)
    }

    @Test
    fun `a clip verse's numbered marker is split from its text`() {
        // As stored: glyphs joined by right-to-left marks, the marker glyph last
        val verse = ClipVerse.fromDecoratedText("‏‏ ‏ ‏ ")

        assertEquals("‏‏ ‏", verse.text)
        assertEquals("‏", verse.marker)
    }

    @Test
    fun `points fall with distance, scaled to the scope`() {
        val near = roundPoints(1.0, scopePages = 604)
        val far = roundPoints(30.0, scopePages = 604)

        assertTrue(near in (far + 1) until MAX_ROUND_POINTS)
        // Five pages off is worse within a juz than across the whole Quran
        assertTrue(roundPoints(5.0, scopePages = 20) < roundPoints(5.0, scopePages = 604))
    }

    @Test
    fun `a juz round is worth less than a whole-Quran round`() {
        assertEquals(MAX_ROUND_POINTS, roundMaxPoints(scopePages = 604))
        assertEquals(910, roundMaxPoints(scopePages = 20))
        // Replaying a memorized juz perfectly earns less than a so-so whole-Quran guess
        assertTrue(roundPoints(0.0, scopePages = 20) < roundPoints(15.0, scopePages = 604))
    }

}
