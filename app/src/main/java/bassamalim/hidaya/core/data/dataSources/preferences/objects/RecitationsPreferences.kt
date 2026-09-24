package bassamalim.hidaya.core.data.dataSources.preferences.objects

import androidx.media3.common.Player
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.customSerializers.IntBooleanPersistentMapSerializer
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.customSerializers.StringBooleanPersistentMapSerializer
import bassamalim.hidaya.core.enums.VerseRepeatMode
import bassamalim.hidaya.features.recitations.recitersMenu.LastPlayedMedia
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable

@Serializable
data class RecitationsPreferences(
    @Serializable(with = IntBooleanPersistentMapSerializer::class)
    val reciterFavorites: PersistentMap<Int, Boolean> = persistentMapOf(),
    @Serializable(with = StringBooleanPersistentMapSerializer::class)
    val narrationSelections: PersistentMap<String, Boolean> = persistentMapOf(),
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    /** 0 off, 1 on (the old PlaybackStateCompat values, kept so saved settings still read) */
    val shuffleMode: Int = 0,
    val lastPlayedMedia: LastPlayedMedia? = null,
    val verseReciterId: Int = 13,
    val verseRepeatMode: VerseRepeatMode = VerseRepeatMode.NO_REPEAT,
    val shouldStopOnSuraEnd: Boolean = false,
    val shouldStopOnPageEnd: Boolean = false,
)