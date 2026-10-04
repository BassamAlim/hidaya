package bassamalim.hidaya.features.quiz.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.QuizFullQuestion
import bassamalim.hidaya.core.nav.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Endless questions from one category, each revealed as soon as it's answered. */
@HiltViewModel
class QuizTestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: QuizTestDomain
): ViewModel() {

    private val category = savedStateHandle.toRoute<Screen.QuizTest>().category

    private var language = Language.ARABIC
    private var questionIds: List<Int> = emptyList()
    private var question: QuizFullQuestion? = null
    /** The next question waits on this, so the one just answered counts as seen */
    private var saveJob: Job? = null
    private var nextJob: Job? = null

    private val _uiState = MutableStateFlow(QuizTestUiState())
    val uiState = combine(
        _uiState,
        domain.observeCurrentStreak()
    ) { state, stats ->
        state.copy(currentStreak = stats.currentStreak)
    }.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = QuizTestUiState()
    )

    private fun initializeData() {
        if (question != null) return

        viewModelScope.launch {
            language = domain.getLanguage()
            questionIds = domain.getQuestionIds(category)
            _uiState.update { it.copy(numeralsLanguage = domain.getNumeralsLanguage()) }

            askNext()
        }
    }

    fun onAnswerClick(index: Int) {
        val question = question ?: return
        if (_uiState.value.isRevealed) return

        val isCorrect = question.answers[index].isCorrect
        _uiState.update { it.copy(
            chosenIndex = index,
            sessionAnswered = it.sessionAnswered + 1,
            sessionCorrect = it.sessionCorrect + if (isCorrect) 1 else 0
        )}
        saveJob = viewModelScope.launch { domain.addAnswer(question.id, isCorrect) }
    }

    fun onNextClick() {
        if (!_uiState.value.isRevealed || nextJob?.isActive == true) return

        nextJob = viewModelScope.launch {
            saveJob?.join()
            askNext()
        }
    }

    private suspend fun askNext() {
        if (questionIds.isEmpty()) return

        val next = domain.getNextQuestion(questionIds, language)
        question = next
        _uiState.update { it.copy(
            isLoading = false,
            question = next.question,
            answers = next.answers.map { answer -> answer.text },
            correctIndex = next.answers.indexOfFirst { answer -> answer.isCorrect },
            chosenIndex = null,
            description = next.description?.takeIf { d -> d.isNotBlank() }
        )}
    }

}
