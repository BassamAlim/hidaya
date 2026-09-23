package bassamalim.hidaya.core.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Activity
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.di.ApplicationScope
import bassamalim.hidaya.core.enums.Reminder
import bassamalim.hidaya.core.enums.StartAction
import bassamalim.hidaya.core.enums.ThemeColor
import bassamalim.hidaya.core.ui.theme.getThemeColor
import bassamalim.hidaya.core.utils.LangUtils.withAppLocale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class AthanService : Service() {

    @Inject @ApplicationScope lateinit var scope: CoroutineScope
    @Inject lateinit var appSettingsRepository: AppSettingsRepository
    @Inject lateinit var prayersRepository: PrayersRepository
    private var notificationId = 0
    private var channelId = ""
    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var playbackJob: Job? = null
    /**
     * Whether the notification should survive the service. It should when the athan played to the
     * end (it then becomes the prayer notification), but not when the user stopped it.
     */
    private var keepNotificationOnStop = false

    // Notification text in the app language (Services don't get it from AppCompat < API 33)
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == StartAction.STOP_ATHAN.name) {
            stopSelf()
            return START_NOT_STICKY
        }

        val reminder = try {
            Reminder.getById(intent.getIntExtra("id", -1))
        } catch (e: IllegalArgumentException) {
            Log.e(Globals.TAG, "Invalid reminder id in athan service intent", e)
            stopSelf()
            return START_NOT_STICKY
        }
        val time = intent.getLongExtra("time", 0L)
        Log.i(Globals.TAG, "In athan service for $reminder")
        notificationId = reminder.id

        // Has to happen synchronously, before any suspending work: the system kills the process
        // if a service started with startForegroundService() doesn't post its notification in time.
        startForeground(notificationId, createBasicNotification(reminder))

        playbackJob?.cancel()
        playbackJob = scope.launch {
            try {
                if (!isOnTime(time)) {
                    Log.i(Globals.TAG, "athan service: not on time")
                    stopSelf()
                    return@launch
                }

                // Read before anything is shown or played, so playback never starts without it.
                val athanAudio = getAthanAudio()

                val fullNotification = createFullNotification(reminder)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(notificationId, fullNotification)

                requestAudioFocus(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)

                play(reminder, athanAudio)
            } catch (e: Exception) {
                Log.e(Globals.TAG, "Error in AthanService", e)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun isOnTime(time: Long): Boolean {
        val max = time + 120000
        return System.currentTimeMillis() <= max
    }

    private suspend fun createFullNotification(reminder: Reminder): Notification {
        return NotificationCompat.Builder(this, channelId).apply {
            setSmallIcon(R.drawable.small_launcher_foreground)
            setTicker(resources.getString(R.string.app_name))

            setContentTitle(getTitle(reminder))
            setContentText(getSubtitle(reminder))

            addAction(0, getString(R.string.stop_athan), getStopIntent())
            setContentIntent(getStopAndOpenIntent())
            setDeleteIntent(getStopIntent())
            priority = NotificationCompat.PRIORITY_MAX
            setAutoCancel(true)
            setOnlyAlertOnce(true)
            color = getThemeColor(
                color = ThemeColor.SURFACE_CONTAINER,
                theme = appSettingsRepository.getTheme().first()
            ).toArgb()
        }.build()
    }

    private fun getTitle(reminder: Reminder): String {
        return if ((reminder == Reminder.Prayer.Dhuhr || reminder == Reminder.PrayerExtra.Dhuhr) &&
            Calendar.getInstance()[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY) {
            resources.getString(R.string.jumuah_title)
        }
        else resources.getStringArray(R.array.prayer_titles)[reminder.id-1]
    }

    private fun getSubtitle(reminder: Reminder): String {
        return if (reminder == Reminder.Prayer.Dhuhr &&
            Calendar.getInstance()[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY)
            resources.getString(R.string.jumuah_subtitle)
        else resources.getStringArray(R.array.prayer_subtitles)[reminder.id-1]
    }

    private fun getStopIntent(): PendingIntent {
        return PendingIntent.getService(
            this,
            11,
            Intent(this, AthanService::class.java)
                .setAction(StartAction.STOP_ATHAN.name)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getStopAndOpenIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            12,
            Intent(this, Activity::class.java).setAction(StartAction.STOP_ATHAN.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = "athan"
            val channel = NotificationChannel(
                channelId,
                getString(R.string.prayer_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Athan notification channel"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun createBasicNotification(reminder: Reminder): Notification {
        return NotificationCompat.Builder(this, channelId).apply {
            setSmallIcon(R.drawable.small_launcher_foreground)
            setTicker(resources.getString(R.string.app_name))
            setContentTitle(getBasicTitle(reminder))
            setContentText(getBasicSubtitle(reminder))
            addAction(0, getString(R.string.stop_athan), getStopIntent())
            priority = NotificationCompat.PRIORITY_MAX
            setAutoCancel(true)
            setOnlyAlertOnce(true)
            setContentIntent(getStopAndOpenIntent())
            setDeleteIntent(getStopIntent())
        }.build()
    }

    private fun getBasicTitle(reminder: Reminder): String {
        return if ((reminder == Reminder.Prayer.Dhuhr || reminder == Reminder.PrayerExtra.Dhuhr) &&
            Calendar.getInstance()[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY) {
            resources.getString(R.string.jumuah_title)
        }
        else resources.getStringArray(R.array.prayer_titles)[reminder.id-1]
    }

    private fun getBasicSubtitle(reminder: Reminder): String {
        return if (reminder == Reminder.Prayer.Dhuhr &&
            Calendar.getInstance()[Calendar.DAY_OF_WEEK] == Calendar.FRIDAY)
            resources.getString(R.string.jumuah_subtitle)
        else resources.getStringArray(R.array.prayer_subtitles)[reminder.id-1]
    }

    private fun play(reminder: Reminder, athanAudio: Int) {
        Log.i(Globals.TAG, "Playing Athan")

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build()
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // The attributes have to be passed to create(): it prepares the player itself, and
        // setAudioAttributes() has no effect once the player is prepared.
        val player = MediaPlayer.create(
            this,
            athanAudio,
            audioAttributes,
            audioManager.generateAudioSessionId()
        )
        if (player == null) {
            Log.e(Globals.TAG, "Failed to create the athan media player")
            stopSelf()
            return
        }
        mediaPlayer = player

        player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        player.setOnCompletionListener {
            scope.launch {
                showReminderNotification(reminder)
                keepNotificationOnStop = true
                stopSelf()
            }
        }
        player.setOnErrorListener { _, what, extra ->
            Log.e(Globals.TAG, "Athan media player error: what=$what, extra=$extra")
            stopSelf()
            true
        }

        // create() returns an already prepared player, so it is started directly; an
        // OnPreparedListener registered after create() is never called and the athan stays silent.
        player.start()
    }

    private suspend fun getAthanAudio(): Int {
        val athanAudioId = prayersRepository.getAthanAudioId().first()
        return when(athanAudioId) {
            1 -> R.raw.athan1
            2 -> R.raw.athan2
            3 -> R.raw.athan3
            else -> R.raw.athan1
        }
    }

    private suspend fun showReminderNotification(reminder: Reminder) {
        requestAudioFocus(AudioAttributes.USAGE_ALARM)

        val havePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        if (havePermission) {
            NotificationManagerCompat
                .from(this)
                .notify(notificationId, createFullNotification(reminder))
        }
    }

    private fun requestAudioFocus(usage: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        abandonAudioFocus()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(usage)
                    .build()
            ).build()
        audioFocusRequest = request

        (getSystemService(AUDIO_SERVICE) as AudioManager).requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        audioFocusRequest?.let {
            (getSystemService(AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it)
        }
        audioFocusRequest = null
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
            } catch (e: IllegalStateException) {
                Log.w(Globals.TAG, "Athan media player was in an invalid state", e)
            }
            player.release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        playbackJob?.cancel()
        playbackJob = null

        releaseMediaPlayer()
        abandonAudioFocus()

        // Detaching keeps the prayer notification around after the athan finished playing;
        // removing it is what the user asked for when they stopped the athan themselves.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(
                if (keepNotificationOnStop) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE
            )
        }
        else stopForeground(!keepNotificationOnStop)

        super.onDestroy()
    }

}
