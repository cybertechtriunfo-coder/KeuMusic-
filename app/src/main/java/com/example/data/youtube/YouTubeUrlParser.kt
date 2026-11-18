package com.example.data.youtube

import android.net.Uri

sealed class ParsedYouTubeInput {
    data class Playlist(val playlistId: String) : ParsedYouTubeInput()
    data class Video(val videoId: String) : ParsedYouTubeInput()
    data object Invalid : ParsedYouTubeInput()
}

object YouTubeUrlParser {

    private val playlistIdRegex = Regex("^(PL|OLAK5uy_|RD|UU|LL|FL)[A-Za-z0-9_-]{8,}$")
    private val videoIdRegex = Regex("^[A-Za-z0-9_-]{11}$")

    fun parse(rawInput: String): ParsedYouTubeInput {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ParsedYouTubeInput.Invalid

        if (playlistIdRegex.matches(trimmed)) {
            return ParsedYouTubeInput.Playlist(trimmed)
        }
        if (videoIdRegex.matches(trimmed)) {
            return ParsedYouTubeInput.Video(trimmed)
        }

        val normalizedUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return try {
            val uri = Uri.parse(normalizedUrl)
            val host = uri.host?.lowercase().orEmpty()
            if (!host.contains("youtube.com") && !host.contains("youtu.be")) {
                return ParsedYouTubeInput.Invalid
            }

            val listParam = uri.getQueryParameter("list")?.trim()
            if (!listParam.isNullOrBlank() && listParam.length >= 10) {
                return ParsedYouTubeInput.Playlist(listParam)
            }

            val vParam = uri.getQueryParameter("v")?.trim()
            if (!vParam.isNullOrBlank() && videoIdRegex.matches(vParam)) {
                return ParsedYouTubeInput.Video(vParam)
            }

            if (host.contains("youtu.be")) {
                val segment = uri.pathSegments.firstOrNull()?.trim()
                if (!segment.isNullOrBlank() && videoIdRegex.matches(segment)) {
                    return ParsedYouTubeInput.Video(segment)
                }
            }

            val pathSegments = uri.pathSegments
            val shortsIdx = pathSegments.indexOf("shorts")
            if (shortsIdx != -1 && shortsIdx + 1 < pathSegments.size) {
                val id = pathSegments[shortsIdx + 1]
                if (videoIdRegex.matches(id)) {
                    return ParsedYouTubeInput.Video(id)
                }
            }

            val liveIdx = pathSegments.indexOf("live")
            if (liveIdx != -1 && liveIdx + 1 < pathSegments.size) {
                val id = pathSegments[liveIdx + 1]
                if (videoIdRegex.matches(id)) {
                    return ParsedYouTubeInput.Video(id)
                }
            }

            ParsedYouTubeInput.Invalid
        } catch (_: Exception) {
            ParsedYouTubeInput.Invalid
        }
    }

    fun hqThumbnail(videoId: String): String =
        "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
}
