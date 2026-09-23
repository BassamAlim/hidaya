package bassamalim.hidaya.core.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import bassamalim.hidaya.core.enums.StartAction

object ActivityUtils {

    fun restartApplication(activity: Activity) {
        val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        activity.startActivity(
            Intent.makeRestartActivityTask(intent?.component).apply {
                action = StartAction.RESET_DATABASE.name
            }
        )
        Runtime.getRuntime().exit(0)
    }

    fun clearAppData(context: Context) {
        try {
            context.getSystemService(ActivityManager::class.java).clearApplicationUserData()
        } catch (e: Exception) {
            e.report()
            e.printStackTrace()
        }
    }

}