package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
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
import java.util.Calendar

@Composable
fun HomeScreen(
    selectedCategory: LibraryCategoryChip,
    playlists: List<PlaylistWithCount>,
    likedTracks: List<TrackEntity>,
    likedCount: Int,
    recentlyPlayed: List<TrackEntity>,
    featuredTracks: List<TrackEntity>,
    currentPlayingVideoId: String?,
    likedVideoIds: Set<String>,
    onSelectCategory: (LibraryCategoryChip) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onQuickStart: () -> Unit,
    onOpenLikedSongs: () -> Unit,
    onOpenImportDialog: () -> Unit,
    onOpenCreateDialog: () -> Unit,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    onToggleLike: (String) -> Unit,
    onAddTrackToPlaylist: (TrackEntity) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onOpenInYouTube: (String) -> Unit,
    modifier: Modifier = Modifier,
    cachedVideoIds: Set<String> = emptySet()
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour in 5..11 -> "Bom dia"
            hour in 12..17 -> "Boa tarde"
            else -> "Boa noite"
        }
    }

    val genreBuckets = remember(playlists, featuredTracks) {
        buildGenreBuckets(playlists, featuredTracks)
    }

    val recentPlaylists = remember(playlists) {
        playlists.sortedByDescending { it.updatedAt }
    }

    val albumPairs = remember(playlists) {
        playlists.chunked(2)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    ),
                    endY = 700f
                )
            ),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = when (selectedCategory) {
                            LibraryCategoryChip.ALL -> "Suas playlists do YouTube no estilo Spotify"
                            LibraryCategoryChip.RECENT -> "Ouvidas recentemente e playlists atualizadas"
                            LibraryCategoryChip.GENRES -> "Biblioteca organizada por gêneros musicais"
                            LibraryCategoryChip.ALBUMS -> "Coleções, álbuns e playlists em destaque"
                            LibraryCategoryChip.LIKED -> "$likedCount músicas favoritas salvas"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenImportDialog,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .testTag("home_import_icon_button")
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .testTag("home_create_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Nova playlist",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        when (selectedCategory) {
            LibraryCategoryChip.ALL -> {
                // Spotify-style 2-column quick access grid
                item {
                    val quickPlaylists = playlists.take(5)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickItems: List<@Composable (Modifier) -> Unit> = buildList {
                            add { m ->
                                QuickGridTile(
                                    title = "Músicas Curtidas",
                                    thumbnailUrl = "",
                                    isLiked = true,
                                    onClick = onOpenLikedSongs,
                                    modifier = m
                                )
                            }
                            quickPlaylists.forEach { pl ->
                                add { m ->
                                    QuickGridTile(
                                        title = pl.title,
                                        thumbnailUrl = pl.thumbnailUrl,
                                        isLiked = false,
                                        onClick = { onOpenPlaylist(pl.id) },
                                        modifier = m
                                    )
                                }
                            }
                        }

                        quickItems.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair[0](Modifier.weight(1f))
                                if (pair.size > 1) {
                                    pair[1](Modifier.weight(1f))
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Featured YouTube Mix Banner
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onQuickStart)
                            .testTag("home_featured_mix_banner")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_discover_wave_1794980084078),
                            contentDescription = "Mix KEUMUSIC pronto para tocar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(148.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.88f),
                                            Color.Black.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            YouTubeBadge()
                            Text(
                                text = "Playlists e músicas prontas",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Aperte play e ouça seus mixes favoritos do YouTube agora mesmo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6DADF)
                            )
                            Button(
                                onClick = onQuickStart,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Tocar Agora",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Horizontal Playlists Carousel
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Suas Playlists do YouTube",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            Column(
                                modifier = Modifier
                                    .width(152.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenPlaylist(pl.id) }
                            ) {
                                Box {
                                    CoverArtwork(
                                        imageUrl = pl.thumbnailUrl,
                                        contentDescription = pl.title,
                                        modifier = Modifier.size(152.dp),
                                        cornerRadius = 10.dp
                                    )
                                    Surface(
                                        onClick = { onPlayPlaylist(pl.id) },
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = "Tocar playlist ${pl.title}",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pl.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${pl.trackCount} músicas • ${pl.subtitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Recently Played Section
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(26.dp))
                        Text(
                            text = "Tocadas Recentemente",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(recentlyPlayed.take(5), key = { "recent_${it.id}" }) { track ->
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

                // Built-in Songs Catalog Section
                if (featuredTracks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(26.dp))
                        Text(
                            text = "Músicas para Ouvir Agora",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(featuredTracks, key = { "featured_${it.id}" }) { track ->
                        TrackRowItem(
                            track = track,
                            index = null,
                            isPlayingNow = track.youtubeVideoId == currentPlayingVideoId,
                            isLiked = likedVideoIds.contains(track.youtubeVideoId),
                            onPlayClick = { onPlayTrack(track, featuredTracks) },
                            onLikeClick = { onToggleLike(track.youtubeVideoId) },
                            onAddToPlaylistClick = { onAddTrackToPlaylist(track) },
                            onPlayNextClick = { onPlayNext(track) },
                            onOpenInYouTubeClick = { onOpenInYouTube(track.youtubeVideoId) },
                            isCachedOffline = cachedVideoIds.contains(track.youtubeVideoId)
                        )
                    }
                }
            }

            LibraryCategoryChip.RECENT -> {
                item {
                    Text(
                        text = "Tocadas Recentemente",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                if (recentlyPlayed.isEmpty()) {
                    item {
                        EmptyCategoryMessageCard(
                            title = "Nenhuma música tocada ainda",
                            subtitle = "Dê play em qualquer faixa ou playlist e ela aparecerá aqui automaticamente.",
                            actionLabel = "Tocar Agora",
                            onActionClick = onQuickStart
                        )
                    }
                } else {
                    items(recentlyPlayed, key = { "cat_recent_${it.id}" }) { track ->
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
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Playlists Recentes",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(recentPlaylists.chunked(2), key = { pair -> "rec_pl_${pair.first().id}" }) { pair ->
                        AlbumGridPairRow(
                            left = pair[0],
                            right = pair.getOrNull(1),
                            onOpenPlaylist = onOpenPlaylist,
                            onPlayPlaylist = onPlayPlaylist
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            LibraryCategoryChip.GENRES -> {
                item {
                    Text(
                        text = "Explorar por Gêneros",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(genreBuckets, key = { "home_genre_${it.id}" }) { bucket ->
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
                item {
                    Text(
                        text = "Álbuns & Playlists (${playlists.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(albumPairs, key = { pair -> "home_album_${pair.first().id}" }) { pair ->
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
                            style = MaterialTheme.typography.headlineSmall,
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
                            title = "Nenhuma música curtida ainda",
                            subtitle = "Toque no coração em qualquer música para salvar suas favoritas aqui.",
                            actionLabel = "Ver Todas as Músicas",
                            onActionClick = { onSelectCategory(LibraryCategoryChip.ALL) }
                        )
                    }
                } else {
                    items(likedTracks, key = { "home_liked_${it.id}" }) { track ->
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
            Text(
                text = "KEUMUSIC • Desenvolvido por Cleudeson Soares",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun QuickGridTile(
    title: String,
    thumbnailUrl: String,
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            CoverArtwork(
                imageUrl = thumbnailUrl,
                contentDescription = title,
                modifier = Modifier.size(58.dp),
                cornerRadius = 0.dp,
                isLikedCollection = isLiked
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}
