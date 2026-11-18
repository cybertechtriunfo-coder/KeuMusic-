package com.example.ui

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cache.OfflineCacheStats
import com.example.data.cache.OfflineMusicCacheManager
import com.example.data.local.AppDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistWithCount
import com.example.data.local.TrackEntity
import com.example.data.repository.MusicRepository
import com.example.data.youtube.YouTubeSearchResponse
import com.example.data.youtube.YouTubeTrackResult
import com.example.data.youtube.YouTubeUrlParser
import com.example.player.PlaybackUiState
import com.example.player.YouTubePlayerController
import com.example.ui.components.LibraryCategoryChip
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.keuMusicDataStore by preferencesDataStore(name = "keumusic_settings")
private val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
private val KEY_BACKGROUND_CACHING_ENABLED = booleanPreferencesKey("background_caching_enabled")

sealed interface YouTubeSearchUiState {
    data object Idle : YouTubeSearchUiState
    data object Loading : YouTubeSearchUiState
    data class Success(val data: YouTubeSearchResponse) : YouTubeSearchUiState
    data class Error(val message: String) : YouTubeSearchUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(AppDatabase.getInstance(application).musicDao())
    private val offlineCacheManager = OfflineMusicCacheManager(application)

    val isBackgroundCachingEnabled: StateFlow<Boolean> = application.keuMusicDataStore.data
        .map { prefs -> prefs[KEY_BACKGROUND_CACHING_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val offlineCacheStats: StateFlow<OfflineCacheStats> = offlineCacheManager.stats

    val playerController = YouTubePlayerController(
        onTrackStarted = { track ->
            viewModelScope.launch {
                repository.markTrackPlayed(track.youtubeVideoId)
                if (isBackgroundCachingEnabled.value) {
                    scheduleBackgroundCacheSync(priorityTrack = track)
                }
            }
        },
        resolveOfflineAudioFile = { videoId ->
            offlineCacheManager.getCachedAudioFile(videoId)
        },
        isNetworkAvailable = {
            OfflineMusicCacheManager.isDeviceOnline(getApplication())
        }
    )

    val playbackState: StateFlow<PlaybackUiState> = playerController.state

    val playlists: StateFlow<List<PlaylistWithCount>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedTracks: StateFlow<List<TrackEntity>> = repository.likedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedCount: StateFlow<Int> = repository.likedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val likedVideoIds: StateFlow<Set<String>> = repository.likedVideoIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val recentlyPlayed: StateFlow<List<TrackEntity>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredTracks: StateFlow<List<TrackEntity>> = repository.featuredTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val localSearchResults: StateFlow<List<TrackEntity>> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.searchLocalTracks(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _topLibraryQuery = MutableStateFlow("")
    val topLibraryQuery: StateFlow<String> = _topLibraryQuery.asStateFlow()

    val topLibraryTrackResults: StateFlow<List<TrackEntity>> = _topLibraryQuery
        .debounce(100)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.searchLocalTracks(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topLibraryPlaylistResults: StateFlow<List<PlaylistWithCount>> = combine(
        _topLibraryQuery,
        playlists
    ) { query, allPlaylists ->
        val clean = query.trim()
        if (clean.isEmpty()) emptyList()
        else allPlaylists.filter {
            it.title.contains(clean, ignoreCase = true) ||
                it.subtitle.contains(clean, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryChip = MutableStateFlow(LibraryCategoryChip.ALL)
    val selectedCategoryChip: StateFlow<LibraryCategoryChip> = _selectedCategoryChip.asStateFlow()

    val isDarkTheme: StateFlow<Boolean> = application.keuMusicDataStore.data
        .map { prefs -> prefs[KEY_IS_DARK_THEME] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _youtubeSearchState = MutableStateFlow<YouTubeSearchUiState>(YouTubeSearchUiState.Idle)
    val youtubeSearchState: StateFlow<YouTubeSearchUiState> = _youtubeSearchState.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedStarterLibraryIfEmpty()
        }
        viewModelScope.launch {
            combine(
                isBackgroundCachingEnabled,
                likedTracks,
                recentlyPlayed,
                featuredTracks
            ) { enabled, liked, recent, featured ->
                enabled to buildCandidateList(null, liked, recent, featured)
            }.collect { (enabled, candidates) ->
                offlineCacheManager.scheduleBackgroundCaching(enabled, candidates)
            }
        }
    }

    fun observePlaylist(playlistId: String): Flow<PlaylistEntity?> =
        repository.observePlaylist(playlistId)

    fun observePlaylistTracks(playlistId: String): Flow<List<TrackEntity>> =
        repository.observeTracksForPlaylist(playlistId)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _youtubeSearchState.value = YouTubeSearchUiState.Idle
        }
    }

    fun updateTopLibraryQuery(query: String) {
        _topLibraryQuery.value = query
    }

    fun clearTopLibraryQuery() {
        _topLibraryQuery.value = ""
    }

    fun selectCategoryChip(chip: LibraryCategoryChip) {
        _selectedCategoryChip.value = if (_selectedCategoryChip.value == chip && chip != LibraryCategoryChip.ALL) {
            LibraryCategoryChip.ALL
        } else {
            chip
        }
        if (_topLibraryQuery.value.isNotBlank()) {
            _topLibraryQuery.value = ""
        }
    }

    fun toggleTheme() {
        val targetDark = !isDarkTheme.value
        viewModelScope.launch {
            getApplication<Application>().keuMusicDataStore.edit { prefs ->
                prefs[KEY_IS_DARK_THEME] = targetDark
            }
        }
    }

    fun setBackgroundCachingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().keuMusicDataStore.edit { prefs ->
                prefs[KEY_BACKGROUND_CACHING_ENABLED] = enabled
            }
            if (enabled) {
                scheduleBackgroundCacheSync(priorityTrack = playbackState.value.currentTrack)
                _snackbarMessage.value = "Cache offline em segundo plano ativado"
            } else {
                offlineCacheManager.cancelBackgroundCaching()
                _snackbarMessage.value = "Cache offline em segundo plano desativado"
            }
        }
    }

    fun toggleBackgroundCaching() {
        setBackgroundCachingEnabled(!isBackgroundCachingEnabled.value)
    }

    fun triggerBackgroundCacheSync() {
        if (!isBackgroundCachingEnabled.value) return
        scheduleBackgroundCacheSync(priorityTrack = playbackState.value.currentTrack)
        _snackbarMessage.value = "Sincronizando cache offline no armazenamento local..."
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            offlineCacheManager.clearCache()
            _snackbarMessage.value = "Cache offline removido do armazenamento local"
        }
    }

    private fun scheduleBackgroundCacheSync(priorityTrack: TrackEntity? = null) {
        val candidates = buildCandidateList(
            priorityTrack = priorityTrack,
            liked = likedTracks.value,
            recent = recentlyPlayed.value,
            featured = featuredTracks.value
        )
        offlineCacheManager.scheduleBackgroundCaching(
            enabled = isBackgroundCachingEnabled.value,
            candidates = candidates
        )
    }

    private fun buildCandidateList(
        priorityTrack: TrackEntity?,
        liked: List<TrackEntity>,
        recent: List<TrackEntity>,
        featured: List<TrackEntity>
    ): List<TrackEntity> {
        return buildList {
            if (priorityTrack != null) add(priorityTrack)
            addAll(liked)
            addAll(recent)
            addAll(featured)
        }.distinctBy { it.youtubeVideoId }
    }

    fun searchYouTube(query: String = _searchQuery.value, playlistsOnly: Boolean = false) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        _searchQuery.value = clean
        viewModelScope.launch {
            _youtubeSearchState.value = YouTubeSearchUiState.Loading
            repository.searchYouTube(clean, playlistsOnly)
                .onSuccess { response ->
                    _youtubeSearchState.value = YouTubeSearchUiState.Success(response)
                }
                .onFailure { err ->
                    _youtubeSearchState.value = YouTubeSearchUiState.Error(
                        err.message ?: "Não foi possível buscar no YouTube."
                    )
                }
        }
    }

    fun importPlaylistFromUrl(rawInput: String, onImported: (PlaylistEntity) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.importFromUrlOrId(rawInput)
                .onSuccess { playlist ->
                    _snackbarMessage.value = "Playlist \"${playlist.title}\" importada!"
                    onImported(playlist)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Falha ao importar playlist."
                }
            _isBusy.value = false
        }
    }

    fun importYouTubePlaylistById(playlistId: String, onImported: (PlaylistEntity) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.importYouTubePlaylistById(playlistId)
                .onSuccess { playlist ->
                    _snackbarMessage.value = "Salvo na biblioteca: ${playlist.title}"
                    onImported(playlist)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Erro ao importar playlist do YouTube."
                }
            _isBusy.value = false
        }
    }

    fun createCustomPlaylist(title: String, subtitle: String, onCreated: (PlaylistEntity) -> Unit = {}) {
        viewModelScope.launch {
            val playlist = repository.createCustomPlaylist(title, subtitle)
            _snackbarMessage.value = "Playlist \"${playlist.title}\" criada!"
            onCreated(playlist)
        }
    }

    fun addTrackUrlToPlaylist(playlistId: String, rawInput: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.addTrackUrlToPlaylist(playlistId, rawInput)
                .onSuccess { track ->
                    _snackbarMessage.value = "Adicionada: ${track.title}"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Não foi possível adicionar a faixa."
                }
            _isBusy.value = false
        }
    }

    fun addYouTubeTrackToPlaylist(playlistId: String, track: YouTubeTrackResult) {
        viewModelScope.launch {
            repository.addYouTubeTrackToPlaylist(playlistId, track)
                .onSuccess {
                    _snackbarMessage.value = "Música adicionada à playlist!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Erro ao adicionar música."
                }
        }
    }

    fun addExistingTrackToPlaylist(playlistId: String, track: TrackEntity) {
        viewModelScope.launch {
            repository.addExistingTrackToPlaylist(playlistId, track)
            _snackbarMessage.value = "Adicionada à playlist!"
        }
    }

    fun removeTrackFromPlaylist(trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(trackId)
            _snackbarMessage.value = "Faixa removida da playlist."
        }
    }

    fun deletePlaylist(playlistId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _snackbarMessage.value = "Playlist removida da biblioteca."
            onDeleted()
        }
    }

    fun syncPlaylist(playlistId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.syncPlaylist(playlistId)
                .onSuccess { playlist ->
                    _snackbarMessage.value = "\"${playlist.title}\" sincronizada com o YouTube!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Falha na sincronização."
                }
            _isBusy.value = false
        }
    }

    fun toggleLike(youtubeVideoId: String) {
        viewModelScope.launch {
            val liked = repository.toggleLike(youtubeVideoId)
            _snackbarMessage.value = if (liked) "Adicionada às Músicas Curtidas" else "Removida das Curtidas"
        }
    }

    fun playTrack(track: TrackEntity, queue: List<TrackEntity>) {
        playerController.playTrack(track, queue)
    }

    fun playYouTubeSearchTrack(track: YouTubeTrackResult, allResults: List<YouTubeTrackResult>) {
        val queue = allResults.mapIndexed { index, item ->
            TrackEntity(
                id = "search_${item.videoId}_$index",
                playlistId = "search",
                youtubeVideoId = item.videoId,
                title = item.title,
                artist = item.artist,
                thumbnailUrl = item.thumbnailUrl.ifBlank { YouTubeUrlParser.hqThumbnail(item.videoId) },
                durationText = item.durationText,
                durationSeconds = item.durationSeconds,
                position = index
            )
        }
        val selected = queue.firstOrNull { it.youtubeVideoId == track.videoId } ?: return
        playerController.playTrack(selected, queue)
    }

    fun playPlaylistTracks(tracks: List<TrackEntity>, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val ordered = if (shuffle) tracks.shuffled() else tracks
        playerController.playTrack(ordered.first(), ordered)
    }

    fun playPlaylistById(playlistId: String, shuffle: Boolean = false) {
        viewModelScope.launch {
            val tracks = repository.getTracksSnapshot(playlistId)
            if (tracks.isNotEmpty()) {
                val ordered = if (shuffle) tracks.shuffled() else tracks
                playerController.playTrack(ordered.first(), ordered)
            } else {
                _snackbarMessage.value = "Esta playlist ainda não possui músicas."
            }
        }
    }

    fun quickStartPlayback() {
        viewModelScope.launch {
            val currentPlaylists = playlists.value
            for (pl in currentPlaylists) {
                val tracks = repository.getTracksSnapshot(pl.id)
                if (tracks.isNotEmpty()) {
                    playerController.playTrack(tracks.first(), tracks)
                    return@launch
                }
            }
            _snackbarMessage.value = "Importe ou selecione uma playlist para tocar."
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        offlineCacheManager.release()
        playerController.release()
    }
}
