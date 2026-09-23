package bassamalim.hidaya.features.quiz.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.features.quiz.QuizResultHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizResultViewModel @Inject constructor(
    private val domain: QuizResultDomain,
    resultHolder: QuizResultHolder,
): ViewModel() {

    private val result = resultHolder.result

    private lateinit var numeralsLanguage: Language

    private val _uiState = MutableStateFlow(QuizResultUiState())
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = QuizResultUiState()
    )

    private fun initializeData() {
        // Can be null if the process was killed and restored while on this screen; the
        // in-memory hand-off does not survive that, so we render an empty result rather
        // than crash.
        val result = result ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            numeralsLanguage = domain.getNumeralsLanguage()

            _uiState.update { it.copy(
                isLoading = false,
                questions = result.questions.mapIndexed { i, q ->
                    QuizResultQuestion(
                        questionNum = i + 1,
                        questionText = q.question,
                        answers = q.answers,
                        chosenAnswerId = result.chosenAnswers[i]
                    )
                },
                scorePercent = result.score * 10,
                numeralsLanguage = numeralsLanguage
            )}
        }
    }

}
