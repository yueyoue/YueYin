package com.yueyin.player

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.yueyin.data.model.TingChapter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient

class AudiobookPlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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

    /** Exposed for PlaybackService to create MediaSession */
    fun getPlayer(): ExoPlayer? = player

    fun init(baseUrl: String, token: String) {
        streamBaseUrl = baseUrl; authToken = token
        buildPlayer()
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
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = duration
                            // Ensure playback continues when transitioning in background
                            player?.let { if (!it.isPlaying && it.playWhenReady) it.play() }
                        }
                        if (playbackState == Player.STATE_ENDED) {
                            player?.let { p ->
                                if (!p.hasNextMediaItem()) {
                                    _isPlaying.value = false
                                }
                            }
                        }
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentPosition.value = 0L
                        _progress.value = 0f
                        updateCurrentFromPlayer()
                        // Ensure playback continues after auto-transition (fixes lock screen stuck)
                        scope.launch {
                            kotlinx.coroutines.delay(300)
                            player?.let {
                                _duration.value = it.duration.coerceAtLeast(0)
                                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                                    // Force play — this is the key fix for background/lock-screen auto-advance
                                    it.playWhenReady = true
                                    if (!it.isPlaying) {
                                        it.play()
                                    }
                                }
                            }
                        }
                        _currentChapter.value?.let { onChapterAutoAdvanced?.invoke(it) }
                    }
                    override fun onPlayerError(error: PlaybackException) {
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

        // Start foreground service for background/lock-screen playback
        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) {}
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

        // Start foreground service for background/lock-screen playback
        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) {}
    }

    fun playChapter(chapterIndex: Int, startPositionMs: Long = 0) {
        val chs = _chapters.value; if (chapterIndex !in chs.indices) return
        player?.apply { seekTo(chapterIndex, startPositionMs); play() }
        _currentChapterIndex.value = chapterIndex; _currentChapter.value = chs[chapterIndex]
    }

    fun togglePlayPause() { player?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun forcePause() { try { player?.let { if (it.isPlaying) it.pause() } } catch (_: Exception) {} }
    fun forward15s() {
        player?.let { p ->
            val target = (p.currentPosition + 15000).coerceAtMost(p.duration.coerceAtLeast(0))
            p.seekTo(target); _currentPosition.value = target
        }
    }
    fun rewind15s() {
        player?.let { p ->
            val target = (p.currentPosition - 15000).coerceAtLeast(0)
            p.seekTo(target); _currentPosition.value = target
        }
    }
    fun skipNext() {
        player?.let { p ->
            if (p.hasNextMediaItem()) {
                p.seekToNext()
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
        try { context.stopService(Intent(context, PlaybackService::class.java)) } catch (_: Exception) {}
        player?.release(); player = null
    }
}
