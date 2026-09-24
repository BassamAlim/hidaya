package bassamalim.hidaya.features.tv

import bassamalim.hidaya.core.data.repositories.AnalyticsRepository
import bassamalim.hidaya.core.data.repositories.LiveContentRepository
import bassamalim.hidaya.core.models.AnalyticsEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvDomain @Inject constructor(
    private val liveContentRepository: LiveContentRepository,
    private val analyticsRepository: AnalyticsRepository
) {

    suspend fun getStreamUrl(channel: TvChannel): String {
        val channelId = when (channel) {
            TvChannel.QURAN -> liveContentRepository.getQuranTvChannelId()
            TvChannel.SUNNAH -> liveContentRepository.getSunnahTvChannelId()
        }
        return liveContentRepository.getTvStreamUrl(channelId)
    }

    fun trackTvChannelViewed(channel: TvChannel) {
        analyticsRepository.trackEvent(AnalyticsEvent.TvChannelViewed(channel.analyticsName))
    }

}
