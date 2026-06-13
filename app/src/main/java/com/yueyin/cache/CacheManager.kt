package com.yueyin.cache

import android.content.Context
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages audio/image caching with configurable size limit.
 * Default 2 GB; user can change in Profile → 缓存设置.
 */
class CacheManager(private val context: Context) {
    companion object {
        private const val DEFAULT_CACHE_GB = 2
        private const val SEVEN_DAYS_SECONDS = 604800L
    }

    var maxCacheBytes: Long = DEFAULT_CACHE_GB * 1024L * 1024L * 1024L
        private set

    private val audioCacheDir = File(context.cacheDir, "audio_cache")
    private var audioCache = Cache(audioCacheDir, maxCacheBytes)

    /** OkHttp client with disk cache for audio streams. */
    var okHttpClient: OkHttpClient = buildCachedClient()
        private set

    private fun buildCachedClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(audioCache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=604800")
                    .removeHeader("Pragma")
                    .build()
            }
            .addInterceptor { chain ->
                var request = chain.request()
                if (request.cacheControl == CacheControl.FORCE_NETWORK) {
                    request = request.newBuilder()
                        .cacheControl(CacheControl.Builder().maxStale(SEVEN_DAYS_SECONDS.toInt(), TimeUnit.SECONDS).build())
                        .build()
                }
                chain.proceed(request)
            }
            .build()
    }

    fun setMaxCacheSize(gb: Int) {
        val bytes = gb.toLong() * 1024L * 1024L * 1024L
        maxCacheBytes = bytes
        // OkHttp Cache doesn't support resizing; create a new one
        audioCache = Cache(audioCacheDir, bytes)
        okHttpClient = buildCachedClient()
    }

    fun getAudioCacheSize(): Long = audioCache.size()

    fun getTotalCacheSize(): Long {
        return audioCache.size() + getDirSize(File(context.cacheDir, "image_cache"))
    }

    fun clearCache() {
        audioCache.evictAll()
        deleteDirContents(File(context.cacheDir, "image_cache"))
    }

    suspend fun computeCacheSizes(): Pair<Long, Long> = withContext(Dispatchers.IO) {
        val audio = audioCache.size()
        val image = getDirSize(File(context.cacheDir, "image_cache"))
        Pair(audio, image)
    }

    private fun getDirSize(dir: File): Long {
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun deleteDirContents(dir: File) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach {
            if (it.isDirectory) deleteDirContents(it)
            it.delete()
        }
    }
}
