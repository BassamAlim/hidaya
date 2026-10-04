package bassamalim.hidaya.features.quiz.lobby

import bassamalim.hidaya.core.enums.Language

data class QuizLobbyUiState(
    val isLoading: Boolean = true,
    val quizCategories: List<String> = emptyList(),
    val progress: QuizProgress = QuizProgress(),
    val numeralsLanguage: Language = Language.ARABIC
)

data class QuizProgress(
    val answered: Int = 0,
    val correct: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    /** 0..100, of the whole question bank */
    val learnedPercent: Double = 0.0
) {
    /** 0..100, or null before the first answer */
    val accuracyPercent get() = if (answered == 0) null else correct * 100 / answered
}
