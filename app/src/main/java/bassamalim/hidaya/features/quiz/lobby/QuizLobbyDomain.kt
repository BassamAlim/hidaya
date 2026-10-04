package bassamalim.hidaya.features.quiz.lobby

import bassamalim.hidaya.core.data.repositories.AnalyticsRepository
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuizRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.models.AnalyticsEvent
import bassamalim.hidaya.core.models.QuizStats
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class QuizLobbyDomain @Inject constructor(
    private val quizRepository: QuizRepository,
    private val userRepository: UserRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val analyticsRepository: AnalyticsRepository
) {

    suspend fun getQuizCategories() = quizRepository.getQuestionTypes()

    fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage()

    fun observeProgress() = combine(
        userRepository.getLocalRecord(),
        userRepository.getQuizStats(),
        flow { emit(quizRepository.getQuestionCount()) }
    ) { record, stats, questionCount ->
        QuizProgress(
            answered = stats.answered,
            correct = stats.correct,
            currentStreak = stats.currentStreak,
            bestStreak = stats.bestStreak,
            learnedPercent = QuizStats.learnedPercent(record.quizLearned, questionCount)
        )
    }

    fun trackQuizCategoryViewed(category: String) {
        analyticsRepository.trackEvent(AnalyticsEvent.QuizCategoryStarted(category))
    }

}
