package bassamalim.hidaya.core.data.dataSources.preferences.serializers

import androidx.datastore.core.Serializer
import bassamalim.hidaya.core.data.dataSources.preferences.objects.NotificationsPreferences
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.InputStream
import java.io.OutputStream

object NotificationsPreferencesSerializer: Serializer<NotificationsPreferences> {

    override val defaultValue: NotificationsPreferences
        get() = NotificationsPreferences()

    override suspend fun readFrom(input: InputStream): NotificationsPreferences {
        return try {
            json.decodeFromString(
                deserializer = NotificationsPreferences.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.report()
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: NotificationsPreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                json.encodeToString(
                    serializer = NotificationsPreferences.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }

}