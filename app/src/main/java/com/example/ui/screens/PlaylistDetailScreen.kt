package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.ui.components.CoverArtwork
import com.example.ui.components.ImportYouTubeDialog
import com.example.ui.components.TrackRowItem
import com.example.ui.components.YouTubeBadge
import com.example.ui.theme.LikedGradientStart

@Composable
fun PlaylistDetailScreen(
    isLikedCollection: Boolean,
    playlist: PlaylistEntity?,
    tracks: List<TrackEntity>,
    currentPlayingVideoId: String?,
    likedVideoIds: Set<String>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onPlayAll: (shuffle: Boolean) -> Unit,
    onPlayTrack: (TrackEntity) -> Unit,
    onSyncPlaylist: () -> Unit,
    onAddTrackUrl: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onAddTrackToAnotherPlaylist: (TrackEntity) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onRemoveTrack: ((String) -> Unit)?,
    onOpenInYouTube: (String) -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    cachedVideoIds: Set<String> = emptySet()
) {
    var showAddTrackDialog by remember { mutableStateOf(false) }

    val headerColor = if (isLikedCollection) LikedGradientStart
    else Color(playlist?.accentColorHex ?: 0xFF1DB954L)

    val title = if (isLikedCollection) "Músicas Curtidas" else (playlist?.title ?: "Playlist")
    val subtitle = if (isLikedCollection) "Suas favoritas do YouTube" else (playlist?.subtitle ?: "")
    val coverUrl = if (isLikedCollection) "" else (playlist?.thumbnailUrl ?: tracks.firstOrNull()?.thumbnailUrl.orEmpty())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Gradient Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                headerColor.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (showBackButton) Arrangement.SpaceBetween else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("playlist_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Voltar",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!isLikedCollection && playlist?.youtubePlaylistId != null) {
                            IconButton(
                                onClick = onSyncPlaylist,
                                enabled = !isBusy,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("sync_playlist_button")
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Sync,
                                        contentDescription = "Sincronizar com YouTube",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                        if (!isLikedCollection) {
                            IconButton(
                                onClick = { showAddTrackDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("add_track_to_playlist_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Adicionar música do YouTube",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CoverArtwork(
                        imageUrl = coverUrl,
                        contentDescription = title,
                        modifier = Modifier.size(204.dp),
                        cornerRadius = 12.dp,
                        isLikedCollection = isLikedCollection
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (playlist?.youtubePlaylistId != null) {
                        YouTubeBadge()
                    }
                    Text(
                        text = "$subtitle • ${tracks.size} músicas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isLikedCollection) {
                            OutlinedButton(
                                onClick = { showAddTrackDialog = true }
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Add Link", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        IconButton(
                            onClick = { onPlayAll(true) },
                            enabled = tracks.isNotEmpty(),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("playlist_shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Tocar em ordem aleatória",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = { onPlayAll(false) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("playlist_play_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Tocar playlist",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLikedCollection) {
                            "Curta qualquer música no coração para vê-la aqui."
                        } else {
                            "Playlist vazia. Toque em \"+ Add Link\" para adicionar músicas do YouTube."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                TrackRowItem(
                    track = track,
                    index = index,
                    isPlayingNow = track.youtubeVideoId == currentPlayingVideoId,
                    isLiked = likedVideoIds.contains(track.youtubeVideoId),
                    onPlayClick = { onPlayTrack(track) },
                    onLikeClick = { onToggleLike(track.youtubeVideoId) },
                    onAddToPlaylistClick = { onAddTrackToAnotherPlaylist(track) },
                    onPlayNextClick = { onPlayNext(track) },
                    onOpenInYouTubeClick = { onOpenInYouTube(track.youtubeVideoId) },
                    onRemoveClick = if (onRemoveTrack != null) {
                        { onRemoveTrack(track.id) }
                    } else null,
                    isCachedOffline = cachedVideoIds.contains(track.youtubeVideoId)
                )
            }
        }
    }

    if (showAddTrackDialog) {
        ImportYouTubeDialog(
            isBusy = isBusy,
            title = "Adicionar do YouTube",
            hint = "https://youtu.be/... ou playlist?list=...",
            helperText = "Cole o link de uma música ou playlist do YouTube para adicionar a esta playlist.",
            onDismiss = { showAddTrackDialog = false },
            onConfirm = { url ->
                onAddTrackUrl(url)
                showAddTrackDialog = false
            }
        )
    }
}
