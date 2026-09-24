package bassamalim.hidaya.features.qibla

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.models.Location
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class QiblaDomain @Inject constructor(
    private val app: Application,
    private val locationRepository: LocationRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    var location: Location? = null
    private val kaabaLat = 21.4224779
    private val kaabaLng = 39.8251832
    private val kaabaLatInRad = Math.toRadians(kaabaLat)
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

        if (location != null) bearing = calculateBearing()

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

    fun getDistance(): Double {
        val earthRadius = 6371.0
        val dLon = Math.toRadians(abs(location!!.coordinates.latitude - kaabaLat))
        val dLat = Math.toRadians(abs(location!!.coordinates.longitude - kaabaLng))
        val a = sin(dLat / 2) * sin(dLat / 2) +
                (cos(Math.toRadians(location!!.coordinates.latitude)) *
                cos(Math.toRadians(kaabaLat)) * sin(dLon / 2) * sin(dLon / 2))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        var distance = earthRadius * c
        distance = (distance * 10).toInt() / 10.0
        return distance
    }

    private fun calculateBearing(): Float {
        val myLatRad = Math.toRadians(location!!.coordinates.latitude)
        val lngDiff = Math.toRadians(kaabaLng - location!!.coordinates.longitude)
        val y = sin(lngDiff) * cos(kaabaLatInRad)
        val x = cos(myLatRad) * sin(kaabaLatInRad) -
                (sin(myLatRad) * cos(kaabaLatInRad) * cos(lngDiff))
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

}