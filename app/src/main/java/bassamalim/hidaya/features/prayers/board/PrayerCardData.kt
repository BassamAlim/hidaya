package bassamalim.hidaya.features.prayers.board

import bassamalim.hidaya.core.enums.NotificationType

data class PrayerCardData(
    val name: String,
    val time: String,
    val status: Status,
    val notificationType: NotificationType,
    val isExtraReminderOffsetSpecified: Boolean,
    val extraReminderOffset: String
) {
    /** Only today's board marks passed and next prayers; other days are all [UPCOMING]. */
    enum class Status { PASSED, NEXT, UPCOMING }
}
