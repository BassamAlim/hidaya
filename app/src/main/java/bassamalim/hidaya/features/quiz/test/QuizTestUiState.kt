package bassamalim.hidaya.features.quiz.test

import bassamalim.hidaya.core.enums.Language

data class QuizTestUiState(
    val isLoading: Boolean = true,
    val question: String = "",
    val answers: List<String> = emptyList(),
    val correctIndex: Int = -1,
    /** Set once the user answers, which reveals the correct answer */
    val chosenIndex: Int? = null,
    /** Background on the answer, shown with the reveal */
    val description: String? = null,
    val sessionAnswered: Int = 0,
    val sessionCorrect: Int = 0,
    val currentStreak: Int = 0,
    val numeralsLanguage: Language = Language.ARABIC
) {
    val isRevealed get() = chosenIndex != null
}
