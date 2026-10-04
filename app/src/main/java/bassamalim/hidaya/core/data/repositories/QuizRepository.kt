package bassamalim.hidaya.core.data.repositories

import bassamalim.hidaya.core.data.dataSources.room.daos.QuizAnswersDao
import bassamalim.hidaya.core.data.dataSources.room.daos.QuizQuestionsDao
import bassamalim.hidaya.core.di.DefaultDispatcher
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.QuizAnswer
import bassamalim.hidaya.core.models.QuizFullQuestion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class QuizRepository @Inject constructor(
    private val quizQuestionsDao: QuizQuestionsDao,
    private val quizAnswerDao: QuizAnswersDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend fun getQuestions() = withContext(dispatcher) {
        quizQuestionsDao.getAll()
    }

    suspend fun getAllQuestionIds() = withContext(dispatcher) {
        quizQuestionsDao.getAllIds()
    }

    suspend fun getQuestionCount() = withContext(dispatcher) {
        quizQuestionsDao.getCount()
    }

    suspend fun getCategoryQuestionIds(category: String) = withContext(dispatcher) {
        quizQuestionsDao.getCategoryIds(category)
    }

    suspend fun getFullQuestions(questionIds: IntArray, language: Language) =
        withContext(dispatcher) {
            questionIds.map { id ->
                val question = quizQuestionsDao.getQuestion(id)
                val answers = quizAnswerDao.getAnswers(id)
                QuizFullQuestion(
                    id = question.id,
                    question = question.question,
                    description = question.description,
                    type = when (language) {
                        Language.ARABIC -> question.typeAr
                        Language.ENGLISH -> question.typeEn
                    },
                    answers = answers.mapIndexed { i, answer ->
                        QuizAnswer(
                            id = answer.id,
                            text = answer.answer,
                            isCorrect = i == 0
                        )
                    }
                )
            }
        }

    suspend fun getQuestion(questionId: Int) = withContext(dispatcher) {
        quizQuestionsDao.getQuestion(questionId)
    }

    suspend fun getAnswers(questionId: Int) = withContext(dispatcher) {
        quizAnswerDao.getAnswers(questionId)
    }

    suspend fun getQuestionTypes() = withContext(dispatcher) {
        quizQuestionsDao.getTypes()
    }

}