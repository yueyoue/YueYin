package com.lechenmusic

import android.app.Application
import com.lechenmusic.data.repository.MusicRepository
import com.lechenmusic.data.repository.SettingsRepository
import com.lechenmusic.data.repository.TingReaderRepository
import com.lechenmusic.player.AudiobookPlayerManager
import com.lechenmusic.player.MusicPlayerManager

class LeChenApp : Application() {
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

    companion object {
        lateinit var instance: LeChenApp
            private set
    }
}
