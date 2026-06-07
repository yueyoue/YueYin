package com.yueyin

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.yueyin.data.repository.SettingsRepository
import com.yueyin.data.repository.TingReaderRepository
import com.yueyin.player.AudiobookPlayerManager
import okhttp3.OkHttpClient

class YueYinApp : Application(), ImageLoaderFactory {
    lateinit var settingsRepository: SettingsRepository
    lateinit var tingRepository: TingReaderRepository
    lateinit var audiobookPlayerManager: AudiobookPlayerManager

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        tingRepository = TingReaderRepository()
        audiobookPlayerManager = AudiobookPlayerManager(this)
        audiobookPlayerManager.init("", "")
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                // Add auth header for Ting Reader API requests (cover proxy, etc.)
                val authHeader = tingRepository.getAuthToken()
                if (authHeader.length > 7 && request.url.host.let { it.contains("tthsdd.top") }) {
                    val newRequest = request.newBuilder()
                        .addHeader("Authorization", authHeader)
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
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizeBytes(512L * 1024 * 1024).build() }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
