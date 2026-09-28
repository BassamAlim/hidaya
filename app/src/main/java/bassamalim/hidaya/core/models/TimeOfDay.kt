package bassamalim.hidaya.core.models

import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
data class TimeOfDay(
    val hour: Int,
    val minute: Int,
    val second: Int = 0
) {
    companion object {
        fun of(time: LocalTime) =
            TimeOfDay(hour = time.hour, minute = time.minute, second = time.second)
    }
}
