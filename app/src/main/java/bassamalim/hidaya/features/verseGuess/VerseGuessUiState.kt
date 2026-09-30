package bassamalim.hidaya.features.verseGuess

import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.features.verseGuess.map.VerseMapItem

enum class VerseGuessPhase {
    SETUP,
    ROUND
}

/** A clip verse's mushaf text, apart from its end marker, which carries the verse's number. */
data class ClipVerse(val text: String, val marker: String) {
    companion object {
        /** The decorated text ends in the marker glyph, after a space. */
        fun fromDecoratedText(decoratedText: String) = decoratedText.trimEnd().let {
            ClipVerse(text = it.substringBeforeLast(' '), marker = it.substringAfterLast(' '))
        }
    }
}

/** Indices point into [VerseGuessUiState.items]. */
data class RoundResult(
    val clipStart: Int,
    val guessIndex: Int,
    val distancePages: Double,
    val points: Int
) {
    val isExact get() = distancePages == 0.0
}

/** The user's standing across every game. */
data class VerseGuessProgress(
    val totalPoints: Long = 0L,
    val roundsPlayed: Int = 0,
    /** Over the latest rounds; null before the first */
    val recentAverage: Int? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

data class VerseGuessUiState(
    val isLoading: Boolean = true,
    val phase: VerseGuessPhase = VerseGuessPhase.SETUP,
    val numeralsLanguage: Language = Language.ARABIC,
    val suraNames: List<String> = emptyList(),
    /** Juz numbers in the user's numerals, for juz 1 to 30 */
    val juzNumTexts: List<String> = emptyList(),
    /** 0 for the whole Quran */
    val scopeJuz: Int = 0,
    /** Indexed by reciter id */
    val reciterNames: List<String> = emptyList(),
    val reciterId: Int = 0,
    /** The verses in scope */
    val items: List<VerseMapItem> = emptyList(),
    val progress: VerseGuessProgress = VerseGuessProgress(),
    /** Rounds this session, the current one included */
    val sessionRounds: Int = 0,
    val sessionPoints: Long = 0L,
    val clipStart: Int? = null,
    /** The current round's verses */
    val clipVerses: List<ClipVerse> = emptyList(),
    val selectedIndex: Int? = null,
    val playback: ClipPlayback = ClipPlayback(),
    /** Set once the current round is guessed */
    val result: RoundResult? = null
) {
    val isRevealed get() = result != null
}
