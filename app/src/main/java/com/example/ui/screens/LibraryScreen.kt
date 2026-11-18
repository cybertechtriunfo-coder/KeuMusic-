package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cache.OfflineCacheStats
import com.example.data.local.PlaylistWithCount
import com.example.data.local.TrackEntity
import com.example.ui.components.AlbumGridPairRow
import com.example.ui.components.CoverArtwork
import com.example.ui.components.EmptyCategoryMessageCard
import com.example.ui.components.GenreSectionCard
import com.example.ui.components.LibraryCategoryChip
import com.example.ui.components.TrackRowItem
import com.example.ui.components.YouTubeBadge
import com.example.ui.components.buildGenreBuckets
import com.example.ui.components.formatStorageBytes

private enum class LibraryFilter {
    ALL,
    YOUTUBE,
    CUSTOM
}

@Composable
fun LibraryScreen(
    selectedCategory: LibraryCategoryChip,
    playlists: List<PlaylistWithCount>,
    recentlyPlayed: List<TrackEntity>,
    featuredTracks: List<TrackEntity>,
    likedTracks: List<TrackEntity>,
    likedCount: Int,
    currentPlayingVideoId: String?,
    likedVideoIds: Set<String>,
    onSelectCategory: (LibraryCategoryChip) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onOpenLikedSongs: () -> Unit,
    onOpenImportDialog: () -> Unit,
    onOpenCreateDialog: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    onToggleLike: (String) -> Unit,
    onAddTrackToPlaylist: (TrackEntity) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onOpenInYouTube: (String) -> Unit,
    modifier: Modifier = Modifier,
    cachedVideoIds: Set<String> = emptySet(),
    isBackgroundCachingEnabled: Boolean = false,
    offlineCacheStats: OfflineCacheStats = OfflineCacheStats(),
    onOpenOfflineSettings: () -> Unit = {}
) {
    var filter by remember { mutableStateOf(LibraryFilter.ALL) }

    val filteredPlaylists = remember(playlists, filter) {
        when (filter) {
            LibraryFilter.ALL -> playlists
            LibraryFilter.YOUTUBE -> playlists.filter { it.youtubePlaylistId != null }
            LibraryFilter.CUSTOM -> playlists.filter { it.youtubePlaylistId == null }
        }
    }

    val recentPlaylists = remember(playlists) {
        playlists.sortedByDescending { it.updatedAt }
    }

    val genreBuckets = remember(playlists, featuredTracks) {
        buildGenreBuckets(playlists, featuredTracks)
    }

    val albumPairs = remember(playlists) {
        playlists.chunked(2)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sua Biblioteca",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (selectedCategory != LibraryCategoryChip.ALL) {
                            Text(
                                text = "Categoria: ${selectedCategory.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Row {
                        IconButton(
                            onClick = onOpenImportDialog,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("library_import_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = "Importar playlist do YouTube",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onOpenCreateDialog,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("library_create_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Criar nova playlist",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                if (selectedCategory == LibraryCategoryChip.ALL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val chipColors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                        FilterChip(
                            selected = filter == LibraryFilter.ALL,
                            onClick = { filter = LibraryFilter.ALL },
                            label = { Text("Todas") },
                            colors = chipColors
                        )
                        FilterChip(
                            selected = filter == LibraryFilter.YOUTUBE,
                            onClick = { filter = LibraryFilter.YOUTUBE },
                            label = { Text("YouTube") },
                            colors = chipColors
                        )
                        FilterChip(
                            selected = filter == LibraryFilter.CUSTOM,
                            onClick = { filter = LibraryFilter.CUSTOM },
                            label = { Text("Personalizadas") },
                            colors = chipColors
                        )
                    }
                }
            }
        }

        when (selectedCategory) {
            LibraryCategoryChip.ALL -> {
                // Pinned Liked Songs row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenLikedSongs)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("library_liked_songs_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoverArtwork(
                            imageUrl = "",
                            contentDescription = "Músicas Curtidas",
                            modifier = Modifier.size(64.dp),
                            cornerRadius = 8.dp,
                            isLikedCollection = true
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Músicas Curtidas",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Playlist • $likedCount músicas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (filteredPlaylists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma playlist nesta categoria. Importe uma playlist do YouTube ou crie a sua!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredPlaylists, key = { it.id }) { pl ->
                        PlaylistLibraryRow(
                            playlist = pl,
                            onOpen = { onOpenPlaylist(pl.id) },
                            onDelete = { onDeletePlaylist(pl.id) }
                        )
                    }
                }
            }

            LibraryCategoryChip.RECENT -> {
                item {
                    Text(
                        text = "Ouvidas Recentemente",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                if (recentlyPlayed.isEmpty()) {
                    item {
                        EmptyCategoryMessageCard(
                            title = "Histórico vazio",
                            subtitle = "Suas faixas reproduzidas recentemente aparecerão aqui para acesso rápido.",
                            actionLabel = "Ver Tudo",
                            onActionClick = { onSelectCategory(LibraryCategoryChip.ALL) }
                        )
                    }
                } else {
                    items(recentlyPlayed, key = { "lib_recent_${it.id}" }) { track ->
                        TrackRowItem(
                            track = track,
                            index = null,
                            isPlayingNow = track.youtubeVideoId == currentPlayingVideoId,
                            isLiked = likedVideoIds.contains(track.youtubeVideoId),
                            onPlayClick = { onPlayTrack(track, recentlyPlayed) },
                            onLikeClick = { onToggleLike(track.youtubeVideoId) },
                            onAddToPlaylistClick = { onAddTrackToPlaylist(track) },
                            onPlayNextClick = { onPlayNext(track) },
                            onOpenInYouTubeClick = { onOpenInYouTube(track.youtubeVideoId) },
                            isCachedOffline = cachedVideoIds.contains(track.youtubeVideoId)
                        )
                    }
                }

                if (recentPlaylists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Playlists Recentes",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    items(recentPlaylists, key = { "lib_rec_pl_${it.id}" }) { pl ->
                        PlaylistLibraryRow(
                            playlist = pl,
                            onOpen = { onOpenPlaylist(pl.id) },
                            onDelete = { onDeletePlaylist(pl.id) }
                        )
                    }
                }
            }

            LibraryCategoryChip.GENRES -> {
                items(genreBuckets, key = { "lib_genre_${it.id}" }) { bucket ->
                    GenreSectionCard(
                        bucket = bucket,
                        onOpenPlaylist = onOpenPlaylist,
                        onPlayGenre = { selected ->
                            if (selected.tracks.isNotEmpty()) {
                                onPlayTrack(selected.tracks.first(), selected.tracks)
                            } else {
                                selected.playlists.firstOrNull()?.let { onPlayPlaylist(it.id) }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            LibraryCategoryChip.ALBUMS -> {
                items(albumPairs, key = { pair -> "lib_album_${pair.first().id}" }) { pair ->
                    AlbumGridPairRow(
                        left = pair[0],
                        right = pair.getOrNull(1),
                        onOpenPlaylist = onOpenPlaylist,
                        onPlayPlaylist = onPlayPlaylist
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            LibraryCategoryChip.LIKED -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Músicas Curtidas ($likedCount)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (likedTracks.isNotEmpty()) {
                            Button(
                                onClick = { onPlayTrack(likedTracks.first(), likedTracks) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tocar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (likedTracks.isEmpty()) {
                    item {
                        EmptyCategoryMessageCard(
                            title = "Sem músicas curtidas",
                            subtitle = "Curta suas músicas favoritas para encontrá-las instantaneamente aqui.",
                            actionLabel = "Explorar Biblioteca",
                            onActionClick = { onSelectCategory(LibraryCategoryChip.ALL) }
                        )
                    }
                } else {
                    items(likedTracks, key = { "lib_liked_${it.id}" }) { track ->
                        TrackRowItem(
                            track = track,
                            index = null,
                            isPlayingNow = track.youtubeVideoId == currentPlayingVideoId,
                            isLiked = true,
                            onPlayClick = { onPlayTrack(track, likedTracks) },
                            onLikeClick = { onToggleLike(track.youtubeVideoId) },
                            onAddToPlaylistClick = { onAddTrackToPlaylist(track) },
                            onPlayNextClick = { onPlayNext(track) },
                            onOpenInYouTubeClick = { onOpenInYouTube(track.youtubeVideoId) },
                            isCachedOffline = cachedVideoIds.contains(track.youtubeVideoId)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                onClick = onOpenOfflineSettings,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("library_offline_cache_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OfflinePin,
                            contentDescription = "Cache offline",
                            tint = if (isBackgroundCachingEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBackgroundCachingEnabled) {
                                "Cache Offline Ativo"
                            } else {
                                "Cache Offline em Segundo Plano"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${offlineCacheStats.cachedCount}/${offlineCacheStats.maxTracks} músicas • ${formatStorageBytes(offlineCacheStats.totalBytes)} locais",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Configurar cache offline",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("developer_credit_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "Desenvolvedor",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "KEUMUSIC",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Desenvolvido por Cleudeson Soares",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistLibraryRow(
    playlist: PlaylistWithCount,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArtwork(
            imageUrl = playlist.thumbnailUrl,
            contentDescription = playlist.title,
            modifier = Modifier.size(64.dp),
            cornerRadius = 8.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (playlist.youtubePlaylistId != null) {
                    YouTubeBadge()
                }
                Text(
                    text = "${playlist.trackCount} músicas • ${playlist.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!playlist.isPinned) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Excluir playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
