package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TrackEntity
import com.example.player.PlaybackUiState
import com.example.player.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingOverlay(
    playbackState: PlaybackUiState,
    isLiked: Boolean,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleVideoStage: (Boolean) -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: (TrackEntity) -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onOpenInYouTube: (String) -> Unit,
    onVideoStagePositioned: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    var showQueueSheet by remember { mutableStateOf(false) }

    BackHandler(onBack = onCollapse)

    var isSeeking by remember { mutableStateOf(false) }
    var sliderDragValue by remember { mutableFloatStateOf(0f) }

    val duration = playbackState.durationSec.coerceAtLeast(1f)
    val displayPos = if (isSeeking) sliderDragValue else playbackState.currentPositionSec.coerceIn(0f, duration)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("collapse_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimizar player",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (playbackState.isPlayingOfflineCache) {
                            "TOCANDO DO CACHE OFFLINE"
                        } else {
                            "TOCANDO DO YOUTUBE"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (playbackState.isPlayingOfflineCache) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_queue_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Fila de reprodução",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Video / Cover toggle
            val segmentedColors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                activeContentColor = MaterialTheme.colorScheme.onSurface,
                inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(0.72f)
            ) {
                SegmentedButton(
                    selected = playbackState.showVideoStage,
                    onClick = { if (!playbackState.showVideoStage) onToggleVideoStage(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = segmentedColors,
                    icon = {
                        Icon(Icons.Rounded.OndemandVideo, null, modifier = Modifier.size(16.dp))
                    }
                ) {
                    Text("Vídeo", style = MaterialTheme.typography.labelMedium)
                }
                SegmentedButton(
                    selected = !playbackState.showVideoStage,
                    onClick = { if (playbackState.showVideoStage) onToggleVideoStage(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = segmentedColors,
                    icon = {
                        Icon(Icons.Rounded.Album, null, modifier = Modifier.size(16.dp))
                    }
                ) {
                    Text("Capa", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stage: either 16:9 YouTube IFrame WebView slot or 1:1 Cover Artwork
            if (playbackState.showVideoStage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .onGloballyPositioned(onVideoStagePositioned),
                    contentAlignment = Alignment.Center
                ) {
                    CoverArtwork(
                        imageUrl = track.thumbnailUrl,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 14.dp
                    )
                }
            } else {
                CoverArtwork(
                    imageUrl = track.thumbnailUrl,
                    contentDescription = "Capa de ${track.title}",
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1f),
                    cornerRadius = 16.dp
                )
            }

            if (playbackState.playerError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = playbackState.playerError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onOpenInYouTube(track.youtubeVideoId) }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.OpenInNew,
                                    null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Abrir no YouTube", color = MaterialTheme.colorScheme.secondary)
                            }
                            TextButton(onClick = onNext) {
                                Text("Próxima faixa", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track title + artist + like & add to playlist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onAddToPlaylist(track) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Adicionar à playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("now_playing_like_button")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isLiked) "Descurtir" else "Curtir",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Seek bar
            Slider(
                value = displayPos,
                onValueChange = {
                    isSeeking = true
                    sliderDragValue = it
                },
                onValueChangeFinished = {
                    isSeeking = false
                    onSeek(sliderDragValue)
                },
                valueRange = 0f..duration,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(displayPos),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (playbackState.durationSec > 0f) formatDuration(playbackState.durationSec)
                    else track.durationText.ifBlank { "--:--" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Ordem aleatória",
                        tint = if (playbackState.isShuffleEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("previous_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Faixa anterior",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Surface(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("now_playing_play_pause")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (playbackState.isBuffering) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.background,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pausar" else "Tocar",
                                tint = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("next_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Próxima faixa",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("repeat_button")
                ) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatMode.ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = "Repetir",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { onOpenInYouTube(track.youtubeVideoId) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Assistir no app YouTube",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Fila de reprodução (${playbackState.queue.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    itemsIndexed(playbackState.queue) { idx, qTrack ->
                        val active = idx == playbackState.currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.surfaceContainerHigh
                                    else Color.Transparent
                                )
                                .clickable {
                                    onSelectQueueIndex(idx)
                                    showQueueSheet = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoverArtwork(
                                imageUrl = qTrack.thumbnailUrl,
                                contentDescription = qTrack.title,
                                modifier = Modifier.size(44.dp),
                                cornerRadius = 6.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = qTrack.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = qTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (active) {
                                PlayingEqualizerBars()
                            } else {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
