package bassamalim.hidaya.features.recitations.player

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
import android.os.Handler
import android.os.Looper
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
import androidx.media3.common.util.UnstableApi
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Activity
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.StartAction
import bassamalim.hidaya.core.enums.ThemeColor
import bassamalim.hidaya.core.helpers.ReceiverWrapper
import bassamalim.hidaya.core.ui.theme.getThemeColor
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.core.utils.LangUtils.withAppLocale
import bassamalim.hidaya.core.utils.report
import bassamalim.hidaya.features.recitations.RecitationMediaId
import bassamalim.hidaya.features.recitations.recitersMenu.LastPlayedMedia
import bassamalim.hidaya.features.recitations.recitersMenu.Recitation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale
import java.util.Random
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class RecitationPlayerService : MediaBrowserServiceCompat(),
    AudioManager.OnAudioFocusChangeListener {

    @Inject
    lateinit var quranRepository: QuranRepository
    @Inject
    lateinit var recitationsRepository: RecitationsRepository
    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository
    @Inject
    lateinit var userRepository: UserRepository
    private val notificationId = 333
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val intentFilter: IntentFilter = IntentFilter()
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notification: Notification
    private lateinit var player: MediaPlayer
    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var wifiLock: WifiManager.WifiLock
    private lateinit var suraNames: List<String>
    private lateinit var playAction: NotificationCompat.Action
    private lateinit var pauseAction: NotificationCompat.Action
    private lateinit var nextAction: NotificationCompat.Action
    private lateinit var prevAction: NotificationCompat.Action
    private var mediaSession: MediaSessionCompat? = null
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    // Callbacks only reach us after initSession() created mediaSession
    private val controller: MediaControllerCompat get() = mediaSession!!.controller
    private lateinit var mediaMetadata: MediaMetadataCompat
    private lateinit var playType: String
    private lateinit var narration: Recitation.Narration
    private var channelId = "channel ID"
    private var mediaId: String? = null
    private var reciterName: String? = null
    private var reciterId = 0
    private var versionId = 0
    private var suraIndex = 0
    private var shuffle = 0
    private var continueFrom = 0
    private var updateRecordCounter = 0

    companion object {
        private const val MY_MEDIA_ROOT_ID = "media_root_id"
        private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"
        private const val ACTION_PLAY =
            "bassamalim.hidaya.features.recitations.player.service.RecitationPlayerService.PLAY"
        private const val ACTION_PAUSE =
            "bassamalim.hidaya.features.recitations.player.service.RecitationPlayerService.PAUSE"
        private const val ACTION_NEXT =
            "bassamalim.hidaya.features.recitations.player.service.RecitationPlayerService.NEXT"
        private const val ACTION_PREV =
            "bassamalim.hidaya.features.recitations.player.service.RecitationPlayerService.PREVIOUS"
        private const val ACTION_STOP =
            "bassamalim.hidaya.features.recitations.player.service.RecitationPlayerService.STOP"
    }

    private val receiver = ReceiverWrapper(
        context = this,
        intentFilter = intentFilter,
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        Log.i(Globals.TAG, "In ACTION_BECOMING_NOISY")
                        callback.onPause()
                    }

                    ACTION_PLAY -> {
                        Log.i(Globals.TAG, "In ACTION_PLAY")
                        callback.onPlay()
                    }

                    ACTION_PAUSE -> {
                        Log.i(Globals.TAG, "In ACTION_PAUSE")
                        callback.onPause()
                    }

                    ACTION_NEXT -> {
                        Log.i(Globals.TAG, "In ACTION_NEXT")
                        skipToNext()
                    }

                    ACTION_PREV -> {
                        Log.i(Globals.TAG, "In ACTION_PREV")
                        skipToPrevious()
                    }

                    ACTION_STOP -> {
                        Log.i(Globals.TAG, "In ACTION_STOP")
                        callback.onStop()
                    }
                }
            }
        }
    )

    // Notification text etc. in the app language (Services don't get it from AppCompat < API 33)
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate() {
        println("OnCreate")
        super.onCreate()

        serviceScope.launch {
            val language = LangUtils.getAppLanguage()
            getSuraNames(language)

            initSession()
            initPlayer()
            setupActions()
            initMetadata()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    val callback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onPlayFromMediaId(givenMediaId: String, extras: Bundle) {
            Log.i(Globals.TAG, "In onPlayFromMediaId of RecitationsPlayerService")

            playType = extras.getString("play_type")!!
            if (givenMediaId != mediaId || playType == "continue") {
                val parts = RecitationMediaId.decode(givenMediaId) ?: return
                mediaId = givenMediaId

                reciterId = parts.reciterId
                versionId = parts.narrationId
                suraIndex = parts.suraIdx
                reciterName = extras.getString("reciter_name")!!
                narration =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        extras.getSerializable("narration", Recitation.Narration::class.java)!!
                    else
                        extras.getSerializable("narration") as Recitation.Narration

                serviceScope.launch {
                    if (playType == "continue")
                        continueFrom = recitationsRepository.getLastPlayedMedia()
                            .first()!!.progress.toInt()

                    buildNotification()
                    updateMetadata(false)

                    wifiLock.acquire()

                    if (controller.playbackState.state == PlaybackStateCompat.STATE_NONE) onPlay()
                    else playOther()
                }
            }
        }

        override fun onPlay() {
            Log.i(Globals.TAG, "In onPlay of RecitationsPlayerService")

            if (mediaId == null) return  // bandage for an error

            serviceScope.launch {
                buildNotification()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val result = audioManager.requestAudioFocus(audioFocusRequest)
                    if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return@launch
                }

                // Start the service
                startService(Intent(applicationContext, RecitationPlayerService::class.java))
                // Set the session active  (and update metadata and state)
                mediaSession?.isActive = true

                receiver.register()
                // Put the service in the foreground, post notification
                startForeground(notificationId, notification)

                // start the player (custom call)
                if (controller.playbackState.state == PlaybackStateCompat.STATE_PAUSED
                    || controller.playbackState.state == PlaybackStateCompat.STATE_STOPPED) {
                    player.start()
                    refresh()
                }
                else startPlaying(suraIndex)

                updatePbState(
                    PlaybackStateCompat.STATE_PLAYING,
                    controller.playbackState.bufferedPosition
                )
                updateNotification(true)
            }
        }

        override fun onPause() {
            Log.i(Globals.TAG, "In onPause of RecitationsPlayerService")

            // Update metadata and state
            updatePbState(
                PlaybackStateCompat.STATE_PAUSED,
                controller.playbackState.bufferedPosition
            )
            updateNotification(false)

            serviceScope.launch {
                saveForLater(player.currentPosition)
                updateDurationRecord(updateRecordCounter)
            }

            handler.removeCallbacks(runnable)
            // pause the player
            player.pause()

            // Take the service out of the foreground, retain the notification
            stopForeground()
        }

        override fun onStop() {
            Log.i(Globals.TAG, "In onStop of RecitationsPlayerService")

            updatePbState(
                PlaybackStateCompat.STATE_STOPPED,
                controller.playbackState.bufferedPosition
            )

            handler.removeCallbacks(runnable)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
            }
            receiver.unregister()
            if (wifiLock.isHeld) wifiLock.release()

            serviceScope.launch {
                saveForLater(player.currentPosition)
                updateDurationRecord(updateRecordCounter)
            }

            player.release()

            stopSelf()    // Stop the service
            mediaSession?.isActive = false    // Set the session inactive
            stopForeground()    // Take the service out of the foreground
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            skipToNext()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            skipToPrevious()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            player.seekTo(pos.toInt())
            updatePbState(
                controller.playbackState.state,
                controller.playbackState.bufferedPosition
            )
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            super.onSetRepeatMode(repeatMode)
            player.isLooping = repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            super.onSetShuffleMode(shuffleMode)
            shuffle = shuffleMode
        }
    }

    private fun playOther() {
        mediaSession?.isActive = true
        // start the player
        startPlaying(suraIndex)

        // Update state
        updatePbState(
            PlaybackStateCompat.STATE_PLAYING,
            controller.playbackState.bufferedPosition
        )
        updateNotification(true)
    }

    private fun skipToNext() {
        var temp = suraIndex
        if (shuffle == PlaybackStateCompat.SHUFFLE_MODE_NONE) {
            do {
                temp++
            } while (temp < Globals.NUM_OF_QURAN_SURAS && !narration.availableSuras.contains(temp+1))
        }
        else if (shuffle == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
            val random = Random()
            do {
                temp = random.nextInt(Globals.NUM_OF_QURAN_SURAS)
            } while (!narration.availableSuras.contains(temp+1))
        }

        if (temp < Globals.NUM_OF_QURAN_SURAS) {
            suraIndex = temp
            updateMetadata(false)
            updateNotification(true)
            startPlaying(suraIndex)
            updatePbState(
                PlaybackStateCompat.STATE_PLAYING,
                controller.playbackState.bufferedPosition
            )
        }
    }

    private fun skipToPrevious() {
        var temp = suraIndex
        if (shuffle == PlaybackStateCompat.SHUFFLE_MODE_NONE) {
            do {
                temp--
            } while (temp >= 0 && !narration.availableSuras.contains(temp+1))
        }
        else if (shuffle == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
            val random = Random()
            do {
                temp = random.nextInt(Globals.NUM_OF_QURAN_SURAS)
            } while (!narration.availableSuras.contains(temp+1))
        }

        if (temp >= 0) {
            suraIndex = temp
            updateMetadata(false)
            updateNotification(true)
            startPlaying(suraIndex)
            updatePbState(
                PlaybackStateCompat.STATE_PLAYING,
                controller.playbackState.bufferedPosition
            )
        }
    }

    override fun onAudioFocusChange(i: Int) {
        when (i) {
            AudioManager.AUDIOFOCUS_GAIN ->
                player.setVolume(1.0f, 1.0f)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                if (player.isPlaying) player.setVolume(0.3f, 0.3f)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS ->
                if (player.isPlaying) callback.onPause()
        }
    }

    private fun initSession() {
        // Create a MediaSessionCompat
        mediaSession = MediaSessionCompat(this, "RecitationsPlayerService")

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE
        )
        mediaSession?.setPlaybackState(stateBuilder.build())

        // callback() has methods that handle callbacks from a media controller
        mediaSession?.setCallback(callback)

        // Set the session's token so that client activities can communicate with it.
        if (sessionToken == null)
            sessionToken = mediaSession?.sessionToken
    }

    private fun setupActions() {
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        intentFilter.addAction(ACTION_PLAY)
        intentFilter.addAction(ACTION_PAUSE)
        intentFilter.addAction(ACTION_NEXT)
        intentFilter.addAction(ACTION_PREV)
        intentFilter.addAction(ACTION_STOP)

        val flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE

        playAction = NotificationCompat.Action(
            R.drawable.ic_play,
            "Play",
            PendingIntent.getBroadcast(
                this,
                notificationId,
                Intent(ACTION_PLAY).setPackage(packageName),
                flags
            )
        )

        pauseAction = NotificationCompat.Action(
            R.drawable.ic_pause,
            "Pause",
            PendingIntent.getBroadcast(
                this,
                notificationId,
                Intent(ACTION_PAUSE).setPackage(packageName),
                flags
            )
        )

        nextAction = NotificationCompat.Action(
            R.drawable.ic_skip_next,
            "Next",
            PendingIntent.getBroadcast(
                this,
                notificationId,
                Intent(ACTION_NEXT).setPackage(packageName),
                flags
            )
        )

        prevAction = NotificationCompat.Action(
            R.drawable.ic_skip_previous,
            "Previous",
            PendingIntent.getBroadcast(
                this,
                notificationId,
                Intent(ACTION_PREV).setPackage(packageName),
                flags
            )
        )
    }

    private suspend fun buildNotification() {
        // Get the session's metadata
        mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        mediaSession?.setSessionActivity(getContentIntent())

        createNotificationChannel()

        // Create a NotificationCompat.Builder
        notificationBuilder = NotificationCompat.Builder(this, channelId)
            // Add the metadata for the currently playing track
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setSubText(description.description)
            .setLargeIcon(description.iconBitmap)
            // Enable launching the player by clicking the notification
            .setContentIntent(getContentIntent())
            // Stop the service when the notification is swiped away
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this, PlaybackStateCompat.ACTION_STOP
                )
            )
            // Make the transport controls visible on the lockscreen
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Add an app icon and set its accent color (Be careful about the color)
            .setSmallIcon(R.drawable.small_launcher_foreground)
            .setColor(
                getThemeColor(
                    color = ThemeColor.SURFACE_CONTAINER,
                    theme = appSettingsRepository.getTheme().first(),
                    context = this
                ).toArgb()
            )
            // Add buttons
            .addAction(prevAction).addAction(pauseAction).addAction(nextAction)
            // So there will be no notification tone
            .setSilent(true)
            // So the user wouldn't swipe it off
            .setOngoing(true)
            // Take advantage of MediaStyle features
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession!!.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    // Add a cancel button
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this, PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
        notification = notificationBuilder.build()

        // Display the notification and place the service in the foreground
        startForeground(notificationId, notification)
    }

    private fun initMetadata() {
        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                // TODO: Migrate to Compose
//                .putBitmap(    //Notification icon in card
//                    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON,
//                    BitmapFactory.decodeResource(resources, R.color.surface_M)
//                )
//                .putBitmap(
//                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
//                    BitmapFactory.decodeResource(resources, R.color.surface_M)
//                )
                .putBitmap(    //lock screen icon for pre lollipop
                    MediaMetadataCompat.METADATA_KEY_ART,
                    BitmapFactory.decodeResource(resources, R.drawable.small_launcher_foreground)
                )
                .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "")
                .putText(MediaMetadataCompat.METADATA_KEY_TITLE, "")
                .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "")
                .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, "")
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 0)
                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 0)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                .build()
        )
    }

    private fun updateMetadata(duration: Boolean) {
        mediaMetadata = MediaMetadataCompat.Builder()
            // TODO: Migrate to Compose
//            .putBitmap(    //Notification icon in card
//                MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON,
//                BitmapFactory.decodeResource(resources, R.color.surface_M)
//            )
//            .putBitmap(
//                MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
//                BitmapFactory.decodeResource(resources, R.color.surface_M)
//            )
            .putBitmap(    //lock screen icon for pre lollipop
                MediaMetadataCompat.METADATA_KEY_ART,
                BitmapFactory.decodeResource(resources, R.drawable.small_launcher_foreground)
            )
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, suraNames[suraIndex])
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, suraNames[suraIndex])
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, reciterName!!)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, reciterName!!)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, narration.name)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, narration.name)
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, suraIndex.toLong())
            .putLong(
                MediaMetadataCompat.METADATA_KEY_NUM_TRACKS,
                narration.availableSuras.size.toLong())
            .putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                (if (duration) player.duration else 0).toLong()
            )
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
            .build()

        mediaSession?.setMetadata(mediaMetadata)
    }

    private fun updatePbState(state: Int, buffered: Long) {
        stateBuilder.setState(state, player.currentPosition.toLong(), 1F)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_SEEK_TO
                        or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                        or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
            )
            .setBufferedPosition(buffered)

        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private val runnable = Runnable {
        if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) refresh()
    }

    private fun refresh() {
        updatePbState(
            controller.playbackState.state,
            controller.playbackState.bufferedPosition
        )

        if (updateRecordCounter++ >= 10) {
            serviceScope.launch {
                updateDurationRecord(updateRecordCounter)
            }
        }

        handler.postDelayed(runnable, 1000)
    }

    private fun updateNotification(playing: Boolean) {
        if (playing)
            notificationBuilder.clearActions()
                .addAction(prevAction).addAction(pauseAction).addAction(nextAction)
        else
            notificationBuilder.clearActions()
                .addAction(prevAction).addAction(playAction).addAction(nextAction)

        notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)
    }

    private fun initPlayer() {
        player = MediaPlayer()
        player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

        wifiLock =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
                    .createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "myLock")
            else
                (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
                    .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "myLock")


        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val attrs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        player.setAudioAttributes(attrs)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(this)
                .setAudioAttributes(attrs)
                .build()
        }

        player.setOnPreparedListener {
            if (playType == "continue") player.seekTo(continueFrom)

            player.start()

            updateMetadata(true) // For the duration
            updatePbState(
                PlaybackStateCompat.STATE_PLAYING,
                controller.playbackState.bufferedPosition
            )
            updateNotification(true)

            refresh()
        }

        player.setOnCompletionListener {
            serviceScope.launch {
                updateDurationRecord(updateRecordCounter)
            }
            skipToNext()
        }

        player.setOnInfoListener { _, what, _ ->
            when (what) {
                MediaPlayer.MEDIA_INFO_BUFFERING_START ->
                    updatePbState(
                        PlaybackStateCompat.STATE_BUFFERING,
                        controller.playbackState.bufferedPosition
                    )
                MediaPlayer.MEDIA_INFO_BUFFERING_END ->
                    updatePbState(
                        PlaybackStateCompat.STATE_PLAYING,
                        controller.playbackState.bufferedPosition
                    )
            }
            false
        }

        player.setOnBufferingUpdateListener { _, percent ->
            val ratio = percent / 100.0
            var bufferingLevel = 0L
            try {
                bufferingLevel = (player.duration * ratio).toLong()
            } catch (_: Exception) {}
            updatePbState(controller.playbackState.state, bufferingLevel)
        }

        player.setOnErrorListener { _, what, _ ->
            Log.e(Globals.TAG, "Error in RecitationsPlayerService player: $what")
            true
        }
    }

    private fun startPlaying(sura: Int) {
        player.reset()

        if (tryOffline(sura)) return

        val text = String.Companion.format(Locale.US, "%s/%03d.mp3", narration.server, sura + 1)

        try {
            player.setDataSource(applicationContext, text.toUri())
            player.prepareAsync()
        } catch (e: IOException) {
            e.report()
            e.printStackTrace()
            Log.e(Globals.TAG, "Problem in RecitationsPlayerService player")
        }
    }

    private fun tryOffline(sura: Int): Boolean {
        val path = (getExternalFilesDir(null).toString() + "/Telawat/" + reciterId
                + "/" + narration.id + "/" + sura + ".mp3")

        return try {
            player.setDataSource(path)
            player.prepare()
            Log.i(Globals.TAG, "Playing Offline")
            true
        } catch (f: FileNotFoundException) {
            Log.i(Globals.TAG, "Not available offline")
            false
        } catch (e: IOException) {
            e.report()
            e.printStackTrace()
            Log.e(Globals.TAG, "Problem in RecitationsPlayerService player")
            false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val description = "Recitations Player Notification Channel"
            channelId = "Recitations"
            val name = getString(R.string.recitations)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(channelId, name, importance)
            notificationChannel.description = description
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager = getSystemService(NotificationManager::class.java)!!
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private suspend fun getSuraNames(language: Language) {
        suraNames = quranRepository.getDecoratedSuraNames(language)
    }

    private fun getContentIntent(): PendingIntent {
        val intent = Intent(this, Activity::class.java).apply {
            action = StartAction.GO_TO_RECITATION.name
            putExtra("media_id", mediaId)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getActivity(this, 36, intent, flags)
    }

    override fun onGetRoot(
        clientPackageName: String, clientUid: Int, rootHints: Bundle?
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

    private fun saveForLater(progress: Int) {
        if (reciterName == null) return

        recitationsRepository.setLastPlayedMedia(
            LastPlayedMedia(
                mediaId = mediaId!!,
                progress = progress.toLong()
            )
        )
    }

    private suspend fun updateDurationRecord(amount: Int) {
        val old = userRepository.getRecitationsRecord().first()
        val new = old + amount * 1000

        userRepository.setRecitationsRecord(new)

        updateRecordCounter = 0
    }

    private fun stopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_DETACH)
        else stopForeground(false)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(Globals.TAG, "In onUnbind of RecitationsPlayerService")
        serviceScope.launch {
            saveForLater(player.currentPosition)
            updateDurationRecord(updateRecordCounter)
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

}