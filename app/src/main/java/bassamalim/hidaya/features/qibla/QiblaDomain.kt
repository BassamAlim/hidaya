package bassamalim.hidaya.features.qibla

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.models.Coordinates
import bassamalim.hidaya.core.models.Location
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val KAABA_LAT = 21.4224779
private const val KAABA_LNG = 39.8251832

/** Degrees clockwise from true north to the Kaaba, in [0, 360). */
fun qiblaBearing(coordinates: Coordinates): Float {
    val myLatRad = Math.toRadians(coordinates.latitude)
    val kaabaLatRad = Math.toRadians(KAABA_LAT)
    val lngDiff = Math.toRadians(KAABA_LNG - coordinates.longitude)
    val y = sin(lngDiff) * cos(kaabaLatRad)
    val x = cos(myLatRad) * sin(kaabaLatRad) - (sin(myLatRad) * cos(kaabaLatRad) * cos(lngDiff))
    return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
}

/** Great-circle (haversine) distance to the Kaaba, in kilometers. */
fun kaabaDistanceKm(coordinates: Coordinates): Double {
    val earthRadius = 6371.0
    val dLat = Math.toRadians(KAABA_LAT - coordinates.latitude)
    val dLng = Math.toRadians(KAABA_LNG - coordinates.longitude)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(coordinates.latitude)) * cos(Math.toRadians(KAABA_LAT)) *
            sin(dLng / 2) * sin(dLng / 2)
    return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
}

@Singleton
class QiblaDomain @Inject constructor(
    private val app: Application,
    private val locationRepository: LocationRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    var location: Location? = null
    private var compass: Compass? = null
    private var currentAzimuth = 0F
    private var bearing = 0F

    suspend fun initialize(
        updateAccuracy: (Int) -> Unit,
        showUnsupported: () -> Unit,
        adjustQiblaDial: (Float) -> Unit,
        adjustNorthDial: (Float) -> Unit
    ) {
        location = locationRepository.getLocation().first()

        location?.let { bearing = qiblaBearing(it.coordinates) }

        setupCompass(
            updateAccuracy = updateAccuracy,
            showUnsupported = showUnsupported,
            adjustQiblaDial = adjustQiblaDial,
            adjustNorthDial = adjustNorthDial
        )
    }

    fun startCompass() {
        if (location != null) compass?.start()
    }

    fun stopCompass() {
        compass?.stop()
    }

    private fun setupCompass(
        updateAccuracy: (Int) -> Unit,
        showUnsupported: () -> Unit,
        adjustQiblaDial: (Float) -> Unit,
        adjustNorthDial: (Float) -> Unit
    ) {
        val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Checking features needed for Qibla
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
            && sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
            && app.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
            && app.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS))
            compass = Compass(app, object : Compass.CompassListener {
                override fun onNewAzimuth(azimuth: Float) {
                    adjustQiblaDial(
                        azimuth = azimuth,
                        adjustQiblaDial = adjustQiblaDial
                    )

                    adjustNorthDial(
                        azimuth = azimuth,
                        adjustNorthDial = adjustNorthDial
                    )
                }

                override fun calibration(accuracy: Int) {
                    updateAccuracy(accuracy)
                }
            })
        else showUnsupported()
    }

    private fun adjustQiblaDial(azimuth: Float, adjustQiblaDial: (Float) -> Unit) {
        val target = bearing - currentAzimuth
        currentAzimuth = azimuth

        adjustQiblaDial(target)
    }

    fun adjustNorthDial(azimuth: Float, adjustNorthDial: (Float) -> Unit) {
        currentAzimuth = azimuth

        adjustNorthDial(-azimuth)
    }

    /** Kilometers to the Kaaba, to one decimal. */
    fun getDistance(): Double = (kaabaDistanceKm(location!!.coordinates) * 10).toInt() / 10.0

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

}