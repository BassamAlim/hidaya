package bassamalim.hidaya.core.data.dataSources.preferences.serializers

import androidx.datastore.core.Serializer
import bassamalim.hidaya.core.data.dataSources.preferences.objects.RemembrancesPreferences
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object RemembrancesPreferencesSerializer: Serializer<RemembrancesPreferences> {

    override val defaultValue: RemembrancesPreferences
        get() = RemembrancesPreferences()

    override suspend fun readFrom(input: InputStream): RemembrancesPreferences {
        return try {
            Json.decodeFromString(
                deserializer = RemembrancesPreferences.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.report()
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: RemembrancesPreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(
                    serializer = RemembrancesPreferences.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }

}