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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TrackEntity
import com.example.data.youtube.YouTubePlaylistSearchResult
import com.example.data.youtube.YouTubeTrackResult
import com.example.ui.YouTubeSearchUiState
import com.example.ui.components.CoverArtwork
import com.example.ui.components.TrackRowItem
import com.example.ui.components.YouTubeBadge

private data class MoodCategory(
    val title: String,
    val query: String,
    val colorStart: Color,
    val colorEnd: Color
)

private val discoverMoods = listOf(
    MoodCategory("Lo-fi & Foco", "lofi hip hop beats playlist", Color(0xFF1E3264), Color(0xFF0D73EC)),
    MoodCategory("Top Brasil", "top hits brasil playlist", Color(0xFF148A08), Color(0xFF1ED760)),
    MoodCategory("MPB & Bossa", "mpb clássicos playlist", Color(0xFF8D67AB), Color(0xFF503750)),
    MoodCategory("Sertanejo", "sertanejo universitário playlist", Color(0xFFE13300), Color(0xFFBA5D07)),
    MoodCategory("Treino & Phonk", "gym workout phonk playlist", Color(0xFF8C1932), Color(0xFFE8115B)),
    MoodCategory("Rock Clássico", "classic rock hits playlist", Color(0xFF27856A), Color(0xFF1E3264))
)

@Composable
fun SearchScreen(
    query: String,
    localResults: List<TrackEntity>,
    youtubeSearchState: YouTubeSearchUiState,
    currentPlayingVideoId: String?,
    likedVideoIds: Set<String>,
    onQueryChange: (String) -> Unit,
    onTriggerYouTubeSearch: (String, Boolean) -> Unit,
    onImportYouTubePlaylist: (String) -> Unit,
    onPlayLocalTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    onPlayYouTubeTrack: (YouTubeTrackResult, List<YouTubeTrackResult>) -> Unit,
    onAddYouTubeTrackToPlaylist: (YouTubeTrackResult) -> Unit,
    onAddLocalTrackToPlaylist: (TrackEntity) -> Unit,
    onToggleLike: (String) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onOpenInYouTube: (String) -> Unit,
    modifier: Modifier = Modifier,
    cachedVideoIds: Set<String> = emptySet()
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var searchInYouTube by remember { mutableStateOf(true) }

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
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Buscar",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Músicas, artistas ou playlists do YouTube") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Limpar busca",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            if (searchInYouTube) onTriggerYouTubeSearch(query, false)
                        }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val searchChipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                    FilterChip(
                        selected = searchInYouTube,
                        onClick = {
                            searchInYouTube = true
                            if (query.isNotBlank()) onTriggerYouTubeSearch(query, false)
                        },
                        label = { Text("No YouTube") },
                        colors = searchChipColors
                    )
                    FilterChip(
                        selected = !searchInYouTube,
                        onClick = { searchInYouTube = false },
                        label = { Text("Na Biblioteca") },
                        colors = searchChipColors
                    )
                    Spacer(Modifier.weight(1f))
                    if (searchInYouTube && query.isNotBlank()) {
                        Button(
                            onClick = {
                                keyboard?.hide()
                                onTriggerYouTubeSearch(query, false)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("search_youtube_submit_button")
                        ) {
                            Text("Buscar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        if (!searchInYouTube) {
            if (query.isBlank()) {
                item {
                    EmptySearchHint("Digite para filtrar faixas salvas nas suas playlists locais.")
                }
            } else if (localResults.isEmpty()) {
                item {
                    EmptySearchHint("Nenhuma música encontrada na biblioteca para \"$query\".")
                }
            } else {
                items(localResults, key = { "local_${it.id}" }) { track ->
                    TrackRowItem(
                        track = track,
                        index = null,
                        isPlayingNow = track.youtubeVideoId == currentPlayingVideoId,
                        isLiked = likedVideoIds.contains(track.youtubeVideoId),
                        onPlayClick = { onPlayLocalTrack(track, localResults) },
                        onLikeClick = { onToggleLike(track.youtubeVideoId) },
                        onAddToPlaylistClick = { onAddLocalTrackToPlaylist(track) },
                        onPlayNextClick = { onPlayNext(track) },
                        onOpenInYouTubeClick = { onOpenInYouTube(track.youtubeVideoId) },
                        isCachedOffline = cachedVideoIds.contains(track.youtubeVideoId)
                    )
                }
            }
        } else {
            when (youtubeSearchState) {
                YouTubeSearchUiState.Idle -> {
                    item {
                        Text(
                            text = "Explorar Playlists do YouTube",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            discoverMoods.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { mood ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(92.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(mood.colorStart, mood.colorEnd)
                                                    )
                                                )
                                                .clickable {
                                                    onTriggerYouTubeSearch(mood.query, true)
                                                }
                                                .padding(14.dp)
                                        ) {
                                            Text(
                                                text = mood.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.align(Alignment.TopStart)
                                            )
                                            YouTubeBadge(
                                                modifier = Modifier.align(Alignment.BottomEnd)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                YouTubeSearchUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Buscando no YouTube...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is YouTubeSearchUiState.Error -> {
                    item {
                        EmptySearchHint(youtubeSearchState.message)
                    }
                }

                is YouTubeSearchUiState.Success -> {
                    val playlists = youtubeSearchState.data.playlists
                    val tracks = youtubeSearchState.data.tracks

                    if (playlists.isEmpty() && tracks.isEmpty()) {
                        item {
                            EmptySearchHint("Nenhum resultado encontrado no YouTube para \"$query\".")
                        }
                    }

                    if (playlists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Playlists do YouTube encontradas",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(playlists, key = { it.playlistId }) { ytPl ->
                                    YouTubeSearchPlaylistCard(
                                        playlist = ytPl,
                                        onImportClick = { onImportYouTubePlaylist(ytPl.playlistId) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Músicas e Vídeos",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(tracks, key = { "yt_track_${it.videoId}" }) { ytTrack ->
                            val isNow = ytTrack.videoId == currentPlayingVideoId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlayYouTubeTrack(ytTrack, tracks) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverArtwork(
                                    imageUrl = ytTrack.thumbnailUrl,
                                    contentDescription = ytTrack.title,
                                    modifier = Modifier.size(54.dp),
                                    cornerRadius = 6.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ytTrack.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            append(ytTrack.artist)
                                            if (ytTrack.durationText.isNotBlank()) {
                                                append(" • ${ytTrack.durationText}")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onAddYouTubeTrackToPlaylist(ytTrack) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                        contentDescription = "Salvar em playlist",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onPlayYouTubeTrack(ytTrack, tracks) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Tocar música",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeSearchPlaylistCard(
    playlist: YouTubePlaylistSearchResult,
    onImportClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(172.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            CoverArtwork(
                imageUrl = playlist.thumbnailUrl,
                contentDescription = playlist.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp),
                cornerRadius = 8.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playlist.videoCountText.ifBlank { playlist.author },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onImportClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Salvar Playlist", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptySearchHint(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
