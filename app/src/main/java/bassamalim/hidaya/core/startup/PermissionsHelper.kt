package bassamalim.hidaya.core.startup

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.utils.report
import javax.inject.Inject

class PermissionsHelper @Inject constructor() {

    fun hasBackgroundLocation(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
            return alarmManager?.canScheduleExactAlarms() != false
        }
        return true
    }

    fun openExactAlarmSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ContextCompat.getSystemService(activity, AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    e.report()
                    Log.e(Globals.TAG, "Failed to request exact alarm permission", e)
                }
            }
        }
    }

    fun extraPermissionsToRequest(context: Context): List<String> {
        val permissions = mutableListOf<String>()

        // Request background location permission for Android Q and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocation(context)) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        return permissions
    }

}
