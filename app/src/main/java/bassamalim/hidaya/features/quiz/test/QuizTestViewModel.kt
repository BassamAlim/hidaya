package bassamalim.hidaya.features.quiz.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.QuizFullQuestion
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.features.quiz.QuizResult
import bassamalim.hidaya.features.quiz.QuizResultHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizTestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: QuizTestDomain,
    private val resultHolder: QuizResultHolder,
    private val navigator: Navigator
): ViewModel() {

    private val category = savedStateHandle.get<String>("category") ?: "all"

    val totalQuestions = 10
    private lateinit var questions: List<QuizFullQuestion>
    private val chosenAs = IntArray(totalQuestions) { -1 }
    private lateinit var language: Language
    private lateinit var numeralsLanguage: Language

    private val _uiState = MutableStateFlow(QuizTestUiState())
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = QuizTestUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            language = domain.getLanguage()
            numeralsLanguage = domain.getNumeralsLanguage()
            questions = domain.getQuizQuestions(category = category, language = language)

            _uiState.update { it.copy(
                isLoading = false
            )}

            updateState()
        }
    }

    fun onPreviousQuestionClick() {
        if (_uiState.value.questionIdx > 0)
            ask(_uiState.value.questionIdx - 1)
    }

    fun onNextQuestionClick() {
        if (_uiState.value.questionIdx == totalQuestions-1) {
            if (_uiState.value.allAnswered) endQuiz()
        }
        else ask(_uiState.value.questionIdx + 1)
    }

    fun onAnswerSelected(answerIndex: Int) {
        val questionIdx = _uiState.value.questionIdx
        chosenAs[questionIdx] = answerIndex

        _uiState.update { it.copy(
            selection = answerIndex,
            allAnswered = !chosenAs.contains(-1),
            answeredQuestions = chosenAs.map { chosen -> chosen != -1 }
        )}
        _uiState.update { it.copy(
            nextButtonEnabled = !(it.questionIdx == totalQuestions-1 && !it.allAnswered)
        )}

        if (questionIdx != totalQuestions-1) {
            viewModelScope.launch {
                // Let the chosen answer show as selected before moving on
                delay(ADVANCE_DELAY_MILLIS)
                // Skip if the user already moved to another question meanwhile
                if (_uiState.value.questionIdx == questionIdx) onNextQuestionClick()
            }
        }
    }

    fun onQuestionClick(questionIdx: Int) {
        ask(questionIdx)
    }

    private fun ask(num: Int) {
        _uiState.update { it.copy(
            questionIdx = num
        )}

        updateState()
    }

    private fun endQuiz() {
        resultHolder.result = QuizResult(
            score = domain.calculateScore(questions, chosenAs),
            questions = questions,
            chosenAnswers = chosenAs.toList()
        )

        navigator.navigate(Screen.QuizResult) {
            popUpTo(Screen.QuizTest(category).route) { inclusive = true }
        }
    }

    private fun updateState() {
        val question = questions[_uiState.value.questionIdx]

        _uiState.update { it.copy(
            titleQuestionNumber = LangUtils.translateNums(
                numeralsLanguage = numeralsLanguage,
                string = (it.questionIdx + 1).toString()
            ),
            question = question.question,
            answers = question.answers.map { answer -> answer.text },
            selection = chosenAs[it.questionIdx],
            answeredQuestions = chosenAs.map { chosen -> chosen != -1 },
            previousButtonEnabled = it.questionIdx != 0,
            nextButtonEnabled = !(it.questionIdx == totalQuestions-1 && !it.allAnswered),
        )}
    }

    private companion object {
        const val ADVANCE_DELAY_MILLIS = 300L
    }

}
