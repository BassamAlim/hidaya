package bassamalim.hidaya.core.data.dataSources.preferences.serializers

import androidx.datastore.core.Serializer
import bassamalim.hidaya.core.data.dataSources.preferences.objects.AppStatePreferences
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object AppStatePreferencesSerializer: Serializer<AppStatePreferences> {

    override val defaultValue: AppStatePreferences
        get() = AppStatePreferences()

    override suspend fun readFrom(input: InputStream): AppStatePreferences {
        return try {
            Json.decodeFromString(
                deserializer = AppStatePreferences.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.report()
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppStatePreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(
                    serializer = AppStatePreferences.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }

}