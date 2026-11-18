package com.example.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.local.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import kotlin.random.Random

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlaybackUiState(
    val currentTrack: TrackEntity? = null,
    val queue: List<TrackEntity> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionSec: Float = 0f,
    val durationSec: Float = 0f,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isPlayerExpanded: Boolean = false,
    val showVideoStage: Boolean = true,
    val isPlayerReady: Boolean = false,
    val isPlayingOfflineCache: Boolean = false,
    val playerError: String? = null
)

class YouTubePlayerController(
    private val onTrackStarted: (TrackEntity) -> Unit,
    private val resolveOfflineAudioFile: (String) -> File? = { null },
    private val isNetworkAvailable: () -> Boolean = { true }
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var webView: WebView? = null
    private var offlineMediaPlayer: MediaPlayer? = null
    private var pendingVideoId: String? = null
    private var ignoreProgressUntilMs: Long = 0L

    private val offlineProgressRunnable = object : Runnable {
        override fun run() {
            val mp = offlineMediaPlayer
            if (mp != null && _state.value.isPlayingOfflineCache) {
                runCatching {
                    if (mp.isPlaying && System.currentTimeMillis() >= ignoreProgressUntilMs) {
                        val curSec = mp.currentPosition / 1000f
                        val durSec = mp.duration / 1000f
                        _state.update { st ->
                            st.copy(
                                currentPositionSec = curSec.coerceAtLeast(0f),
                                durationSec = if (durSec > 0f) durSec else st.durationSec
                            )
                        }
                    }
                }
                mainHandler.postDelayed(this, 500L)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(context: Context): WebView {
        webView?.let { return it }

        val wv = WebView(context.applicationContext).apply {
            setBackgroundColor(Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString().orEmpty()
                    return !(url.contains("youtube.com") || url.contains("youtube-nocookie.com") || url.contains("googlevideo.com"))
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    mainHandler.post {
                        webView?.destroy()
                        webView = null
                        _state.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                isPlayerReady = false,
                                playerError = "Player reiniciado. Toque em Play novamente."
                            )
                        }
                    }
                    return true
                }
            }
            addJavascriptInterface(PlayerJsBridge(), "AndroidBridge")
            loadDataWithBaseURL(
                "https://www.youtube.com",
                buildPlayerHtml(),
                "text/html",
                "UTF-8",
                null
            )
        }
        webView = wv
        return wv
    }

    fun playTrack(track: TrackEntity, queue: List<TrackEntity>) {
        val effectiveQueue = if (queue.isNotEmpty()) queue else listOf(track)
        val idx = effectiveQueue.indexOfFirst {
            it.id == track.id || it.youtubeVideoId == track.youtubeVideoId
        }.let { if (it >= 0) it else 0 }

        startTrackPlayback(track, effectiveQueue, idx)
    }

    fun playFromQueueIndex(index: Int) {
        val current = _state.value
        val track = current.queue.getOrNull(index) ?: return
        startTrackPlayback(track, current.queue, index)
    }

    private fun startTrackPlayback(
        track: TrackEntity,
        queue: List<TrackEntity>,
        index: Int
    ) {
        stopOfflinePlayer()
        _state.update {
            it.copy(
                currentTrack = track,
                queue = queue,
                currentIndex = index,
                isBuffering = true,
                currentPositionSec = 0f,
                durationSec = track.durationSeconds.toFloat(),
                isPlayingOfflineCache = false,
                playerError = null
            )
        }
        ignoreProgressUntilMs = 0L
        onTrackStarted(track)

        val online = isNetworkAvailable()
        val cachedFile = resolveOfflineAudioFile(track.youtubeVideoId)
        if (!online) {
            if (cachedFile != null && cachedFile.exists()) {
                playOfflineCachedFile(track, cachedFile)
            } else {
                evalJs("if(window.ytPlayer && ytPlayer.pauseVideo) ytPlayer.pauseVideo();")
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        isPlayingOfflineCache = false,
                        playerError = "Sem internet. Ative o cache em segundo plano para ouvir músicas offline."
                    )
                }
            }
            return
        }

        loadVideoOnWebView(track.youtubeVideoId)
    }

    fun togglePlayPause() {
        val st = _state.value
        if (st.currentTrack == null) return

        if (st.isPlayingOfflineCache) {
            val mp = offlineMediaPlayer
            if (mp != null) {
                if (st.isPlaying) {
                    runCatching { mp.pause() }
                    _state.update { it.copy(isPlaying = false) }
                } else {
                    runCatching { mp.start() }
                    _state.update { it.copy(isPlaying = true, playerError = null) }
                    mainHandler.removeCallbacks(offlineProgressRunnable)
                    mainHandler.post(offlineProgressRunnable)
                }
                return
            }
        }

        if (st.isPlaying) {
            evalJs("if(window.ytPlayer && ytPlayer.pauseVideo) ytPlayer.pauseVideo();")
            _state.update { it.copy(isPlaying = false) }
        } else {
            val online = isNetworkAvailable()
            if (!online) {
                val cachedFile = resolveOfflineAudioFile(st.currentTrack.youtubeVideoId)
                if (cachedFile != null && cachedFile.exists()) {
                    playOfflineCachedFile(st.currentTrack, cachedFile)
                } else {
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            playerError = "Sem internet e esta faixa não está salva no cache local."
                        )
                    }
                }
                return
            }
            if (!st.isPlayerReady) {
                loadVideoOnWebView(st.currentTrack.youtubeVideoId)
            } else {
                evalJs("if(window.ytPlayer && ytPlayer.playVideo) ytPlayer.playVideo();")
            }
            _state.update { it.copy(isPlaying = true, playerError = null) }
        }
    }

    fun playNext(autoAdvance: Boolean = false) {
        val st = _state.value
        if (st.queue.isEmpty()) return

        if (autoAdvance && st.repeatMode == RepeatMode.ONE) {
            seekTo(0f)
            if (st.isPlayingOfflineCache) {
                runCatching { offlineMediaPlayer?.start() }
            } else {
                evalJs("if(window.ytPlayer && ytPlayer.playVideo) ytPlayer.playVideo();")
            }
            return
        }

        val nextIdx = when {
            st.isShuffleEnabled && st.queue.size > 1 -> {
                var candidate: Int
                do {
                    candidate = Random.nextInt(st.queue.size)
                } while (candidate == st.currentIndex)
                candidate
            }
            st.currentIndex + 1 < st.queue.size -> st.currentIndex + 1
            st.repeatMode == RepeatMode.ALL || !autoAdvance -> 0
            else -> {
                _state.update { it.copy(isPlaying = false) }
                return
            }
        }
        playFromQueueIndex(nextIdx)
    }

    fun playPrevious() {
        val st = _state.value
        if (st.queue.isEmpty()) return
        if (st.currentPositionSec > 4f) {
            seekTo(0f)
            return
        }
        val prevIdx = if (st.currentIndex - 1 >= 0) st.currentIndex - 1 else st.queue.lastIndex
        playFromQueueIndex(prevIdx)
    }

    fun seekTo(seconds: Float) {
        val clamped = seconds.coerceAtLeast(0f)
        ignoreProgressUntilMs = System.currentTimeMillis() + 900L
        _state.update { it.copy(currentPositionSec = clamped) }
        if (_state.value.isPlayingOfflineCache) {
            runCatching { offlineMediaPlayer?.seekTo((clamped * 1000f).toInt()) }
        } else {
            evalJs("if(window.ytPlayer && ytPlayer.seekTo) ytPlayer.seekTo(${clamped.toInt()}, true);")
        }
    }

    fun toggleShuffle() {
        _state.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun cycleRepeatMode() {
        _state.update { st ->
            val next = when (st.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            st.copy(repeatMode = next)
        }
    }

    fun expandPlayer(expanded: Boolean) {
        _state.update { it.copy(isPlayerExpanded = expanded) }
    }

    fun toggleVideoStage() {
        _state.update { it.copy(showVideoStage = !it.showVideoStage) }
    }

    fun addTrackToQueueNext(track: TrackEntity) {
        _state.update { st ->
            if (st.currentTrack == null) return@update st
            val mutable = st.queue.toMutableList()
            val insertAt = (st.currentIndex + 1).coerceAtMost(mutable.size)
            mutable.add(insertAt, track)
            st.copy(queue = mutable)
        }
    }

    fun release() {
        mainHandler.post {
            stopOfflinePlayer()
            webView?.destroy()
            webView = null
        }
    }

    private fun playOfflineCachedFile(track: TrackEntity, file: File) {
        stopOfflinePlayer()
        evalJs("if(window.ytPlayer && ytPlayer.pauseVideo) ytPlayer.pauseVideo();")

        mainHandler.post {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    setOnPreparedListener { mp ->
                        mp.start()
                        val durSec = (mp.duration / 1000f).coerceAtLeast(track.durationSeconds.toFloat())
                        _state.update {
                            it.copy(
                                isPlaying = true,
                                isBuffering = false,
                                isPlayingOfflineCache = true,
                                durationSec = durSec,
                                playerError = null
                            )
                        }
                        mainHandler.removeCallbacks(offlineProgressRunnable)
                        mainHandler.post(offlineProgressRunnable)
                    }
                    setOnCompletionListener {
                        playNext(autoAdvance = true)
                    }
                    setOnErrorListener { _, _, _ ->
                        _state.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                isPlayingOfflineCache = false,
                                playerError = "Não foi possível reproduzir o áudio em cache local."
                            )
                        }
                        true
                    }
                    prepareAsync()
                }
                offlineMediaPlayer = player
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        isPlayingOfflineCache = false,
                        playerError = "Falha ao abrir arquivo em cache local."
                    )
                }
            }
        }
    }

    private fun stopOfflinePlayer() {
        mainHandler.removeCallbacks(offlineProgressRunnable)
        offlineMediaPlayer?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.stop()
                mp.reset()
                mp.release()
            }
        }
        offlineMediaPlayer = null
    }

    private fun loadVideoOnWebView(videoId: String) {
        if (!_state.value.isPlayerReady) {
            pendingVideoId = videoId
            evalJs("if(window.loadVideoSafe) window.loadVideoSafe('$videoId');")
        } else {
            evalJs("if(window.loadVideoSafe) window.loadVideoSafe('$videoId');")
        }
    }

    private fun evalJs(script: String) {
        mainHandler.post {
            webView?.evaluateJavascript(script, null)
        }
    }

    private inner class PlayerJsBridge {
        @JavascriptInterface
        fun onReady() {
            mainHandler.post {
                _state.update { it.copy(isPlayerReady = true) }
                pendingVideoId?.let { vid ->
                    pendingVideoId = null
                    evalJs("if(window.loadVideoSafe) window.loadVideoSafe('$vid');")
                }
            }
        }

        @JavascriptInterface
        fun onStateChange(playerState: Int) {
            mainHandler.post {
                when (playerState) {
                    1 -> _state.update {
                        it.copy(isPlaying = true, isBuffering = false, playerError = null)
                    }
                    2 -> _state.update {
                        it.copy(isPlaying = false, isBuffering = false)
                    }
                    3 -> _state.update {
                        it.copy(isBuffering = true)
                    }
                    0 -> playNext(autoAdvance = true)
                }
            }
        }

        @JavascriptInterface
        fun onProgress(currentSec: Float, durationSec: Float) {
            mainHandler.post {
                val skipPositionUpdate = System.currentTimeMillis() < ignoreProgressUntilMs
                _state.update { st ->
                    st.copy(
                        currentPositionSec = if (skipPositionUpdate) st.currentPositionSec else currentSec.coerceAtLeast(0f),
                        durationSec = if (durationSec > 0f) durationSec else st.durationSec
                    )
                }
            }
        }

        @JavascriptInterface
        fun onError(errorCode: Int) {
            mainHandler.post {
                val activeTrack = _state.value.currentTrack
                val cachedFile = activeTrack?.let { resolveOfflineAudioFile(it.youtubeVideoId) }
                if (activeTrack != null && cachedFile != null && cachedFile.exists()) {
                    playOfflineCachedFile(activeTrack, cachedFile)
                    return@post
                }
                val msg = when (errorCode) {
                    101, 150, 153 -> "Este vídeo restringiu reprodução incorporada. Toque em 'Abrir no YouTube' ou pule para a próxima."
                    100 -> "Vídeo não encontrado ou removido do YouTube."
                    else -> "Erro ao reproduzir vídeo ($errorCode). Tente a próxima faixa."
                }
                _state.update {
                    it.copy(isPlaying = false, isBuffering = false, playerError = msg)
                }
            }
        }
    }

    private fun buildPlayerHtml(): String = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
            #player { width: 100%; height: 100%; }
          </style>
        </head>
        <body>
          <div id="player"></div>
          <script>
            var tag = document.createElement('script');
            tag.src = "https://www.youtube.com/iframe_api";
            var firstScriptTag = document.getElementsByTagName('script')[0];
            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

            var ytPlayer = null;
            var pendingId = null;
            var progressTimer = null;

            function onYouTubeIframeAPIReady() {
              ytPlayer = new YT.Player('player', {
                width: '100%',
                height: '100%',
                playerVars: {
                  'playsinline': 1,
                  'autoplay': 1,
                  'controls': 0,
                  'rel': 0,
                  'modestbranding': 1,
                  'fs': 0,
                  'iv_load_policy': 3,
                  'origin': 'https://www.youtube.com'
                },
                events: {
                  'onReady': function() {
                    if (window.AndroidBridge) AndroidBridge.onReady();
                    if (pendingId) {
                      ytPlayer.loadVideoById(pendingId);
                      pendingId = null;
                    }
                    startProgressLoop();
                  },
                  'onStateChange': function(e) {
                    if (window.AndroidBridge) AndroidBridge.onStateChange(e.data);
                  },
                  'onError': function(e) {
                    if (window.AndroidBridge) AndroidBridge.onError(e.data);
                  }
                }
              });
            }

            window.loadVideoSafe = function(videoId) {
              if (ytPlayer && typeof ytPlayer.loadVideoById === 'function') {
                ytPlayer.loadVideoById(videoId);
              } else {
                pendingId = videoId;
              }
            };

            function startProgressLoop() {
              if (progressTimer) clearInterval(progressTimer);
              progressTimer = setInterval(function() {
                if (ytPlayer && typeof ytPlayer.getCurrentTime === 'function') {
                  var cur = ytPlayer.getCurrentTime() || 0;
                  var dur = ytPlayer.getDuration() || 0;
                  if (window.AndroidBridge) AndroidBridge.onProgress(cur, dur);
                }
              }, 500);
            }
          </script>
        </body>
        </html>
    """.trimIndent()
}
