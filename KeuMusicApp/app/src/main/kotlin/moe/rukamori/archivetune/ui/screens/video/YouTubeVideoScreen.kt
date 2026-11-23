/*
 * KeuMusic (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */
package moe.rukamori.archivetune.ui.screens.video

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.models.MediaMetadata as KeuMediaMetadata
import moe.rukamori.archivetune.ui.PlayerViewModel
import moe.rukamori.archivetune.utils.video.InnerTubeVideoResolver

@OptIn(UnstableApi::class)
@Composable
fun YouTubeVideoScreen(videoId: String) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
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

        // Reconnect to the service-owned player when returning to this screen.
        playerViewModel.currentVideoPlayer(validId)?.let { existing ->
            playerState.value = existing
            status.value = ""
            return@LaunchedEffect
        }

        status.value = "Extraindo vídeo do YouTube..."

        try {
            val stream = withContext(Dispatchers.IO) {
                InnerTubeVideoResolver.resolve(
                    videoId = validId,
                    preferredMaxHeight = 720,
                )
            }

            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(stream.requestHeaders["User-Agent"].orEmpty())
                .setDefaultRequestProperties(stream.requestHeaders)

            val keuMetadata = KeuMediaMetadata(
                id = stream.id,
                title = stream.title,
                artists = listOf(KeuMediaMetadata.Artist(id = null, name = stream.uploader)),
                duration = stream.durationSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                thumbnailUrl = stream.thumbnailUrl,
                isMusicVideo = true,
            )

            val videoMetadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(stream.title)
                .setArtist(stream.uploader)
                .setArtworkUri(stream.thumbnailUrl?.let(Uri::parse))
                .build()

            val videoItem = MediaItem.Builder()
                .setMediaId(stream.id)
                .setTag(keuMetadata)
                .setMediaMetadata(videoMetadata)
                .setUri(stream.videoUrl)
                .setMimeType(stream.videoMimeType)
                .build()

            val mediaSource = when (stream.sourceType) {
                InnerTubeVideoResolver.SourceType.MERGED -> {
                    val audioUrl = stream.audioUrl
                        ?: error("O extrator não retornou a faixa de áudio.")
                    val audioMimeType = stream.audioMimeType ?: "audio/mp4"

                    val videoSource = ProgressiveMediaSource.Factory(httpFactory)
                        .createMediaSource(videoItem)
                    val audioItem = MediaItem.Builder()
                        .setMediaId("${stream.id}:audio")
                        .setUri(audioUrl)
                        .setMimeType(audioMimeType)
                        .build()
                    val audioSource = ProgressiveMediaSource.Factory(httpFactory)
                        .createMediaSource(audioItem)
                    MergingMediaSource(videoSource, audioSource)
                }

                InnerTubeVideoResolver.SourceType.PROGRESSIVE ->
                    ProgressiveMediaSource.Factory(httpFactory)
                        .createMediaSource(videoItem)

            }

            // MusicService owns this player and exposes it through the same
            // MediaSession used by songs. Leaving the screen no longer releases it.
            val exo = playerViewModel.startVideoPlayback(mediaSource, keuMetadata)
                ?: error("O player principal do KeuMusic ainda não está conectado.")

            playerState.value = exo
            status.value = ""
            Log.i(
                "KeuMusicVideo",
                "Vídeo iniciado na sessão principal: client=" + stream.clientName +
                    ", source=" + stream.sourceType +
                    ", altura=" + stream.selectedHeight +
                    ", áudioSeparado=" + (stream.audioUrl != null),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Log.e("KeuMusicVideo", "Falha ao iniciar vídeo $validId", e)
            val detail = e.message?.takeIf { it.isNotBlank() } ?: "falha na extração. Tente novamente."
            status.value = "Não foi possível reproduzir este vídeo: $detail"
        }
    }

    val activePlayer = playerState.value

    // Detach only the surface when the screen closes; MusicService keeps audio alive.
    DisposableEffect(activePlayer) {
        onDispose {
            activePlayer?.clearVideoSurface()
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
                        controllerShowTimeoutMs = 3500
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        player = activePlayer
                    }
                },
                update = { it.player = activePlayer },
                onRelease = { view ->
                    // Player belongs to MusicService; detach the UI view only.
                    // This prevents a disposed PlayerView from retaining the Activity
                    // or screen while playback continues in the background.
                    view.player = null
                },
                modifier = Modifier.fillMaxSize(),
            )

            IconButton(
                onClick = {
                    playerViewModel.stopVideoPlayback()
                    playerState.value = null
                    status.value = "Vídeo encerrado."
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(42.dp)
                    .background(Color(0xFF6C3BDB), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Encerrar vídeo",
                    tint = Color.White,
                )
            }
        } else {
            Text(
                text = status.value,
                color = Color.White,
            )
        }
    }
}
