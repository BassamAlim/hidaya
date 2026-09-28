package bassamalim.hidaya.features.verseGuess.map

import kotlin.math.abs

/** A verse as the map shows it. The texts are already in the user's numerals. */
data class VerseMapItem(
    val suraNum: Int,
    val verseNum: Int,
    val pageNum: Int,
    val juzNum: Int,
    val textLength: Int,
    val verseNumText: String,
    val pageNumText: String,
    val juzNumText: String
)

sealed interface VerseMapRow {
    data class SuraHeader(val suraNum: Int): VerseMapRow
    data class Line(val cells: List<VerseMapCell>): VerseMapRow
}

/** [start] is measured from the line's start edge, which is the right edge since the map is RTL. */
sealed interface VerseMapCell {
    val start: Float
    val width: Float

    data class PageMarker(
        override val start: Float,
        override val width: Float,
        val pageNumText: String
    ): VerseMapCell

    /** [index] points into the item list the rows were laid out from. */
    data class VerseBox(
        override val start: Float,
        override val width: Float,
        val index: Int
    ): VerseMapCell
}

/**
 * Wraps [items] into lines of [lineWidth], like text. Every sura starts a new line under its
 * header, and each page change puts a marker right before the page's first verse, on that
 * verse's line.
 */
fun layoutVerseMap(
    items: List<VerseMapItem>,
    lineWidth: Float,
    gap: Float,
    markerWidth: Float,
    boxWidth: (VerseMapItem) -> Float
): List<VerseMapRow> {
    val rows = mutableListOf<VerseMapRow>()
    var cells = mutableListOf<VerseMapCell>()
    var x = 0f

    fun breakLine() {
        if (cells.isNotEmpty()) rows.add(VerseMapRow.Line(cells))
        cells = mutableListOf()
        x = 0f
    }

    items.forEachIndexed { index, item ->
        val previous = items.getOrNull(index - 1)
        if (item.suraNum != previous?.suraNum) {
            breakLine()
            rows.add(VerseMapRow.SuraHeader(item.suraNum))
        }

        val startsPage = item.pageNum != previous?.pageNum
        val width = boxWidth(item).coerceAtMost(lineWidth)
        val needed = width + if (startsPage) markerWidth + gap else 0f
        if (x + needed > lineWidth) breakLine()

        if (startsPage) {
            cells.add(VerseMapCell.PageMarker(x, markerWidth, item.pageNumText))
            x += markerWidth + gap
        }
        cells.add(VerseMapCell.VerseBox(x, width, index))
        x += width + gap
    }
    breakLine()

    return rows
}

/** The verse whose box center is closest to [position], measured like [VerseMapCell.start]. */
fun VerseMapRow.Line.nearestVerse(position: Float): Int? =
    cells.filterIsInstance<VerseMapCell.VerseBox>()
        .minByOrNull { abs(it.start + it.width / 2 - position) }
        ?.index

/** For each row, the first verse at or after it. A sura header gets its sura's first verse. */
fun List<VerseMapRow>.firstVerseIndices(): IntArray {
    val result = IntArray(size)
    var next = 0
    for (i in indices.reversed()) {
        (this[i] as? VerseMapRow.Line)?.cells
            ?.firstNotNullOfOrNull { it as? VerseMapCell.VerseBox }
            ?.let { next = it.index }
        result[i] = next
    }
    return result
}

/** For each verse, the row it's on. */
fun List<VerseMapRow>.verseRowIndices(verseCount: Int): IntArray {
    val result = IntArray(verseCount)
    forEachIndexed { rowIndex, row ->
        (row as? VerseMapRow.Line)?.cells?.forEach {
            if (it is VerseMapCell.VerseBox) result[it.index] = rowIndex
        }
    }
    return result
}

fun VerseMapRow.Line.box(index: Int) =
    cells.firstOrNull { it is VerseMapCell.VerseBox && it.index == index } as VerseMapCell.VerseBox?
