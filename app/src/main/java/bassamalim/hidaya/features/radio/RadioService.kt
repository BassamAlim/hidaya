package bassamalim.hidaya.features.radio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Activity
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.enums.ThemeColor
import bassamalim.hidaya.core.helpers.ReceiverWrapper
import bassamalim.hidaya.core.ui.theme.getThemeColor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class RadioService : MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener {

    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository
    private lateinit var playPauseAction: NotificationCompat.Action
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var controller: MediaControllerCompat
    private var channelId = "channel ID"
    private val id = 444
    private var flags = 0
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notification: Notification
    private lateinit var player: MediaPlayer
    private lateinit var audioManager: AudioManager
    private val intentFilter: IntentFilter = IntentFilter()
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var wifiLock: WifiManager.WifiLock
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var staticUrl: String? = null
    private var dynamicUrl: String? = null
    private var resolveUrlJob: Job? = null

    companion object {
        private const val MY_MEDIA_ROOT_ID = "media_root_id"
        private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"
        private const val ACTION_PLAY_PAUSE =
            "bassamalim.hidaya.features.radio.service.RadioService.playpause"
        private const val ACTION_STOP = "bassamalim.hidaya.features.radio.service.RadioService.stop"
    }

    private val receiverWrapper = ReceiverWrapper(
        context = this,
        intentFilter = intentFilter,
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        Log.i(Globals.TAG, "In ACTION_BECOMING_NOISY of RadioService")
                        callback.onPause()
                    }

                    ACTION_PLAY_PAUSE -> {
                        if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                            Log.i(Globals.TAG, "In ACTION_PAUSE of RadioService")
                            callback.onPause()
                        } else if (controller.playbackState.state
                            == PlaybackStateCompat.STATE_PAUSED
                        ) {
                            Log.i(Globals.TAG, "In ACTION_PLAY of RadioService")
                            callback.onPlay()
                        }
                    }

                    ACTION_STOP -> {
                        Log.i(Globals.TAG, "In ACTION_STOP of RadioService")
                        callback.onStop()
                    }
                }
            }
        }
    )

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            initSession()
            initPlayer()
            setActions()
            initMediaSessionMetadata()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    val callback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
            Log.i(Globals.TAG, "In onPlayFromMediaId of RadioClient")
            super.onPlayFromMediaId(mediaId, extras)

            staticUrl = mediaId
            // Retry if an earlier attempt failed (e.g. offline), else play would hit a null url
            if (dynamicUrl == null) {
                if (resolveUrlJob?.isActive != true) resolveUrlJob = resolveDynamicUrl()
            }
            else if (player.isPlaying)
                updatePbState(PlaybackStateCompat.STATE_PLAYING, player.currentPosition)
            else
                updatePbState(PlaybackStateCompat.STATE_STOPPED, 0)
        }

        override fun onPlay() {
            super.onPlay()

            serviceScope.launch {
                buildNotification()
                var result = AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    result = audioManager.requestAudioFocus(audioFocusRequest)

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    startService(Intent(applicationContext, RadioService::class.java))
                    mediaSession.isActive = true

                    startPlaying()

                    updatePbState(PlaybackStateCompat.STATE_PLAYING, player.currentPosition)
                    updateNotification(true)

                    receiverWrapper.register()
                    startForeground(id, notification)

                    wifiLock.acquire()
                }
            }
        }

        override fun onStop() {
            Log.i(Globals.TAG, "In onStop of RadioClient")
            super.onStop()

            updatePbState(PlaybackStateCompat.STATE_STOPPED, player.currentPosition)

            receiverWrapper.unregister()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
            }
            if (wifiLock.isHeld) wifiLock.release()
            player.release()

            stopSelf()
            mediaSession.isActive = false
            stopForeground()
        }

        override fun onPause() {
            super.onPause()
            Log.i(Globals.TAG, "In onPause of RadioClient")

            mediaSession.isActive = false
            player.stop()

            updatePbState(PlaybackStateCompat.STATE_PAUSED, player.currentPosition)
            updateNotification(false)

            stopForeground()
        }
    }

    override fun onAudioFocusChange(i: Int) {
        when (i) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                player.setVolume(1.0f, 1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (player.isPlaying) player.setVolume(0.3f, 0.3f)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS -> {
                if (player.isPlaying) callback.onPause()
            }
        }
    }

    private fun initSession() {
        mediaSession = MediaSessionCompat(this, "RadioService")

        val state = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
            .build()
        mediaSession.setPlaybackState(state)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mediaSession.setCallback(callback)

        sessionToken = mediaSession.sessionToken
    }

    private suspend fun buildNotification() {
        controller = mediaSession.controller
        mediaSession.setSessionActivity(getContentIntent())
        createNotificationChannel()

        notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(controller.metadata.description.title)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.small_launcher_foreground)
            .setColorized(true)
            .setColor(
                getThemeColor(
                    color = ThemeColor.SURFACE_CONTAINER,
                    theme = appSettingsRepository.getTheme().first()
                ).toArgb()
            )
            .setContentIntent(controller.sessionActivity)
            .addAction(playPauseAction)
            .setSilent(true)
            .setOngoing(true)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this, PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )

        notification = notificationBuilder.build()

        startForeground(id, notification)
    }

    private fun initMediaSessionMetadata() {
        val metadataBuilder = MediaMetadataCompat.Builder()
        // TODO: Migrate to Compose
//        .putBitmap(
//            MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON,
//            BitmapFactory.decodeResource(resources, R.color.surface_M)
//        )
//        .putBitmap(
//            MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
//            BitmapFactory.decodeResource(resources, R.color.surface_M)
//        )
        //lock screen icon for pre lollipop
            .putBitmap(
                MediaMetadataCompat.METADATA_KEY_ART,
                BitmapFactory.decodeResource(resources, R.drawable.small_launcher_foreground)
            )
            .putText(
                MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                getString(R.string.quran_radio)
            )
            .putText(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                getString(R.string.quran_radio)
            )

        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun updatePbState(state: Int, position: Int) {
        val state = PlaybackStateCompat.Builder()
            .setState(state, position.toLong(), 1F)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()

        mediaSession.setPlaybackState(state)
    }

    private fun updateNotification(playing: Boolean) {
        playPauseAction =
            if (playing) {
                NotificationCompat.Action(
                    R.drawable.ic_pause,
                    "play_pause",
                    PendingIntent.getBroadcast(
                        this,
                        id,
                        Intent(ACTION_PLAY_PAUSE).setPackage(packageName),
                        flags
                    )
                )
            }
            else {
                NotificationCompat.Action(
                    R.drawable.ic_play,
                    "play_pause",
                    PendingIntent.getBroadcast(
                        this,
                        id,
                        Intent(ACTION_PLAY_PAUSE).setPackage(packageName),
                        flags
                    )
                )
            }
        notificationBuilder.clearActions().addAction(playPauseAction)

        notification = notificationBuilder.build()
        notificationManager.notify(id, notification)
    }

    private fun initPlayer() {
        player = MediaPlayer()
        player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

        val wifiManager = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
        wifiLock =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "myLock")
            else
                wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "myLock")

        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(this)
                .setAudioAttributes(attrs)
                .build()
        }

        player.setOnPreparedListener {
            player.start()
            updatePbState(PlaybackStateCompat.STATE_PLAYING, player.currentPosition)
            updateNotification(true)
        }

        player.setOnInfoListener { _, what, _ ->
            when (what) {
                MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                    updatePbState(PlaybackStateCompat.STATE_BUFFERING, player.currentPosition)
                }
                MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                    updatePbState(PlaybackStateCompat.STATE_PLAYING, player.currentPosition)
                }
            }
            false
        }

        player.setOnErrorListener { _: MediaPlayer?, what: Int, _: Int ->
            Log.e(Globals.TAG, "Error in RadioService player: $what")
            true
        }
    }

    private fun setActions() {
        intentFilter.apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_STOP)
        }

        flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE

        playPauseAction = NotificationCompat.Action(
            R.drawable.ic_play,
            "play_pause",
            PendingIntent.getBroadcast(
                this,
                id,
                Intent(ACTION_PLAY_PAUSE).setPackage(packageName),
                flags
            )
        )
    }

    private fun startPlaying() {
        val dynamicUrl = dynamicUrl ?: return

        player.reset()
        player.setDataSource(applicationContext, dynamicUrl.toUri())
        player.prepareAsync()
    }

    // Other Links:
    // https://www.aloula.sa/83c0bda5-18e7-4c80-9c0a-21e764537d47
    // https://m.live.net.sa:1935/live/quransa/playlist.m3u8

    // Follows the static link's redirect to get the final dynamic link
    private fun resolveDynamicUrl() = serviceScope.launch {
        try {
            val resolved = withContext(Dispatchers.IO) {
                val connection = URL(staticUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                URL(connection.getHeaderField("Location")).toString()
                    .replaceFirst("http:".toRegex(), "https:")
            }
            dynamicUrl = resolved
            Log.i(Globals.TAG, "Dynamic Quran Radio URL: $resolved")

            updatePbState(PlaybackStateCompat.STATE_STOPPED, 0)
        } catch (e: IOException) {
            Log.e(Globals.TAG, "Problem in RadioService player")
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = "QuranRadio"
            val name = getString(R.string.quran_radio)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(channelId, name, importance).apply {
                description = "quran radio"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager = getSystemService(NotificationManager::class.java)!!
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun stopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            stopForeground(STOP_FOREGROUND_DETACH)
        else
            stopForeground(false)
    }

    private fun getContentIntent(): PendingIntent {
        val intent = Intent(this, Activity::class.java).apply {
            action = "back"
//            putExtra("start_route", Screen.RadioClient.route)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getActivity(this, 37, intent, flags)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        // (Optional) Control the level of access for the specified package name.
        return if (allowBrowsing(clientPackageName, clientUid)) {
            // Returns a root ID that clients can use with onLoadChildren() to retrieve
            // the content hierarchy.
            BrowserRoot(MY_MEDIA_ROOT_ID, null)
        } else {
            // Clients can connect, but this BrowserRoot is an empty hierarchy
            // so onLoadChildren returns nothing. This disables the ability to browse for content.
            BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null)
        }
    }

    override fun onLoadChildren(
        parentId: String, result: Result<List<MediaBrowserCompat.MediaItem?>?>
    ) {
        /*//  Browsing not allowed
        if (TextUtils.equals(MY_EMPTY_MEDIA_ROOT_ID, parentMediaId)) {
            result.sendResult(null);
            return;
        }
        // Assume for example that the music catalog is already loaded/cached.
        List<MediaItem> mediaItems = mutableListOf<>();
        // Check if this is the root menu:
        if (MY_MEDIA_ROOT_ID.equals(parentMediaId)) {
            // Build the MediaItem objects for the top level,
            // and put them in the mediaItems list...
        } else {
            // Examine the passed parentMediaId to see which submenu we're at,
            // and put the children of that menu in the mediaItems list...
        }
        result.sendResult(mediaItems);*/
    }

    private fun allowBrowsing(clientPackageName: String, clientUid: Int): Boolean {
        return true
    }

}