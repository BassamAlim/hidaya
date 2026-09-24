package bassamalim.hidaya.features.leaderboard

import android.app.Application
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.models.Response
import bassamalim.hidaya.core.utils.OsUtils
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardDomain @Inject constructor(
    app: Application,
    private val userRepository: UserRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    private val deviceId = OsUtils.getDeviceId(app)
    private val previousLastDocuments: MutableMap<RankType, DocumentSnapshot?> = mutableMapOf(
        RankType.BY_READING to null,
        RankType.BY_LISTENING to null
    )

    suspend fun getUserRank(userId: Int): Map<RankType, Int> {
        val readingRank = userRepository.getUserReadingRank(userId) ?: -1
        val listeningRank = userRepository.getUserListeningRank(userId) ?: -1
        return mapOf(
            RankType.BY_READING to readingRank,
            RankType.BY_LISTENING to listeningRank
        )
    }

    suspend fun getUserRecord() = userRepository.getRemoteRecord(deviceId)?.first()

    suspend fun getRanks(): Map<RankType, Response<Map<Int, Long>>>? {
        val rawReadingRanks = userRepository.getReadingRanks()
        val rawListeningRanks = userRepository.getListeningRanks()

        if (rawReadingRanks == null || rawListeningRanks == null) return null

        val (readingRanks, lastReading) = rawReadingRanks
        val (listeningRanks, lastListening) = rawListeningRanks

        previousLastDocuments[RankType.BY_READING] = lastReading
        previousLastDocuments[RankType.BY_LISTENING] = lastListening

        return mapOf(
            RankType.BY_READING to readingRanks,
            RankType.BY_LISTENING to listeningRanks
        )
    }

    suspend fun getMoreRanks(rankType: RankType): Response<Map<Int, Long>>? {
        val rawRanks =  when (rankType) {
            RankType.BY_READING -> {
                userRepository.getReadingRanks(previousLastDocuments[rankType])
            }
            RankType.BY_LISTENING -> {
                userRepository.getListeningRanks(previousLastDocuments[rankType])
            }
        }
        if (rawRanks == null) return null
        val (ranks, last) = rawRanks

        previousLastDocuments[rankType] = last

        return ranks
    }

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

}