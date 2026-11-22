/*
 * KeuMusic video stream resolver.
 * Adapted from the stream-selection approach in Plyvanta (GPL-3.0):
 * https://github.com/Plyvanta/Plyvanta
 */
package moe.rukamori.archivetune.utils.video

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream

internal object NewPipeVideoResolver {
    enum class SourceType {
        PROGRESSIVE,
        MERGED,
        HLS,
        DASH,
    }

    data class ResolvedVideo(
        val id: String,
        val title: String,
        val uploader: String,
        val durationSeconds: Long,
        val thumbnailUrl: String?,
        val sourceType: SourceType,
        val videoUrl: String,
        val videoMimeType: String,
        val audioUrl: String?,
        val audioMimeType: String?,
        val selectedHeight: Int,
    )

    fun resolve(videoId: String, preferredMaxHeight: Int): ResolvedVideo {
        NewPipeVideoRuntime.initialize()

        val info = StreamInfo.getInfo("https://www.youtube.com/watch?v=$videoId")
        val thumbnailUrl = info.thumbnails
            ?.lastOrNull()
            ?.url

        if (isLive(info.streamType) && !info.hlsUrl.isNullOrBlank()) {
            return result(
                info, thumbnailUrl, SourceType.HLS, info.hlsUrl,
                "application/x-mpegURL", null, null, 0,
            )
        }

        val progressive = chooseVideo(info.videoStreams, preferredMaxHeight, requireVideoOnly = false)
        val videoOnly = chooseVideo(info.videoOnlyStreams, preferredMaxHeight, requireVideoOnly = true)
        val audio = chooseAudio(info.audioStreams)

        val mergedIsHigherQuality = videoOnly != null && audio != null &&
            (progressive == null || effectiveHeight(videoOnly) > effectiveHeight(progressive))

        if (mergedIsHigherQuality) {
            return result(
                info, thumbnailUrl, SourceType.MERGED,
                videoOnly!!.content, mimeType(videoOnly.format, "video/mp4"),
                audio!!.content, mimeType(audio.format, "audio/mp4"),
                effectiveHeight(videoOnly),
            )
        }

        if (progressive != null) {
            return result(
                info, thumbnailUrl, SourceType.PROGRESSIVE,
                progressive.content, mimeType(progressive.format, "video/mp4"),
                null, null, effectiveHeight(progressive),
            )
        }

        if (videoOnly != null && audio != null) {
            return result(
                info, thumbnailUrl, SourceType.MERGED,
                videoOnly.content, mimeType(videoOnly.format, "video/mp4"),
                audio.content, mimeType(audio.format, "audio/mp4"),
                effectiveHeight(videoOnly),
            )
        }

        if (!info.hlsUrl.isNullOrBlank()) {
            return result(
                info, thumbnailUrl, SourceType.HLS, info.hlsUrl,
                "application/x-mpegURL", null, null, 0,
            )
        }

        if (!info.dashMpdUrl.isNullOrBlank()) {
            return result(
                info, thumbnailUrl, SourceType.DASH, info.dashMpdUrl,
                "application/dash+xml", null, null, 0,
            )
        }

        throw IllegalStateException("O YouTube não retornou uma faixa de vídeo reproduzível.")
    }

    private fun chooseVideo(
        streams: List<VideoStream>?,
        preferredMaxHeight: Int,
        requireVideoOnly: Boolean,
    ): VideoStream? {
        if (streams.isNullOrEmpty()) return null

        val candidates = streams.filter { it.isUrl && it.isVideoOnly == requireVideoOnly }
        val byQuality = compareBy<VideoStream> { effectiveHeight(it) }.thenBy { it.bitrate }

        return candidates
            .filter { effectiveHeight(it) <= preferredMaxHeight }
            .maxWithOrNull(byQuality)
            ?: candidates.minWithOrNull(byQuality)
    }

    private fun chooseAudio(streams: List<AudioStream>?): AudioStream? =
        streams.orEmpty()
            .filter { it.isUrl }
            .maxByOrNull { maxOf(it.averageBitrate, it.bitrate) }

    private fun effectiveHeight(stream: VideoStream): Int {
        if (stream.height > 0) return stream.height
        val digits = stream.resolution?.replace(Regex("[^0-9].*$"), "")
        return digits?.toIntOrNull() ?: 0
    }

    private fun isLive(streamType: StreamType): Boolean =
        streamType == StreamType.LIVE_STREAM || streamType == StreamType.AUDIO_LIVE_STREAM

    private fun mimeType(format: MediaFormat?, fallback: String): String =
        format?.mimeType?.takeIf(String::isNotBlank) ?: fallback

    private fun result(
        info: StreamInfo,
        thumbnailUrl: String?,
        type: SourceType,
        videoUrl: String,
        videoMime: String,
        audioUrl: String?,
        audioMime: String?,
        selectedHeight: Int,
    ) = ResolvedVideo(
        id = info.id,
        title = info.name,
        uploader = info.uploaderName,
        durationSeconds = info.duration,
        thumbnailUrl = thumbnailUrl,
        sourceType = type,
        videoUrl = videoUrl,
        videoMimeType = videoMime,
        audioUrl = audioUrl,
        audioMimeType = audioMime,
        selectedHeight = selectedHeight,
    )
}
