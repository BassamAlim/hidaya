package bassamalim.hidaya.features.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val domain: QiblaDomain,
    private val navigator: Navigator
): ViewModel() {

    private var numeralsLanguage = Language.ARABIC
    private var isReturningFromLocator = false

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState = _uiState.onStart {
        loadData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = QiblaUiState()
    )

    private suspend fun loadData() {
        numeralsLanguage = domain.getNumeralsLanguage()

        var isSupported = true
        domain.initialize(
            updateAccuracy = { newAccuracy ->
                _uiState.update { it.copy(accuracy = newAccuracy) }
            },
            showUnsupported = { isSupported = false },
            adjustQiblaDial = ::onQiblaAngleChange,
            adjustNorthDial = { compassAngle ->
                _uiState.update { it.copy(compassAngle = compassAngle) }
            }
        )

        val hasLocation = domain.location != null
        _uiState.update { it.copy(
            isLoading = false,
            errorMessageResId = when {
                !isSupported -> R.string.feature_not_supported
                !hasLocation -> R.string.location_permission_for_qibla
                else -> null
            },
            distanceToKaaba =
                if (hasLocation) translateNums(
                    numeralsLanguage = numeralsLanguage,
                    string = domain.getDistance().toString()
                )
                else ""
        )}
    }

    private fun onQiblaAngleChange(qiblaAngle: Float) {
        // The raw angle can be anywhere in -360..360; normalize to -180..180
        val offset = ((qiblaAngle % 360) + 540) % 360 - 180

        _uiState.update { it.copy(
            qiblaAngle = qiblaAngle,
            // Wider exit than entry so hovering at the edge doesn't flicker (and re-buzz)
            isOnPoint = abs(offset) < if (it.isOnPoint) ON_POINT_EXIT else ON_POINT_ENTER,
            offsetDegrees = offset.roundToInt(),
            offsetText = translateNums(
                numeralsLanguage = numeralsLanguage,
                string = "${abs(offset.roundToInt())}°"
            )
        )}
    }

    fun onStart() {
        // Coming back from setting the location: reload so the compass can start
        if (isReturningFromLocator) {
            isReturningFromLocator = false
            viewModelScope.launch {
                loadData()
                domain.startCompass()
            }
        }
        else domain.startCompass()
    }

    fun onStop() {
        domain.stopCompass()
    }

    fun onSetLocationClick() {
        isReturningFromLocator = true
        navigator.navigate(Screen.Locator(isInitial = false.toString()))
    }

    fun onAccuracyIndicatorClick() {
        _uiState.update { it.copy(calibrationDialogShown = true) }
    }

    fun onCalibrationDialogDismiss() {
        _uiState.update { it.copy(calibrationDialogShown = false) }
    }

    private companion object {
        const val ON_POINT_ENTER = 2f
        const val ON_POINT_EXIT = 5f
    }

}
