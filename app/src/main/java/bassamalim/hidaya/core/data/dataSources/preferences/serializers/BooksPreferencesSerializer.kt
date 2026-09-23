package bassamalim.hidaya.core.data.dataSources.preferences.serializers

import androidx.datastore.core.Serializer
import bassamalim.hidaya.core.data.dataSources.preferences.objects.BooksPreferences
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object BooksPreferencesSerializer: Serializer<BooksPreferences> {

    override val defaultValue: BooksPreferences
        get() = BooksPreferences()

    override suspend fun readFrom(input: InputStream): BooksPreferences {
        return try {
            Json.decodeFromString(
                deserializer = BooksPreferences.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.report()
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: BooksPreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(
                    serializer = BooksPreferences.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }

}