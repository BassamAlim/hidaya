package bassamalim.hidaya.core.receivers

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.enums.LocationType
import bassamalim.hidaya.core.helpers.Alarm
import bassamalim.hidaya.core.utils.PrayerTimeUtils
import bassamalim.hidaya.core.utils.report
import bassamalim.hidaya.core.widgets.refreshWidgets
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.Random
import javax.inject.Inject

@AndroidEntryPoint
class DailyUpdateReceiver : BroadcastReceiver() {

    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var prayersRepository: PrayersRepository
    @Inject lateinit var quranRepository: QuranRepository
    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var alarm: Alarm

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(Globals.TAG, "in DailyUpdateReceiver")

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                // No DB revival here: deleting the file under the app's open Room instance
                // corrupts it. The next app launch checks and revives (DbRecoveryHelper).
                val now = Calendar.getInstance()
                if ((intent.action == "daily" && notUpdatedToday(now)) || intent.action == "boot") {
                    val location = locationRepository.getLocation().first() ?: return@launch
                    when (location.type) {
                        // Awaited, so alarms are set before goAsync() finishes and the process
                        // can be killed. No fresh fix still updates, from the stored location.
                        LocationType.AUTO ->
                            update(context = context, location = lastLocation(context), now = now)
                        LocationType.MANUAL -> update(context = context, location = null, now = now)
                        LocationType.NONE -> return@launch
                    }

                    pickWerd()
                }
                else Log.i(Globals.TAG, "dead intent in daily update receiver")
            } finally {
                // Every path, including early returns and failures, or the daily chain stops
                // until the app is opened again, and with it the athan alarms
                try {
                    setTomorrow(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun notUpdatedToday(now: Calendar): Boolean {
        val lastUpdate = Calendar.getInstance()
        lastUpdate.timeInMillis = appStateRepository.getLastDailyUpdateMillis().first()
        return !isSameDay(lastUpdate, now)
    }

    /** Null without permission or on failure (e.g. no background location access). */
    private suspend fun lastLocation(context: Context): Location? {
        val hasPermission = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        return try {
            // goAsync() has a time budget; on timeout the stored location is used instead
            withTimeoutOrNull(5_000) {
                LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
            }
        } catch (e: Exception) {
            e.report()
            null
        }
    }

    private suspend fun update(context: Context, location: Location?, now: Calendar) {
        if (location != null)
            locationRepository.setLocation(location)

        val latestLocation = locationRepository.getLocation().first()
        if (latestLocation == null) {
            Log.e(Globals.TAG, "no available location in DailyUpdate")
            return
        }

        val prayerTimes = PrayerTimeUtils.getPrayerTimes(
            settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
            selectedTimeZoneId = locationRepository.getTimeZone(latestLocation.ids.cityId),
            location = latestLocation,
            calendar = now
        )

        alarm.setAll(prayerTimes)

        refreshWidgets(context)

        setUpdated(now)
    }

    private fun setUpdated(now: Calendar) {
        appStateRepository.setLastDailyUpdateMillis(now.timeInMillis)
    }

    private suspend fun pickWerd() {
        val randomWerd = Random().nextInt(Globals.NUM_OF_QURAN_PAGES)
        quranRepository.setWerdPageNum(randomWerd)
        quranRepository.setWerdDone(false)
    }

    private fun setTomorrow(context: Context) {
        val intent = Intent(context.applicationContext, DailyUpdateReceiver::class.java)
        intent.action = "daily"

        val time = nextDailyUpdateTime(Calendar.getInstance())

        val pendIntent = PendingIntent.getBroadcast(
            context.applicationContext, 1210, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarm = context.applicationContext
            .getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.timeInMillis, pendIntent)
    }

}

/** Same calendar day. Comparing only the day of month took a month-old update for today's. */
internal fun isSameDay(a: Calendar, b: Calendar) =
    a[Calendar.YEAR] == b[Calendar.YEAR] && a[Calendar.DAY_OF_YEAR] == b[Calendar.DAY_OF_YEAR]

/** The next day's update time, in [now]'s time zone. */
internal fun nextDailyUpdateTime(now: Calendar): Calendar = (now.clone() as Calendar).apply {
    add(Calendar.DATE, 1)
    set(Calendar.HOUR_OF_DAY, Globals.DAILY_UPDATE_HOUR)
    set(Calendar.MINUTE, Globals.DAILY_UPDATE_MINUTE)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
