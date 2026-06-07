package com.yueyin.player

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
import com.yueyin.MainActivity
import com.yueyin.R
import com.yueyin.data.model.TingChapter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient

class AudiobookPlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var broadcastReceiver: BroadcastReceiver? = null

    private val _currentBookId = MutableStateFlow("")
    val currentBookId: StateFlow<String> = _currentBookId.asStateFlow()
    private val _currentBookTitle = MutableStateFlow("")
    val currentBookTitle: StateFlow<String> = _currentBookTitle.asStateFlow()
    private val _currentBookAuthor = MutableStateFlow("")
    val currentBookAuthor: StateFlow<String> = _currentBookAuthor.asStateFlow()
    private val _currentBookNarrator = MutableStateFlow("")
    val currentBookNarrator: StateFlow<String> = _currentBookNarrator.asStateFlow()
    private val _currentBookCoverUrl = MutableStateFlow<String?>(null)
    val currentBookCoverUrl: StateFlow<String?> = _currentBookCoverUrl.asStateFlow()
    private val _currentBookDescription = MutableStateFlow("")
    val currentBookDescription: StateFlow<String> = _currentBookDescription.asStateFlow()
    private val _currentChapter = MutableStateFlow<TingChapter?>(null)
    val currentChapter: StateFlow<TingChapter?> = _currentChapter.asStateFlow()
    private val _chapters = MutableStateFlow<List<TingChapter>>(emptyList())
    val chapters: StateFlow<List<TingChapter>> = _chapters.asStateFlow()
    private val _currentChapterIndex = MutableStateFlow(-1)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    var onProgressUpdate: ((bookId: String, chapterId: String, position: Long, duration: Long) -> Unit)? = null
    var onChapterAutoAdvanced: ((TingChapter) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private var streamBaseUrl: String = ""
    private var authToken: String = ""

    companion object {
        const val ACTION_NEXT = "com.yueyin.AUDIOBOOK_NEXT"
        const val ACTION_PREV = "com.yueyin.AUDIOBOOK_PREV"
        const val ACTION_PLAY_PAUSE = "com.yueyin.AUDIOBOOK_PLAY_PAUSE"
        const val ACTION_FORWARD = "com.yueyin.AUDIOBOOK_FORWARD"
        const val ACTION_REWIND = "com.yueyin.AUDIOBOOK_REWIND"
        const val CHANNEL_ID = "yueyin_audiobook_playback"
        const val NOTIFICATION_ID = 2001
    }

    fun init(baseUrl: String, token: String) {
        streamBaseUrl = baseUrl; authToken = token
        buildPlayer(); createNotificationChannel()
        mediaSessionCompat = MediaSessionCompat(context, "YueYinSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { togglePlayPause() }
                override fun onPause() { togglePlayPause() }
                override fun onSkipToNext() { skipNext() }
                override fun onSkipToPrevious() { skipPrevious() }
                override fun onFastForward() { forward15s() }
                override fun onRewind() { rewind15s() }
                override fun onStop() { forcePause() }
            })
        }
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_NEXT -> skipNext()
                    ACTION_PREV -> skipPrevious()
                    ACTION_PLAY_PAUSE -> togglePlayPause()
                    ACTION_FORWARD -> forward15s()
                    ACTION_REWIND -> rewind15s()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_NEXT); addAction(ACTION_PREV); addAction(ACTION_PLAY_PAUSE); addAction(ACTION_FORWARD); addAction(ACTION_REWIND)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else { context.registerReceiver(broadcastReceiver, filter) }
        scope.launch { while (true) { delay(1000); updateProgress() } }
    }

    private fun buildPlayer() {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().addHeader("Authorization", "Bearer $authToken").build()) }
            .build()
        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(okHttpClient)))
            .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).setUsage(C.USAGE_MEDIA).build(), true)
            .setHandleAudioBecomingNoisy(true).setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying; updateNotification() }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = duration
                            // If we have chapters loaded and player was supposed to be playing, resume
                            if (_chapters.value.isNotEmpty() && _currentChapterIndex.value >= 0) {
                                player?.let { if (!it.isPlaying && it.playWhenReady) it.play() }
                            }
                            updateNotification()
                        }
                        // When a chapter finishes and no next item, stop
                        if (playbackState == Player.STATE_ENDED) {
                            player?.let { p ->
                                if (!p.hasNextMediaItem()) {
                                    _isPlaying.value = false
                                    updateNotification()
                                }
                            }
                        }
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // Reset position for new chapter
                        _currentPosition.value = 0L
                        _progress.value = 0f
                        updateCurrentFromPlayer()
                        // Delay notification update slightly to let player state stabilize
                        scope.launch {
                            kotlinx.coroutines.delay(500)
                            player?.let {
                                _duration.value = it.duration.coerceAtLeast(0)
                                // Ensure playback continues after auto-transition
                                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                                    if (!it.isPlaying && it.playbackState != Player.STATE_IDLE) {
                                        it.play()
                                    }
                                }
                            }
                            updateNotification()
                        }
                        _currentChapter.value?.let { onChapterAutoAdvanced?.invoke(it) }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        // On error, try to skip to next and continue playing
                        player?.let { p ->
                            if (p.hasNextMediaItem()) {
                                p.seekToNext()
                                p.play()
                            } else {
                                onError?.invoke("播放出错: ${error.message}")
                            }
                        }
                    }
                })
            }
    }

    private var currentAuthUrl = ""; private var currentAuthToken = ""
    fun updateAuth(baseUrl: String, token: String) {
        if (baseUrl == currentAuthUrl && token == currentAuthToken) return
        currentAuthUrl = baseUrl; currentAuthToken = token; streamBaseUrl = baseUrl; authToken = token
        player?.release(); buildPlayer()
    }
    fun updateStreamAuth(baseUrl: String, token: String) {
        streamBaseUrl = baseUrl; authToken = token; currentAuthUrl = baseUrl; currentAuthToken = token
    }

    fun playBook(bookId: String, bookTitle: String, bookAuthor: String, narrator: String, coverUrl: String?, description: String = "", chapters: List<TingChapter>, startChapterIndex: Int = 0, startPositionMs: Long = 0) {
        _currentBookId.value = bookId; _currentBookTitle.value = bookTitle; _currentBookAuthor.value = bookAuthor
        _currentBookNarrator.value = narrator; _currentBookCoverUrl.value = coverUrl; _currentBookDescription.value = description; _chapters.value = chapters
        player?.apply {
            val items = chapters.map { ch ->
                MediaItem.Builder().setUri(getStreamUrl(ch.id)).setMediaId(ch.id)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(ch.title).setArtist(bookTitle).setAlbumTitle(bookTitle).build()).build()
            }
            setMediaItems(items, startChapterIndex, startPositionMs); prepare(); play()
        }
        _currentChapterIndex.value = startChapterIndex
        if (startChapterIndex in chapters.indices) _currentChapter.value = chapters[startChapterIndex]
        updateNotification()
    }

    fun loadBook(bookId: String, bookTitle: String, bookAuthor: String, narrator: String, coverUrl: String?, description: String = "", chapters: List<TingChapter>, startChapterIndex: Int = 0, startPositionMs: Long = 0) {
        _currentBookId.value = bookId; _currentBookTitle.value = bookTitle; _currentBookAuthor.value = bookAuthor
        _currentBookNarrator.value = narrator; _currentBookCoverUrl.value = coverUrl; _currentBookDescription.value = description; _chapters.value = chapters
        player?.apply {
            val items = chapters.map { ch ->
                MediaItem.Builder().setUri(getStreamUrl(ch.id)).setMediaId(ch.id)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(ch.title).setArtist(bookTitle).setAlbumTitle(bookTitle).build()).build()
            }
            setMediaItems(items, startChapterIndex, startPositionMs); prepare()
        }
        _currentChapterIndex.value = startChapterIndex
        if (startChapterIndex in chapters.indices) _currentChapter.value = chapters[startChapterIndex]
        updateNotification()
    }

    fun playChapter(chapterIndex: Int, startPositionMs: Long = 0) {
        val chs = _chapters.value; if (chapterIndex !in chs.indices) return
        player?.apply { seekTo(chapterIndex, startPositionMs); play() }
        _currentChapterIndex.value = chapterIndex; _currentChapter.value = chs[chapterIndex]; updateNotification()
    }

    fun togglePlayPause() { player?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun forcePause() { try { player?.let { if (it.isPlaying) it.pause() } } catch (_: Exception) {} }
    fun forward15s() {
        player?.let { p ->
            val target = (p.currentPosition + 15000).coerceAtMost(p.duration.coerceAtLeast(0))
            p.seekTo(target)
            _currentPosition.value = target
            updateNotification()
        }
    }
    fun rewind15s() {
        player?.let { p ->
            val target = (p.currentPosition - 15000).coerceAtLeast(0)
            p.seekTo(target)
            _currentPosition.value = target
            updateNotification()
        }
    }
    fun skipNext() {
        player?.let { p ->
            if (p.hasNextMediaItem()) {
                p.seekToNext()
                // Ensure playback continues after skip
                if (!p.isPlaying && p.playbackState != Player.STATE_IDLE && p.playbackState != Player.STATE_ENDED) {
                    p.play()
                }
            }
        }
        updateCurrentFromPlayer()
    }
    fun skipPrevious() { player?.let { if (it.currentPosition > 3000) it.seekTo(0) else if (it.hasPreviousMediaItem()) it.seekToPrevious() }; updateCurrentFromPlayer() }
    fun seekToProgress(progress: Float) { player?.let { it.seekTo((it.duration * progress).toLong().coerceIn(0, it.duration)) } }
    fun setSpeed(speed: Float) { _playbackSpeed.value = speed; player?.setPlaybackSpeed(speed) }
    fun isBookLoaded(): Boolean = _currentBookId.value.isNotBlank() && _chapters.value.isNotEmpty()

    private fun getStreamUrl(chapterId: String): String {
        var base = streamBaseUrl.trim()
        if (!base.startsWith("http://") && !base.startsWith("https://")) base = "http://$base"
        if (!base.endsWith("/")) base = "$base/"
        return "${base}api/stream/$chapterId"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(CHANNEL_ID, "有声书播放", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "悦音听书"; setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC; setSound(null, null); enableVibration(false)
                })
        }
    }

    private fun updateNotification() {
        val chapter = _currentChapter.value ?: return
        val bookTitle = _currentBookTitle.value
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val session = mediaSessionCompat ?: return

        val meta = android.support.v4.media.MediaMetadataCompat.Builder()
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, chapter.title)
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, bookTitle)
            .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION, _duration.value)
            .build()
        session.setMetadata(meta)

        val state = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_REWIND
            )
            .addCustomAction(PlaybackStateCompat.CustomAction.Builder("REWIND_15", "后退15秒", R.drawable.ic_notif_rewind).build())
            .addCustomAction(PlaybackStateCompat.CustomAction.Builder("FORWARD_15", "前进15秒", R.drawable.ic_notif_forward).build())
            .setState(if (_isPlaying.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, _currentPosition.value, 1.0f)
            .build()
        session.setPlaybackState(state)

        val openPI = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val prevPI = PendingIntent.getBroadcast(context, 20, Intent(ACTION_PREV).setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val rewindPI = PendingIntent.getBroadcast(context, 14, Intent(ACTION_REWIND).setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val playPausePI = PendingIntent.getBroadcast(context, 11, Intent(ACTION_PLAY_PAUSE).setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val forwardPI = PendingIntent.getBroadcast(context, 13, Intent(ACTION_FORWARD).setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val nextPI = PendingIntent.getBroadcast(context, 21, Intent(ACTION_NEXT).setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentIntent(openPI).setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(chapter.title).setContentText(bookTitle)
            .setSubText("第${_currentChapterIndex.value + 1}章")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(_isPlaying.value).setShowWhen(false).setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(1, 2, 3))
            .addAction(R.drawable.ic_notif_prev, "上一章", prevPI)
            .addAction(R.drawable.ic_notif_rewind, "后退15秒", rewindPI)
            .addAction(if (_isPlaying.value) R.drawable.ic_notif_pause else R.drawable.ic_notif_play, if (_isPlaying.value) "暂停" else "播放", playPausePI)
            .addAction(R.drawable.ic_notif_forward, "前进15秒", forwardPI)
            .addAction(R.drawable.ic_notif_next, "下一章", nextPI)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun updateCurrentFromPlayer() {
        player?.let { p ->
            val idx = p.currentMediaItemIndex; _currentChapterIndex.value = idx
            if (idx in _chapters.value.indices) _currentChapter.value = _chapters.value[idx]
        }
    }

    private fun updateProgress() {
        player?.let {
            _currentPosition.value = it.currentPosition; _duration.value = it.duration.coerceAtLeast(0)
            _progress.value = if (it.duration > 0) it.currentPosition.toFloat() / it.duration else 0f
            val ch = _currentChapter.value
            if (ch != null && _isPlaying.value && it.currentPosition > 0) {
                onProgressUpdate?.invoke(_currentBookId.value, ch.id, it.currentPosition, it.duration)
            }
        }
    }

    fun release() {
        broadcastReceiver?.let { try { context.unregisterReceiver(it) } catch (_: Exception) {} }
        mediaSessionCompat?.let { it.isActive = false; it.release() }; mediaSessionCompat = null
        player?.release(); player = null
        try { (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID) } catch (_: Exception) {}
    }
}
