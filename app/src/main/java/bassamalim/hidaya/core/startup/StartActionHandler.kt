package bassamalim.hidaya.core.startup

import android.app.Activity
import android.content.Intent
import android.util.Log
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.enums.StartAction
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.services.AthanService
import bassamalim.hidaya.core.utils.report
import javax.inject.Inject

data class StartActionResult(val overrideRoute: String? = null)

class StartActionHandler @Inject constructor(
    private val dbRecoveryHelper: DbRecoveryHelper,
) {

    suspend fun handle(activity: Activity, intent: Intent): StartActionResult {
        val action = intent.action
        if (action == null || action == Intent.ACTION_MAIN) return StartActionResult()

        try {
            val startAction = StartAction.valueOf(action)
            when (startAction) {
                StartAction.STOP_ATHAN -> {
                    activity.stopService(Intent(activity, AthanService::class.java))
                    Log.d(Globals.TAG, "Athan service stopped")
                }
                StartAction.GO_TO_RECITATION -> {
                    val mediaId = intent.getStringExtra("media_id")
                    if (mediaId != null) {
                        return StartActionResult(
                            overrideRoute = Screen.RecitationPlayer("back", mediaId).route
                        )
                    }
                    else {
                        Log.w(Globals.TAG, "Media ID not found for recitation")
                    }
                }
                StartAction.RESET_DATABASE -> {
                    dbRecoveryHelper.restoreData()
                }
            }
        } catch (e: IllegalArgumentException) {
            e.report()
            Log.e(Globals.TAG, "Unknown start action: $action", e)
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Error handling action: $action", e)
        }

        return StartActionResult()
    }

}
