package bassamalim.hidaya.core.models

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * The where's-the-verse game's local stats. The lifetime totals live in [UserRecord], which is
 * synced to the leaderboard; these don't sync, since they can't be merged by taking the larger.
 */
@Serializable
data class VerseGuessStats(
    val roundsPlayed: Int = 0,
    /** The latest rounds' points, oldest first */
    val recentPoints: List<Int> = emptyList(),
    /** Exact guesses in a row, up to the latest round */
    val currentStreak: Int = 0
) {

    /** Over the last [RECENT_ROUNDS] rounds, so it follows how the user plays now */
    val recentAverage get() = recentPoints.takeIf { it.isNotEmpty() }?.average()?.roundToInt()

    fun afterRound(points: Int, isExact: Boolean) = VerseGuessStats(
        roundsPlayed = roundsPlayed + 1,
        recentPoints = (recentPoints + points).takeLast(RECENT_ROUNDS),
        currentStreak = if (isExact) currentStreak + 1 else 0
    )

    companion object {
        const val RECENT_ROUNDS = 20
    }

}
