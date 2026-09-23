package bassamalim.hidaya.core.data.dataSources.preferences.dataSources

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import bassamalim.hidaya.core.data.dataSources.preferences.objects.BooksPreferences
import bassamalim.hidaya.core.utils.report
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class BooksPreferencesDataSource(
    private val dataStore: DataStore<BooksPreferences>
) {

    private val flow: Flow<BooksPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                exception.report()
                emit(BooksPreferences())
            }
            else throw exception
        }

    fun getChapterFavorites() = flow.map { it.chapterFavorites }
    suspend fun updateChapterFavorites(favorites: PersistentMap<Int, PersistentMap<Int, Boolean>>) {
        dataStore.updateData { preferences ->
            preferences.copy(chapterFavorites = favorites)
        }
    }

    fun getTextSize() = flow.map { it.textSize }
    suspend fun updateTextSize(size: Float) {
        dataStore.updateData { preferences ->
            preferences.copy(textSize = size)
        }
    }

    fun getSearchSelections() = flow.map { it.searchSelections }
    suspend fun updateSearchSelections(selections: PersistentMap<Int, Boolean>) {
        dataStore.updateData { preferences ->
            preferences.copy(searchSelections = selections)
        }
    }

    fun getSearchMaxMatches() = flow.map { it.searchMaxMatches }
    suspend fun updateSearchMaxMatches(maxMatches: Int) {
        dataStore.updateData { preferences ->
            preferences.copy(searchMaxMatches = maxMatches)
        }
    }

    fun getShouldShowTutorial() = flow.map { it.shouldShowTutorial }
    suspend fun updateShouldShowTutorial(shouldShow: Boolean) {
        dataStore.updateData { preferences ->
            preferences.copy(shouldShowTutorial = shouldShow)
        }
    }

}