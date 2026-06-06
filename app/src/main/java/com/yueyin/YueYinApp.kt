package com.yueyin

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.yueyin.data.repository.SettingsRepository
import com.yueyin.data.repository.TingReaderRepository
import com.yueyin.player.AudiobookPlayerManager

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
        return ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.30).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizeBytes(512L * 1024 * 1024).build() }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
