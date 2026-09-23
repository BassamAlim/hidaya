package bassamalim.hidaya.features.misbaha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MisbahaViewModel @Inject constructor(
    val domain: MisbahaDomain
): ViewModel() {

    private var numeralsLanguage: Language? = null
    private var target: Int? = TARGETS.first()
    var count = 0
        private set

    private val _uiState = MutableStateFlow(MisbahaUiState())
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = MisbahaUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            numeralsLanguage = domain.getNumeralsLanguage().first()
            _uiState.update { it.copy(isLoading = false) }
            updateState()
        }
    }

    /** Returns true when this tap completed a round, so the screen can give stronger feedback. */
    fun onIncrementClick(): Boolean {
        count++
        updateState()

        val target = target ?: return false
        return count % target == 0
    }

    fun onResetClick() {
        count = 0
        updateState()
    }

    fun onTargetChange(target: Int?) {
        this.target = target
        count = 0
        updateState()
    }

    private fun updateState() {
        val numeralsLanguage = numeralsLanguage ?: return
        val target = target

        // In a round the count runs 1..target (showing the target itself on completion),
        // then starts over at 1 on the next tap
        val countInRound =
            if (target == null || count == 0) count
            else (count - 1) % target + 1
        val rounds = if (target == null) 0 else count / target

        _uiState.update { it.copy(
            countText = translateNums(
                string = countInRound.toString(),
                numeralsLanguage = numeralsLanguage
            ),
            roundsText =
                if (rounds == 0) ""
                else translateNums(string = "× $rounds", numeralsLanguage = numeralsLanguage),
            progress = if (target == null) 0f else countInRound.toFloat() / target,
            target = target,
            numeralsLanguage = numeralsLanguage
        )}
    }

    companion object {
        /** Selectable round sizes; null means counting without a target. */
        val TARGETS = listOf(33, 100, null)
    }

}
