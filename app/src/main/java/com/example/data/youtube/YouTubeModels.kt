package com.example.data.youtube

data class YouTubeTrackResult(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationText: String = "",
    val durationSeconds: Int = 0
)

data class ImportedPlaylist(
    val youtubePlaylistId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val tracks: List<YouTubeTrackResult>
)

data class YouTubePlaylistSearchResult(
    val playlistId: String,
    val title: String,
    val author: String,
    val videoCountText: String,
    val thumbnailUrl: String
)

data class YouTubeSearchResponse(
    val playlists: List<YouTubePlaylistSearchResult>,
    val tracks: List<YouTubeTrackResult>
)
