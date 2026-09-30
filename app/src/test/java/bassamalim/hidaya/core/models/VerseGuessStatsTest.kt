package bassamalim.hidaya.core.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** How a where's-the-verse round updates the local stats. */
class VerseGuessStatsTest {

    @Test
    fun `exact guesses build a streak and a miss ends it`() {
        val stats = VerseGuessStats()
            .afterRound(5000, isExact = true)
            .afterRound(5000, isExact = true)

        assertEquals(2, stats.currentStreak)
        assertEquals(0, stats.afterRound(1200, isExact = false).currentStreak)
    }

    @Test
    fun `the average follows only the recent rounds`() {
        var stats = VerseGuessStats()
        repeat(VerseGuessStats.RECENT_ROUNDS) { stats = stats.afterRound(0, isExact = false) }
        repeat(VerseGuessStats.RECENT_ROUNDS) { stats = stats.afterRound(4000, isExact = false) }

        assertEquals(2 * VerseGuessStats.RECENT_ROUNDS, stats.roundsPlayed)
        assertEquals(VerseGuessStats.RECENT_ROUNDS, stats.recentPoints.size)
        assertEquals(4000, stats.recentAverage)
    }

    @Test
    fun `there's no average before the first round`() {
        assertNull(VerseGuessStats().recentAverage)
    }

}
