package bassamalim.hidaya.core.data.dataSources.preferences.serializers

import androidx.datastore.core.Serializer
import bassamalim.hidaya.core.data.dataSources.preferences.objects.QuranPreferences
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object QuranPreferencesSerializer: Serializer<QuranPreferences> {

    override val defaultValue: QuranPreferences
        get() = QuranPreferences()

    override suspend fun readFrom(input: InputStream): QuranPreferences {
        return try {
            Json.decodeFromString(
                deserializer = QuranPreferences.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.report()
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: QuranPreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(
                    serializer = QuranPreferences.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }

}