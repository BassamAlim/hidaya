package bassamalim.hidaya.core.data.dataSources.preferences.dataSources

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import bassamalim.hidaya.core.data.dataSources.preferences.objects.AppStatePreferences
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AppStatePreferencesDataSource(
    private val dataStore: DataStore<AppStatePreferences>
) {

    private val flow: Flow<AppStatePreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                exception.report()
                emit(AppStatePreferences())
            }
            else throw exception
        }

    fun getOnboardingCompleted() = flow.map { it.isOnboardingCompleted }
    suspend fun updateOnboardingCompleted(isCompleted: Boolean) {
        dataStore.updateData { preferences ->
            preferences.copy(isOnboardingCompleted = isCompleted)
        }
    }

    fun getLastDailyUpdateMillis() = flow.map { it.lastDailyUpdateMillis }
    suspend fun updateLastDailyUpdateMillis(millis: Long) {
        dataStore.updateData { preferences ->
            preferences.copy(lastDailyUpdateMillis = millis)
        }
    }

    fun getLastDBVersion() = flow.map { it.lastDBVersion }
    suspend fun updateLastDBVersion(version: Int) {
        dataStore.updateData { preferences ->
            preferences.copy(lastDBVersion = version)
        }
    }

}