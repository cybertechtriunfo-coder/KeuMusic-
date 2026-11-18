package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query(
        """
        SELECT p.*, COUNT(t.id) AS trackCount
        FROM playlists p
        LEFT JOIN tracks t ON p.id = t.playlistId
        GROUP BY p.id
        ORDER BY p.isPinned DESC, p.updatedAt DESC
        """
    )
    fun observePlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    fun observePlaylistById(playlistId: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE youtubePlaylistId = :youtubePlaylistId LIMIT 1")
    suspend fun getPlaylistByYouTubeId(youtubePlaylistId: String): PlaylistEntity?

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int

    @Query("SELECT * FROM tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getTracksForPlaylist(playlistId: String): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE isLiked = 1 GROUP BY youtubeVideoId ORDER BY lastPlayedAt DESC, title ASC")
    fun observeLikedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(DISTINCT youtubeVideoId) FROM tracks WHERE isLiked = 1")
    fun observeLikedCount(): Flow<Int>

    @Query("SELECT youtubeVideoId FROM tracks WHERE isLiked = 1")
    fun observeLikedVideoIds(): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE lastPlayedAt > 0 GROUP BY youtubeVideoId ORDER BY lastPlayedAt DESC LIMIT 15")
    fun observeRecentlyPlayed(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks GROUP BY youtubeVideoId ORDER BY position ASC, title ASC LIMIT 30")
    fun observeAllUniqueTracks(): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'
        GROUP BY youtubeVideoId
        ORDER BY title ASC
        LIMIT 40
        """
    )
    fun searchLocalTracks(query: String): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE playlistId = :playlistId")
    suspend fun deleteTracksByPlaylist(playlistId: String)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    @Query("UPDATE tracks SET isLiked = :liked WHERE youtubeVideoId = :youtubeVideoId")
    suspend fun setTrackLikedByVideoId(youtubeVideoId: String, liked: Boolean)

    @Query("SELECT EXISTS(SELECT 1 FROM tracks WHERE youtubeVideoId = :youtubeVideoId AND isLiked = 1)")
    suspend fun isVideoLiked(youtubeVideoId: String): Boolean

    @Query("UPDATE tracks SET lastPlayedAt = :timestamp WHERE youtubeVideoId = :youtubeVideoId")
    suspend fun updateLastPlayedAt(youtubeVideoId: String, timestamp: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tracks WHERE playlistId = :playlistId")
    suspend fun getNextPositionForPlaylist(playlistId: String): Int

    @Transaction
    suspend fun replacePlaylistAndTracks(playlist: PlaylistEntity, tracks: List<TrackEntity>) {
        insertPlaylist(playlist)
        deleteTracksByPlaylist(playlist.id)
        if (tracks.isNotEmpty()) {
            insertTracks(tracks)
        }
    }
}
