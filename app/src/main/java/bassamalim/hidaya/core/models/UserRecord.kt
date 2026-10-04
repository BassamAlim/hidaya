package bassamalim.hidaya.core.models

import kotlinx.serialization.Serializable

/**
 * The user's leaderboard record. Every value only ever grows, so syncing keeps the larger of
 * the local and remote copies.
 */
@Serializable
data class UserRecord(
    val userId: Int = -1,
    val quranPages: Int = 0,
    val recitationsTime: Long = 0L,
    /** Lifetime points in the where's-the-verse game */
    val verseGuessPoints: Long = 0L,
    /** Most exact guesses in a row in the where's-the-verse game */
    val verseGuessBestStreak: Int = 0,
    /** Different quiz questions ever answered correctly */
    val quizLearned: Int = 0
)
