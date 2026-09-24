package bassamalim.hidaya.core.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import bassamalim.hidaya.core.receivers.NextPrayerWidgetReceiver
import bassamalim.hidaya.core.receivers.PrayersWidgetReceiver

/**
 * Redraws every placed home screen widget, e.g. for new prayer times or after the language,
 * numerals or time format changed. Widgets only redraw when told to.
 */
fun refreshWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)

    for (receiver in listOf(PrayersWidgetReceiver::class.java, NextPrayerWidgetReceiver::class.java)) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver))
        if (ids.isEmpty()) continue

        context.sendBroadcast(
            Intent(context, receiver)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        )
    }
}
