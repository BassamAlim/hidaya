package bassamalim.hidaya.features.quiz.result

import bassamalim.hidaya.core.enums.Language

data class QuizResultUiState(
    val isLoading: Boolean = true,
    /** 0..100, kept numeric; numerals are translated only when displayed */
    val scorePercent: Int = 0,
    val numeralsLanguage: Language = Language.ARABIC,
    val questions: List<QuizResultQuestion> = emptyList(),
)
