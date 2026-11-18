package com.example.data.cache

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.TrackEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class CachedTrackEntry(
    val videoId: String,
    val title: String,
    val artist: String,
    val fileName: String,
    val sizeBytes: Long,
    val cachedAtMs: Long,
    val lastAccessedMs: Long
)

data class OfflineCacheStats(
    val cachedVideoIds: Set<String> = emptySet(),
    val cachedCount: Int = 0,
    val totalBytes: Long = 0L,
    val maxTracks: Int = OfflineMusicCacheManager.MAX_CACHED_TRACKS,
    val maxBytes: Long = OfflineMusicCacheManager.MAX_CACHE_BYTES,
    val isCachingInBackground: Boolean = false,
    val activeTrackTitle: String? = null
)

fun interface AudioStreamDownloader {
    suspend fun downloadAudio(videoId: String, destinationFile: File, maxBytes: Long): Boolean
}

class YouTubeAudioStreamDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : AudioStreamDownloader {

    override suspend fun downloadAudio(
        videoId: String,
        destinationFile: File,
        maxBytes: Long
    ): Boolean {
        val streamUrl = resolveAudioStreamUrl(videoId) ?: return false
        val request = Request.Builder()
            .url(streamUrl)
            .header("User-Agent", ANDROID_USER_AGENT)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                val contentLength = body.contentLength()
                if (contentLength > maxBytes) return false

                body.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var totalRead = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            totalRead += read
                            if (totalRead > maxBytes) {
                                return false
                            }
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                        totalRead > 0L
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveAudioStreamUrl(videoId: String): String? {
        val payload = JSONObject().apply {
            put("videoId", videoId)
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "ANDROID")
                    put("clientVersion", "19.29.37")
                    put("androidSdkVersion", 34)
                    put("hl", "pt")
                    put("gl", "BR")
                })
            })
        }

        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("User-Agent", ANDROID_USER_AGENT)
            .header("Content-Type", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) return null
                val json = JSONObject(raw)
                val streamingData = json.optJSONObject("streamingData") ?: return null
                val formats = streamingData.optJSONArray("adaptiveFormats") ?: return null

                var bestUrl: String? = null
                var bestBitrate = Int.MAX_VALUE
                for (i in 0 until formats.length()) {
                    val fmt = formats.optJSONObject(i) ?: continue
                    val mime = fmt.optString("mimeType")
                    val directUrl = fmt.optString("url")
                    if (mime.startsWith("audio/mp4", ignoreCase = true) && directUrl.isNotBlank()) {
                        val bitrate = fmt.optInt("bitrate", Int.MAX_VALUE)
                        if (bitrate < bestBitrate) {
                            bestBitrate = bitrate
                            bestUrl = directUrl
                        }
                    }
                }
                if (bestUrl != null) return bestUrl

                for (i in 0 until formats.length()) {
                    val fmt = formats.optJSONObject(i) ?: continue
                    val mime = fmt.optString("mimeType")
                    val directUrl = fmt.optString("url")
                    if (mime.startsWith("audio/", ignoreCase = true) && directUrl.isNotBlank()) {
                        return directUrl
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val ANDROID_USER_AGENT =
            "com.google.android.youtube/19.29.37 (Linux; U; Android 14) gzip"
    }
}

class OfflineMusicCacheManager(
    context: Context,
    private val downloader: AudioStreamDownloader = YouTubeAudioStreamDownloader(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxTracks: Int = MAX_CACHED_TRACKS,
    private val maxCacheBytes: Long = MAX_CACHE_BYTES,
    private val maxSingleTrackBytes: Long = MAX_SINGLE_TRACK_BYTES,
    private val networkCheck: () -> Boolean = { isDeviceOnline(context) }
) {
    private val appContext = context.applicationContext
    val cacheDir: File = File(appContext.filesDir, CACHE_FOLDER_NAME).apply {
        if (!exists()) mkdirs()
    }
    private val indexFile = File(cacheDir, INDEX_FILE_NAME)

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var backgroundJob: Job? = null

    private val _stats = MutableStateFlow(OfflineCacheStats(maxTracks = maxTracks, maxBytes = maxCacheBytes))
    val stats: StateFlow<OfflineCacheStats> = _stats.asStateFlow()

    init {
        refreshStatsFromDiskSync()
    }

    fun getCachedAudioFile(videoId: String): File? {
        val entries = readIndex()
        val entry = entries.firstOrNull { it.videoId == videoId } ?: return null
        val file = File(cacheDir, entry.fileName)
        if (!file.exists() || file.length() <= 0L) return null

        // Touch access timestamp for LRU retention
        val now = System.currentTimeMillis()
        val updated = entries.map {
            if (it.videoId == videoId) it.copy(lastAccessedMs = now) else it
        }
        writeIndex(updated)
        return file
    }

    fun isTrackCached(videoId: String): Boolean =
        _stats.value.cachedVideoIds.contains(videoId)

    fun scheduleBackgroundCaching(
        enabled: Boolean,
        candidates: List<TrackEntity>
    ) {
        if (!enabled) {
            cancelBackgroundCaching()
            return
        }
        if (candidates.isEmpty()) return

        backgroundJob?.cancel()
        backgroundJob = scope.launch {
            cacheCandidatesInternal(candidates)
        }
    }

    suspend fun cacheCandidatesNow(candidates: List<TrackEntity>) {
        cacheCandidatesInternal(candidates)
    }

    fun cancelBackgroundCaching() {
        backgroundJob?.cancel()
        backgroundJob = null
        cleanupTempFiles()
        _stats.update { it.copy(isCachingInBackground = false, activeTrackTitle = null) }
    }

    suspend fun clearCache() {
        cancelBackgroundCaching()
        mutex.withLock {
            cacheDir.listFiles()?.forEach { file ->
                runCatching { file.delete() }
            }
            refreshStatsFromDiskLocked()
        }
    }

    fun release() {
        cancelBackgroundCaching()
        scope.cancel()
    }

    private suspend fun cacheCandidatesInternal(candidates: List<TrackEntity>) {
        if (!networkCheck()) return

        val eligible = candidates
            .asSequence()
            .filter { it.youtubeVideoId.isNotBlank() && it.durationSeconds > 0 }
            .distinctBy { it.youtubeVideoId }
            .take(maxTracks)
            .toList()

        if (eligible.isEmpty()) return

        try {
            for (track in eligible) {
                if (!currentCoroutineContext().isActive) break
                val alreadyExists = mutex.withLock {
                    val entries = readIndex()
                    val existing = entries.firstOrNull { it.videoId == track.youtubeVideoId }
                    existing != null && File(cacheDir, existing.fileName).let { it.exists() && it.length() > 0L }
                }
                if (alreadyExists) continue

                _stats.update {
                    it.copy(
                        isCachingInBackground = true,
                        activeTrackTitle = track.title
                    )
                }

                val tempFile = File(cacheDir, "${track.youtubeVideoId}.tmp")
                val finalFileName = "${track.youtubeVideoId}.m4a"
                val finalFile = File(cacheDir, finalFileName)
                if (tempFile.exists()) tempFile.delete()

                val success = downloader.downloadAudio(
                    videoId = track.youtubeVideoId,
                    destinationFile = tempFile,
                    maxBytes = maxSingleTrackBytes
                )

                if (success && tempFile.exists() && tempFile.length() > 0L && currentCoroutineContext().isActive) {
                    mutex.withLock {
                        if (finalFile.exists()) finalFile.delete()
                        val moved = tempFile.renameTo(finalFile)
                        if (moved && finalFile.exists()) {
                            val now = System.currentTimeMillis()
                            val entries = readIndex()
                                .filterNot { it.videoId == track.youtubeVideoId }
                                .toMutableList()
                            entries.add(
                                CachedTrackEntry(
                                    videoId = track.youtubeVideoId,
                                    title = track.title,
                                    artist = track.artist,
                                    fileName = finalFileName,
                                    sizeBytes = finalFile.length(),
                                    cachedAtMs = now,
                                    lastAccessedMs = now
                                )
                            )
                            val trimmed = enforceLimitsLocked(entries)
                            writeIndex(trimmed)
                            refreshStatsFromDiskLocked()
                        } else {
                            tempFile.delete()
                        }
                    }
                } else {
                    tempFile.delete()
                }
            }
        } finally {
            cleanupTempFiles()
            _stats.update {
                it.copy(
                    isCachingInBackground = false,
                    activeTrackTitle = null
                )
            }
        }
    }

    private fun enforceLimitsLocked(entries: MutableList<CachedTrackEntry>): List<CachedTrackEntry> {
        val sortedByLru = entries.sortedBy { it.lastAccessedMs }.toMutableList()
        var totalSize = sortedByLru.sumOf { it.sizeBytes }

        while (sortedByLru.size > maxTracks || totalSize > maxCacheBytes) {
            if (sortedByLru.isEmpty()) break
            val oldest = sortedByLru.removeAt(0)
            totalSize -= oldest.sizeBytes
            File(cacheDir, oldest.fileName).delete()
        }
        return sortedByLru
    }

    private fun cleanupTempFiles() {
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".tmp")) {
                runCatching { file.delete() }
            }
        }
    }

    private fun refreshStatsFromDiskSync() {
        val entries = readIndex().filter { entry ->
            val f = File(cacheDir, entry.fileName)
            f.exists() && f.length() > 0L
        }
        writeIndex(entries)
        _stats.value = OfflineCacheStats(
            cachedVideoIds = entries.map { it.videoId }.toSet(),
            cachedCount = entries.size,
            totalBytes = entries.sumOf { it.sizeBytes },
            maxTracks = maxTracks,
            maxBytes = maxCacheBytes,
            isCachingInBackground = _stats.value.isCachingInBackground,
            activeTrackTitle = _stats.value.activeTrackTitle
        )
    }

    private fun refreshStatsFromDiskLocked() {
        refreshStatsFromDiskSync()
    }

    private fun readIndex(): List<CachedTrackEntry> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val raw = indexFile.readText()
            if (raw.isBlank()) return emptyList()
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val videoId = obj.optString("videoId")
                    val fileName = obj.optString("fileName")
                    if (videoId.isBlank() || fileName.isBlank()) continue
                    add(
                        CachedTrackEntry(
                            videoId = videoId,
                            title = obj.optString("title"),
                            artist = obj.optString("artist"),
                            fileName = fileName,
                            sizeBytes = obj.optLong("sizeBytes", 0L),
                            cachedAtMs = obj.optLong("cachedAtMs", 0L),
                            lastAccessedMs = obj.optLong("lastAccessedMs", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeIndex(entries: List<CachedTrackEntry>) {
        try {
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val array = JSONArray()
            entries.forEach { entry ->
                array.put(JSONObject().apply {
                    put("videoId", entry.videoId)
                    put("title", entry.title)
                    put("artist", entry.artist)
                    put("fileName", entry.fileName)
                    put("sizeBytes", entry.sizeBytes)
                    put("cachedAtMs", entry.cachedAtMs)
                    put("lastAccessedMs", entry.lastAccessedMs)
                })
            }
            indexFile.writeText(array.toString())
        } catch (_: Exception) {
        }
    }

    companion object {
        const val CACHE_FOLDER_NAME = "offline_music_cache"
        const val INDEX_FILE_NAME = "cache_index.json"
        const val MAX_CACHED_TRACKS = 12
        const val MAX_CACHE_BYTES = 50L * 1024L * 1024L
        const val MAX_SINGLE_TRACK_BYTES = 12L * 1024L * 1024L

        fun isDeviceOnline(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
}
