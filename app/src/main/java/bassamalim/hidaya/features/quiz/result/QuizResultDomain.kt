package bassamalim.hidaya.features.quiz.result

import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizResultDomain @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) {

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

}