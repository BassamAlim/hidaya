package bassamalim.hidaya.core.data.dataSources.preferences.dataSources

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import bassamalim.hidaya.core.data.dataSources.preferences.objects.UserPreferences
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.models.UserRecord
import bassamalim.hidaya.core.models.VerseGuessStats
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
    /** Atomic read-modify-write, so concurrent page and listening updates don't overwrite each other. */
    suspend fun updateUserRecord(transform: (UserRecord) -> UserRecord): UserRecord =
        dataStore.updateData {
            it.copy(userRecord = transform(it.userRecord))
        }.userRecord

    fun getVerseGuessStats() = flow.map { it.verseGuessStats }
    /** Atomic across both, since a game round updates the local stats and the synced record. */
    suspend fun updateVerseGuess(
        transform: (VerseGuessStats, UserRecord) -> Pair<VerseGuessStats, UserRecord>
    ) {
        dataStore.updateData {
            val (stats, record) = transform(it.verseGuessStats, it.userRecord)
            it.copy(verseGuessStats = stats, userRecord = record)
        }
    }

}