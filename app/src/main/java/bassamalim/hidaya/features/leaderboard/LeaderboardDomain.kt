package bassamalim.hidaya.features.leaderboard

import android.app.Application
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuizRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.models.Response
import bassamalim.hidaya.core.models.UserRecord
import bassamalim.hidaya.core.utils.OsUtils
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardDomain @Inject constructor(
    app: Application,
    private val userRepository: UserRepository,
    private val quizRepository: QuizRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    private val deviceId = OsUtils.getDeviceId(app)
    private val lastDocuments = mutableMapOf<RankType, DocumentSnapshot?>()
    private val reachedEnd = mutableSetOf<RankType>()

    suspend fun getUserRanks(record: UserRecord) = RankType.entries.associateWith {
        userRepository.getUserRank(it.field, it.valueIn(record))
    }

    suspend fun getUserRecord() = userRepository.getRemoteRecord(deviceId)

    /** Fetches the first page of each ranking, resetting pagination. */
    suspend fun getRanks(): Map<RankType, Response<List<Pair<Int, Long>>>>? {
        lastDocuments.clear()
        reachedEnd.clear()
        return RankType.entries.associateWith { getMoreRanks(it) ?: return null }
    }

    fun hasMore(rankType: RankType) = rankType !in reachedEnd

    suspend fun getMoreRanks(rankType: RankType): Response<List<Pair<Int, Long>>>? {
        val (ranks, last) = userRepository.getRanks(rankType.field, lastDocuments[rankType])
            ?: return null

        if (ranks is Response.Success) {
            // an empty page means the end; keep the old cursor so we never restart from the top
            if (last == null) reachedEnd += rankType
            else lastDocuments[rankType] = last
        }
        return ranks
    }

    suspend fun getQuizQuestionCount() = quizRepository.getQuestionCount()

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

}
