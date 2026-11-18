package com.example.data.youtube

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class YouTubeScraperClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    private val userAgent =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    private val desktopUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    suspend fun fetchPlaylist(playlistId: String): Result<ImportedPlaylist> = withContext(Dispatchers.IO) {
        try {
            val htmlResult = fetchPlaylistFromHtml(playlistId)
            if (htmlResult != null && htmlResult.tracks.isNotEmpty()) {
                return@withContext Result.success(htmlResult)
            }

            val rssResult = fetchPlaylistFromRss(playlistId)
            if (rssResult != null && rssResult.tracks.isNotEmpty()) {
                return@withContext Result.success(rssResult)
            }

            Result.failure(
                IllegalStateException(
                    "Não foi possível carregar faixas dessa playlist. Verifique se a playlist é pública."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSingleTrack(videoId: String): Result<YouTubeTrackResult> = withContext(Dispatchers.IO) {
        try {
            val oembedUrl =
                "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", desktopUserAgent)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val title = json.optString("title").ifBlank { "Vídeo $videoId" }
                        val author = json.optString("author_name").ifBlank { "YouTube" }
                        val thumb = json.optString("thumbnail_url")
                            .ifBlank { YouTubeUrlParser.hqThumbnail(videoId) }
                        return@withContext Result.success(
                            YouTubeTrackResult(
                                videoId = videoId,
                                title = title,
                                artist = author,
                                thumbnailUrl = thumb
                            )
                        )
                    }
                }
            }
            Result.success(
                YouTubeTrackResult(
                    videoId = videoId,
                    title = "Música do YouTube ($videoId)",
                    artist = "YouTube",
                    thumbnailUrl = YouTubeUrlParser.hqThumbnail(videoId)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchYouTube(
        query: String,
        playlistsOnly: Boolean = false
    ): Result<YouTubeSearchResponse> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val spParam = if (playlistsOnly) "&sp=EgIQAw%253D%253D" else ""
            val url = "https://www.youtube.com/results?search_query=$encoded$spParam&hl=pt&persist_hl=1"
            val html = executeGet(url, desktopUserAgent)
                ?: return@withContext Result.failure(IllegalStateException("Sem resposta do YouTube"))

            val initialData = extractJsonObject(html, "var ytInitialData = ")
                ?: extractJsonObject(html, "window[\"ytInitialData\"] = ")
                ?: return@withContext Result.success(YouTubeSearchResponse(emptyList(), emptyList()))

            val playlists = mutableListOf<YouTubePlaylistSearchResult>()
            val seenPlaylists = mutableSetOf<String>()
            val tracks = mutableListOf<YouTubeTrackResult>()
            val seenTracks = mutableSetOf<String>()

            traverseJson(initialData) { obj ->
                if (obj.has("playlistRenderer")) {
                    val p = obj.getJSONObject("playlistRenderer")
                    val pId = p.optString("playlistId")
                    if (pId.isNotBlank() && seenPlaylists.add(pId)) {
                        val title = extractText(p.optJSONObject("title")).ifBlank { "Playlist do YouTube" }
                        val author = extractText(p.optJSONObject("longBylineText"))
                            .ifBlank { extractText(p.optJSONObject("shortBylineText")) }
                            .ifBlank { "YouTube" }
                        val count = extractText(p.optJSONObject("videoCountText"))
                            .ifBlank { "${p.optString("videoCount", "")} faixas".trim() }
                        val thumb = extractBestThumbnail(p.optJSONObject("thumbnailRenderer")
                            ?.optJSONObject("playlistVideoThumbnailRenderer")
                            ?.optJSONObject("thumbnail"))
                            ?: extractBestThumbnail(p.optJSONArray("thumbnails"))
                            ?: ""
                        playlists.add(
                            YouTubePlaylistSearchResult(
                                playlistId = pId,
                                title = title,
                                author = author,
                                videoCountText = count,
                                thumbnailUrl = thumb
                            )
                        )
                    }
                } else if (obj.has("lockupViewModel")) {
                    val lockup = obj.getJSONObject("lockupViewModel")
                    val contentType = lockup.optString("contentType")
                    val pId = lockup.optString("contentId")
                    if ((contentType.contains("PLAYLIST", ignoreCase = true) || pId.startsWith("PL") || pId.startsWith("OLAK")) &&
                        pId.isNotBlank() && seenPlaylists.add(pId)
                    ) {
                        val metadata = lockup.optJSONObject("metadata")
                            ?.optJSONObject("lockupMetadataViewModel")
                        val title = metadata?.optJSONObject("title")?.optString("content").orEmpty()
                            .ifBlank { "Playlist do YouTube" }
                        val thumb = extractBestThumbnailFromLockup(lockup)
                        playlists.add(
                            YouTubePlaylistSearchResult(
                                playlistId = pId,
                                title = title,
                                author = "YouTube",
                                videoCountText = "Playlist",
                                thumbnailUrl = thumb
                            )
                        )
                    }
                } else if (obj.has("videoRenderer")) {
                    val v = obj.getJSONObject("videoRenderer")
                    val vId = v.optString("videoId")
                    if (vId.isNotBlank() && seenTracks.add(vId)) {
                        val title = extractText(v.optJSONObject("title"))
                        val artist = extractText(v.optJSONObject("ownerText"))
                            .ifBlank { extractText(v.optJSONObject("shortBylineText")) }
                            .ifBlank { "YouTube" }
                        val durationText = extractText(v.optJSONObject("lengthText"))
                        val durationSec = parseDurationSeconds(durationText)
                        val thumb = extractBestThumbnail(v.optJSONObject("thumbnail"))
                            ?: YouTubeUrlParser.hqThumbnail(vId)
                        if (title.isNotBlank()) {
                            tracks.add(
                                YouTubeTrackResult(
                                    videoId = vId,
                                    title = title,
                                    artist = artist,
                                    thumbnailUrl = thumb,
                                    durationText = durationText,
                                    durationSeconds = durationSec
                                )
                            )
                        }
                    }
                }
            }

            Result.success(
                YouTubeSearchResponse(
                    playlists = playlists.take(15),
                    tracks = tracks.take(25)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchPlaylistFromHtml(playlistId: String): ImportedPlaylist? {
        val url = "https://www.youtube.com/playlist?list=$playlistId&hl=pt&persist_hl=1"
        val html = executeGet(url, desktopUserAgent) ?: return null
        val initialData = extractJsonObject(html, "var ytInitialData = ")
            ?: extractJsonObject(html, "window[\"ytInitialData\"] = ")
            ?: return null

        var playlistTitle = ""
        var playlistAuthor = ""
        var playlistThumb = ""
        val tracks = mutableListOf<YouTubeTrackResult>()
        val seenVideoIds = mutableSetOf<String>()

        traverseJson(initialData) { obj ->
            if (playlistTitle.isBlank() && obj.has("playlistMetadataRenderer")) {
                val meta = obj.getJSONObject("playlistMetadataRenderer")
                playlistTitle = meta.optString("title")
            }
            if (obj.has("playlistHeaderRenderer")) {
                val header = obj.getJSONObject("playlistHeaderRenderer")
                if (playlistTitle.isBlank()) {
                    playlistTitle = extractText(header.optJSONObject("title"))
                }
                if (playlistAuthor.isBlank()) {
                    playlistAuthor = extractText(header.optJSONObject("ownerText"))
                }
                if (playlistThumb.isBlank()) {
                    playlistThumb = extractBestThumbnail(
                        header.optJSONObject("playlistHeaderBanner")
                            ?.optJSONObject("heroPlaylistThumbnailRenderer")
                            ?.optJSONObject("thumbnail")
                    ).orEmpty()
                }
            }
            if (obj.has("playlistVideoRenderer")) {
                val v = obj.getJSONObject("playlistVideoRenderer")
                val vId = v.optString("videoId")
                val isPlayable = v.optBoolean("isPlayable", true)
                if (vId.isNotBlank() && isPlayable && seenVideoIds.add(vId)) {
                    val title = extractText(v.optJSONObject("title"))
                    val artist = extractText(v.optJSONObject("shortBylineText"))
                        .ifBlank { "YouTube" }
                    val durationText = extractText(v.optJSONObject("lengthText"))
                    val durationSec = v.optString("lengthSeconds").toIntOrNull()
                        ?: parseDurationSeconds(durationText)
                    val thumb = extractBestThumbnail(v.optJSONObject("thumbnail"))
                        ?: YouTubeUrlParser.hqThumbnail(vId)
                    if (title.isNotBlank() && !title.equals("[Deleted video]", ignoreCase = true) &&
                        !title.equals("[Private video]", ignoreCase = true)
                    ) {
                        tracks.add(
                            YouTubeTrackResult(
                                videoId = vId,
                                title = title,
                                artist = artist,
                                thumbnailUrl = thumb,
                                durationText = durationText,
                                durationSeconds = durationSec
                            )
                        )
                    }
                }
            }
        }

        if (tracks.isEmpty()) return null
        val resolvedTitle = playlistTitle.ifBlank { "Playlist do YouTube" }
        val resolvedAuthor = playlistAuthor.ifBlank { tracks.firstOrNull()?.artist ?: "YouTube" }
        val resolvedThumb = playlistThumb.ifBlank { tracks.first().thumbnailUrl }
        return ImportedPlaylist(
            youtubePlaylistId = playlistId,
            title = resolvedTitle,
            author = resolvedAuthor,
            thumbnailUrl = resolvedThumb,
            tracks = tracks
        )
    }

    private fun fetchPlaylistFromRss(playlistId: String): ImportedPlaylist? {
        val rssUrl = "https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId"
        val xml = executeGet(rssUrl, desktopUserAgent) ?: return null

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var feedTitle = ""
        var feedAuthor = ""
        val tracks = mutableListOf<YouTubeTrackResult>()

        var inEntry = false
        var inAuthor = false
        var currentTag = ""
        var videoId = ""
        var entryTitle = ""
        var entryAuthor = ""
        var entryThumb = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.orEmpty()
                    when (currentTag) {
                        "entry" -> {
                            inEntry = true
                            videoId = ""
                            entryTitle = ""
                            entryAuthor = ""
                            entryThumb = ""
                        }
                        "author" -> inAuthor = true
                        "media:thumbnail" -> {
                            if (inEntry) {
                                entryThumb = parser.getAttributeValue(null, "url").orEmpty()
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        if (!inEntry) {
                            when (currentTag) {
                                "title" -> if (feedTitle.isBlank()) feedTitle = text
                                "name" -> if (inAuthor && feedAuthor.isBlank()) feedAuthor = text
                            }
                        } else {
                            when (currentTag) {
                                "yt:videoId" -> videoId = text
                                "title" -> if (entryTitle.isBlank()) entryTitle = text
                                "name" -> if (inAuthor && entryAuthor.isBlank()) entryAuthor = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "author" -> inAuthor = false
                        "entry" -> {
                            if (videoId.isNotBlank() && entryTitle.isNotBlank()) {
                                tracks.add(
                                    YouTubeTrackResult(
                                        videoId = videoId,
                                        title = entryTitle,
                                        artist = entryAuthor.ifBlank { "YouTube" },
                                        thumbnailUrl = entryThumb.ifBlank {
                                            YouTubeUrlParser.hqThumbnail(videoId)
                                        }
                                    )
                                )
                            }
                            inEntry = false
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        if (tracks.isEmpty()) return null
        return ImportedPlaylist(
            youtubePlaylistId = playlistId,
            title = feedTitle.ifBlank { "Playlist do YouTube" },
            author = feedAuthor.ifBlank { "YouTube" },
            thumbnailUrl = tracks.first().thumbnailUrl,
            tracks = tracks
        )
    }

    private fun executeGet(url: String, agent: String = userAgent): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", agent)
            .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun extractJsonObject(html: String, marker: String): JSONObject? {
        val markerIdx = html.indexOf(marker)
        if (markerIdx == -1) return null
        val startIdx = html.indexOf('{', markerIdx + marker.length)
        if (startIdx == -1) return null

        var depth = 0
        var inString = false
        var escaped = false
        for (i in startIdx until html.length) {
            val c = html[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\' && inString) {
                escaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') depth++
                else if (c == '}') {
                    depth--
                    if (depth == 0) {
                        val jsonStr = html.substring(startIdx, i + 1)
                        return try {
                            JSONObject(jsonStr)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }
        }
        return null
    }

    private fun traverseJson(node: Any?, onObject: (JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> {
                onObject(node)
                val keys = node.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    traverseJson(node.opt(key), onObject)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    traverseJson(node.opt(i), onObject)
                }
            }
        }
    }

    private fun extractText(json: JSONObject?): String {
        if (json == null) return ""
        val simple = json.optString("simpleText")
        if (simple.isNotBlank()) return simple
        val content = json.optString("content")
        if (content.isNotBlank()) return content
        val runs = json.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text").orEmpty())
        }
        return sb.toString().trim()
    }

    private fun extractBestThumbnail(thumbnailContainer: JSONObject?): String? {
        val arr = thumbnailContainer?.optJSONArray("thumbnails") ?: return null
        return extractBestThumbnail(arr)
    }

    private fun extractBestThumbnail(arr: JSONArray?): String? {
        if (arr == null || arr.length() == 0) return null
        var bestUrl: String? = null
        var bestWidth = -1
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val url = item.optString("url")
            val w = item.optInt("width", 0)
            if (url.isNotBlank() && w >= bestWidth) {
                bestWidth = w
                bestUrl = if (url.startsWith("//")) "https:$url" else url
            }
        }
        return bestUrl
    }

    private fun extractBestThumbnailFromLockup(lockup: JSONObject): String {
        val sources = lockup.optJSONObject("contentImage")
            ?.optJSONObject("collectionThumbnailViewModel")
            ?.optJSONObject("primaryThumbnail")
            ?.optJSONObject("thumbnailViewModel")
            ?.optJSONObject("image")
            ?.optJSONArray("sources")
        return extractBestThumbnail(sources).orEmpty()
    }

    private fun parseDurationSeconds(text: String): Int {
        if (text.isBlank()) return 0
        val parts = text.trim().split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }
}
