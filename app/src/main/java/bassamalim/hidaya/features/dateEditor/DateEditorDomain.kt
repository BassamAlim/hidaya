package bassamalim.hidaya.features.dateEditor

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateEditorDomain @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) {

    private var dateOffset = 0

    suspend fun assignDateOffset() {
        dateOffset = appSettingsRepository.getDateOffset().first()
    }

    fun getDateOffset() = dateOffset

    fun incrementDateOffset() {
        dateOffset++
    }

    fun decrementDateOffset() {
        dateOffset--
    }

    fun saveDateOffset() {
        appSettingsRepository.setDateOffset(dateOffset)
    }

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

}