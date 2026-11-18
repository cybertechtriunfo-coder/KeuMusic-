package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["youtubeVideoId"]),
        Index(value = ["isLiked"]),
        Index(value = ["lastPlayedAt"])
    ]
)
data class TrackEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val youtubeVideoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationText: String = "",
    val durationSeconds: Int = 0,
    val position: Int = 0,
    val isLiked: Boolean = false,
    val lastPlayedAt: Long = 0L
)
