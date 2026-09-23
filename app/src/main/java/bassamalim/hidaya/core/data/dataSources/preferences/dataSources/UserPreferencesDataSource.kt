package bassamalim.hidaya.core.data.dataSources.preferences.dataSources

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import bassamalim.hidaya.core.data.dataSources.preferences.objects.UserPreferences
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.models.UserRecord
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class UserPreferencesDataSource(
    private val dataStore: DataStore<UserPreferences>
) {

    private val flow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                exception.report()
                emit(UserPreferences())
            }
            else throw exception
        }

    fun getLocation() = flow.map { it.location }
    suspend fun updateLocation(location: Location) {
        dataStore.updateData {
            it.copy(location = location)
        }
    }

    fun getUserRecord() = flow.map { it.userRecord }
    suspend fun updateUserRecord(userRecord: UserRecord) {
        dataStore.updateData {
            it.copy(userRecord = userRecord)
        }
    }

}