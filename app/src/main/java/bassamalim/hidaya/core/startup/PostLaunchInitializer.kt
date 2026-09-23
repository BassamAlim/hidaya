package bassamalim.hidaya.core.startup

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.helpers.Alarm
import bassamalim.hidaya.core.models.Response
import bassamalim.hidaya.core.receivers.DailyUpdateReceiver
import bassamalim.hidaya.core.receivers.DeviceBootReceiver
import bassamalim.hidaya.core.services.PrayersNotificationService
import bassamalim.hidaya.core.utils.OsUtils
import bassamalim.hidaya.core.utils.PrayerTimeUtils
import bassamalim.hidaya.core.utils.report
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class PostLaunchInitializer @Inject constructor(
    private val app: Application,
    private val prayersRepository: PrayersRepository,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository,
    private val alarm: Alarm,
) {

    companion object {
        private const val FIREBASE_FETCH_INTERVAL = 3600 * 6L // 6 hours
    }

    suspend fun run(activity: Activity) {
        try {
            initFirebase()
            fetchAndActivateRemoteConfig()
            dailyUpdate(activity)
            setAlarms()
            setupBootReceiver(activity)
            startPrayerServiceIfNeeded(activity)
            registerLeaderboardUser()
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Error during post-launch initialization", e)
        }
    }

    private fun initFirebase() {
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(FIREBASE_FETCH_INTERVAL)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
            Log.d(Globals.TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Failed to initialize Firebase", e)
        }
    }

    private fun fetchAndActivateRemoteConfig() {
        try {
            FirebaseRemoteConfig.getInstance()
                .fetchAndActivate()
                .addOnSuccessListener {
                    Log.i(Globals.TAG, "RemoteConfig update successful")
                }
                .addOnFailureListener { e ->
                    Log.e(Globals.TAG, "RemoteConfig update failed", e)
                }
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Error fetching remote config", e)
        }
    }

    private fun dailyUpdate(activity: Activity) {
        try {
            activity.sendBroadcast(
                Intent(activity, DailyUpdateReceiver::class.java).apply {
                    action = "daily"
                }
            )
            Log.d(Globals.TAG, "Daily update broadcast sent")
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Failed to send daily update broadcast", e)
        }
    }

    private suspend fun setAlarms() {
        try {
            val location = locationRepository.getLocation().first()
            if (location != null) {
                val prayerTimes = PrayerTimeUtils.getPrayerTimes(
                    settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
                    selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
                    location = location,
                    calendar = Calendar.getInstance()
                )
                alarm.setAll(prayerTimes)
                Log.d(Globals.TAG, "Prayer alarms set successfully")
            }
            else {
                Log.w(Globals.TAG, "Cannot set alarms - location is null")
            }
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Failed to set prayer alarms", e)
        }
    }

    private fun setupBootReceiver(activity: Activity) {
        activity.packageManager.setComponentEnabledSetting(
            ComponentName(activity, DeviceBootReceiver::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun runPrayerReminderService(activity: Activity) {
        try {
            val serviceIntent = Intent(activity, PrayersNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(serviceIntent)
            }
            else {
                activity.startService(serviceIntent)
            }
            Log.d(Globals.TAG, "Prayer reminder service started")
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Failed to start prayer reminder service", e)
        }
    }

    private suspend fun startPrayerServiceIfNeeded(activity: Activity) {
        try {
            val isNotificationEnabled =
                prayersRepository.getContinuousPrayersNotificationEnabled().first()
            if (isNotificationEnabled && !PrayersNotificationService.isRunning) {
                runPrayerReminderService(activity)
            }
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Failed to check prayer notification settings", e)
        }
    }

    private suspend fun registerLeaderboardUser() {
        val deviceId = OsUtils.getDeviceId(app)
        val remoteRecord = userRepository.getRemoteRecord(deviceId)?.first()

        if (remoteRecord is Response.Error && remoteRecord.message == "Device not registered") {
            val newRecord = userRepository.registerDevice(deviceId)
            if (newRecord != null) {
                userRepository.setLocalRecord(newRecord)
            }
        }
    }

}
