package bassamalim.hidaya.features.verseGuess.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** How verses wrap into map lines. Every box is 10 wide, markers 4, gaps 1. */
class VerseMapLayoutTest {

    private fun verse(sura: Int, num: Int, page: Int) = VerseMapItem(
        id = 0,
        suraNum = sura,
        verseNum = num,
        pageNum = page,
        juzNum = 1,
        textLength = 0,
        verseNumText = "$num",
        pageNumText = "$page",
        juzNumText = "1"
    )

    private fun layout(items: List<VerseMapItem>, lineWidth: Float = 100f) =
        layoutVerseMap(items, lineWidth, gap = 1f, markerWidth = 4f) { 10f }

    private val VerseMapRow.Line.verseIndices
        get() = cells.filterIsInstance<VerseMapCell.VerseBox>().map { it.index }

    @Test
    fun `each sura starts a new line under its header`() {
        val rows = layout(listOf(verse(1, 1, 1), verse(1, 2, 1), verse(2, 1, 1)))

        assertEquals(4, rows.size)
        assertEquals(VerseMapRow.SuraHeader(1), rows[0])
        assertEquals(listOf(0, 1), (rows[1] as VerseMapRow.Line).verseIndices)
        assertEquals(VerseMapRow.SuraHeader(2), rows[2])
        assertEquals(listOf(2), (rows[3] as VerseMapRow.Line).verseIndices)
    }

    @Test
    fun `a page marker comes right before the page's first verse`() {
        val cells = (layout(listOf(verse(1, 1, 1), verse(1, 2, 2)))[1] as VerseMapRow.Line).cells

        assertEquals(
            listOf("1", null, "2", null),
            cells.map { (it as? VerseMapCell.PageMarker)?.pageNumText }
        )
    }

    @Test
    fun `lines wrap without overflowing`() {
        val rows = layout(List(20) { verse(1, it + 1, 1) }, lineWidth = 50f)
        val lines = rows.filterIsInstance<VerseMapRow.Line>()

        assertEquals(20, lines.sumOf { it.verseIndices.size })
        lines.forEach { line ->
            assertTrue(line.cells.all { it.start + it.width <= 50f })
        }
    }

    @Test
    fun `a page marker moves to the next line with its verse`() {
        // The first line fills up to 38 of 45: the next marker alone would fit, but not with its verse
        val rows = layout(
            listOf(verse(1, 1, 1), verse(1, 2, 1), verse(1, 3, 1), verse(1, 4, 2)),
            lineWidth = 45f
        )
        val secondLine = rows[2] as VerseMapRow.Line

        assertTrue(secondLine.cells.first() is VerseMapCell.PageMarker)
        assertEquals(listOf(3), secondLine.verseIndices)
    }

    @Test
    fun `the nearest verse is picked by box center`() {
        val line = layout(listOf(verse(1, 1, 1), verse(1, 2, 1)))[1] as VerseMapRow.Line
        // Marker 0..4, box 0 at 5..15, box 1 at 16..26

        assertEquals(0, line.nearestVerse(0f))
        assertEquals(0, line.nearestVerse(15f))
        assertEquals(1, line.nearestVerse(17f))
        assertEquals(1, line.nearestVerse(90f))
    }

    @Test
    fun `each row maps to its first verse, a header to its sura's first`() {
        // Header 1, [0, 1], [2], header 2, [3]
        val rows = layout(
            listOf(verse(1, 1, 1), verse(1, 2, 1), verse(1, 3, 1), verse(2, 1, 1)),
            lineWidth = 30f
        )

        assertEquals(listOf(0, 0, 2, 3, 3), rows.firstVerseIndices().toList())
    }

    @Test
    fun `each verse knows its row`() {
        // Header 1, [0, 1], [2], header 2, [3]
        val rows = layout(
            listOf(verse(1, 1, 1), verse(1, 2, 1), verse(1, 3, 1), verse(2, 1, 1)),
            lineWidth = 30f
        )

        assertEquals(listOf(1, 1, 2, 4), rows.verseRowIndices(4).toList())
    }

}
