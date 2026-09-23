package bassamalim.hidaya.core.data.dataSources.preferences.dataSources

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import bassamalim.hidaya.core.data.dataSources.preferences.objects.RemembrancesPreferences
import bassamalim.hidaya.core.utils.report
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RemembrancePreferencesDataSource(
    private val dataStore: DataStore<RemembrancesPreferences>
) {

    private val flow: Flow<RemembrancesPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                exception.report()
                emit(RemembrancesPreferences())
            }
            else throw exception
        }

    fun getFavorites() = flow.map { it.favorites }
    suspend fun updateFavorites(favorites: PersistentMap<Int, Boolean>) {
        dataStore.updateData { preferences ->
            preferences.copy(favorites = favorites)
        }
    }

    fun getTextSize() = flow.map { it.textSize }
    suspend fun updateTextSize(size: Float) {
        dataStore.updateData { preferences ->
            preferences.copy(textSize = size)
        }
    }

}