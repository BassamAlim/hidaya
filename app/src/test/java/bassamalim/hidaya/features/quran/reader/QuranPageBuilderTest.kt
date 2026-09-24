package bassamalim.hidaya.features.quran.reader

import androidx.compose.ui.graphics.Color
import bassamalim.hidaya.core.data.dataSources.room.entities.Verse
import bassamalim.hidaya.core.enums.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** How a mushaf page becomes the reader's sections. */
class QuranPageBuilderTest {

    private var nextId = 1
    private fun verse(suraNum: Int, num: Int, pageNum: Int, line: Int) = Verse(
        id = nextId++, num = num, juzNum = 1, suraNum = suraNum, pageNum = pageNum,
        decoratedText = "v$suraNum:$num", plainText = "", startLineNum = line, endLineNum = line,
        translationEn = "t$suraNum:$num", interpretation = ""
    )

    // Page 1: all of sura 1. Page 2: end of sura 8, then sura 9 (no basmalah) and sura 10
    private val verses = listOf(
        verse(suraNum = 1, num = 1, pageNum = 1, line = 1),
        verse(suraNum = 1, num = 2, pageNum = 1, line = 2),
        verse(suraNum = 8, num = 75, pageNum = 2, line = 1),
        verse(suraNum = 9, num = 1, pageNum = 2, line = 3),
        verse(suraNum = 9, num = 2, pageNum = 2, line = 4),
        verse(suraNum = 10, num = 1, pageNum = 2, line = 6)
    )
    private val suraNames = (1..114).map { "Sura $it" }

    private fun builder(language: Language = Language.ARABIC) =
        QuranPageBuilder(verses, suraNames, language)

    private fun QuranPageBuilder.page(num: Int, selected: Int? = null, tracked: Int = -1) =
        buildPage(num, selected, tracked, Color.Black, Color.Red, Color.Blue)

    @Test
    fun `a page opening a sura starts with its header, and Al-Fatiha has no basmalah`() {
        val sections = builder().page(1)

        assertEquals(2, sections.size)
        assertEquals(SuraHeaderSection(suraNum = 1, suraName = "Sura 1"), sections[0])
        val verses = sections[1] as VersesSection
        assertEquals("v1:1 v1:2 ", verses.annotatedString.text)
        assertEquals(2, verses.numOfLines)
    }

    @Test
    fun `suras starting mid-page split the text, with a basmalah except for At-Tawbah`() {
        val sections = builder().page(2)

        assertEquals(6, sections.size)
        assertEquals(8, (sections[0] as VersesSection).suraNum)
        assertEquals(SuraHeaderSection(suraNum = 9, suraName = "Sura 9"), sections[1])
        assertEquals("v9:1 v9:2 ", (sections[2] as VersesSection).annotatedString.text)
        assertEquals(SuraHeaderSection(suraNum = 10, suraName = "Sura 10"), sections[3])
        assertTrue(sections[4] is BasmalahSection)
        assertEquals("v10:1 ", (sections[5] as VersesSection).annotatedString.text)
    }

    @Test
    fun `each verse is tagged with its id and colored by selection, then tracking`() {
        val text = (builder().page(1, selected = 1, tracked = 2)[1] as VersesSection)
            .annotatedString

        assertEquals(listOf("1", "2"), text.getStringAnnotations(0, text.length).map { it.tag })
        assertEquals(listOf(Color.Red, Color.Blue), text.spanStyles.map { it.item.color })
    }

    @Test
    fun `english reverses the verse text`() {
        val text = (builder(Language.ENGLISH).page(1)[1] as VersesSection).annotatedString.text

        assertEquals(" 1:1v 2:1v", text)
    }

    @Test
    fun `list view has one row per verse with its translation`() {
        val sections = builder().buildListPage(
            1, null, -1, Color.Black, Color.Red, Color.Blue
        )

        assertEquals(3, sections.size)
        val rows = sections.filterIsInstance<ListVerse>()
        assertEquals(listOf(1, 2), rows.map { it.id })
        assertEquals(listOf("t1:1", "t1:2"), rows.map { it.translation })
    }

    @Test
    fun `page verses carry their sura name`() {
        val pageVerses = builder().getPageVerses(2)

        assertEquals(listOf(3, 4, 5, 6), pageVerses.map { it.id })
        assertEquals("Sura 8", pageVerses.first().suraName)
    }

    @Test
    fun `a page without verses is empty`() {
        assertEquals(emptyList<Section>(), builder().page(99))
    }

}
