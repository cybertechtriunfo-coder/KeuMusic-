package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlaylistWithCount
import com.example.data.local.TrackEntity

@Composable
fun KeuMusicTopBar(
    title: String,
    isDarkTheme: Boolean,
    canNavigateBack: Boolean,
    onBackClick: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    isBackgroundCachingEnabled: Boolean = false,
    onOpenSettings: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_navigation_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (canNavigateBack) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("top_bar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Voltar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "KEUMUSIC",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_button")
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Configurações e cache offline",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            if (isBackgroundCachingEnabled) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }

                    val themeLabel = if (isDarkTheme) "Claro" else "Escuro"
                    val themeDescription = if (isDarkTheme) {
                        "Alternar para tema claro"
                    } else {
                        "Alternar para tema escuro"
                    }
                    Surface(
                        onClick = onToggleTheme,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .semantics { contentDescription = themeDescription }
                            .testTag("theme_switch_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = themeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

enum class LibraryCategoryChip(
    val label: String,
    val icon: ImageVector
) {
    ALL("Tudo", Icons.Rounded.LibraryMusic),
    RECENT("Recentes", Icons.Rounded.History),
    GENRES("Gêneros", Icons.Rounded.Category),
    ALBUMS("Álbuns", Icons.Rounded.Album),
    LIKED("Curtidas", Icons.Rounded.Favorite)
}

data class GenreBucket(
    val id: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val playlists: List<PlaylistWithCount>,
    val tracks: List<TrackEntity>
)

@Composable
fun LibraryCategoryChipsRow(
    selectedChip: LibraryCategoryChip,
    onSelectChip: (LibraryCategoryChip) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("library_category_chips_row")
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(LibraryCategoryChip.entries, key = { it.name }) { chip ->
                val selected = chip == selectedChip
                FilterChip(
                    selected = selected,
                    onClick = { onSelectChip(chip) },
                    label = {
                        Text(
                            text = chip.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = chip.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(50),
                    border = if (selected) {
                        null
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("category_chip_${chip.name.lowercase()}")
                )
            }
        }
    }
}

fun buildGenreBuckets(
    playlists: List<PlaylistWithCount>,
    allTracks: List<TrackEntity>
): List<GenreBucket> {
    fun matches(pl: PlaylistWithCount, vararg keywords: String): Boolean {
        val text = "${pl.title} ${pl.subtitle}".lowercase()
        return keywords.any { text.contains(it) }
    }

    val assignedIds = mutableSetOf<String>()

    fun takeMatching(vararg keywords: String): List<PlaylistWithCount> {
        return playlists.filter { pl ->
            pl.id !in assignedIds && matches(pl, *keywords)
        }.also { matched ->
            matched.forEach { assignedIds.add(it.id) }
        }
    }

    val popBrasil = takeMatching("pop", "brasil", "sertanejo", "mpb", "hits")
    val electronic = takeMatching("eletrônica", "eletronica", "festa", "dance", "edm")
    val rock = takeMatching("rock", "alternativo", "clássicos", "classicos", "indie")
    val lofi = takeMatching("lo-fi", "lofi", "foco", "chill", "study", "relax")
    val remaining = playlists.filter { it.id !in assignedIds }

    val trackByPlaylist = allTracks.groupBy { it.playlistId }

    val specs = listOf(
        Triple("pop_brasil", "Pop & Hits Brasil" to "Sucessos nacionais e internacionais", Color(0xFF158A3E) to popBrasil),
        Triple("eletronica", "Eletrônica & Festa" to "Batidas, dance e festivais", Color(0xFF0D63C9) to electronic),
        Triple("rock", "Rock & Alternativo" to "Clássicos e energia de guitarra", Color(0xFF8C1932) to rock),
        Triple("lofi", "Lo-fi & Foco" to "Beats suaves para estudar ou relaxar", Color(0xFF3E6188) to lofi),
        Triple("outros", "Coleções & Mixes do YouTube" to "Playlists importadas e personalizadas", Color(0xFF6246E5) to remaining)
    )

    return specs.mapNotNull { (id, titles, colorAndPlaylists) ->
        val (title, subtitle) = titles
        val (accent, groupPlaylists) = colorAndPlaylists
        if (groupPlaylists.isEmpty()) return@mapNotNull null
        val groupTracks = groupPlaylists
            .flatMap { pl -> trackByPlaylist[pl.id].orEmpty() }
            .distinctBy { it.youtubeVideoId }
            .ifEmpty {
                allTracks.filter { track ->
                    groupPlaylists.any { it.id == track.playlistId }
                }
            }
        GenreBucket(
            id = id,
            title = title,
            subtitle = subtitle,
            accentColor = accent,
            playlists = groupPlaylists,
            tracks = groupTracks
        )
    }
}

@Composable
fun GenreSectionCard(
    bucket: GenreBucket,
    onOpenPlaylist: (String) -> Unit,
    onPlayGenre: (GenreBucket) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("genre_card_${bucket.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                bucket.accentColor,
                                bucket.accentColor.copy(alpha = 0.78f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bucket.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${bucket.playlists.size} playlists • ${bucket.subtitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        onClick = { onPlayGenre(bucket) },
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_genre_${bucket.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Tocar gênero ${bucket.title}",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                bucket.playlists.forEach { pl ->
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
                            modifier = Modifier.size(46.dp),
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
        }
    }
}

@Composable
fun AlbumGridPairRow(
    left: PlaylistWithCount,
    right: PlaylistWithCount?,
    onOpenPlaylist: (String) -> Unit,
    onPlayPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AlbumGridCard(
            playlist = left,
            onOpen = { onOpenPlaylist(left.id) },
            onPlay = { onPlayPlaylist(left.id) },
            modifier = Modifier.weight(1f)
        )
        if (right != null) {
            AlbumGridCard(
                playlist = right,
                onOpen = { onOpenPlaylist(right.id) },
                onPlay = { onPlayPlaylist(right.id) },
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AlbumGridCard(
    playlist: PlaylistWithCount,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .testTag("album_card_${playlist.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                CoverArtwork(
                    imageUrl = playlist.thumbnailUrl,
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 8.dp
                )
                Surface(
                    onClick = onPlay,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Tocar ${playlist.title}",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.trackCount} músicas • ${playlist.subtitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyCategoryMessageCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
