package bassamalim.hidaya.core.data.repositories

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class LiveContentRepository @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val dispatcher: CoroutineDispatcher
) {

    fun getQuranTvChannelId() = remoteConfig.getLong("quran_tv_channel_id")

    fun getSunnahTvChannelId() = remoteConfig.getLong("sunnah_tv_channel_id")

    /**
     * Fetches a freshly signed HLS url for the given SBA (Al Aloula) channel.
     * The signature expires within seconds, so the url must be fetched right before playback.
     */
    suspend fun getTvStreamUrl(channelId: Long): String = withContext(dispatcher) {
        val apiUrl = remoteConfig.getString("live_tv_stream_api_url")
            .replace("{channel_id}", channelId.toString())

        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val hlsUrl = Json.parseToJsonElement(body)
                .jsonObject["streams"]
                ?.jsonObject?.get("hls")
                ?.jsonPrimitive?.content
                ?: throw IOException("No HLS stream in the TV API response")

            // the dvr playlist carries hours of rewind history and is re-downloaded every few
            // seconds, while the plain live playlist is tiny and accepts the same token
            hlsUrl.replace("/playlist_dvr.m3u8", "/playlist.m3u8")
        } finally {
            connection.disconnect()
        }
    }

    fun getRadioUrl() = remoteConfig.getString("quran_radio_url")

}
