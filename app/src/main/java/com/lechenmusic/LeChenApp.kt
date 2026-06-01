package com.lechenmusic

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.lechenmusic.data.repository.MusicRepository
import com.lechenmusic.data.repository.SettingsRepository
import com.lechenmusic.data.repository.TingReaderRepository
import com.lechenmusic.player.AudiobookPlayerManager
import com.lechenmusic.player.MusicPlayerManager

class LeChenApp : Application(), ImageLoaderFactory {
    lateinit var repository: MusicRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var playerManager: MusicPlayerManager
    lateinit var tingRepository: TingReaderRepository
    lateinit var audiobookPlayerManager: AudiobookPlayerManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = MusicRepository()
        settingsRepository = SettingsRepository(this)
        playerManager = MusicPlayerManager(this)
        playerManager.init(repository)
        tingRepository = TingReaderRepository()
        audiobookPlayerManager = AudiobookPlayerManager(this)
        audiobookPlayerManager.init("", "")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024) // 256MB
                    .build()
            }
            .crossfade(true)
            .build()
    }

    companion object {
        lateinit var instance: LeChenApp
            private set
    }
}
