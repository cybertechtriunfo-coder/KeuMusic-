package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.PlaylistWithCount
import com.example.data.local.TrackEntity
import com.example.player.PlaybackUiState
import com.example.ui.theme.LikedGradientEnd
import com.example.ui.theme.LikedGradientStart
import com.example.ui.theme.YouTubeCrimson
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun CoverArtwork(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    isLikedCollection: Boolean = false
) {
    if (isLikedCollection) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.linearGradient(listOf(LikedGradientStart, LikedGradientEnd))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.fillMaxSize(0.45f)
            )
        }
    } else if (imageUrl.isNotBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize(0.42f)
            )
        }
    }
}

@Composable
fun YouTubeBadge(modifier: Modifier = Modifier) {
    Surface(
        color = YouTubeCrimson.copy(alpha = 0.16f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayCircleFilled,
                contentDescription = null,
                tint = YouTubeCrimson,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "YouTube",
                style = MaterialTheme.typography.labelSmall,
                color = YouTubeCrimson
            )
        }
    }
}

@Composable
fun PlayingEqualizerBars(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bar1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "b3"
    )
    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp * fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun MiniPlayerWaveBackdrop(modifier: Modifier = Modifier) {
    val waveColor = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "mini_player_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val step = 12f
        val baseY1 = height * 0.72f
        val amp1 = height * 0.10f
        val path1 = Path().apply {
            moveTo(0f, height)
            var x = 0f
            while (x <= width + step) {
                val clampedX = x.coerceAtMost(width)
                val angle = (clampedX / width) * (3.2f * PI).toFloat() + phase
                val y = baseY1 + sin(angle) * amp1
                lineTo(clampedX, y)
                x += step
            }
            lineTo(width, height)
            close()
        }
        drawPath(path = path1, color = waveColor.copy(alpha = 0.09f))

        val baseY2 = height * 0.80f
        val amp2 = height * 0.07f
        val path2 = Path().apply {
            moveTo(0f, height)
            var x = 0f
            while (x <= width + step) {
                val clampedX = x.coerceAtMost(width)
                val angle = (clampedX / width) * (4.4f * PI).toFloat() - phase * 1.25f
                val y = baseY2 + sin(angle) * amp2
                lineTo(clampedX, y)
                x += step
            }
            lineTo(width, height)
            close()
        }
        drawPath(path = path2, color = waveColor.copy(alpha = 0.06f))
    }
}

@Composable
private fun MiniPlayerWaveBars(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mini_wave_bars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_bars_phase"
    )

    Row(
        modifier = modifier
            .height(14.dp)
            .semantics { contentDescription = "Música em reprodução" },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 4) {
            val wave = (sin(phase + i * 1.15f) + 1f) * 0.5f
            val fraction = 0.28f + wave * 0.72f
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(14.dp * fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun TrackRowItem(
    track: TrackEntity,
    index: Int?,
    isPlayingNow: Boolean,
    isLiked: Boolean,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onPlayNextClick: () -> Unit,
    onOpenInYouTubeClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isCachedOffline: Boolean = false
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index != null) {
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isPlayingNow) {
                    PlayingEqualizerBars()
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                }
            }
        }

        CoverArtwork(
            imageUrl = track.thumbnailUrl,
            contentDescription = "Capa de ${track.title}",
            modifier = Modifier.size(52.dp),
            cornerRadius = 6.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isPlayingNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isCachedOffline) {
                    Icon(
                        imageVector = Icons.Rounded.OfflinePin,
                        contentDescription = "Disponível offline",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (track.durationText.isNotBlank()) {
                    Text(
                        text = "• ${track.durationText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }

        IconButton(
            onClick = onLikeClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("like_track_button_${track.youtubeVideoId}")
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isLiked) "Descurtir música" else "Curtir música",
                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Mais opções",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                DropdownMenuItem(
                    text = { Text("Tocar a seguir") },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                    onClick = {
                        menuExpanded = false
                        onPlayNextClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Adicionar à playlist") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                    onClick = {
                        menuExpanded = false
                        onAddToPlaylistClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Abrir no YouTube") },
                    leadingIcon = { Icon(Icons.Rounded.Link, null) },
                    onClick = {
                        menuExpanded = false
                        onOpenInYouTubeClick()
                    }
                )
                if (onRemoveClick != null) {
                    DropdownMenuItem(
                        text = { Text("Remover desta playlist", color = YouTubeCrimson) },
                        leadingIcon = { Icon(Icons.Rounded.Close, null, tint = YouTubeCrimson) },
                        onClick = {
                            menuExpanded = false
                            onRemoveClick()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayerBar(
    playbackState: PlaybackUiState,
    isLiked: Boolean,
    onExpandClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onLikeClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onVideoSlotPositioned: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack
    val isActivelyPlaying = track != null && playbackState.isPlaying
    var isSeeking by remember(track?.youtubeVideoId) { mutableStateOf(false) }
    var sliderDragValue by remember(track?.youtubeVideoId) { mutableFloatStateOf(0f) }

    val totalDuration = remember(track?.youtubeVideoId, playbackState.durationSec, track?.durationSeconds) {
        when {
            playbackState.durationSec > 0f -> playbackState.durationSec
            track != null && track.durationSeconds > 0 -> track.durationSeconds.toFloat()
            else -> 0f
        }
    }
    val hasSeekableDuration = track != null && totalDuration > 0f
    val safeMax = if (hasSeekableDuration) totalDuration else 1f
    val displayPos = when {
        !hasSeekableDuration -> 0f
        isSeeking -> sliderDragValue.coerceIn(0f, safeMax)
        else -> playbackState.currentPositionSec.coerceIn(0f, safeMax)
    }
    val elapsedText = if (track != null) formatDuration(displayPos) else "0:00"
    val totalText = when {
        track == null -> "--:--"
        hasSeekableDuration -> formatDuration(totalDuration)
        else -> track.durationText.ifBlank { "--:--" }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                if (track != null) onExpandClick() else onPlayPauseClick()
            }
            .testTag("mini_player_bar")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isActivelyPlaying) {
                MiniPlayerWaveBackdrop(
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("mini_player_wave_animation")
                )
            }
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slot for the persistent WebView preview (or idle artwork when no track selected yet)
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (track != null) Color.Black
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .then(
                                if (track != null) Modifier.onGloballyPositioned(onVideoSlotPositioned)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track != null) {
                            CoverArtwork(
                                imageUrl = track.thumbnailUrl,
                                contentDescription = track.title,
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = 6.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = "KEUMUSIC Mini-Player",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnimatedVisibility(
                                visible = isActivelyPlaying,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                MiniPlayerWaveBars(
                                    modifier = Modifier.testTag("mini_player_wave_indicator")
                                )
                            }
                            Text(
                                text = track?.title ?: "KEUMUSIC",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (playbackState.isPlayingOfflineCache) {
                                Icon(
                                    imageVector = Icons.Rounded.OfflinePin,
                                    contentDescription = "Reproduzindo do cache offline",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = track?.artist ?: "Toque em Play para ouvir suas playlists",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                if (track != null) {
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mini_player_like")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isLiked) "Descurtir" else "Curtir",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onPreviousClick,
                    enabled = track != null,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_prev")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Faixa anterior",
                        tint = if (track != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_play_pause")
                ) {
                    if (playbackState.isBuffering) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pausar" else "Tocar",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Próxima faixa",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = elapsedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSeeking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("mini_player_elapsed_time")
                )

                Slider(
                    value = displayPos,
                    onValueChange = { newPos ->
                        isSeeking = true
                        sliderDragValue = newPos
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        onSeek(sliderDragValue)
                    },
                    enabled = hasSeekableDuration,
                    valueRange = 0f..safeMax,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(if (isSeeking) 14.dp else 10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !hasSeekableDuration -> Color.Transparent
                                        isSeeking -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                        )
                    },
                    track = {
                        val fraction = if (hasSeekableDuration) {
                            (displayPos / safeMax).coerceIn(0f, 1f)
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isSeeking) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_progress_slider")
                )

                Text(
                    text = totalText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("mini_player_total_time")
                )
            }
            }
        }
    }
}

@Composable
fun ImportYouTubeDialog(
    isBusy: Boolean,
    title: String = "Importar do YouTube",
    hint: String = "https://www.youtube.com/playlist?list=...",
    helperText: String = "Cole o link de qualquer playlist pública ou vídeo do YouTube para importar todas as músicas.",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Link ou ID do YouTube") },
                    placeholder = { Text(hint, maxLines = 1) },
                    singleLine = true,
                    enabled = !isBusy,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_url_input")
                )
                if (isBusy) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Buscando faixas no YouTube...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(input) },
                enabled = input.isNotBlank() && !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("confirm_import_button")
            ) {
                Text("Importar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, subtitle: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Nova Playlist", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da playlist") },
                    placeholder = { Text("Ex: Treino Pesado, Viagem...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_playlist_title_input")
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Descrição (opcional)") },
                    placeholder = { Text("Minhas favoritas do YouTube") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, subtitle) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("confirm_create_playlist_button")
            ) {
                Text("Criar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistWithCount>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (PlaylistWithCount) -> Unit,
    onCreateNewClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Adicionar à playlist", style = MaterialTheme.typography.headlineSmall) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCreateNewClick)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Criar nova playlist",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                items(playlists, key = { it.id }) { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectPlaylist(pl) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoverArtwork(
                            imageUrl = pl.thumbnailUrl,
                            contentDescription = pl.title,
                            modifier = Modifier.size(44.dp),
                            cornerRadius = 6.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pl.trackCount} músicas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

fun formatDuration(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val mins = total / 60
    val secs = total % 60
    return "%d:%02d".format(mins, secs)
}

@Composable
fun TopLibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Buscar músicas ou artistas na biblioteca...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Buscar na biblioteca",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onClear()
                            keyboard?.hide()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("top_library_search_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Limpar busca",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("top_library_search_input")
        )
    }
}

@Composable
fun TopLibrarySearchResultsPanel(
    query: String,
    playlistResults: List<PlaylistWithCount>,
    trackResults: List<TrackEntity>,
    currentPlayingVideoId: String?,
    likedVideoIds: Set<String>,
    onOpenPlaylist: (String) -> Unit,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    onToggleLike: (String) -> Unit,
    onAddToPlaylist: (TrackEntity) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onOpenInYouTube: (String) -> Unit,
    onSearchOnYouTubeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("top_library_search_results"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        if (playlistResults.isNotEmpty()) {
            item {
                Text(
                    text = "Playlists (${playlistResults.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            items(playlistResults, key = { "top_pl_${it.id}" }) { pl ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPlaylist(pl.id) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverArtwork(
                        imageUrl = pl.thumbnailUrl,
                        contentDescription = pl.title,
                        modifier = Modifier.size(52.dp),
                        cornerRadius = 8.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pl.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
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

        if (trackResults.isNotEmpty()) {
            item {
                Text(
                    text = "Músicas e Artistas (${trackResults.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            items(trackResults, key = { "top_tr_${it.id}" }) { track ->
                TrackRowItem(
                    track = track,
                    index = null,
                    isPlayingNow = track.youtubeVideoId == currentPlayingVideoId,
                    isLiked = likedVideoIds.contains(track.youtubeVideoId),
                    onPlayClick = { onPlayTrack(track, trackResults) },
                    onLikeClick = { onToggleLike(track.youtubeVideoId) },
                    onAddToPlaylistClick = { onAddToPlaylist(track) },
                    onPlayNextClick = { onPlayNext(track) },
                    onOpenInYouTubeClick = { onOpenInYouTube(track.youtubeVideoId) }
                )
            }
        }

        if (playlistResults.isEmpty() && trackResults.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Nenhuma música ou artista encontrado na biblioteca para \"${query.trim()}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { onSearchOnYouTubeClick(query.trim()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buscar no YouTube", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

