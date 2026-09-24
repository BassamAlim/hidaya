package bassamalim.hidaya.features.quran.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.Verse
import bassamalim.hidaya.core.data.dataSources.room.entities.Verse as VerseEntity

/**
 * Turns a mushaf page's verses into the sections the reader draws: sura headers, basmalahs and
 * the verses, each verse tagged with its id so taps can find it.
 */
class QuranPageBuilder(
    private val allVerses: List<VerseEntity>,
    private val suraNames: List<String>,
    private val language: Language
) {

    fun getPageVerses(pageNumber: Int) =
        allVerses.filter { it.pageNum == pageNumber }.map(::toModel)

    /** Page view: the verses between sura starts run together as one text block. */
    fun buildPage(
        pageNumber: Int,
        selectedVerseId: Int?,
        trackedVerseId: Int,
        defaultVerseColor: Color,
        selectedVerseColor: Color,
        trackedVerseColor: Color
    ): List<Section> {
        val sections = mutableListOf<Section>()
        val pendingVerses = mutableListOf<Verse>()

        fun flushVerses() {
            if (pendingVerses.isEmpty()) return

            sections.add(
                VersesSection(
                    suraNum = pendingVerses.last().suraNum,
                    annotatedString = versesToAnnotatedString(
                        verses = pendingVerses,
                        selectedVerseId = selectedVerseId,
                        trackedVerseId = trackedVerseId,
                        defaultVerseColor = defaultVerseColor,
                        selectedVerseColor = selectedVerseColor,
                        trackedVerseColor = trackedVerseColor
                    ),
                    numOfLines =
                        pendingVerses.last().endLineNum - pendingVerses.first().startLineNum + 1
                )
            )
            pendingVerses.clear()
        }

        for (verse in pageVerseEntities(pageNumber)) {
            if (verse.num == 1) {
                flushVerses()
                addSuraStart(sections, verse)
            }
            pendingVerses.add(toModel(verse))
        }
        flushVerses()

        return sections
    }

    /** List view: each verse is its own row, with its translation. */
    fun buildListPage(
        pageNumber: Int,
        selectedVerseId: Int?,
        trackedVerseId: Int,
        defaultVerseColor: Color,
        selectedVerseColor: Color,
        trackedVerseColor: Color
    ): List<Section> {
        val sections = mutableListOf<Section>()

        for (verse in pageVerseEntities(pageNumber)) {
            if (verse.num == 1) addSuraStart(sections, verse)

            sections.add(
                ListVerse(
                    id = verse.id,
                    text = versesToAnnotatedString(
                        verses = listOf(toModel(verse)),
                        selectedVerseId = selectedVerseId,
                        trackedVerseId = trackedVerseId,
                        defaultVerseColor = defaultVerseColor,
                        selectedVerseColor = selectedVerseColor,
                        trackedVerseColor = trackedVerseColor
                    ),
                    translation = verse.translationEn
                )
            )
        }

        return sections
    }

    /** The page's verses, which are contiguous since verses are ordered by page. */
    private fun pageVerseEntities(pageNumber: Int): List<VerseEntity> {
        val start = allVerses.indexOfFirst { it.pageNum == pageNumber }
        if (start == -1) return emptyList()

        var end = start
        while (end + 1 < allVerses.size && allVerses[end + 1].pageNum == pageNumber) end++
        return allVerses.subList(start, end + 1)
    }

    private fun addSuraStart(sections: MutableList<Section>, firstVerse: VerseEntity) {
        sections.add(
            SuraHeaderSection(
                suraNum = firstVerse.suraNum,
                suraName = suraNames[firstVerse.suraNum - 1]
            )
        )

        // Al-Fatiha's basmalah is its first verse, and At-Tawbah has none
        if (firstVerse.suraNum != 1 && firstVerse.suraNum != 9)
            sections.add(BasmalahSection())
    }

    private fun toModel(verse: VerseEntity) = Verse(
        id = verse.id,
        juzNum = verse.juzNum,
        suraNum = verse.suraNum,
        suraName = suraNames[verse.suraNum - 1],
        num = verse.num,
        text = "${verse.decoratedText} ",
        startLineNum = verse.startLineNum,
        endLineNum = verse.endLineNum,
        translation = verse.translationEn,
        interpretation = verse.interpretation
    )

    private fun versesToAnnotatedString(
        verses: List<Verse>,
        selectedVerseId: Int?,
        trackedVerseId: Int,
        defaultVerseColor: Color,
        selectedVerseColor: Color,
        trackedVerseColor: Color
    ): AnnotatedString {
        return buildAnnotatedString {
            for (verse in verses) {
                val text = verse.text.orEmpty()
                val color = when (verse.id) {
                    selectedVerseId -> selectedVerseColor
                    trackedVerseId -> trackedVerseColor
                    else -> defaultVerseColor
                }

                pushStringAnnotation(tag = verse.id.toString(), annotation = verse.id.toString())
                withStyle(style = SpanStyle(color = color)) {
                    append(
                        when (language) {
                            Language.ARABIC -> text
                            Language.ENGLISH -> text.reversed()
                        }
                    )
                }
                pop()
            }
        }
    }

}
