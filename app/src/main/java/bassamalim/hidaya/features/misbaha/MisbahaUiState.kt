package bassamalim.hidaya.features.misbaha

import bassamalim.hidaya.core.enums.Language

data class MisbahaUiState(
    val isLoading: Boolean = true,
    val countText: String = "",
    val roundsText: String = "",
    val progress: Float = 0f,
    val target: Int? = MisbahaViewModel.TARGETS.first(),
    val numeralsLanguage: Language = Language.ARABIC
)
