package bassamalim.hidaya.features.remembrances.reader

import androidx.core.text.isDigitsOnly
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemembranceReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: RemembranceReaderDomain
): ViewModel() {

    private val id = savedStateHandle.get<Int>("remembrance_id") ?: 0

    private lateinit var language: Language
    private lateinit var numeralsLanguage: Language

    private val _uiState = MutableStateFlow(RemembranceReaderUiState())
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getTextSize()
    ) { state, textSize -> state.copy(
        textSize = textSize,
    )}.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = RemembranceReaderUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            language = domain.getLanguage()
            numeralsLanguage = domain.getNumeralsLanguage()

            _uiState.update { it.copy(
                title = domain.getTitle(id, language),
                items = getItems()
            )}
        }
    }

    private suspend fun getItems(): List<RemembrancePassage> {
        val remembrancePassages = domain.getRemembrancePassages(id)
        return remembrancePassages.filter {
            !(language == Language.ENGLISH && it.textEn.isNullOrEmpty())
        }.map { passage ->
            when (language) {
                Language.ARABIC -> {
                    val countableRepetition = passage.repetitionAr.isDigitsOnly()

                    RemembrancePassage(
                        id = passage.id,
                        title = passage.titleAr,
                        text = passage.textAr!!,
                        virtue = passage.virtueAr,
                        reference = passage.referenceAr,
                        repetitionText = translateNums(
                            string =
                                if (countableRepetition) "0/${passage.repetitionAr}"
                                else passage.repetitionAr,
                            numeralsLanguage = numeralsLanguage
                        ),
                        repetitionTotal = passage.repetitionAr.toIntOrNull(),
                        repetitionCurrent = 0,
                        isTitleAvailable = isTitleAvailable(passage.titleAr),
                        isTranslationAvailable = false,
                        isVirtueAvailable = isVirtueAvailable(passage.virtueAr),
                        isReferenceAvailable = isReferenceAvailable(passage.referenceAr)
                    )
                }
                Language.ENGLISH -> {
                    val countableRepetition = passage.repetitionEn.isDigitsOnly()

                    RemembrancePassage(
                        id = passage.id,
                        title = passage.titleEn,
                        text = passage.textEn!!,
                        translation = passage.textEnTranslation,
                        virtue = passage.virtueEn,
                        reference = passage.referenceEn,
                        repetitionText = translateNums(
                            string =
                                if (countableRepetition) "0/${passage.repetitionEn}"
                                else passage.repetitionEn,
                            numeralsLanguage = numeralsLanguage
                        ),
                        repetitionTotal = passage.repetitionEn.toIntOrNull(),
                        repetitionCurrent = 0,
                        isTitleAvailable = isTitleAvailable(passage.titleEn),
                        isTranslationAvailable = isTranslationAvailable(passage.textEnTranslation),
                        isVirtueAvailable = isVirtueAvailable(passage.virtueEn),
                        isReferenceAvailable = isReferenceAvailable(passage.referenceEn)
                    )
                }
            }
        }
    }

    fun onTextSizeSliderChange(textSize: Float) {
        viewModelScope.launch {
            domain.setTextSize(textSize)
        }
    }

    fun onRepetitionClick(passageId: Int): RepetitionResult {
        val passage = _uiState.value.items.first { it.id == passageId }
        val total = passage.repetitionTotal ?: return RepetitionResult.IGNORED
        if (passage.isRepetitionComplete) return RepetitionResult.IGNORED

        val newCurrent = (passage.repetitionCurrent ?: 0) + 1
        _uiState.update { it.copy(
            items = it.items.map { item ->
                if (item.id == passageId) item.copy(
                    repetitionCurrent = newCurrent,
                    repetitionText = translateNums(
                        string = "$newCurrent/$total",
                        numeralsLanguage = numeralsLanguage
                    )
                )
                else item
            }
        )}

        return if (newCurrent == total) RepetitionResult.COMPLETED else RepetitionResult.COUNTED
    }

    private fun isTitleAvailable(title: String?) = !title.isNullOrEmpty()

    private fun isTranslationAvailable(translation: String?) =
        language != Language.ARABIC && !translation.isNullOrEmpty()

    private fun isVirtueAvailable(virtue: String?) = !virtue.isNullOrEmpty()

    private fun isReferenceAvailable(reference: String?) = !reference.isNullOrEmpty()

}