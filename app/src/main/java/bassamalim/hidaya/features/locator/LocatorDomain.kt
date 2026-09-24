package bassamalim.hidaya.features.locator

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.app.ActivityCompat
import bassamalim.hidaya.core.data.repositories.LocationRepository
import com.google.android.gms.location.LocationServices
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocatorDomain @Inject constructor(
    private val app: Application,
    private val locationRepository: LocationRepository
) {

    private lateinit var locationRequestLauncher:
            ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    private lateinit var showBackgroundLocationPermissionNeeded: () -> Unit
    private lateinit var launch: () -> Unit

    @SuppressLint("MissingPermission")
    fun locate() {
        if (isPermissionsGranted()) {
            LocationServices.getFusedLocationProviderClient(app).lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) setAutoLocation(location)

                    requestBackgroundLocationPermission()

                    launch()
                }
                .addOnFailureListener {
                    launch()
                }
        }
        else requestPermissions()
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ActivityCompat.checkSelfPermission(
                app,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

            showBackgroundLocationPermissionNeeded()

            locationRequestLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            )
        }
    }

    private fun requestPermissions() {
        locationRequestLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    fun handleLocationRequestResult(result: Map<String, Boolean>) {
        if (result.keys.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            return

        val fineLoc = result[Manifest.permission.ACCESS_FINE_LOCATION]
        val coarseLoc = result[Manifest.permission.ACCESS_COARSE_LOCATION]
        if (fineLoc != null && fineLoc && coarseLoc != null && coarseLoc) {
            locate()
        }
        else launch()
    }

    private fun setAutoLocation(location: Location) {
        locationRepository.setLocation(location)
    }

    suspend fun setManualLocation(cityId: Int) {
        val city = locationRepository.getCity(cityId)
        locationRepository.setLocation(
            countryId = city.countryId,
            cityId = city.id
        )
    }

    private fun isPermissionsGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            app, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    app, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun setShowBackgroundLocationPermissionNeeded(showToast: () -> Unit) {
        this.showBackgroundLocationPermissionNeeded = showToast
    }

    fun setLocationRequestLauncher(
        locationRequestLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    ) {
        this.locationRequestLauncher = locationRequestLauncher
    }

    fun setLaunch(launch: () -> Unit) {
        this.launch = launch
    }

}