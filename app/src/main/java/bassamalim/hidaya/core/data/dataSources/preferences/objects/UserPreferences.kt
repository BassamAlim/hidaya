package bassamalim.hidaya.core.data.dataSources.preferences.objects

import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.models.QuizStats
import bassamalim.hidaya.core.models.UserRecord
import bassamalim.hidaya.core.models.VerseGuessStats
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val location: Location? = null,
    val userRecord: UserRecord = UserRecord(),
    val verseGuessStats: VerseGuessStats = VerseGuessStats(),
    val quizStats: QuizStats = QuizStats(),
)