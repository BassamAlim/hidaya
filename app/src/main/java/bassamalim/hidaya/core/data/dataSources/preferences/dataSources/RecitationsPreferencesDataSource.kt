package bassamalim.hidaya.core.data.dataSources.preferences.dataSources

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import bassamalim.hidaya.core.data.dataSources.preferences.objects.RecitationsPreferences
import bassamalim.hidaya.core.enums.VerseRepeatMode
import bassamalim.hidaya.core.utils.report
import bassamalim.hidaya.features.recitations.recitersMenu.LastPlayedMedia
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RecitationsPreferencesDataSource(
    private val dataStore: DataStore<RecitationsPreferences>
) {

    private val flow: Flow<RecitationsPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                exception.report()
                emit(RecitationsPreferences())
            }
            else throw exception
        }

    fun getReciterFavorites() = flow.map { it.reciterFavorites }
    suspend fun updateReciterFavorites(favorites: PersistentMap<Int, Boolean>) {
        dataStore.updateData { preferences ->
            preferences.copy(reciterFavorites = favorites)
        }
    }

    fun getNarrationSelections() = flow.map { it.narrationSelections }
    suspend fun updateNarrationSelections(selections: PersistentMap<String, Boolean>) {
        dataStore.updateData { preferences ->
            preferences.copy(narrationSelections = selections)
        }
    }

    fun getRepeatMode() = flow.map { it.repeatMode }
    suspend fun updateRepeatMode(mode: Int) {
        dataStore.updateData { preferences ->
            preferences.copy(repeatMode = mode)
        }
    }

    fun getShuffleMode() = flow.map { it.shuffleMode }
    suspend fun updateShuffleMode(mode: Int) {
        dataStore.updateData { preferences ->
            preferences.copy(shuffleMode = mode)
        }
    }

    fun getLastPlayedMedia() = flow.map { it.lastPlayedMedia }
    suspend fun updateLastPlayedMedia(media: LastPlayedMedia?) {
        dataStore.updateData { preferences ->
            preferences.copy(lastPlayedMedia = media)
        }
    }

    fun getVerseReciterId() = flow.map { it.verseReciterId }
    suspend fun updateVerseReciterId(id: Int) {
        dataStore.updateData { preferences ->
            preferences.copy(verseReciterId = id)
        }
    }

    fun getVerseRepeatMode() = flow.map { it.verseRepeatMode }
    suspend fun updateVerseRepeatMode(mode: VerseRepeatMode) {
        dataStore.updateData { preferences ->
            preferences.copy(verseRepeatMode = mode)
        }
    }

    fun getShouldStopOnSuraEnd() = flow.map { it.shouldStopOnSuraEnd }
    suspend fun updateShouldStopOnSuraEnd(shouldStop: Boolean) {
        dataStore.updateData { preferences ->
            preferences.copy(shouldStopOnSuraEnd = shouldStop)
        }
    }

    fun getShouldStopOnPageEnd() = flow.map { it.shouldStopOnPageEnd }
    suspend fun updateShouldStopOnPageEnd(shouldStop: Boolean) {
        dataStore.updateData { preferences ->
            preferences.copy(shouldStopOnPageEnd = shouldStop)
        }
    }

}