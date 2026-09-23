package bassamalim.hidaya.core.data.dataSources.preferences.serializers

import androidx.datastore.core.Serializer
import bassamalim.hidaya.core.data.dataSources.preferences.objects.AppSettingsPreferences
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object AppSettingsPreferencesSerializer: Serializer<AppSettingsPreferences> {

    override val defaultValue: AppSettingsPreferences
        get() = AppSettingsPreferences()

    override suspend fun readFrom(input: InputStream): AppSettingsPreferences {
        return try {
            Json.decodeFromString(
                deserializer = AppSettingsPreferences.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.report()
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppSettingsPreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(
                    serializer = AppSettingsPreferences.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }

}