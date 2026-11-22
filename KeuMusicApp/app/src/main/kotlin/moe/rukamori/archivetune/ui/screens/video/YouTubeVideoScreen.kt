/*
 * KeuMusic (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */
package moe.rukamori.archivetune.ui.screens.video

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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.obfuscator.quickjs.youtubei.YoutubeiNetworkConfiguration
import moe.rukamori.archivetune.obfuscator.quickjs.youtubei.YoutubeiResolutionPriority
import moe.rukamori.archivetune.obfuscator.quickjs.youtubei.YoutubeiResolver
import moe.rukamori.archivetune.obfuscator.quickjs.youtubei.YoutubeiVideoRequest
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.utils.potoken.BotGuardTokenGenerator
import okhttp3.Dns
import java.net.Proxy

@Composable
fun YouTubeVideoScreen(videoId: String) {
    val context = LocalContext.current
    val validId = remember(videoId) {
        videoId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    }

    val playerState = remember(validId) { mutableStateOf<ExoPlayer?>(null) }
    val status = remember(validId) { mutableStateOf("Preparando vídeo...") }

    val resolver = remember(context) {
        YoutubeiResolver(
            context = context.applicationContext,
            diagnostics = { message -> Log.d("KeuMusicVideo", message) },
            networkConfigurationProvider = {
                YoutubeiNetworkConfiguration(
                    proxy = null,
                    proxyUsername = null,
                    proxyPassword = null,
                    dns = Dns.SYSTEM,
                )
            },
        )
    }

    LaunchedEffect(validId) {
        playerState.value = null

        if (validId == null) {
            status.value = "Vídeo inválido."
            return@LaunchedEffect
        }

        status.value = "Extraindo vídeo do YouTube..."

        try {
            val stream = withContext(Dispatchers.IO) {
                // Reuse the same authenticated YouTube session already used by audio playback.
                val authState = YouTube.currentPlaybackAuthState()
                val sessionId = authState.sessionId ?: authState.visitorData
                val mintedTokens = sessionId?.let {
                    BotGuardTokenGenerator.mintToken(validId, it)
                }

                resolver.resolveVideo(
                    request = YoutubeiVideoRequest(
                        mediaId = validId,
                        authFingerprint = "keumusic-video-${authState.fingerprint}-$validId",
                        maxHeight = 720,
                        cookie = authState.cookie,
                        visitorData = authState.visitorData,
                        dataSyncId = authState.dataSyncId,
                        sessionPoToken = authState.poTokenGvs
                            ?: authState.poToken
                            ?: mintedTokens?.sessionToken,
                        videoPoToken = authState.poTokenPlayer
                            ?: authState.poToken
                            ?: mintedTokens?.playerToken,
                        language = "pt-BR",
                        location = "BR",
                        timezone = "America/Sao_Paulo",
                    ),
                    priority = YoutubeiResolutionPriority.FOREGROUND,
                    videoPoTokenProvider = { requestedMediaId ->
                        if (requestedMediaId == validId && sessionId != null) {
                            mintedTokens?.playerToken
                                ?: BotGuardTokenGenerator.mintToken(requestedMediaId, sessionId)?.playerToken
                        } else {
                            null
                        }
                    },
                )
            }

            if (!stream.url.startsWith("https://") && !stream.url.startsWith("http://")) {
                error("O resolvedor retornou um endereço de vídeo inválido.")
            }

            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .setDefaultRequestProperties(stream.requestHeaders)

            val mediaSource = ProgressiveMediaSource.Factory(httpFactory)
                .createMediaSource(MediaItem.fromUri(stream.url))

            val exo = ExoPlayer.Builder(context).build()
            exo.setMediaSource(mediaSource)
            exo.prepare()
            exo.playWhenReady = true

            playerState.value = exo
            status.value = ""
            Log.i(
                "KeuMusicVideo",
                "Vídeo resolvido: itag=${stream.formatId}, mime=${stream.mimeType}, altura=${stream.height}",
            )
        } catch (e: Exception) {
            Log.e("KeuMusicVideo", "Falha ao iniciar vídeo", e)
            status.value = "Não foi possível reproduzir este vídeo: ${e.message ?: "falha na extração"}"
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
                        this.player = activePlayer
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
