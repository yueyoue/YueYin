package com.yueyin

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.yueyin.cache.CacheManager
import com.yueyin.data.repository.SettingsRepository
import com.yueyin.data.repository.TingReaderRepository
import com.yueyin.player.AudiobookPlayerManager
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class YueYinApp : Application(), ImageLoaderFactory {
    lateinit var settingsRepository: SettingsRepository
    lateinit var tingRepository: TingReaderRepository
    lateinit var audiobookPlayerManager: AudiobookPlayerManager
    lateinit var cacheManager: CacheManager

    override fun onCreate() {
        super.onCreate()
        cacheManager = CacheManager(this)
        settingsRepository = SettingsRepository(this)
        tingRepository = TingReaderRepository()
        audiobookPlayerManager = AudiobookPlayerManager(this)
        audiobookPlayerManager.init("", "", cacheManager.okHttpClient)
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val authHeader = tingRepository.getAuthToken()
                val isApiHost = request.url.host.let { it.contains("tthsdd.top") }
                if (authHeader.length > 7 && isApiHost) {
                    val newRequest = request.newBuilder()
                        .addHeader("Authorization", authHeader)
                        .cacheControl(CacheControl.Builder().maxStale(604800, TimeUnit.SECONDS).build())
                        .build()
                    chain.proceed(newRequest)
                } else {
                    chain.proceed(request)
                }
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.30).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(cacheManager.maxCacheBytes)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
