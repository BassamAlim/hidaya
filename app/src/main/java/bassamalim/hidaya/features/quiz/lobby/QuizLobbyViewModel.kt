package bassamalim.hidaya.features.quiz.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizLobbyViewModel @Inject constructor(
    private val domain: QuizLobbyDomain,
    private val navigator: Navigator
): ViewModel() {

    private val _uiState = MutableStateFlow(QuizLobbyUiState())
    val uiState = combine(
        _uiState,
        domain.observeProgress(),
        domain.getNumeralsLanguage()
    ) { state, progress, numeralsLanguage ->
        state.copy(progress = progress, numeralsLanguage = numeralsLanguage)
    }.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = QuizLobbyUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = false,
                quizCategories = domain.getQuizCategories()
            )}
        }
    }

    fun onStartQuizClick() {
        navigator.navigate(Screen.QuizTest())
    }

    fun onCategoryClick(category: String) {
        navigator.navigate(Screen.QuizTest(category = category))

        domain.trackQuizCategoryViewed(category)
    }

    fun onLeaderboardClick() {
        navigator.navigate(Screen.Leaderboard)
    }

}
