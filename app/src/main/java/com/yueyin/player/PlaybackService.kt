package com.yueyin.player

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground service that keeps audio playback alive when the screen is locked
 * or the app is in the background. Uses Media3's MediaSessionService which
 * automatically manages the foreground state and notification.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // Retrieve the player from AudiobookPlayerManager (singleton via YueYinApp)
        val app = application as com.yueyin.YueYinApp
        val player = app.audiobookPlayerManager.getPlayer()
        if (player != null) {
            mediaSession = MediaSession.Builder(this, player)
                .build()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
