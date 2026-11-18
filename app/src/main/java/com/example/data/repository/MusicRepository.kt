package com.example.data.repository

import com.example.data.local.MusicDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistWithCount
import com.example.data.local.TrackEntity
import com.example.data.youtube.ParsedYouTubeInput
import com.example.data.youtube.YouTubeScraperClient
import com.example.data.youtube.YouTubeSearchResponse
import com.example.data.youtube.YouTubeTrackResult
import com.example.data.youtube.YouTubeUrlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicRepository(
    private val dao: MusicDao,
    private val youtubeClient: YouTubeScraperClient = YouTubeScraperClient()
) {
    private val accentPalette = listOf(
        0xFF1DB954L,
        0xFF8C1932L,
        0xFF1E3264L,
        0xFFE13300L,
        0xFF7358FFL,
        0xFF0D73ECL,
        0xFF503750L
    )

    val playlists: Flow<List<PlaylistWithCount>> = dao.observePlaylistsWithCount()
    val likedTracks: Flow<List<TrackEntity>> = dao.observeLikedTracks()
    val likedCount: Flow<Int> = dao.observeLikedCount()
    val likedVideoIds: Flow<Set<String>> = dao.observeLikedVideoIds().map { it.toSet() }
    val recentlyPlayed: Flow<List<TrackEntity>> = dao.observeRecentlyPlayed()
    val featuredTracks: Flow<List<TrackEntity>> = dao.observeAllUniqueTracks()

    fun observePlaylist(playlistId: String): Flow<PlaylistEntity?> =
        dao.observePlaylistById(playlistId)

    fun observeTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>> =
        dao.observeTracksForPlaylist(playlistId)

    suspend fun getTracksSnapshot(playlistId: String): List<TrackEntity> =
        dao.getTracksForPlaylist(playlistId)

    fun searchLocalTracks(query: String): Flow<List<TrackEntity>> =
        dao.searchLocalTracks(query)

    suspend fun seedStarterLibraryIfEmpty() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val isFirstRun = dao.getPlaylistCount() == 0

        data class SeedTrack(
            val videoId: String,
            val title: String,
            val artist: String,
            val durationText: String,
            val durationSeconds: Int
        )

        data class SeedPlaylist(
            val id: String,
            val title: String,
            val subtitle: String,
            val accentColorHex: Long,
            val isPinned: Boolean,
            val tracks: List<SeedTrack>
        )

        val builtInCatalog = listOf(
            SeedPlaylist(
                id = "builtin_top_brasil",
                title = "Top Hits & Pop Brasil",
                subtitle = "YouTube Mix • Sucessos",
                accentColorHex = 0xFF1DB954L,
                isPinned = true,
                tracks = listOf(
                    SeedTrack("JVpTp8IHdEg", "Hear Me Now", "Alok, Bruno Martini & Zeeba", "3:15", 195),
                    SeedTrack("qu3yTnbs8yg", "Envolver", "Anitta", "3:14", 194),
                    SeedTrack("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", "3:22", 202),
                    SeedTrack("k2qgadSvNyU", "New Rules", "Dua Lipa", "3:45", 225),
                    SeedTrack("orJSJGHjBLI", "Bad Habits", "Ed Sheeran", "4:01", 241)
                )
            ),
            SeedPlaylist(
                id = "builtin_eletronica",
                title = "Eletrônica & Festa",
                subtitle = "YouTube Beats • Dance",
                accentColorHex = 0xFF0D73ECL,
                isPinned = false,
                tracks = listOf(
                    SeedTrack("IcrbM1l_BoI", "Wake Me Up", "Avicii", "4:32", 272),
                    SeedTrack("5NV6Rdv1a3I", "Get Lucky", "Daft Punk ft. Pharrell Williams", "4:08", 248),
                    SeedTrack("AuI-g0SdEf0", "Faded", "Alan Walker", "3:32", 212),
                    SeedTrack("JVpTp8IHdEg", "Hear Me Now", "Alok & Bruno Martini", "3:15", 195),
                    SeedTrack("841W6zKuAkU", "apart (NCS)", "sumu", "3:12", 192)
                )
            ),
            SeedPlaylist(
                id = "builtin_pop_rock",
                title = "Pop & Rock Global",
                subtitle = "YouTube Clássicos • Alternativo",
                accentColorHex = 0xFF8C1932L,
                isPinned = false,
                tracks = listOf(
                    SeedTrack("yKNXEGYrfUc", "Yellow", "Coldplay", "4:33", 273),
                    SeedTrack("7wtfhSpvozo", "Believer", "Imagine Dragons", "3:36", 216),
                    SeedTrack("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", "3:22", 202),
                    SeedTrack("orJSJGHjBLI", "Bad Habits", "Ed Sheeran", "4:01", 241),
                    SeedTrack("k2qgadSvNyU", "New Rules", "Dua Lipa", "3:45", 225)
                )
            ),
            SeedPlaylist(
                id = "starter_chill_lofi",
                title = "Foco & Lo-fi Radio",
                subtitle = "YouTube Beats • Relax & Study",
                accentColorHex = 0xFF1E3264L,
                isPinned = true,
                tracks = listOf(
                    SeedTrack("jfKfPfyJRdk", "lofi hip hop radio - beats to relax/study to", "Lofi Girl", "AO VIVO", 0),
                    SeedTrack("5yx6BWlEVcY", "Chillhop Radio - jazzy & lofi hip hop beats", "Chillhop Music", "AO VIVO", 0),
                    SeedTrack("841W6zKuAkU", "sumu - apart", "NoCopyrightSounds (NCS)", "3:12", 192)
                )
            )
        )

        builtInCatalog.forEachIndexed { idx, seed ->
            if (dao.getPlaylistById(seed.id) == null) {
                val ts = now - (idx * 1000L)
                val playlist = PlaylistEntity(
                    id = seed.id,
                    title = seed.title,
                    subtitle = seed.subtitle,
                    thumbnailUrl = YouTubeUrlParser.hqThumbnail(seed.tracks.first().videoId),
                    youtubePlaylistId = null,
                    accentColorHex = seed.accentColorHex,
                    isPinned = seed.isPinned,
                    createdAt = ts,
                    updatedAt = ts
                )
                val tracks = seed.tracks.mapIndexed { pos, t ->
                    TrackEntity(
                        id = "${seed.id}_${t.videoId}_$pos",
                        playlistId = seed.id,
                        youtubeVideoId = t.videoId,
                        title = t.title,
                        artist = t.artist,
                        thumbnailUrl = YouTubeUrlParser.hqThumbnail(t.videoId),
                        durationText = t.durationText,
                        durationSeconds = t.durationSeconds,
                        position = pos
                    )
                }
                dao.replacePlaylistAndTracks(playlist, tracks)
            }
        }

        // Also enrich automatically with real YouTube playlists on first launch when online
        if (isFirstRun || dao.getPlaylistCount() <= 4) {
            try {
                val discoveryQueries = listOf(
                    "Sertanejo Universitário playlist",
                    "MPB clássicos playlist",
                    "Rock Nacional playlist"
                )
                for (query in discoveryQueries) {
                    val searchResult = youtubeClient.searchYouTube(query, playlistsOnly = true).getOrNull()
                    val firstPlaylist = searchResult?.playlists?.firstOrNull()
                    if (firstPlaylist != null && dao.getPlaylistByYouTubeId(firstPlaylist.playlistId) == null) {
                        importYouTubePlaylistById(firstPlaylist.playlistId)
                    }
                }
            } catch (_: Exception) {
                // Keep built-in playlists when offline
            }
        }
    }

    suspend fun importFromUrlOrId(rawInput: String): Result<PlaylistEntity> {
        return when (val parsed = YouTubeUrlParser.parse(rawInput)) {
            is ParsedYouTubeInput.Playlist -> importYouTubePlaylistById(parsed.playlistId)
            is ParsedYouTubeInput.Video -> importSingleVideoAsPlaylist(parsed.videoId)
            ParsedYouTubeInput.Invalid -> Result.failure(
                IllegalArgumentException("Cole um link válido de playlist ou vídeo do YouTube.")
            )
        }
    }

    suspend fun importYouTubePlaylistById(youtubePlaylistId: String): Result<PlaylistEntity> {
        val fetched = youtubeClient.fetchPlaylist(youtubePlaylistId)
        val imported = fetched.getOrElse { return Result.failure(it) }

        val existing = dao.getPlaylistByYouTubeId(youtubePlaylistId)
        val localId = existing?.id ?: "yt_${youtubePlaylistId}"
        val now = System.currentTimeMillis()
        val accent = existing?.accentColorHex
            ?: accentPalette[kotlin.math.abs(youtubePlaylistId.hashCode()) % accentPalette.size]

        val existingTracksByVideoId = dao.getTracksForPlaylist(localId).associateBy { it.youtubeVideoId }

        val playlistEntity = PlaylistEntity(
            id = localId,
            title = imported.title,
            subtitle = "YouTube • ${imported.author}",
            thumbnailUrl = imported.thumbnailUrl,
            youtubePlaylistId = youtubePlaylistId,
            accentColorHex = accent,
            isPinned = existing?.isPinned ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        val trackEntities = imported.tracks.mapIndexed { index, ytTrack ->
            val prev = existingTracksByVideoId[ytTrack.videoId]
            val wasLiked = prev?.isLiked ?: dao.isVideoLiked(ytTrack.videoId)
            TrackEntity(
                id = "${localId}_${ytTrack.videoId}",
                playlistId = localId,
                youtubeVideoId = ytTrack.videoId,
                title = ytTrack.title,
                artist = ytTrack.artist,
                thumbnailUrl = ytTrack.thumbnailUrl,
                durationText = ytTrack.durationText,
                durationSeconds = ytTrack.durationSeconds,
                position = index,
                isLiked = wasLiked,
                lastPlayedAt = prev?.lastPlayedAt ?: 0L
            )
        }

        dao.replacePlaylistAndTracks(playlistEntity, trackEntities)
        return Result.success(playlistEntity)
    }

    private suspend fun importSingleVideoAsPlaylist(videoId: String): Result<PlaylistEntity> {
        val track = youtubeClient.fetchSingleTrack(videoId).getOrElse { return Result.failure(it) }
        val playlistId = "custom_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val playlist = PlaylistEntity(
            id = playlistId,
            title = track.title,
            subtitle = "YouTube • ${track.artist}",
            thumbnailUrl = track.thumbnailUrl,
            youtubePlaylistId = null,
            accentColorHex = accentPalette[kotlin.math.abs(videoId.hashCode()) % accentPalette.size],
            createdAt = now,
            updatedAt = now
        )
        val trackEntity = TrackEntity(
            id = "${playlistId}_${track.videoId}",
            playlistId = playlistId,
            youtubeVideoId = track.videoId,
            title = track.title,
            artist = track.artist,
            thumbnailUrl = track.thumbnailUrl,
            durationText = track.durationText,
            durationSeconds = track.durationSeconds,
            position = 0,
            isLiked = dao.isVideoLiked(track.videoId)
        )
        dao.replacePlaylistAndTracks(playlist, listOf(trackEntity))
        return Result.success(playlist)
    }

    suspend fun createCustomPlaylist(title: String, subtitle: String = "Playlist Personalizada"): PlaylistEntity {
        val now = System.currentTimeMillis()
        val id = "custom_${UUID.randomUUID().toString().take(8)}"
        val entity = PlaylistEntity(
            id = id,
            title = title.trim().ifBlank { "Minha Playlist" },
            subtitle = subtitle.trim().ifBlank { "Playlist Personalizada" },
            thumbnailUrl = "",
            youtubePlaylistId = null,
            accentColorHex = accentPalette[kotlin.math.abs(id.hashCode()) % accentPalette.size],
            createdAt = now,
            updatedAt = now
        )
        dao.insertPlaylist(entity)
        return entity
    }

    suspend fun addTrackUrlToPlaylist(playlistId: String, rawInput: String): Result<TrackEntity> {
        return when (val parsed = YouTubeUrlParser.parse(rawInput)) {
            is ParsedYouTubeInput.Video -> {
                val track = youtubeClient.fetchSingleTrack(parsed.videoId)
                    .getOrElse { return Result.failure(it) }
                addYouTubeTrackToPlaylist(playlistId, track)
            }
            is ParsedYouTubeInput.Playlist -> {
                val imported = youtubeClient.fetchPlaylist(parsed.playlistId)
                    .getOrElse { return Result.failure(it) }
                var lastAdded: TrackEntity? = null
                for (t in imported.tracks) {
                    lastAdded = addYouTubeTrackToPlaylist(playlistId, t).getOrNull()
                }
                if (lastAdded != null) Result.success(lastAdded)
                else Result.failure(IllegalStateException("Nenhuma faixa encontrada na playlist."))
            }
            ParsedYouTubeInput.Invalid -> Result.failure(
                IllegalArgumentException("Cole um link válido de vídeo ou playlist do YouTube.")
            )
        }
    }

    suspend fun addYouTubeTrackToPlaylist(
        playlistId: String,
        track: YouTubeTrackResult
    ): Result<TrackEntity> {
        val playlist = dao.getPlaylistById(playlistId)
            ?: return Result.failure(IllegalStateException("Playlist não encontrada."))
        val nextPos = dao.getNextPositionForPlaylist(playlistId)
        val liked = dao.isVideoLiked(track.videoId)
        val entity = TrackEntity(
            id = "${playlistId}_${track.videoId}",
            playlistId = playlistId,
            youtubeVideoId = track.videoId,
            title = track.title,
            artist = track.artist,
            thumbnailUrl = track.thumbnailUrl,
            durationText = track.durationText,
            durationSeconds = track.durationSeconds,
            position = nextPos,
            isLiked = liked
        )
        dao.insertTrack(entity)
        if (playlist.thumbnailUrl.isBlank()) {
            dao.insertPlaylist(
                playlist.copy(
                    thumbnailUrl = track.thumbnailUrl,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return Result.success(entity)
    }

    suspend fun addExistingTrackToPlaylist(targetPlaylistId: String, track: TrackEntity) {
        addYouTubeTrackToPlaylist(
            targetPlaylistId,
            YouTubeTrackResult(
                videoId = track.youtubeVideoId,
                title = track.title,
                artist = track.artist,
                thumbnailUrl = track.thumbnailUrl,
                durationText = track.durationText,
                durationSeconds = track.durationSeconds
            )
        )
    }

    suspend fun removeTrackFromPlaylist(trackId: String) {
        dao.deleteTrackById(trackId)
    }

    suspend fun deletePlaylist(playlistId: String) {
        dao.deletePlaylistById(playlistId)
    }

    suspend fun toggleLike(youtubeVideoId: String): Boolean {
        val currentlyLiked = dao.isVideoLiked(youtubeVideoId)
        val newLiked = !currentlyLiked
        dao.setTrackLikedByVideoId(youtubeVideoId, newLiked)
        return newLiked
    }

    suspend fun markTrackPlayed(youtubeVideoId: String) {
        dao.updateLastPlayedAt(youtubeVideoId, System.currentTimeMillis())
    }

    suspend fun syncPlaylist(playlistId: String): Result<PlaylistEntity> {
        val playlist = dao.getPlaylistById(playlistId)
            ?: return Result.failure(IllegalStateException("Playlist não encontrada."))
        val ytId = playlist.youtubePlaylistId
            ?: return Result.failure(IllegalStateException("Esta playlist é local e não possui link do YouTube."))
        return importYouTubePlaylistById(ytId)
    }

    suspend fun searchYouTube(query: String, playlistsOnly: Boolean = false): Result<YouTubeSearchResponse> {
        return youtubeClient.searchYouTube(query, playlistsOnly)
    }
}
