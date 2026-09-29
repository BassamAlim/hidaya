package bassamalim.hidaya.features.verseGuess

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class VerseGuessDomain @Inject constructor(
    private val quranRepository: QuranRepository,
    private val recitationsRepository: RecitationsRepository,
    private val userRepository: UserRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    suspend fun getSuraNames(language: Language) = quranRepository.getDecoratedSuraNames(language)

    suspend fun getAllVerses() = quranRepository.getAllVerses().sortedBy { it.id }

    /** Indexed by reciter id */
    suspend fun getReciterNames() = recitationsRepository.getVerseReciterNames()

    /** The reader's reciter, so a game sounds like the user's Quran by default */
    suspend fun getDefaultReciterId() = recitationsRepository.getVerseReciterId().first()

    fun observeProgress() = combine(
        userRepository.getLocalRecord(),
        userRepository.getVerseGuessStats()
    ) { record, stats ->
        VerseGuessProgress(
            totalPoints = record.verseGuessPoints,
            roundsPlayed = stats.roundsPlayed,
            recentAverage = stats.recentAverage,
            currentStreak = stats.currentStreak,
            bestStreak = record.verseGuessBestStreak
        )
    }

    suspend fun addRound(result: RoundResult) =
        userRepository.addVerseGuessRound(result.points, result.isExact)

    /** Each reciter's best-quality recitation source */
    suspend fun getRecitationSources() = recitationsRepository.getAllVerseRecitations()
        .groupBy { it.reciterId }
        .mapValues { (_, recitations) -> recitations.maxBy { it.bitrate }.source }

}
