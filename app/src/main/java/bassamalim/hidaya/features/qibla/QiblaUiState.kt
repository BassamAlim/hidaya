package bassamalim.hidaya.features.qibla

import androidx.annotation.StringRes

data class QiblaUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessageResId: Int? = null,
    val qiblaAngle: Float = 0F,
    val compassAngle: Float = 0F,
    val accuracy: Int = 0,
    val distanceToKaaba: String = "",
    val isOnPoint: Boolean = false,
    /** How far off the Qibla the phone points; positive means turn right. */
    val offsetDegrees: Int = 0,
    val offsetText: String = "",
    val calibrationDialogShown: Boolean = false
)
