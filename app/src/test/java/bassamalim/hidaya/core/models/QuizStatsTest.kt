package bassamalim.hidaya.core.models

import org.junit.Assert.assertEquals
import org.junit.Test

/** How a quiz answer updates the local stats. */
class QuizStatsTest {

    @Test
    fun `correct answers build a streak, a wrong one ends it, and the best is kept`() {
        val stats = QuizStats()
            .afterAnswer(1, isCorrect = true)
            .afterAnswer(2, isCorrect = true)
            .afterAnswer(3, isCorrect = false)

        assertEquals(0, stats.currentStreak)
        assertEquals(2, stats.bestStreak)
    }

    @Test
    fun `a question is learned once, however often it's answered right`() {
        val stats = QuizStats()
            .afterAnswer(1, isCorrect = true)
            .afterAnswer(1, isCorrect = true)
            .afterAnswer(2, isCorrect = false)

        assertEquals(3, stats.answered)
        assertEquals(2, stats.correct)
        assertEquals(setOf(1, 2), stats.seenIds)
        assertEquals(setOf(1), stats.learnedIds)
    }

    @Test
    fun `the learned share is capped and safe on an empty bank`() {
        assertEquals(25.0, QuizStats.learnedPercent(learned = 50, totalQuestions = 200), 0.0)
        assertEquals(100.0, QuizStats.learnedPercent(learned = 210, totalQuestions = 200), 0.0)
        assertEquals(0.0, QuizStats.learnedPercent(learned = 5, totalQuestions = 0), 0.0)
    }

}
