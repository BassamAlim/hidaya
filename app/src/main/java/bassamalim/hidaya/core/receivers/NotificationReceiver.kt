package bassamalim.hidaya.core.receivers

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Activity
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.NotificationsRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.enums.NotificationType
import bassamalim.hidaya.core.enums.Reminder
import bassamalim.hidaya.core.enums.ThemeColor
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.services.AthanService
import bassamalim.hidaya.core.ui.theme.getThemeColor
import bassamalim.hidaya.core.utils.LangUtils.withAppLocale
import bassamalim.hidaya.features.quran.reader.QuranTarget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationsRepository: NotificationsRepository
    @Inject lateinit var quranRepository: QuranRepository
    @Inject lateinit var appSettingsRepository: AppSettingsRepository
    private var notificationId = 0
    private var channelId = ""

    override fun onReceive(context: Context, intent: Intent) {
        // In the app language, not the device's (matters before API 33)
        val appContext = context.applicationContext.withAppLocale()

        val reminder = Reminder.getById(intent.getIntExtra("id", -1))
        Log.d(Globals.TAG, "notification receiver: received $reminder")
        val time = intent.getLongExtra("time", 0L)
        notificationId = reminder.id

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                if (!isOnTime(time) || isAlreadyNotified(reminder)) {
                    Log.i(Globals.TAG, "notification receiver: not on time or already notified")
                    return@launch
                }

                when (reminder) {
                    is Reminder.Prayer -> handlePrayerReminder(appContext, reminder, time)
                    is Reminder.PrayerExtra -> handlePrayerExtraReminder(appContext, reminder)
                    is Reminder.Devotional -> handleDevotionalReminder(appContext, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handlePrayerReminder(context: Context, reminder: Reminder.Prayer, time: Long) {
        Log.i(Globals.TAG, "in notification receiver for $reminder prayer")

        val notificationType = notificationsRepository.getNotificationType(reminder).first()

        if (notificationType != NotificationType.OFF) {
            if (notificationType == NotificationType.ATHAN) {
                // Starting a foreground service from a background alarm can be rejected by the
                // system (ForegroundServiceStartNotAllowedException on Android 12+). If that
                // happens, fall back to a regular notification instead of crashing.
                val started = startService(context, reminder, time)
                if (!started) showNotification(context, reminder, NotificationType.NOTIFICATION)
            }
            else showNotification(context, reminder, notificationType)
        }
    }

    private suspend fun handlePrayerExtraReminder(context: Context, reminder: Reminder.PrayerExtra) {
        Log.i(Globals.TAG, "in notification receiver for $reminder reminder")

        showNotification(context, reminder)
    }

    private suspend fun handleDevotionalReminder(context: Context, reminder: Reminder.Devotional) {
        Log.i(Globals.TAG, "in notification receiver for $reminder extra")

        showNotification(context, reminder)
    }

    private fun isOnTime(time: Long): Boolean {
        val max = time + 120000
        return System.currentTimeMillis() <= max
    }

    private suspend fun isAlreadyNotified(reminder: Reminder): Boolean {
        val lastDate = notificationsRepository.getLastNotificationDates().first()[reminder]
        return lastDate == Calendar.getInstance()[Calendar.DAY_OF_YEAR]
    }

    private suspend fun showNotification(
        context: Context,
        reminder: Reminder,
        notificationType: NotificationType = NotificationType.NOTIFICATION
    ) {
        createNotificationChannel(context, reminder)

        if (reminder is Reminder.Prayer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioManager = context.getSystemService(Service.AUDIO_SERVICE) as AudioManager
            // Request audio focus
            audioManager.requestAudioFocus(                   // Request permanent focus
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    ).build()
            )
        }

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
            val notification = build(context, reminder, notificationType)
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }

        markAsNotified(reminder)
    }

    /**
     * Tries to start the [AthanService] to play the athan.
     * Returns true if the service was started, false otherwise (e.g. the required permission is
     * missing, or the system rejected the foreground service start). When it returns false the
     * caller should fall back to a regular notification.
     */
    private fun startService(context: Context, reminder: Reminder.Prayer, time: Long): Boolean {
        val intent = Intent(context, AthanService::class.java).apply {
            action = Globals.PLAY_ATHAN
            putExtra("id", reminder.id)
            putExtra("time", time)
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val havePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else true

                if (!havePermission) return false

                context.startForegroundService(intent)
            }
            else
                context.startService(intent)

            markAsNotified(reminder)
            true
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (Android 12+) or any other failure while
            // starting the service from the background. Degrade gracefully to a notification.
            Log.e(Globals.TAG, "Failed to start AthanService", e)
            false
        }
    }

    private suspend fun build(
        context: Context,
        reminder: Reminder,
        notificationType: NotificationType
    ): Notification {
        return NotificationCompat.Builder(context, channelId).apply {
            setSmallIcon(R.drawable.small_launcher_foreground)
            setTicker(context.resources.getString(R.string.app_name))

            setContentTitle(getTitle(context, reminder))
            setContentText(getSubtitle(context, reminder))

            priority = NotificationCompat.PRIORITY_MAX
            setAutoCancel(true)
            setOnlyAlertOnce(true)
            this.color = getThemeColor(
                color = ThemeColor.SURFACE_CONTAINER,
                theme = appSettingsRepository.getTheme().first()
            ).toArgb()
            setContentIntent(onClick(context, reminder))

            if (notificationType == NotificationType.SILENT)
                setSilent(true)
        }.build()
    }

    private fun getTitle(context: Context, reminder: Reminder): String {
        return if ((reminder == Reminder.Prayer.Dhuhr || reminder == Reminder.PrayerExtra.Dhuhr) &&
            Calendar.getInstance()[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY) {
            if (reminder is Reminder.PrayerExtra)
                context.resources.getString(R.string.jumuah_reminder_title)
            else
                context.resources.getString(R.string.jumuah_title)
        }
        else context.resources.getStringArray(R.array.prayer_titles)[reminder.id-1]
    }

    private suspend fun getSubtitle(context: Context, reminder: Reminder): String {
        return if (reminder is Reminder.PrayerExtra) {
            val offset = notificationsRepository.getPrayerExtraReminderTimeOffsets()
                .first()[reminder]!!
            String.format(
                format =
                    if (offset < 0) context.resources.getString(R.string.reminder_before)
                    else context.resources.getString(R.string.reminder_after),
                    if (reminder == Reminder.PrayerExtra.Dhuhr &&
                        Calendar.getInstance()[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY)
                        context.resources.getString(R.string.jumuah)
                    else
                        context.resources.getStringArray(R.array.prayer_names)
                            [reminder.toPrayer().toReminder().id-1],
                abs(offset) // to remove - sign
            )
        }
        else {
            if (reminder == Reminder.Prayer.Dhuhr &&
                Calendar.getInstance()[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY)
                context.resources.getString(R.string.jumuah_subtitle)
            else context.resources.getStringArray(R.array.prayer_subtitles)[reminder.id-1]
        }
    }

    private suspend fun onClick(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, Activity::class.java)

        val route = when (reminder) {
            Reminder.Devotional.MorningRemembrances -> Screen.RemembranceReader(0.toString()).route
            Reminder.Devotional.EveningRemembrances -> Screen.RemembranceReader(1.toString()).route
            Reminder.Devotional.DailyWerd -> {
                Screen.QuranReader(
                    targetType = QuranTarget.PAGE.name,
                    targetValue = quranRepository.getWerdPageNum().first().toString()
                ).route
            }
            Reminder.Devotional.FridayKahf -> {
                Screen.QuranReader(
                    targetType = QuranTarget.SURA.name,
                    targetValue = 17.toString() // surat al-kahf
                ).route
            }
            else -> Screen.Main.route
        }
        intent.putExtra("start_route", route)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        return PendingIntent.getActivity(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel(context: Context, reminder: Reminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = getChannelId(reminder)
            val channel = NotificationChannel(
                channelId,
                getChannelName(context, reminder),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "description"
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getChannelId(reminder: Reminder): String {
        return when (reminder) {
            is Reminder.Prayer -> "prayers_channel"
            is Reminder.PrayerExtra -> "prayers_extra_channel"
            is Reminder.Devotional -> {
                when (reminder) {
                    Reminder.Devotional.MorningRemembrances,
                    Reminder.Devotional.EveningRemembrances ->
                        "morning_and_evening_remembrances_channel"
                    Reminder.Devotional.DailyWerd -> "daily_werd_channel"
                    Reminder.Devotional.FridayKahf -> "friday_kahf_channel"
                }
            }
        }
    }

    private fun getChannelName(context: Context, reminder: Reminder): String {
        return when (reminder) {
            is Reminder.Prayer -> context.getString(R.string.prayer_notification_channel)
            is Reminder.PrayerExtra -> context.getString(R.string.prayer_extra_notification_channel)
            is Reminder.Devotional -> {
                when (reminder) {
                    Reminder.Devotional.MorningRemembrances,
                    Reminder.Devotional.EveningRemembrances -> context.getString(
                        R.string.morning_and_evening_remembrances_notification_channel
                    )
                    Reminder.Devotional.DailyWerd -> context.getString(
                        R.string.daily_werd_notification_channel
                    )
                    Reminder.Devotional.FridayKahf -> context.getString(
                        R.string.friday_kahf_notification_channel
                    )
                }
            }
        }
    }

    private fun markAsNotified(reminder: Reminder) {
        notificationsRepository.setLastNotificationDate(
            reminder = reminder,
            dayOfYear = Calendar.getInstance()[Calendar.DAY_OF_YEAR]
        )
    }

}