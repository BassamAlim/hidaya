package bassamalim.hidaya.features.recitations.player

import androidx.media3.common.Player
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.PlaybackStatus

data class RecitationPlayerUiState(
    val isLoading: Boolean = true,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isShuffleOn: Boolean = false,
    val duration: String = "00:00",
    val progress: String = "00:00",
    val secondaryProgress: Long = 0,
    val btnState: PlaybackStatus = PlaybackStatus.CONNECTING,
    val suraName: String = "",
    val narrationName: String = "",
    val reciterName: String = "",
    val controlsEnabled: Boolean = false,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
)
