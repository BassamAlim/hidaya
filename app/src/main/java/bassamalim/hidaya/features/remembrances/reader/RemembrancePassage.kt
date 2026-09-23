package bassamalim.hidaya.features.remembrances.reader

data class RemembrancePassage(
    val id: Int,
    val title: String?,
    val text: String,
    val translation: String? = null,
    val virtue: String?,
    val reference: String?,
    val repetitionText: String,
    val repetitionTotal: Int? = null,
    val repetitionCurrent: Int? = null,
    val isTitleAvailable: Boolean,
    val isTranslationAvailable: Boolean = false,
    val isVirtueAvailable: Boolean,
    val isReferenceAvailable: Boolean,
) {
    val isRepetitionComplete: Boolean
        get() = repetitionTotal != null && repetitionCurrent == repetitionTotal
}

enum class RepetitionResult {
    /** Not countable, or already complete */
    IGNORED,
    COUNTED,
    /** This tap reached the target */
    COMPLETED
}