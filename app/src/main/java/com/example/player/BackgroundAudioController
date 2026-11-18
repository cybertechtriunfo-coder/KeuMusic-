
package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

class BackgroundAudioController(context: Context) {

    private val appContext = context.applicationContext

    private var mediaController: MediaController? = null
    private var pendingTrack: Pair<String, String>? = null

    private val sessionToken = SessionToken(
        appContext,
        ComponentName(appContext, BackgroundAudioService::class.java)
    )

    private val controllerFuture =
        MediaController.Builder(appContext, sessionToken).buildAsync()

    init {
        controllerFuture.addListener(
            {
                try {
                    mediaController = controllerFuture.get()

                    pendingTrack?.let { track ->
                        play(track.first, track.second)
                        pendingTrack = null
                    }
                } catch (_: Exception) {
                    pendingTrack = null
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun play(url: String, title: String): Boolean {
        val uri = Uri.parse(url.trim())
        val host = uri.host.orEmpty().lowercase()

        if (uri.scheme != "https" || host.isBlank()) {
            return false
        }

        if (
            host == "youtube.com" ||
            host.endsWith(".youtube.com") ||
            host == "youtu.be"
        ) {
            return false
        }

        val controller = mediaController

        if (controller == null) {
            pendingTrack = url to title
            return true
        }

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()

        return true
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.play()
    }

    fun release() {
        pendingTrack = null
        mediaController = null
        MediaController.releaseFuture(controllerFuture)
    }
}
