
package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.TrackEntity
import com.example.data.youtube.YouTubeTrackResult
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AuthorizedAudioDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.ImportYouTubeDialog
import com.example.ui.components.KeuMusicTopBar
import com.example.ui.components.LibraryCategoryChipsRow
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingOverlay
import com.example.ui.components.OfflineSettingsSheet
import com.example.ui.components.TopLibrarySearchBar
import com.example.ui.components.TopLibrarySearchResultsPanel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SearchScreen
import kotlin.math.roundToInt

private sealed class TopDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : TopDestination("home", "Início", Icons.Filled.Home, Icons.Outlined.Home)
    data object Search : TopDestination("search", "Buscar", Icons.Filled.Search, Icons.Outlined.Search)
    data object Library : TopDestination("library", "Biblioteca", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
}

@Composable
fun PulseTubeApp(
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val likedTracks by viewModel.likedTracks.collectAsStateWithLifecycle()
    val likedCount by viewModel.likedCount.collectAsStateWithLifecycle()
    val likedVideoIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val featuredTracks by viewModel.featuredTracks.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val localSearchResults by viewModel.localSearchResults.collectAsStateWithLifecycle()
    val topLibraryQuery by viewModel.topLibraryQuery.collectAsStateWithLifecycle()
    val topLibraryTrackResults by viewModel.topLibraryTrackResults.collectAsStateWithLifecycle()
    val topLibraryPlaylistResults by viewModel.topLibraryPlaylistResults.collectAsStateWithLifecycle()
    val selectedCategoryChip by viewModel.selectedCategoryChip.collectAsStateWithLifecycle()
    val youtubeSearchState by viewModel.youtubeSearchState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isBackgroundCachingEnabled by viewModel.isBackgroundCachingEnabled.collectAsStateWithLifecycle()
    val offlineCacheStats by viewModel.offlineCacheStats.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    var showImportDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAuthorizedAudioDialog by remember { mutableStateOf(false) }
    var pendingLocalTrackForPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    var pendingYtTrackForPlaylist by remember { mutableStateOf<YouTubeTrackResult?>(null) }

    var miniSlotBounds by remember { mutableStateOf<Rect?>(null) }
    var expandedSlotBounds by remember { mutableStateOf<Rect?>(null) }

    val topDestinations = remember {
        listOf(TopDestination.Home, TopDestination.Search, TopDestination.Library)
    }

    val anyDialogOpen = showImportDialog || showCreateDialog || showSettingsSheet ||
        showAuthorizedAudioDialog ||
        pendingLocalTrackForPlaylist != null || pendingYtTrackForPlaylist != null

    val navigateToTop: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 700.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                ) {
                    topDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTop(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (!playbackState.isPlayerExpanded) {
                            MiniPlayerBar(
                                playbackState = playbackState,
                                isLiked = likedVideoIds.contains(playbackState.currentTrack?.youtubeVideoId),
                                onExpandClick = { viewModel.playerController.expandPlayer(true) },
                                onPlayPauseClick = {
                                    if (playbackState.currentTrack != null) {
                                        viewModel.playerController.togglePlayPause()
                                    } else {
                                        viewModel.quickStartPlayback()
                                    }
                                },
                                onPreviousClick = { viewModel.playerController.playPrevious() },
                                onNextClick = {
                                    if (playbackState.currentTrack != null) {
                                        viewModel.playerController.playNext(autoAdvance = false)
                                    } else {
                                        viewModel.quickStartPlayback()
                                    }
                                },
                                onLikeClick = {
                                    playbackState.currentTrack?.let {
                                        viewModel.toggleLike(it.youtubeVideoId)
                                    }
                                },
                                onSeek = { positionSec ->
                                    viewModel.playerController.seekTo(positionSec)
                                },
                                onVideoSlotPositioned = { coords ->
                                    miniSlotBounds = coords.boundsInRoot()
                                },
                                modifier = if (isWideScreen) Modifier.navigationBarsPadding() else Modifier
                            )
                        }

                        if (!isWideScreen) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                topDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTop(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${dest.route}")
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                val showTopLibrarySearch = currentRoute == TopDestination.Home.route ||
                    currentRoute == TopDestination.Library.route
                val canNavigateBack = currentRoute != null &&
                    currentRoute != TopDestination.Home.route &&
                    currentRoute != TopDestination.Search.route &&
                    currentRoute != TopDestination.Library.route
                val topBarTitle = when (currentRoute) {
                    TopDestination.Home.route -> "Início"
                    TopDestination.Search.route -> "Buscar"
                    TopDestination.Library.route -> "Biblioteca"
                    "liked" -> "Músicas Curtidas"
                    else -> "Playlist"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding)
                ) {
                    KeuMusicTopBar(
                        title = topBarTitle,
                        isDarkTheme = isDarkTheme,
                        canNavigateBack = canNavigateBack,
                        onBackClick = { navController.popBackStack() },
                        onToggleTheme = { viewModel.toggleTheme() },
                        isBackgroundCachingEnabled = isBackgroundCachingEnabled,
                        onOpenSettings = { showSettingsSheet = true }
                    )

                    Button(
                        onClick = { showAuthorizedAudioDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Áudio externo")
                    }

                    if (showTopLibrarySearch) {
                        TopLibrarySearchBar(
                            query = topLibraryQuery,
                            onQueryChange = { viewModel.updateTopLibraryQuery(it) },
                            onClear = { viewModel.clearTopLibraryQuery() }
                        )
                        LibraryCategoryChipsRow(
                            selectedChip = selectedCategoryChip,
                            onSelectChip = { viewModel.selectCategoryChip(it) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = TopDestination.Home.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(TopDestination.Home.route) {
                                HomeScreen(
                                    selectedCategory = selectedCategoryChip,
                                    playlists = playlists,
                                    likedTracks = likedTracks,
                                    likedCount = likedCount,
                                    recentlyPlayed = recentlyPlayed,
                                    featuredTracks = featuredTracks,
                                    currentPlayingVideoId = playbackState.currentTrack?.youtubeVideoId,
                                    likedVideoIds = likedVideoIds,
                                    onSelectCategory = { viewModel.selectCategoryChip(it) },
                                    onOpenPlaylist = { id ->
                                        viewModel.clearTopLibraryQuery()
                                        navController.navigate("playlist/$id")
                                    },
                                    onPlayPlaylist = { id -> viewModel.playPlaylistById(id) },
                                    onQuickStart = { viewModel.quickStartPlayback() },
                                    onOpenLikedSongs = {
                                        viewModel.clearTopLibraryQuery()
                                        navController.navigate("liked")
                                    },
                                    onOpenImportDialog = { showImportDialog = true },
                                    onOpenCreateDialog = { showCreateDialog = true },
                                    onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                                    onToggleLike = { vid -> viewModel.toggleLike(vid) },
                                    onAddTrackToPlaylist = { track -> pendingLocalTrackForPlaylist = track },
                                    onPlayNext = { track ->
                                        viewModel.playerController.addTrackToQueueNext(track)
                                        viewModel.showMessage("Adicionada a seguir na fila")
                                    },
                                    onOpenInYouTube = { vid -> openVideoInYouTube(context, vid) },
                                    cachedVideoIds = offlineCacheStats.cachedVideoIds
                                )
                            }

                            composable(TopDestination.Search.route) {
                                SearchScreen(
                                    query = searchQuery,
                                    localResults = localSearchResults,
                                    youtubeSearchState = youtubeSearchState,
                                    currentPlayingVideoId = playbackState.currentTrack?.youtubeVideoId,
                                    likedVideoIds = likedVideoIds,
                                    onQueryChange = { viewModel.updateSearchQuery(it) },
                                    onTriggerYouTubeSearch = { q, playlistsOnly ->
                                        viewModel.searchYouTube(q, playlistsOnly)
                                    },
                                    onImportYouTubePlaylist = { ytPlaylistId ->
                                        viewModel.importYouTubePlaylistById(ytPlaylistId) { imported ->
                                            navController.navigate("playlist/${imported.id}")
                                        }
                                    },
                                    onPlayLocalTrack = { track, queue -> viewModel.playTrack(track, queue) },
                                    onPlayYouTubeTrack = { track, all ->
                                        viewModel.playYouTubeSearchTrack(track, all)
                                    },
                                    onAddYouTubeTrackToPlaylist = { ytTrack ->
                                        pendingYtTrackForPlaylist = ytTrack
                                    },
                                    onAddLocalTrackToPlaylist = { track ->
                                        pendingLocalTrackForPlaylist = track
                                    },
                                    onToggleLike = { vid -> viewModel.toggleLike(vid) },
                                    onPlayNext = { track ->
                                        viewModel.playerController.addTrackToQueueNext(track)
                                        viewModel.showMessage("Adicionada a seguir na fila")
                                    },
                                    onOpenInYouTube = { vid -> openVideoInYouTube(context, vid) },
                                    cachedVideoIds = offlineCacheStats.cachedVideoIds
                                )
                            }

                            composable(TopDestination.Library.route) {
                                LibraryScreen(
                                    selectedCategory = selectedCategoryChip,
                                    playlists = playlists,
                                    recentlyPlayed = recentlyPlayed,
                                    featuredTracks = featuredTracks,
                                    likedTracks = likedTracks,
                                    likedCount = likedCount,
                                    currentPlayingVideoId = playbackState.currentTrack?.youtubeVideoId,
                                    likedVideoIds = likedVideoIds,
                                    onSelectCategory = { viewModel.selectCategoryChip(it) },
                                    onOpenPlaylist = { id ->
                                        viewModel.clearTopLibraryQuery()
                                        navController.navigate("playlist/$id")
                                    },
                                    onPlayPlaylist = { id -> viewModel.playPlaylistById(id) },
                                    onOpenLikedSongs = {
                                        viewModel.clearTopLibraryQuery()
                                        navController.navigate("liked")
                                    },
                                    onOpenImportDialog = { showImportDialog = true },
                                    onOpenCreateDialog = { showCreateDialog = true },
                                    onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                                    onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                                    onToggleLike = { vid -> viewModel.toggleLike(vid) },
                                    onAddTrackToPlaylist = { track -> pendingLocalTrackForPlaylist = track },
                                    onPlayNext = { track ->
                                        viewModel.playerController.addTrackToQueueNext(track)
                                        viewModel.showMessage("Adicionada a seguir na fila")
                                    },
                                    onOpenInYouTube = { vid -> openVideoInYouTube(context, vid) },
                                    cachedVideoIds = offlineCacheStats.cachedVideoIds,
                                    isBackgroundCachingEnabled = isBackgroundCachingEnabled,
                                    offlineCacheStats = offlineCacheStats,
                                    onOpenOfflineSettings = { showSettingsSheet = true }
                                )
                            }

                            composable(
                                route = "playlist/{playlistId}",
                                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                            ) { entry ->
                                val playlistId = entry.arguments?.getString("playlistId").orEmpty()
                                val playlist by viewModel.observePlaylist(playlistId)
                                    .collectAsStateWithLifecycle(initialValue = null)
                                val tracks by viewModel.observePlaylistTracks(playlistId)
                                    .collectAsStateWithLifecycle(initialValue = emptyList())

                                PlaylistDetailScreen(
                                    isLikedCollection = false,
                                    playlist = playlist,
                                    tracks = tracks,
                                    currentPlayingVideoId = playbackState.currentTrack?.youtubeVideoId,
                                    likedVideoIds = likedVideoIds,
                                    isBusy = isBusy,
                                    showBackButton = false,
                                    onBack = { navController.popBackStack() },
                                    onPlayAll = { shuffle -> viewModel.playPlaylistTracks(tracks, shuffle) },
                                    onPlayTrack = { track -> viewModel.playTrack(track, tracks) },
                                    onSyncPlaylist = { viewModel.syncPlaylist(playlistId) },
                                    onAddTrackUrl = { url -> viewModel.addTrackUrlToPlaylist(playlistId, url) },
                                    onToggleLike = { vid -> viewModel.toggleLike(vid) },
                                    onAddTrackToAnotherPlaylist = { track -> pendingLocalTrackForPlaylist = track },
                                    onPlayNext = { track ->
                                        viewModel.playerController.addTrackToQueueNext(track)
                                        viewModel.showMessage("Adicionada a seguir na fila")
                                    },
                                    onRemoveTrack = { trackId -> viewModel.removeTrackFromPlaylist(trackId) },
                                    onOpenInYouTube = { vid -> openVideoInYouTube(context, vid) },
                                    cachedVideoIds = offlineCacheStats.cachedVideoIds
                                )
                            }

                            composable("liked") {
                                PlaylistDetailScreen(
                                    isLikedCollection = true,
                                    playlist = null,
                                    tracks = likedTracks,
                                    currentPlayingVideoId = playbackState.currentTrack?.youtubeVideoId,
                                    likedVideoIds = likedVideoIds,
                                    isBusy = isBusy,
                                    showBackButton = false,
                                    onBack = { navController.popBackStack() },
                                    onPlayAll = { shuffle -> viewModel.playPlaylistTracks(likedTracks, shuffle) },
                                    onPlayTrack = { track -> viewModel.playTrack(track, likedTracks) },
                                    onSyncPlaylist = {},
                                    onAddTrackUrl = {},
                                    onToggleLike = { vid -> viewModel.toggleLike(vid) },
                                    onAddTrackToAnotherPlaylist = { track -> pendingLocalTrackForPlaylist = track },
                                    onPlayNext = { track ->
                                        viewModel.playerController.addTrackToQueueNext(track)
                                        viewModel.showMessage("Adicionada a seguir na fila")
                                    },
                                    onRemoveTrack = null,
                                    onOpenInYouTube = { vid -> openVideoInYouTube(context, vid) },
                                    cachedVideoIds = offlineCacheStats.cachedVideoIds
                                )
                            }
                        }

                        if (showTopLibrarySearch && topLibraryQuery.isNotBlank()) {
                            BackHandler { viewModel.clearTopLibraryQuery() }
                            TopLibrarySearchResultsPanel(
                                query = topLibraryQuery,
                                playlistResults = topLibraryPlaylistResults,
                                trackResults = topLibraryTrackResults,
                                currentPlayingVideoId = playbackState.currentTrack?.youtubeVideoId,
                                likedVideoIds = likedVideoIds,
                                onOpenPlaylist = { id ->
                                    viewModel.clearTopLibraryQuery()
                                    navController.navigate("playlist/$id")
                                },
                                onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                                onToggleLike = { vid -> viewModel.toggleLike(vid) },
                                onAddToPlaylist = { track -> pendingLocalTrackForPlaylist = track },
                                onPlayNext = { track ->
                                    viewModel.playerController.addTrackToQueueNext(track)
                                    viewModel.showMessage("Adicionada a seguir na fila")
                                },
                                onOpenInYouTube = { vid -> openVideoInYouTube(context, vid) },
                                onSearchOnYouTubeClick = { queryText ->
                                    viewModel.clearTopLibraryQuery()
                                    viewModel.searchYouTube(queryText, playlistsOnly = false)
                                    navigateToTop(TopDestination.Search.route)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Full-Screen Now Playing Overlay
        AnimatedVisibility(
            visible = playbackState.isPlayerExpanded && playbackState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.zIndex(8f)
        ) {
            NowPlayingOverlay(
                playbackState = playbackState,
                isLiked = likedVideoIds.contains(playbackState.currentTrack?.youtubeVideoId),
                onCollapse = { viewModel.playerController.expandPlayer(false) },
                onPlayPause = { viewModel.playerController.togglePlayPause() },
                onNext = { viewModel.playerController.playNext(autoAdvance = false) },
                onPrevious = { viewModel.playerController.playPrevious() },
                onSeek = { viewModel.playerController.seekTo(it) },
                onToggleShuffle = { viewModel.playerController.toggleShuffle() },
                onCycleRepeat = { viewModel.playerController.cycleRepeatMode() },
                onToggleVideoStage = { showVideo ->
                    if (showVideo != playbackState.showVideoStage) {
                        viewModel.playerController.toggleVideoStage()
                    }
                },
                onToggleLike = {
                    playbackState.currentTrack?.let { viewModel.toggleLike(it.youtubeVideoId) }
                },
                onAddToPlaylist = { track -> pendingLocalTrackForPlaylist = track },
                onSelectQueueIndex = { idx -> viewModel.playerController.playFromQueueIndex(idx) },
                onOpenInYouTube = { vid -> openVideoInYouTube(context, vid) },
                onVideoStagePositioned = { coords ->
                    expandedSlotBounds = coords.boundsInRoot()
                }
            )
        }

        // Persistent Single WebView Container
        val activeBounds = when {
            anyDialogOpen || playbackState.currentTrack == null -> null
            playbackState.isPlayerExpanded && playbackState.showVideoStage -> expandedSlotBounds
            !playbackState.isPlayerExpanded -> miniSlotBounds
            else -> null
        }

        val density = LocalDensity.current
        val isVisibleStage = activeBounds != null && activeBounds.width > 4f && activeBounds.height > 4f
        val targetWidthDp = if (activeBounds != null && activeBounds.width > 4f && activeBounds.height > 4f) {
            with(density) { activeBounds.width.toDp() }
        } else 1.dp
        val targetHeightDp = if (activeBounds != null && activeBounds.width > 4f && activeBounds.height > 4f) {
            with(density) { activeBounds.height.toDp() }
        } else 1.dp
        val targetOffset = if (activeBounds != null && activeBounds.width > 4f && activeBounds.height > 4f) {
            IntOffset(activeBounds.left.roundToInt(), activeBounds.top.roundToInt())
        } else {
            IntOffset(0, 0)
        }
        val cornerDp = if (playbackState.isPlayerExpanded) 14.dp else 6.dp

        if (playbackState.currentTrack != null) {
            Box(
                modifier = Modifier
                    .zIndex(if (isVisibleStage && playbackState.isPlayerExpanded) 10f else if (isVisibleStage) 3f else 0f)
                    .offset { targetOffset }
                    .size(width = targetWidthDp, height = targetHeightDp)
                    .clip(RoundedCornerShape(cornerDp))
                    .alpha(if (isVisibleStage) 1f else 0.01f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            val wv = viewModel.playerController.getOrCreateWebView(ctx)
                            (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                            addView(
                                wv,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Dialogs
    if (showImportDialog) {
        ImportYouTubeDialog(
            isBusy = isBusy,
            onDismiss = { showImportDialog = false },
            onConfirm = { url ->
                viewModel.importPlaylistFromUrl(url) { imported ->
                    showImportDialog = false
                    navController.navigate("playlist/${imported.id}")
                }
            }
        )
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, subtitle ->
                viewModel.createCustomPlaylist(title, subtitle) { created ->
                    showCreateDialog = false
                    pendingLocalTrackForPlaylist?.let { t ->
                        viewModel.addExistingTrackToPlaylist(created.id, t)
                        pendingLocalTrackForPlaylist = null
                    }
                    pendingYtTrackForPlaylist?.let { yt ->
                        viewModel.addYouTubeTrackToPlaylist(created.id, yt)
                        pendingYtTrackForPlaylist = null
                    }
                    navController.navigate("playlist/${created.id}")
                }
            }
        )
    }

    if (pendingLocalTrackForPlaylist != null || pendingYtTrackForPlaylist != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = {
                pendingLocalTrackForPlaylist = null
                pendingYtTrackForPlaylist = null
            },
            onSelectPlaylist = { targetPlaylist ->
                pendingLocalTrackForPlaylist?.let {
                    viewModel.addExistingTrackToPlaylist(targetPlaylist.id, it)
                }
                pendingYtTrackForPlaylist?.let {
                    viewModel.addYouTubeTrackToPlaylist(targetPlaylist.id, it)
                }
                pendingLocalTrackForPlaylist = null
                pendingYtTrackForPlaylist = null
            },
            onCreateNewClick = {
                showCreateDialog = true
            }
        )
    }

    if (showAuthorizedAudioDialog) {
        AuthorizedAudioDialog(
            onDismiss = { showAuthorizedAudioDialog = false },
            onPlay = { url, title ->
                viewModel.playAuthorizedAudio(url, title)
                showAuthorizedAudioDialog = false
            },
            onPause = { viewModel.pauseAuthorizedAudio() }
        )
    }

    if (showSettingsSheet) {
        OfflineSettingsSheet(
            isBackgroundCachingEnabled = isBackgroundCachingEnabled,
            cacheStats = offlineCacheStats,
            isDarkTheme = isDarkTheme,
            onToggleBackgroundCaching = { enabled ->
                viewModel.setBackgroundCachingEnabled(enabled)
            },
            onSyncNow = { viewModel.triggerBackgroundCacheSync() },
            onClearCache = { viewModel.clearOfflineCache() },
            onToggleTheme = { viewModel.toggleTheme() },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

private fun openVideoInYouTube(context: Context, videoId: String) {
    try {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=$videoId")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
