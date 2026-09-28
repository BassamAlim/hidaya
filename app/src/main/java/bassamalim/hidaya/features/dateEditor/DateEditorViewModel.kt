package bassamalim.hidaya.features.dateEditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.utils.LangUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class DateEditorViewModel @Inject constructor(
    private val domain: DateEditorDomain,
    private val navigator: Navigator
): ViewModel() {

    private var numeralsLanguage: Language? = null

    private val _uiState = MutableStateFlow(DateEditorUiState())
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = DateEditorUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            domain.assignDateOffset()

            numeralsLanguage = domain.getNumeralsLanguage()

            updateState()
        }
    }

    fun onNextDayClick() {
        domain.incrementDateOffset()

        updateState()
    }

    fun onPreviousDayClick() {
        domain.decrementDateOffset()

        updateState()
    }

    fun onSave() {
        viewModelScope.launch {
            domain.saveDateOffset()

            navigator.navigateBackWithResult(data = null)
        }
    }

    fun onDismiss() {
        navigator.navigateBackWithResult(data = null)
    }

    private fun updateState() {
        val numeralsLanguage = numeralsLanguage ?: return

        val dateOffset = domain.getDateOffset()
        if (dateOffset == 0) {
            _uiState.update { it.copy(
                isUnchanged = true,
                dateText = getDateText(numeralsLanguage)
            )}
        }
        else {
            _uiState.update { it.copy(
                isUnchanged = false,
                dateOffsetText = getDateOffsetText(dateOffset, numeralsLanguage),
                dateText = getDateText(numeralsLanguage)
            )}
        }
    }

    private fun getDateText(numeralsLanguage: Language): String {
        val date = HijrahDate.now().plus(domain.getDateOffset().toLong(), ChronoUnit.DAYS)

        return LangUtils.translateNums(
            numeralsLanguage = numeralsLanguage,
            string = "${date.get(ChronoField.DAY_OF_MONTH)}/" +
                    "${date.get(ChronoField.MONTH_OF_YEAR)}/${date.get(ChronoField.YEAR)}"
        )
    }

    private fun getDateOffsetText(dateOffset: Int, numeralsLanguage: Language): String {
        var offsetStr = dateOffset.toString()
        if (dateOffset > 0) offsetStr = "+$offsetStr"
        return LangUtils.translateNums(
            numeralsLanguage = numeralsLanguage,
            string = offsetStr
        )
    }

}