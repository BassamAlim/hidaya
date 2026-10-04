package bassamalim.hidaya.core.models

import kotlinx.serialization.Serializable
import kotlin.math.max

/**
 * The quiz's local stats. The count of questions learned lives in [UserRecord], which is synced
 * to the leaderboard; these don't sync, since they can't be merged by taking the larger.
 */
@Serializable
data class QuizStats(
    val answered: Int = 0,
    val correct: Int = 0,
    /** Correct answers in a row, up to the latest question */
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    /** Questions already asked, so none comes back before its category runs out */
    val seenIds: Set<Int> = emptySet(),
    /** Questions ever answered correctly. Never cleared, so a repeat can't count twice */
    val learnedIds: Set<Int> = emptySet()
) {

    fun afterAnswer(questionId: Int, isCorrect: Boolean): QuizStats {
        val streak = if (isCorrect) currentStreak + 1 else 0
        return QuizStats(
            answered = answered + 1,
            correct = correct + if (isCorrect) 1 else 0,
            currentStreak = streak,
            bestStreak = max(bestStreak, streak),
            seenIds = seenIds + questionId,
            learnedIds = if (isCorrect) learnedIds + questionId else learnedIds
        )
    }

    companion object {
        /**
         * 0..100. Shown instead of the count so the size of the question bank stays out of
         * view. Capped, since questions removed from the bank may still be counted.
         */
        fun learnedPercent(learned: Int, totalQuestions: Int) =
            if (totalQuestions == 0) 0.0
            else minOf(learned, totalQuestions) * 100.0 / totalQuestions
    }

}
