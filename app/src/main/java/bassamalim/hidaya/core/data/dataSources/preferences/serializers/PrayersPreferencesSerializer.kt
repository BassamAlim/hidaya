package bassamalim.hidaya.core.data.dataSources.preferences.serializers

import androidx.datastore.core.Serializer
import bassamalim.hidaya.core.data.dataSources.preferences.objects.PrayersPreferences
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object PrayersPreferencesSerializer: Serializer<PrayersPreferences> {

    override val defaultValue: PrayersPreferences
        get() = PrayersPreferences()

    override suspend fun readFrom(input: InputStream): PrayersPreferences {
        return try {
            Json.decodeFromString(
                deserializer = PrayersPreferences.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.report()
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: PrayersPreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(
                    serializer = PrayersPreferences.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }

}