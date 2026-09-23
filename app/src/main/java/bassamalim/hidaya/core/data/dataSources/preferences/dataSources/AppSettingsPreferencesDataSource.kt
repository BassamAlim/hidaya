package bassamalim.hidaya.core.data.dataSources.preferences.dataSources

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import bassamalim.hidaya.core.data.dataSources.preferences.objects.AppSettingsPreferences
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.enums.TimeFormat
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AppSettingsPreferencesDataSource(
    private val dataStore: DataStore<AppSettingsPreferences>
) {

    private val flow: Flow<AppSettingsPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                exception.report()
                emit(AppSettingsPreferences())
            }
            else throw exception
        }

    fun getNumeralsLanguage() = flow.map { it.numeralsLanguage }
    suspend fun updateNumeralsLanguage(numeralsLanguage: Language) {
        dataStore.updateData { preferences ->
            preferences.copy(numeralsLanguage = numeralsLanguage)
        }
    }

    fun getTheme() = flow.map { it.theme }
    suspend fun updateTheme(theme: Theme) {
        dataStore.updateData { preferences ->
            preferences.copy(theme = theme)
        }
    }

    fun getTimeFormat() = flow.map { it.timeFormat }
    suspend fun updateTimeFormat(timeFormat: TimeFormat) {
        dataStore.updateData { preferences ->
            preferences.copy(timeFormat = timeFormat)
        }
    }

    fun getDateOffset() = flow.map { it.dateOffset }
    suspend fun updateDateOffset(dateOffset: Int) {
        dataStore.updateData { preferences ->
            preferences.copy(dateOffset = dateOffset)
        }
    }

}