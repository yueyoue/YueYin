package com.lechenmusic.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.lechenmusic.MainActivity
import com.lechenmusic.R
import com.lechenmusic.data.model.TingChapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class AudiobookPlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var alarmReceiver: BroadcastReceiver? = null

    // Current book info
    private val _currentBookId = MutableStateFlow("")
    val currentBookId: StateFlow<String> = _currentBookId.asStateFlow()

    private val _currentBookTitle = MutableStateFlow("")
    val currentBookTitle: StateFlow<String> = _currentBookTitle.asStateFlow()

    private val _currentBookAuthor = MutableStateFlow("")
    val currentBookAuthor: StateFlow<String> = _currentBookAuthor.asStateFlow()

    private val _currentBookCoverUrl = MutableStateFlow<String?>(null)
    val currentBookCoverUrl: StateFlow<String?> = _currentBookCoverUrl.asStateFlow()

    // Current chapter
    private val _currentChapter = MutableStateFlow<TingChapter?>(null)
    val currentChapter: StateFlow<TingChapter?> = _currentChapter.asStateFlow()

    private val _chapters = MutableStateFlow<List<TingChapter>>(emptyList())
    val chapters: StateFlow<List<TingChapter>> = _chapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(-1)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    // RepeatMode: 0=OFF, 1=ALL, 2=ONE
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // Callback for progress saving
    var onProgressUpdate: ((bookId: String, chapterId: String, position: Long, duration: Long) -> Unit)? = null
    var onChapterAutoAdvanced: ((TingChapter) -> Unit)? = null

    // Stream base URL and auth
    private var streamBaseUrl: String = ""
    private var authToken: String = ""

    companion object {
        const val ACTION_PREV = "com.lechenmusic.AUDIOBOOK_PREV"
        const val ACTION_NEXT = "com.lechenmusic.AUDIOBOOK_NEXT"
        const val ACTION_PLAY_PAUSE = "com.lechenmusic.AUDIOBOOK_PLAY_PAUSE"
        const val ACTION_FORWARD = "com.lechenmusic.AUDIOBOOK_FORWARD"
        const val ACTION_REWIND = "com.lechenmusic.AUDIOBOOK_REWIND"
        const val CHANNEL_ID = "lechen_audiobook_playback"
        const val NOTIFICATION_ID = 2001
    }

    fun init(baseUrl: String, token: String) {
        streamBaseUrl = baseUrl
        authToken = token

        buildPlayer()

        createNotificationChannel()

        mediaSessionCompat = MediaSessionCompat(context, "LeChenAudiobookSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { togglePlayPause() }
                override fun onPause() { togglePlayPause() }
                override fun onSkipToNext() { skipNext() }
                override fun onSkipToPrevious() { skipPrevious() }
                override fun onStop() { forcePause() }
            })
        }

        alarmReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_PREV -> skipPrevious()
                    ACTION_NEXT -> skipNext()
                    ACTION_PLAY_PAUSE -> togglePlayPause()
                    ACTION_FORWARD -> forward30s()
                    ACTION_REWIND -> rewind30s()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_PREV)
            addAction(ACTION_NEXT)
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_FORWARD)
            addAction(ACTION_REWIND)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(alarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(alarmReceiver, filter)
        }

        // Progress update loop
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                updateProgress()
            }
        }
    }

    private fun buildPlayer() {
        // Create OkHttp client with auth header interceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $authToken")
                    .build()
                chain.proceed(request)
            }
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        updateNotification()
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = duration
                        }
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrentFromPlayer()
                        updateNotification()
                        val chapter = _currentChapter.value
                        if (chapter != null) {
                            onChapterAutoAdvanced?.invoke(chapter)
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        skipNext()
                    }
                })
            }
    }

    private var currentAuthUrl: String = ""
    private var currentAuthToken: String = ""

    fun updateAuth(baseUrl: String, token: String) {
        // Only rebuild player if auth actually changed
        if (baseUrl == currentAuthUrl && token == currentAuthToken) return
        currentAuthUrl = baseUrl
        currentAuthToken = token
        streamBaseUrl = baseUrl
        authToken = token
        // Rebuild player with new auth
        player?.release()
        buildPlayer()
    }

    /**
     * Update stream URL and token without rebuilding the player.
     * Safe to call while audio is playing.
     */
    fun updateStreamAuth(baseUrl: String, token: String) {
        streamBaseUrl = baseUrl
        authToken = token
        currentAuthUrl = baseUrl
        currentAuthToken = token
    }

    fun playBook(
        bookId: String,
        bookTitle: String,
        bookAuthor: String,
        coverUrl: String?,
        chapters: List<TingChapter>,
        startChapterIndex: Int = 0,
        startPositionMs: Long = 0
    ) {
        _currentBookId.value = bookId
        _currentBookTitle.value = bookTitle
        _currentBookAuthor.value = bookAuthor
        _currentBookCoverUrl.value = coverUrl
        _chapters.value = chapters

        player?.apply {
            val mediaItems = chapters.map { chapter ->
                val url = getStreamUrl(chapter.id)
                MediaItem.Builder()
                    .setUri(url)
                    .setMediaId(chapter.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(chapter.title)
                            .setArtist(bookTitle)
                            .setAlbumTitle(bookTitle)
                            .build()
                    )
                    .build()
            }
            setMediaItems(mediaItems, startChapterIndex, startPositionMs)
            prepare()
            play()
        }
        _currentChapterIndex.value = startChapterIndex
        if (startChapterIndex in chapters.indices) {
            _currentChapter.value = chapters[startChapterIndex]
        }
        updateNotification()
    }

    fun playChapter(chapterIndex: Int, startPositionMs: Long = 0) {
        val chs = _chapters.value
        if (chapterIndex !in chs.indices) return
        player?.apply {
            seekTo(chapterIndex, startPositionMs)
            play()
        }
        _currentChapterIndex.value = chapterIndex
        _currentChapter.value = chs[chapterIndex]
        updateNotification()
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun forcePause() {
        try {
            player?.let {
                if (it.isPlaying) it.pause()
            }
        } catch (_: Exception) { }
    }

    fun forward30s() {
        player?.let {
            val newPos = (it.currentPosition + 30000).coerceAtMost(it.duration)
            it.seekTo(newPos)
        }
    }

    fun rewind30s() {
        player?.let {
            val newPos = (it.currentPosition - 30000).coerceAtLeast(0)
            it.seekTo(newPos)
        }
    }

    fun skipNext() {
        player?.let {
            if (_shuffleMode.value) {
                val randomIndex = (_chapters.value.indices).random()
                it.seekTo(randomIndex, 0)
            } else if (it.hasNextMediaItem()) {
                it.seekToNext()
            } else if (_repeatMode.value == 1) { // ALL
                it.seekTo(0, 0)
            }
        }
        updateCurrentFromPlayer()
    }

    fun skipPrevious() {
        player?.let {
            if (it.currentPosition > 3000) {
                it.seekTo(0)
            } else if (it.hasPreviousMediaItem()) {
                it.seekToPrevious()
            }
        }
        updateCurrentFromPlayer()
    }

    fun seekToProgress(progress: Float) {
        player?.let {
            val pos = (it.duration * progress).toLong().coerceIn(0, it.duration)
            it.seekTo(pos)
        }
    }

    fun toggleShuffle() {
        _shuffleMode.value = !_shuffleMode.value
        player?.shuffleModeEnabled = _shuffleMode.value
    }

    fun toggleRepeat() {
        val next = when (_repeatMode.value) {
            0 -> 1  // OFF -> ALL
            1 -> 2  // ALL -> ONE
            else -> 0 // ONE -> OFF
        }
        _repeatMode.value = next
        player?.repeatMode = when (next) {
            0 -> Player.REPEAT_MODE_OFF
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_ALL
        }
    }

    fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
    }

    fun isBookLoaded(): Boolean {
        return _currentBookId.value.isNotBlank() && _chapters.value.isNotEmpty()
    }

    private fun getStreamUrl(chapterId: String): String {
        var baseUrl = streamBaseUrl.trim()
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "http://$baseUrl"
        }
        if (!baseUrl.endsWith("/")) baseUrl = "$baseUrl/"
        return "${baseUrl}api/stream/$chapterId"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "有声书播放",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "悦音有声书播放控制"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val chapter = _currentChapter.value ?: return
        val bookTitle = _currentBookTitle.value
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sessionCompat = mediaSessionCompat ?: return

        // Update MediaSessionCompat metadata
        val metadataBuilder = android.support.v4.media.MediaMetadataCompat.Builder()
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, chapter.title)
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, bookTitle)
            .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION, _duration.value)
        sessionCompat.setMetadata(metadataBuilder.build())

        // Update playback state
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
            .setState(
                if (_isPlaying.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                _currentPosition.value,
                1.0f
            )
        sessionCompat.setPlaybackState(stateBuilder.build())

        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(ACTION_PREV).setPackage(context.packageName)
        val prevPending = PendingIntent.getBroadcast(context, 10, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIntent = Intent(ACTION_PLAY_PAUSE).setPackage(context.packageName)
        val playPausePending = PendingIntent.getBroadcast(context, 11, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(ACTION_NEXT).setPackage(context.packageName)
        val nextPending = PendingIntent.getBroadcast(context, 12, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val forwardIntent = Intent(ACTION_FORWARD).setPackage(context.packageName)
        val forwardPending = PendingIntent.getBroadcast(context, 13, forwardIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val rewindIntent = Intent(ACTION_REWIND).setPackage(context.packageName)
        val rewindPending = PendingIntent.getBroadcast(context, 14, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (_isPlaying.value) R.drawable.ic_notif_pause else R.drawable.ic_notif_play

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(chapter.title)
            .setContentText(bookTitle)
            .setSubText("第${_currentChapterIndex.value + 1}章")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(_isPlaying.value)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(
                MediaStyle()
                    .setMediaSession(sessionCompat.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(R.drawable.ic_notif_prev, "上一章", prevPending)
            .addAction(playPauseIcon, if (_isPlaying.value) "暂停" else "播放", playPausePending)
            .addAction(R.drawable.ic_notif_next, "下一章", nextPending)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun updateCurrentFromPlayer() {
        player?.let { p ->
            val index = p.currentMediaItemIndex
            _currentChapterIndex.value = index
            if (index in _chapters.value.indices) {
                _currentChapter.value = _chapters.value[index]
            }
        }
    }

    fun updateProgress() {
        player?.let {
            _currentPosition.value = it.currentPosition
            _duration.value = it.duration.coerceAtLeast(0)
            _progress.value = if (it.duration > 0) it.currentPosition.toFloat() / it.duration else 0f

            // Auto-save progress every 5 seconds
            val chapter = _currentChapter.value
            if (chapter != null && _isPlaying.value && it.currentPosition > 0) {
                onProgressUpdate?.invoke(
                    _currentBookId.value,
                    chapter.id,
                    it.currentPosition,
                    it.duration
                )
            }
        }
    }

    fun release() {
        alarmReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) { }
        }
        mediaSessionCompat?.let {
            it.isActive = false
            it.release()
        }
        mediaSessionCompat = null
        player?.release()
        player = null
        // Cancel notification
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        } catch (_: Exception) { }
    }
}
