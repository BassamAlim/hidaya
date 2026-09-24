package bassamalim.hidaya.features.quiz.test

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuizRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.QuizFullQuestion
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizTestDomain @Inject constructor(
    private val quizRepository: QuizRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    fun calculateScore(questions: List<QuizFullQuestion>, chosenAs: IntArray): Int {
        var score = 0
        questions.forEachIndexed { i, q ->
            if (q.answers[chosenAs[i]].isCorrect) score++
        }
        return score
    }

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    suspend fun getQuizQuestions(category: String, language: Language): List<QuizFullQuestion> {
        val ids = getRandomIds(category)
        return getFullQuestions(ids = ids, language = language)
    }

    private suspend fun getRandomIds(category: String): List<Int> {
        val ids = if (category == "all") quizRepository.getAllQuestionIds().toMutableList()
        else quizRepository.getCategoryQuestionIds(category).toMutableList()
        ids.shuffle()
        return ids.subList(0, 10)
    }

    private suspend fun getFullQuestions(
        ids: List<Int>,
        language: Language
    ): List<QuizFullQuestion> {
        val questions = quizRepository.getFullQuestions(
            questionIds = ids.toIntArray(),
            language = language
        )
        return questions.map { question ->
            question.copy(answers = question.answers.shuffled())
        }
    }

}