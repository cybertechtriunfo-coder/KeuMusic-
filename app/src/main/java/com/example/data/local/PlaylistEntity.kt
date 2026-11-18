package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val youtubePlaylistId: String? = null,
    val accentColorHex: Long = 0xFF1DB954L,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class PlaylistWithCount(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val youtubePlaylistId: String?,
    val accentColorHex: Long,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val trackCount: Int
)
