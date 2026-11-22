/*
 * KeuMusic (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */
package moe.rukamori.archivetune.ui.screens.video

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.utils.video.NewPipeVideoDownloader
import moe.rukamori.archivetune.utils.video.NewPipeVideoResolver

@Composable
fun YouTubeVideoScreen(videoId: String) {
    val context = LocalContext.current
    val validId = remember(videoId) {
        videoId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    }

    val playerState = remember(validId) { mutableStateOf<ExoPlayer?>(null) }
    val status = remember(validId) { mutableStateOf("Preparando vídeo...") }

    LaunchedEffect(validId) {
        playerState.value = null

        if (validId == null) {
            status.value = "Vídeo inválido."
            return@LaunchedEffect
        }

        status.value = "Extraindo vídeo do YouTube..."

        try {
            val stream = withContext(Dispatchers.IO) {
                NewPipeVideoResolver.resolve(
                    videoId = validId,
                    preferredMaxHeight = 720,
                )
            }

            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(NewPipeVideoDownloader.USER_AGENT)
                .setDefaultRequestProperties(
                    mapOf("Accept-Language" to "en-US,en;q=0.8"),
                )

            val mediaSource = when (stream.sourceType) {
                NewPipeVideoResolver.SourceType.MERGED -> {
                    val audioUrl = stream.audioUrl
                        ?: error("O extrator não retornou a faixa de áudio.")
                    val audioMimeType = stream.audioMimeType ?: "audio/mp4"

                    val videoSource = ProgressiveMediaSource.Factory(httpFactory)
                        .createMediaSource(
                            MediaItem.Builder()
                                .setUri(stream.videoUrl)
                                .setMimeType(stream.videoMimeType)
                                .build(),
                        )
                    val audioSource = ProgressiveMediaSource.Factory(httpFactory)
                        .createMediaSource(
                            MediaItem.Builder()
                                .setUri(audioUrl)
                                .setMimeType(audioMimeType)
                                .build(),
                        )
                    MergingMediaSource(videoSource, audioSource)
                }

                NewPipeVideoResolver.SourceType.PROGRESSIVE ->
                    ProgressiveMediaSource.Factory(httpFactory)
                        .createMediaSource(
                            MediaItem.Builder()
                                .setUri(stream.videoUrl)
                                .setMimeType(stream.videoMimeType)
                                .build(),
                        )

                NewPipeVideoResolver.SourceType.HLS,
                NewPipeVideoResolver.SourceType.DASH ->
                    DefaultMediaSourceFactory(httpFactory)
                        .createMediaSource(
                            MediaItem.Builder()
                                .setUri(stream.videoUrl)
                                .setMimeType(stream.videoMimeType)
                                .build(),
                        )
            }

            val exo = ExoPlayer.Builder(context).build()
            exo.setMediaSource(mediaSource)
            exo.prepare()
            exo.playWhenReady = true

            playerState.value = exo
            status.value = ""
            Log.i(
                "KeuMusicVideo",
                "Vídeo resolvido: source=" + stream.sourceType +
                    ", altura=" + stream.selectedHeight +
                    ", áudioSeparado=" + (stream.audioUrl != null),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Log.e("KeuMusicVideo", "Falha ao iniciar vídeo", e)
            status.value = "Não foi possível reproduzir este vídeo: " + (e.message ?: "falha na extração")
        }
    }

    val activePlayer = playerState.value

    DisposableEffect(activePlayer) {
        onDispose {
            activePlayer?.release()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (activePlayer != null) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = true
                        player = activePlayer
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                update = { it.player = activePlayer },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = status.value,
                color = Color.White,
            )
        }
    }
}
