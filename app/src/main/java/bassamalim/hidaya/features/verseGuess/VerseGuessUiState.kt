package bassamalim.hidaya.features.verseGuess

import bassamalim.hidaya.features.verseGuess.map.VerseMapItem

data class VerseGuessUiState(
    val isLoading: Boolean = true,
    val suraNames: List<String> = emptyList(),
    /** Juz numbers in the user's numerals, for juz 1 to 30 */
    val juzNumTexts: List<String> = emptyList(),
    /** 0 for the whole Quran */
    val scopeJuz: Int = 0,
    /** The verses in scope */
    val items: List<VerseMapItem> = emptyList(),
    /** Index into [items] */
    val selectedIndex: Int? = null
)
