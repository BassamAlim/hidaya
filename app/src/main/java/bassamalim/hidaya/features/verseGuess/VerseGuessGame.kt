package bassamalim.hidaya.features.verseGuess

import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.features.verseGuess.map.VerseMapItem
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** A whole-Quran round's maximum; smaller scopes get less, see [roundMaxPoints] */
const val MAX_ROUND_POINTS = 5000
/** Each round plays this many consecutive verses, all from one sura. */
const val CLIP_LENGTH = 3

/** At a falloff's distance a guess keeps about a third of the points; this share of the scope's pages */
private const val FALLOFF_SCOPE_SHARE = 1 / 40.0
/** So a single juz still gives credit for landing a page or two off */
private const val MIN_FALLOFF_PAGES = 1.0

/**
 * Where each verse sits along the mushaf, in pages: its page's index plus how far into the page
 * it is. Distances in these units read like distances in the printed mushaf, where verse
 * counts don't: a page of Ash-Shu'ara holds several times as many verses as one of Al-Baqarah.
 */
fun pagePositions(items: List<VerseMapItem>): DoubleArray {
    val positions = DoubleArray(items.size)
    var pageStart = 0
    while (pageStart < items.size) {
        var pageEnd = pageStart
        while (pageEnd + 1 < items.size && items[pageEnd + 1].pageNum == items[pageStart].pageNum)
            pageEnd++

        val count = pageEnd - pageStart + 1
        for (i in pageStart..pageEnd)
            positions[i] = items[i].pageNum - 1 + (i - pageStart).toDouble() / count
        pageStart = pageEnd + 1
    }
    return positions
}

/**
 * A clip start, as an index into [items], or null if no clip fits. Clips stay inside one sura,
 * which also covers a sura's first and last verses. Clips sharing a verse with a [recent] one
 * are skipped, until every clip has been.
 */
fun pickClipStart(items: List<VerseMapItem>, random: Random, recent: Collection<Int>): Int? {
    val candidates = (0..items.size - CLIP_LENGTH)
        .filter { items[it].suraNum == items[it + CLIP_LENGTH - 1].suraNum }
    val fresh = candidates.filter { start -> recent.none { abs(it - start) < CLIP_LENGTH } }
    return fresh.ifEmpty { candidates }.randomOrNull(random)
}

/** Pages from [guess] to the nearest of the clip's verses, 0 when it's one of them. */
fun distanceInPages(guess: Double, clip: List<Double>) = clip.minOf { abs(it - guess) }

/**
 * A round's maximum, by the square root of the scope's share of the mushaf: about 910 for a
 * juz against 5000 for the whole Quran. Without it, replaying one memorized juz would earn
 * full points as fast as the whole Quran does.
 */
fun roundMaxPoints(scopePages: Int) =
    (MAX_ROUND_POINTS * sqrt(scopePages.toDouble() / Globals.NUM_OF_QURAN_PAGES)).roundToInt()

/**
 * The scope's maximum for a guess on the clip, falling off exponentially with distance. The
 * falloff scales with the scope, so precision is judged relative to how much there is to search.
 */
fun roundPoints(distancePages: Double, scopePages: Int): Int {
    val falloff = maxOf(scopePages * FALLOFF_SCOPE_SHARE, MIN_FALLOFF_PAGES)
    return (roundMaxPoints(scopePages) * exp(-distancePages / falloff)).roundToInt()
}
