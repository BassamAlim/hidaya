package bassamalim.hidaya.features.radio

import bassamalim.hidaya.core.data.repositories.LiveContentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioDomain @Inject constructor(
    private val liveContentRepository: LiveContentRepository
) {

    fun getUrl() = liveContentRepository.getRadioUrl()

}
