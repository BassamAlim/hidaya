package bassamalim.hidaya.core.startup

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.enums.LocationType
import bassamalim.hidaya.core.utils.report
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LocationStartupHelper @Inject constructor(
    private val locationRepository: LocationRepository,
) {

    suspend fun getStoredLocationType(): LocationType? {
        return locationRepository.getLocation().first()?.type
    }

    fun hasFineAndCoarsePermission(activity: Activity): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun fetchLastLocation(activity: Activity, onResult: (Location?) -> Unit) {
        try {
            LocationServices.getFusedLocationProviderClient(activity)
                .lastLocation
                .addOnSuccessListener { location: Location? ->
                    onResult(location)
                }
                .addOnFailureListener { e ->
                    Log.e(Globals.TAG, "Failed to get location", e)
                    onResult(null)
                }
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Error during location request", e)
            onResult(null)
        }
    }

    suspend fun storeLocation(location: Location) {
        try {
            locationRepository.setLocation(location)
            Log.d(Globals.TAG, "Location stored: lat=${location.latitude}, " +
                    "lng=${location.longitude}")
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Failed to store location", e)
        }
    }

}
