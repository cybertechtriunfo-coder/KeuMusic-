/*
 * KeuMusic video stream resolver.
 * Reuses the app's existing InnerTube playback/authentication pipeline.
 */
package moe.rukamori.archivetune.utils.video

import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import moe.rukamori.archivetune.constants.PlayerStreamClient
import moe.rukamori.archivetune.innertube.PlaybackAuthState
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.YouTubeClient
import moe.rukamori.archivetune.innertube.models.YouTubeClient.Companion.WEB_REMIX
import moe.rukamori.archivetune.innertube.models.YouTubeClient.Companion.VISIONOS
import moe.rukamori.archivetune.innertube.models.response.PlayerResponse
import moe.rukamori.archivetune.utils.resolver.PlaybackStreamUrlDecipher
import moe.rukamori.archivetune.utils.potoken.BotGuardTokenGenerator
import moe.rukamori.archivetune.utils.resolver.PlaybackAuthCoordinator
import moe.rukamori.archivetune.utils.resolver.PlaybackClientSelector
import timber.log.Timber

internal object InnerTubeVideoResolver {
    private const val CLIENT_REQUEST_TIMEOUT_MS = 6_000L
    private const val CACHED_CLIENT_REQUEST_TIMEOUT_MS = 4_500L
    private const val MAX_CLIENT_ATTEMPTS = 7

    @Volatile
    private var lastSuccessfulVideoClient: YouTubeClient? = null

    enum class SourceType {
        PROGRESSIVE,
        MERGED,
    }

    data class ResolvedVideoStream(
        val id: String,
        val title: String,
        val uploader: String,
        val durationSeconds: Long,
        val thumbnailUrl: String?,
        val videoUrl: String,
        val videoMimeType: String,
        val audioUrl: String?,
        val audioMimeType: String?,
        val sourceType: SourceType,
        val selectedHeight: Int?,
        val clientName: String,
        val requestHeaders: Map<String, String>,
    )

    suspend fun resolve(
        videoId: String,
        preferredMaxHeight: Int = 720,
    ): ResolvedVideoStream {
        require(videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) { "ID de vídeo inválido." }

        val resolutionStartedAt = SystemClock.elapsedRealtime()
        var authState = YouTube.currentPlaybackAuthState()
        authState = PlaybackAuthCoordinator.ensureVisitorDataReady(
            videoId = videoId,
            authState = authState,
            reason = "YouTube video playback",
        )
        Timber.tag("KeuMusicVideo").i(
            "Auth ready for $videoId in ${SystemClock.elapsedRealtime() - resolutionStartedAt}ms",
        )

        val hasWebTokens = hasCompleteWebTokens(authState)
        val signatureTimestamp = PlaybackStreamUrlDecipher.getSignatureTimestampOrNull(videoId)
        val clientOrder = PlaybackClientSelector
            .buildStreamClientOrder(PlayerStreamClient.ANDROID_VR, authState)
            .filterNot { it == WEB_REMIX }
            .let { clients ->
                clients.filterNot { it == VISIONOS } + clients.filter { it == VISIONOS }
            }
            .let { fallback ->
                if (hasWebTokens) listOf(WEB_REMIX) + fallback else fallback
            }
        val cachedVideoClient = lastSuccessfulVideoClient
        val prioritizedClientOrder = if (cachedVideoClient == null) {
            clientOrder
        } else {
            listOf(cachedVideoClient) + clientOrder.filterNot { it == cachedVideoClient }
        }

        for (client in prioritizedClientOrder.take(MAX_CLIENT_ATTEMPTS)) {
            val resolved = tryClient(
                videoId = videoId,
                client = client,
                authState = authState,
                signatureTimestamp = signatureTimestamp,
                preferredMaxHeight = preferredMaxHeight,
                timeoutMs = if (client == cachedVideoClient) CACHED_CLIENT_REQUEST_TIMEOUT_MS else CLIENT_REQUEST_TIMEOUT_MS,
            )
            if (resolved != null) {
                lastSuccessfulVideoClient = client
                Timber.tag("KeuMusicVideo").i(
                    "Resolved $videoId with ${client.clientName} in ${SystemClock.elapsedRealtime() - resolutionStartedAt}ms",
                )
                return resolved
            }
        }

        // Web Remix is the client for which this app already has a WebView BotGuard
        // token generator. Mint a correctly paired player/GVS token only after the
        // normal client ladder has failed, avoiding a cold WebView start for videos
        // that are playable through the faster fallback clients.
        val visitorData = authState.visitorData?.takeIf { it.isNotBlank() }
        if (visitorData != null) {
            val minted = try {
                BotGuardTokenGenerator.mintToken(videoId, visitorData)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Timber.w(error, "BotGuard token generation failed for video $videoId")
                null
            }

            if (minted != null) {
                authState = authState.copy(
                    poTokenPlayer = minted.playerToken,
                    poTokenGvs = minted.sessionToken,
                    webClientPoTokenEnabled = true,
                ).normalized()

                tryClient(
                    videoId = videoId,
                    client = WEB_REMIX,
                    authState = authState,
                    signatureTimestamp = signatureTimestamp,
                    preferredMaxHeight = preferredMaxHeight,
                    timeoutMs = CLIENT_REQUEST_TIMEOUT_MS,
                )?.let {
                    lastSuccessfulVideoClient = WEB_REMIX
                    Timber.tag("KeuMusicVideo").i(
                        "Resolved $videoId with WEB_REMIX after token generation in ${SystemClock.elapsedRealtime() - resolutionStartedAt}ms",
                    )
                    return it
                }
            }
        }

        throw IllegalStateException(
            "O YouTube não forneceu um fluxo de vídeo compatível. Tente novamente mais tarde.",
        )
    }

    private suspend fun tryClient(
        videoId: String,
        client: YouTubeClient,
        authState: PlaybackAuthState,
        signatureTimestamp: Int?,
        preferredMaxHeight: Int,
        timeoutMs: Long,
    ): ResolvedVideoStream? {
        val requestStartedAt = SystemClock.elapsedRealtime()
        val result = try {
            withTimeoutOrNull(timeoutMs) {
                YouTube.player(
                    videoId = videoId,
                    client = client,
                    signatureTimestamp = signatureTimestamp,
                    setLogin = authState.hasPlaybackLoginContext,
                    authState = authState,
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Timber.tag("KeuMusicVideo").w(error, "Player request failed for client ${client.clientName}")
            null
        }

        if (result == null) {
            Timber.tag("KeuMusicVideo").w(
                "Player request timed out for client ${client.clientName} after ${SystemClock.elapsedRealtime() - requestStartedAt}ms",
            )
            return null
        }
        Timber.tag("KeuMusicVideo").d(
            "Player response from ${client.clientName} in ${SystemClock.elapsedRealtime() - requestStartedAt}ms",
        )

        val response = result.getOrElse { error ->
            if (error is CancellationException) throw error
            Timber.tag("KeuMusicVideo").w(error, "Player request returned an error for client ${client.clientName}")
            return null
        }

        if (response.playabilityStatus.status != "OK") {
            Timber.tag("KeuMusicVideo").w(
                "YouTube client ${client.clientName} rejected video $videoId: ${response.playabilityStatus.reason}",
            )
            return null
        }

        val streamingData = response.streamingData ?: return null
        val details = response.videoDetails
        val title = details?.title?.takeIf { it.isNotBlank() } ?: "Vídeo do YouTube"
        val uploader = details?.author.orEmpty()
        val durationSeconds = details?.lengthSeconds?.toLongOrNull() ?: 0L
        val thumbnail = details?.thumbnail?.thumbnails
            ?.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }
            ?.normalizedUrl

        // First prefer a muxed progressive format: one URL, one source and less
        // startup work. If YouTube only exposes adaptive streams, merge video+audio.
        val progressiveCandidates = streamingData.formats.orEmpty()
            .filter { !it.isAudio && it.width != null && it.height != null }
            .sortedWith(videoFormatComparator(preferredMaxHeight))

        for (format in progressiveCandidates) {
            val url = PlaybackStreamUrlDecipher.findUrlOrNull(
                format = format,
                videoId = videoId,
                client = client,
                authState = authState,
            ) ?: continue

            return ResolvedVideoStream(
                id = videoId,
                title = title,
                uploader = uploader,
                durationSeconds = durationSeconds,
                thumbnailUrl = thumbnail,
                videoUrl = url,
                videoMimeType = format.mimeType.substringBefore(';'),
                audioUrl = null,
                audioMimeType = null,
                sourceType = SourceType.PROGRESSIVE,
                selectedHeight = format.height,
                clientName = client.clientName,
                requestHeaders = buildStreamHeaders(client, authState),
            )
        }

        val videoCandidates = streamingData.adaptiveFormats
            .filter { !it.isAudio && it.width != null && it.height != null }
            .sortedWith(videoFormatComparator(preferredMaxHeight))
        val audioCandidates = streamingData.adaptiveFormats
            .filter { it.isAudio && it.bitrate > 0 }
            .sortedByDescending { it.bitrate }

        for (videoFormat in videoCandidates) {
            val videoUrl = PlaybackStreamUrlDecipher.findUrlOrNull(
                format = videoFormat,
                videoId = videoId,
                client = client,
                authState = authState,
            ) ?: continue

            for (audioFormat in audioCandidates) {
                val audioUrl = PlaybackStreamUrlDecipher.findUrlOrNull(
                    format = audioFormat,
                    videoId = videoId,
                    client = client,
                    authState = authState,
                ) ?: continue

                return ResolvedVideoStream(
                    id = videoId,
                    title = title,
                    uploader = uploader,
                    durationSeconds = durationSeconds,
                    thumbnailUrl = thumbnail,
                    videoUrl = videoUrl,
                    videoMimeType = videoFormat.mimeType.substringBefore(';'),
                    audioUrl = audioUrl,
                    audioMimeType = audioFormat.mimeType.substringBefore(';'),
                    sourceType = SourceType.MERGED,
                    selectedHeight = videoFormat.height,
                    clientName = client.clientName,
                    requestHeaders = buildStreamHeaders(client, authState),
                )
            }
        }

        return null
    }

    private fun videoFormatComparator(maxHeight: Int): Comparator<PlayerResponse.StreamingData.Format> =
        compareBy<PlayerResponse.StreamingData.Format>(
            { it.height?.let { height -> if (height <= maxHeight) 0 else 1 } ?: 1 },
            { it.height?.let { height -> if (height <= maxHeight) -height else height } ?: Int.MAX_VALUE },
            { -(it.bitrate) },
        )

    private fun buildStreamHeaders(
        client: YouTubeClient,
        authState: PlaybackAuthState,
    ): Map<String, String> = buildMap {
        put("User-Agent", client.userAgent)
        put("Origin", client.requestOrigin())
        put("Referer", client.requestReferer())
        put("Accept-Language", "en-US,en;q=0.8")
        put("X-YouTube-Client-Name", client.clientId)
        put("X-YouTube-Client-Version", client.clientVersion)
        authState.cookie?.takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
    }

    private fun hasCompleteWebTokens(authState: PlaybackAuthState): Boolean =
        authState.webClientPoTokenEnabled &&
            !authState.resolvePlayerPoToken(WEB_REMIX).isNullOrBlank() &&
            !authState.resolveGvsPoToken(WEB_REMIX).isNullOrBlank()
}
