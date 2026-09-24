package bassamalim.hidaya.features.radio

import bassamalim.hidaya.core.enums.PlaybackStatus

data class RadioClientUiState(
    val btnState: PlaybackStatus = PlaybackStatus.STOPPED
)
