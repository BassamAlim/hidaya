package bassamalim.hidaya.core.data.repositories

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.utils.OsUtils
import bassamalim.hidaya.core.utils.report
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PrayerTimesReportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) {

    suspend fun submitReport(report: PrayerTimesReport): Boolean {
        if (!OsUtils.isNetworkAvailable(context)) return false

        return try {
            firestore.collection("PrayerTimesReports")
                .add(buildPayload(report) + ("created_at" to System.currentTimeMillis()))
                .await()
            true
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Failed to submit prayer times report: $e")
            false
        }
    }

    private fun buildPayload(report: PrayerTimesReport): Map<String, Any?> = mapOf(
        "device_id" to OsUtils.getDeviceId(context),
        "app_version" to getAppVersionName(),
        "language" to report.language.name,
        "location_type" to report.location?.type?.name,
        "latitude" to report.location?.coordinates?.latitude,
        "longitude" to report.location?.coordinates?.longitude,
        "country_id" to report.location?.ids?.countryId,
        "city_id" to report.location?.ids?.cityId,
        "location_name" to report.locationName,
        "calculation_method" to report.calculatorSettings.calculationMethod.name,
        "calculation_method_name" to getMethodName(report.calculatorSettings.calculationMethod.ordinal),
        "juristic_method" to report.calculatorSettings.juristicMethod.name,
        "high_latitudes_adjustment" to report.calculatorSettings.highLatitudesAdjustmentMethod.name,
        "computed_times" to report.computedTimes.mapKeys { it.key.name },
        "wrong_prayers" to report.wrongPrayers.map { prayer ->
            mapOf(
                "prayer" to prayer.name,
                "computed" to report.computedTimes[prayer],
                "correct" to report.correctTimes[prayer].orEmpty()
            )
        },
        "notes" to report.notes
    )

    private fun getAppVersionName(): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(Globals.TAG, "Package not found when reading version name: $e")
            ""
        }

    fun getMethodName(ordinal: Int): String =
        context.resources.getStringArray(R.array.prayer_times_calc_method_entries)
            .getOrNull(ordinal)
            .orEmpty()

}
