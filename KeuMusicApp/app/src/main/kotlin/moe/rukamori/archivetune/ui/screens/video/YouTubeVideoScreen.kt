/*
 * KeuMusic (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */
package moe.rukamori.archivetune.ui.screens.video

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
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
fun YouTubeVideoScreen(
    videoId: String,
    onVideoStarted: () -> Boolean = { false },
) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val validId = remember(videoId) {
        videoId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    }

    var playerState by remember(validId) { mutableStateOf<ExoPlayer?>(null) }
    var status by remember(validId) { mutableStateOf("Preparando vídeo...") }
    var isLoading by remember(validId) { mutableStateOf(true) }
    var retryCount by remember(validId) { mutableIntStateOf(0) }

    LaunchedEffect(validId, retryCount) {
        playerState = null
        if (validId == null) {
            status = "Vídeo inválido."
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        status = "🎬 Preparando seu vídeo..."
        try {
            val existing = playerViewModel.currentVideoPlayer(validId)
            val activePlayer = existing ?: run {
                val stream = withContext(Dispatchers.IO) {
                    InnerTubeVideoResolver.resolve(videoId = validId, preferredMaxHeight = 720)
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
                        val audioUrl = stream.audioUrl ?: error("O extrator não retornou a faixa de áudio.")
                        val audioMimeType = stream.audioMimeType ?: "audio/mp4"
                        val videoSource = ProgressiveMediaSource.Factory(httpFactory).createMediaSource(videoItem)
                        val audioItem = MediaItem.Builder()
                            .setMediaId("${stream.id}:audio")
                            .setUri(audioUrl)
                            .setMimeType(audioMimeType)
                            .build()
                        val audioSource = ProgressiveMediaSource.Factory(httpFactory).createMediaSource(audioItem)
                        MergingMediaSource(videoSource, audioSource)
                    }
                    InnerTubeVideoResolver.SourceType.PROGRESSIVE ->
                        ProgressiveMediaSource.Factory(httpFactory).createMediaSource(videoItem)
                }
                playerViewModel.startVideoPlayback(mediaSource, keuMetadata)
                    ?: error("O player principal do KeuMusic ainda não está conectado.")
            }

            playerViewModel.registerVideoPlayback(validId)
            playerViewModel.requestSheetExpand()
            status = ""
            if (!onVideoStarted()) playerState = activePlayer
            Log.i("KeuMusicVideo", "Vídeo iniciado no player principal: $validId")
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e("KeuMusicVideo", "Falha ao iniciar vídeo $validId", error)
            status = "Não foi possível reproduzir: ${error.message?.takeIf { it.isNotBlank() } ?: "Tente novamente."}"
        } finally {
            isLoading = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        val activePlayer = playerState
        if (activePlayer != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = true
                        controllerShowTimeoutMs = 3500
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        player = activePlayer
                    }
                },
                update = { it.player = activePlayer },
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = {
                    playerViewModel.stopVideoPlayback()
                    playerState = null
                    status = "Vídeo encerrado."
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).size(42.dp)
                    .background(Color(0xFF6C3BDB), CircleShape),
            ) {
                Text("✕", color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (isLoading) {
                    Text("Preparando seu vídeo", color = Color.White)
                    Spacer(Modifier.height(18.dp))
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFF424242),
                    )
                } else {
                    Text(status, color = Color.White)
                    if (validId != null) {
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { retryCount++ }) { Text("Tentar novamente") }
                    }
                }
            }
        }
    }
}
