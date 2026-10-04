package bassamalim.hidaya.features.leaderboard

import bassamalim.hidaya.core.models.UserRecord

/** [field] is the Firestore field in the Leaderboard collection that this ranking orders by. */
enum class RankType(val field: String) {
    BY_READING("reading_record"),
    BY_LISTENING("listening_record"),
    BY_VERSE_GUESS_POINTS("verse_guess_points"),
    BY_VERSE_GUESS_STREAK("verse_guess_streak"),
    BY_QUIZ_LEARNED("quiz_learned");

    /** The user's own value for this ranking */
    fun valueIn(record: UserRecord): Long = when (this) {
        BY_READING -> record.quranPages.toLong()
        BY_LISTENING -> record.recitationsTime
        BY_VERSE_GUESS_POINTS -> record.verseGuessPoints
        BY_VERSE_GUESS_STREAK -> record.verseGuessBestStreak.toLong()
        BY_QUIZ_LEARNED -> record.quizLearned.toLong()
    }
}
