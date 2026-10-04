package bassamalim.hidaya.core.data.repositories

import android.app.Application
import android.util.Log
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.UserPreferencesDataSource
import bassamalim.hidaya.core.models.Response
import bassamalim.hidaya.core.models.UserRecord
import bassamalim.hidaya.core.utils.OsUtils
import bassamalim.hidaya.core.utils.report
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.max

class UserRepository @Inject constructor(
    private val app: Application,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val RANKS_PAGE_SIZE = 100L
        private const val DEVICE_NOT_REGISTERED = "Device not registered"
    }

    private val leaderboard get() = firestore.collection("Leaderboard")

    fun getLocalRecord() = userPreferencesDataSource.getUserRecord()

    suspend fun updateLocalRecord(transform: (UserRecord) -> UserRecord) =
        userPreferencesDataSource.updateUserRecord(transform)

    suspend fun addQuranPage() = updateLocalRecord { it.copy(quranPages = it.quranPages + 1) }

    suspend fun addRecitationsTime(millis: Long) =
        updateLocalRecord { it.copy(recitationsTime = it.recitationsTime + millis) }

    fun getVerseGuessStats() = userPreferencesDataSource.getVerseGuessStats()

    /** Adds a where's-the-verse round to the local stats and the record synced for ranking. */
    suspend fun addVerseGuessRound(points: Int, isExact: Boolean) =
        userPreferencesDataSource.updateVerseGuess { stats, record ->
            val newStats = stats.afterRound(points, isExact)
            newStats to record.copy(
                verseGuessPoints = record.verseGuessPoints + points,
                verseGuessBestStreak = max(record.verseGuessBestStreak, newStats.currentStreak)
            )
        }

    fun getQuizStats() = userPreferencesDataSource.getQuizStats()

    /** Adds a quiz answer to the local stats and the record synced for ranking. */
    suspend fun addQuizAnswer(questionId: Int, isCorrect: Boolean) =
        userPreferencesDataSource.updateQuiz { stats, record ->
            val newStats = stats.afterAnswer(questionId, isCorrect)
            // Counted up rather than taken from the set's size, so a reinstall, which empties
            // the set but syncs back the count, doesn't stall the count until the set catches up
            val isNewlyLearned = newStats.learnedIds.size > stats.learnedIds.size
            newStats to record.copy(quizLearned = record.quizLearned + if (isNewlyLearned) 1 else 0)
        }

    /** Lets [questionIds] be asked again, once a category has run out. */
    suspend fun forgetQuizQuestions(questionIds: Collection<Int>) =
        userPreferencesDataSource.updateQuiz { stats, record ->
            stats.copy(seenIds = stats.seenIds - questionIds.toSet()) to record
        }

    suspend fun getRemoteRecord(deviceId: String): Response<UserRecord>? {
        if (!OsUtils.isNetworkAvailable(app)) return null

        return try {
            val document = leaderboard.document(deviceId).get().await()
            if (!document.exists()) Response.Error(DEVICE_NOT_REGISTERED)
            else document.toUserRecord()?.let { Response.Success(it) }
                ?: Response.Error("Malformed record")
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Error getting user record: $e")
            Response.Error("Error fetching data")
        }
    }

    /**
     * Registers the device if needed, then merges the local and remote records, keeping the larger
     * of each value, and pushes the result wherever it's newer.
     * @return whether the device is connected to the leaderboard.
     */
    suspend fun syncRecords(deviceId: String): Boolean {
        val remote = when (val response = getRemoteRecord(deviceId)) {
            is Response.Success -> response.data!!
            is Response.Error ->
                if (response.message == DEVICE_NOT_REGISTERED) registerDevice(deviceId) ?: return false
                else return false
            null -> return false
        }

        val merged = updateLocalRecord { local ->
            UserRecord(
                userId = remote.userId,
                quranPages = max(local.quranPages, remote.quranPages),
                recitationsTime = max(local.recitationsTime, remote.recitationsTime),
                verseGuessPoints = max(local.verseGuessPoints, remote.verseGuessPoints),
                verseGuessBestStreak = max(local.verseGuessBestStreak, remote.verseGuessBestStreak),
                quizLearned = max(local.quizLearned, remote.quizLearned)
            )
        }
        if (merged != remote) setRemoteRecord(deviceId, merged)
        return true
    }

    private suspend fun setRemoteRecord(deviceId: String, record: UserRecord) {
        try {
            // merge, so fields like created_at survive
            leaderboard.document(deviceId)
                .set(
                    mapOf(
                        "user_id" to record.userId,
                        "reading_record" to record.quranPages,
                        "listening_record" to record.recitationsTime,
                        "verse_guess_points" to record.verseGuessPoints,
                        "verse_guess_streak" to record.verseGuessBestStreak,
                        "quiz_learned" to record.quizLearned
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Error setting user record: $e")
        }
    }

    private suspend fun registerDevice(deviceId: String): UserRecord? {
        val localRecord = getLocalRecord().first()

        return try {
            firestore.runTransaction { transaction ->
                val leaderboardDocRef = leaderboard.document(deviceId)
                val existingSnapshot = transaction.get(leaderboardDocRef)
                if (existingSnapshot.exists()) {
                    return@runTransaction existingSnapshot.toUserRecord()
                        ?: throw FirebaseFirestoreException(
                            "Malformed record", FirebaseFirestoreException.Code.DATA_LOSS
                        )
                }

                val counterDocRef = firestore.collection("Counters").document("users")
                val counterSnapshot = transaction.get(counterDocRef)

                if (!counterSnapshot.exists() || counterSnapshot.data == null) {
                    throw FirebaseFirestoreException(
                        "Counter document not found",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }

                val newUserId = (counterSnapshot.getLong("last_id") ?: 0L).toInt() + 1

                transaction.update(counterDocRef, "last_id", newUserId)
                transaction.set(leaderboardDocRef, mapOf(
                    "user_id" to newUserId,
                    "reading_record" to localRecord.quranPages,
                    "listening_record" to localRecord.recitationsTime,
                    "verse_guess_points" to localRecord.verseGuessPoints,
                    "verse_guess_streak" to localRecord.verseGuessBestStreak,
                    "quiz_learned" to localRecord.quizLearned,
                    "created_at" to System.currentTimeMillis()
                ))

                localRecord.copy(userId = newUserId)
            }.await()
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Transaction failed: $e")
            null
        }
    }

    /**
     * A page of users with a non-zero [field], best first, as (user id, value) pairs, plus the
     * cursor for the next page. Ties are ordered by user id so pages are stable.
     */
    suspend fun getRanks(
        field: String,
        previousLast: DocumentSnapshot? = null
    ): Pair<Response<List<Pair<Int, Long>>>, DocumentSnapshot?>? {
        if (!OsUtils.isNetworkAvailable(app)) return null

        return try {
            var query = leaderboard
                .whereGreaterThan(field, 0)
                .orderBy(field, Query.Direction.DESCENDING)
                .orderBy("user_id")
            if (previousLast != null) query = query.startAfter(previousLast)
            val documents = query.limit(RANKS_PAGE_SIZE).get().await().documents

            // skip malformed documents rather than failing the whole page
            val ranks = documents.mapNotNull { document ->
                val userId = document.getLong("user_id") ?: return@mapNotNull null
                val value = document.getLong(field) ?: return@mapNotNull null
                userId.toInt() to value
            }
            Response.Success(ranks) to documents.lastOrNull()
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Error getting ranks: $e")
            Response.Error<List<Pair<Int, Long>>>("Error fetching data") to null
        }
    }

    /**
     * Competition rank (ties share a rank) of a user whose [field] is [value], or null if they have
     * no record or it couldn't be fetched.
     */
    suspend fun getUserRank(field: String, value: Long): Int? {
        if (value <= 0 || !OsUtils.isNetworkAvailable(app)) return null

        return try {
            leaderboard.whereGreaterThan(field, value)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count.toInt() + 1
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Error getting user rank: $e")
            null
        }
    }

    private fun DocumentSnapshot.toUserRecord(): UserRecord? {
        return UserRecord(
            userId = getLong("user_id")?.toInt() ?: return null,
            quranPages = getLong("reading_record")?.toInt() ?: return null,
            recitationsTime = getLong("listening_record") ?: return null,
            // Records from before the games lack their fields
            verseGuessPoints = getLong("verse_guess_points") ?: 0L,
            verseGuessBestStreak = getLong("verse_guess_streak")?.toInt() ?: 0,
            quizLearned = getLong("quiz_learned")?.toInt() ?: 0
        )
    }

}
