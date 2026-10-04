package bassamalim.hidaya.features.quiz.test

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuizRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.QuizFullQuestion
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class QuizTestDomain @Inject constructor(
    private val quizRepository: QuizRepository,
    private val userRepository: UserRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    suspend fun getQuestionIds(category: String) =
        if (category == ALL_CATEGORIES) quizRepository.getAllQuestionIds()
        else quizRepository.getCategoryQuestionIds(category)

    fun observeCurrentStreak() = userRepository.getQuizStats()

    /**
     * A random question from [ids] the user hasn't been asked yet. Once they've all been asked,
     * they're forgotten and the category starts over.
     */
    suspend fun getNextQuestion(ids: List<Int>, language: Language): QuizFullQuestion {
        val seenIds = userRepository.getQuizStats().first().seenIds
        val id = ids.filter { it !in seenIds }.randomOrNull()
            ?: ids.random().also { userRepository.forgetQuizQuestions(ids) }

        val question = quizRepository.getFullQuestions(intArrayOf(id), language).single()
        return question.copy(answers = question.answers.shuffled())
    }

    suspend fun addAnswer(questionId: Int, isCorrect: Boolean) =
        userRepository.addQuizAnswer(questionId, isCorrect)

    companion object {
        const val ALL_CATEGORIES = "all"
    }

}
