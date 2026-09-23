package bassamalim.hidaya.core.data.dataSources.preferences.objects

import bassamalim.hidaya.core.data.dataSources.preferences.serializers.customSerializers.IntBooleanPersistentMapSerializer
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.customSerializers.booksPreferences.ChapterFavoritesSerializer
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable

@Serializable
data class BooksPreferences(
    @Serializable(with = ChapterFavoritesSerializer::class)
    val chapterFavorites: PersistentMap<Int, PersistentMap<Int, Boolean>> = persistentMapOf(),
    val textSize: Float = 15f,
    @Serializable(with = IntBooleanPersistentMapSerializer::class)
    val searchSelections: PersistentMap<Int, Boolean> = persistentMapOf(),
    val searchMaxMatches: Int = 10,
    val shouldShowTutorial: Boolean = true,
    /** Where the user last stopped reading, keyed by book id */
    val readingPositions: Map<Int, BookReadingPosition> = emptyMap(),
)

@Serializable
data class BookReadingPosition(
    val chapterId: Int,
    /** Index of the first visible door in the chapter */
    val doorIndex: Int = 0,
    /** Pixel offset into that door, as reported by the list */
    val scrollOffset: Int = 0
)