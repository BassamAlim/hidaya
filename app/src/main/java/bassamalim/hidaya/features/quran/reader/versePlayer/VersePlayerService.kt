package bassamalim.hidaya.features.quran.reader.versePlayer

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
import android.media.AudioManager.OnAudioFocusChangeListener
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.util.UnstableApi
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.dataSources.room.entities.Verse
import bassamalim.hidaya.core.data.dataSources.room.entities.VerseRecitation
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.enums.ThemeColor
import bassamalim.hidaya.core.helpers.ReceiverWrapper
import bassamalim.hidaya.core.ui.theme.getThemeColor
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.core.utils.LangUtils.withAppLocale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class VersePlayerService : MediaBrowserServiceCompat(), OnAudioFocusChangeListener, PlayerCallback {

    @Inject lateinit var quranRepository: QuranRepository
    @Inject lateinit var recitationsRepository: RecitationsRepository
    @Inject lateinit var appSettingsRepository: AppSettingsRepository
    @Inject lateinit var userRepository: UserRepository
    private lateinit var allRecitations: List<VerseRecitation>
    private val intentFilter = IntentFilter()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var apm: AlternatingPlayersManager
    private lateinit var wifiLock: WifiManager.WifiLock
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var controller: MediaControllerCompat
    private lateinit var audioManager: AudioManager
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private lateinit var mediaMetadata: MediaMetadataCompat
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notification: Notification
    private lateinit var playAction: NotificationCompat.Action
    private lateinit var pauseAction: NotificationCompat.Action
    private lateinit var nextAction: NotificationCompat.Action
    private lateinit var prevAction: NotificationCompat.Action
    private lateinit var allVerses: List<Verse>
    private lateinit var reciterNames: List<String>
    private lateinit var suarNames: List<String>
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val readySignal = CompletableDeferred<Unit>()
    private val notificationId = 101
    private var updateRecordCounter = 0
    private var channelId = "channel ID"

    companion object {
        private const val MY_MEDIA_ROOT_ID = "media_root_id"
        private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"
        private const val ACTION_PLAY = "bassamalim.hidaya.features.quranViewer.AyaPlayerService.PLAY"
        private const val ACTION_PAUSE = "bassamalim.hidaya.features.quranViewer.AyaPlayerService.PAUSE"
        private const val ACTION_NEXT = "bassamalim.hidaya.features.quranViewer.AyaPlayerService.NEXT"
        private const val ACTION_PREV = "bassamalim.hidaya.features.quranViewer.AyaPlayerService.PREVIOUS"
        private const val ACTION_STOP = "bassamalim.hidaya.features.quranViewer.AyaPlayerService.STOP"
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
                        serviceScope.launch { readySignal.await(); apm.nextVerse() }
                    }
                    ACTION_PREV -> {
                        Log.i(Globals.TAG, "In ACTION_PREV")
                        serviceScope.launch { readySignal.await(); apm.previousVerse() }
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
        super.onCreate()

        initSession()
        setupActions()
        initMetadata()

        serviceScope.launch {
            allVerses = quranRepository.getAllVerses()
            allRecitations = recitationsRepository.getAllVerseRecitations()
            reciterNames = recitationsRepository.getVerseReciterNames()
            suarNames = quranRepository.getDecoratedSuraNames(language = LangUtils.getAppLanguage())
            initAPM()
            readySignal.complete(Unit)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    val callback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
            Log.i(Globals.TAG, "In onPlayFromMediaId of AyaPlayerService")

            val ayaId = mediaId.toInt()

            mediaSession.isActive = true

            serviceScope.launch {
                readySignal.await()

                if (apm.isNotInitialized()) {
                    // Start the service
                    startService(Intent(applicationContext, VersePlayerService::class.java))
                }

                buildNotification()

                // Put the service in the foreground, post notification
                startForeground(notificationId, notification)

                wifiLock.acquire()
                updatePbState(PlaybackStateCompat.STATE_PLAYING)
                updateNotification(true)
                updateMetadata(ayaId, false)

                // Request audio focus for playback, this registers the afChangeListener
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && audioManager.requestAudioFocus(audioFocusRequest)
                    != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    return@launch

                receiver.register()

                apm.playFromMediaId(verseIdx = ayaId-1)
            }
        }

        // used as onResume
        override fun onPlay() {
            Log.i(Globals.TAG, "In onPlay of AyaPlayerService")

            mediaSession.isActive = true

            serviceScope.launch {
                readySignal.await()

                if (apm.isNotInitialized()) {
                    startService(Intent(applicationContext, VersePlayerService::class.java))
                }

                buildNotification()

                startForeground(notificationId, notification)

                wifiLock.acquire()

                updatePbState(PlaybackStateCompat.STATE_PLAYING)
                updateNotification(true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && audioManager.requestAudioFocus(audioFocusRequest)
                    != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    return@launch

                receiver.register()

                apm.resume()
            }
        }

        override fun onPause() {
            Log.i(Globals.TAG, "In onPause of AyaPlayerService")

            serviceScope.launch {
                readySignal.await()

                updatePbState(PlaybackStateCompat.STATE_PAUSED)
                updateNotification(false)

                apm.pause()

                updateDurationRecord(updateRecordCounter)

                handler.removeCallbacks(runnable)
                abandonAudioFocus()

                stopForeground()
            }
        }

        override fun onStop() {
            Log.i(Globals.TAG, "In onStop of AyaPlayerService")

            serviceScope.launch {
                readySignal.await()

                updatePbState(PlaybackStateCompat.STATE_STOPPED)

                handler.removeCallbacks(runnable)
                abandonAudioFocus()
                receiver.unregister()
                if (wifiLock.isHeld) wifiLock.release()

                updateDurationRecord(updateRecordCounter)

                apm.release()

                stopSelf()    // Stop the service
                mediaSession.isActive = false    // Set the session inactive
                stopForeground()
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            serviceScope.launch { readySignal.await(); apm.nextVerse() }
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            serviceScope.launch { readySignal.await(); apm.previousVerse() }
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            serviceScope.launch { readySignal.await(); apm.seekTo(pos); updatePbState(getPbState()) }
        }
    }

    private fun initSession() {
        // Create a MediaSessionCompat
        mediaSession = MediaSessionCompat(this, "AyaPlayerService")

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE
        )
        mediaSession.setPlaybackState(stateBuilder.build())

        // callback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(callback)

        // Set the session's token so that client activities can communicate with it.
        sessionToken = mediaSession.sessionToken
    }

    /**
     * Initialize the two media players and the wifi lock
     */
    private fun initAPM() {
        apm = AlternatingPlayersManager(
            context = this,
            allVerses = allVerses,
            recitationFlow = recitationsRepository.getVerseReciterId().map { reciterId ->
                val reciterRecitations = allRecitations.filter { it.reciterId == reciterId }
                return@map reciterRecitations.maxBy<VerseRecitation, Int> { it.bitrate }
            },
            repeatModeFlow = recitationsRepository.getVerseRepeatMode(),
            stopOnPageEndFlow = recitationsRepository.getShouldStopOnPageEnd(),
            stopOnSuraEndFlow = recitationsRepository.getShouldStopOnSuraEnd(),
            callback = this@VersePlayerService,
            scope = serviceScope
        )

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

        apm.setAudioAttributes(attrs)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(this)
                .setAudioAttributes(attrs)
                .build()
        }
    }

    override fun updatePbState(state: Int) {
        var currentPosition = 0L
        try {
            currentPosition = apm.getCurrentPosition().toLong()
        } catch (_: Exception) {}

        stateBuilder.setState(state, currentPosition, 1F)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SEEK_TO
                        or PlaybackStateCompat.ACTION_STOP
            )
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    override fun getPbState(): Int {
        return controller.playbackState.state
    }

    override suspend fun track(verseId: Int) {
        updateMetadata(verseId, true)
    }

    private fun setupActions() {
        intentFilter.apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREV)
            addAction(ACTION_STOP)
        }

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
        controller = mediaSession.controller
        mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        createNotificationChannel()

        // Create a NotificationCompat.Builder
        notificationBuilder = NotificationCompat.Builder(this, channelId)
            // Add the metadata for the currently playing track
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setSubText(description.description)
            .setLargeIcon(description.iconBitmap)
            // Enable launching the player by clicking the notification
            .setContentIntent(controller.sessionActivity)
            // Stop the service when the notification is swiped away
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this, PlaybackStateCompat.ACTION_STOP
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.small_launcher_foreground)
            .setColor(
                getThemeColor(
                    color = ThemeColor.SURFACE_CONTAINER,
                    theme = appSettingsRepository.getTheme().first()
                ).toArgb()
            )
            .addAction(prevAction).addAction(pauseAction).addAction(nextAction)
            .setSilent(true)
            .setOngoing(true)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )

        notification = notificationBuilder.build()

        // Display the notification and place the service in the foreground
        startForeground(notificationId, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val description = "quran listening"
            channelId = "AyaPlayer"
            val name = getString(R.string.recitations)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(channelId, name, importance)
            notificationChannel.description = description
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
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

    private val runnable = Runnable {
        if (getPbState() == PlaybackStateCompat.STATE_PLAYING)
            refresh()
    }

    private fun refresh() {
        updatePbState(getPbState())

        if (updateRecordCounter++ >= 10) {
            serviceScope.launch {
                updateDurationRecord(updateRecordCounter)
            }
        }

        handler.postDelayed(runnable, 1000)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                apm.setVolume(1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (getPbState() == PlaybackStateCompat.STATE_PLAYING)
                    apm.setVolume(0.3f)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS -> {
                if (getPbState() == PlaybackStateCompat.STATE_PLAYING)
                    callback.onPause()
            }
        }
    }

    private fun abandonAudioFocus(): Boolean {
        // Set up async after onCreate; stop/destroy can arrive first, and then no focus was held
        if (!::audioFocusRequest.isInitialized) return true

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                    audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
        else true
    }

    private fun initMetadata() {
        mediaSession.setMetadata(
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
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 0)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                .build()
        )
    }

    private suspend fun updateMetadata(ayaId: Int = apm.verseIdx, duration: Boolean) {
        val aya = getAya(ayaId)
        val reciterId = recitationsRepository.getVerseReciterId().first()

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
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, suarNames[aya.suraNum-1])
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, suarNames[aya.suraNum-1])
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, reciterNames[reciterId])
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, reciterNames[reciterId])
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, aya.id.toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                (if (duration) apm.getDuration() else 0).toLong()
            )
            .putLong("page_num", aya.pageNum.toLong())
            .build()

        mediaSession.setMetadata(mediaMetadata)
    }

    private fun getAya(id: Int) = allVerses[id-1]

    private suspend fun updateDurationRecord(amount: Int) {
        val old = userRepository.getRecitationsRecord().first()
        val new = old + amount * 1000

        userRepository.setRecitationsRecord(new)

        updateRecordCounter = 0
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
        val mediaItems = mutableListOf()
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

    private fun stopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_DETACH)
        else stopForeground(false)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(Globals.TAG, "In onUnbind of AyaPlayerService")
        serviceScope.launch {
            updateDurationRecord(updateRecordCounter)
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()

        handler.removeCallbacks(runnable)

        if (::apm.isInitialized) apm.release()

        if (::wifiLock.isInitialized && wifiLock.isHeld) wifiLock.release()

        abandonAudioFocus()

        receiver.unregister()
    }

}