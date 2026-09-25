/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.krtirtho.spotube.modules.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.di.injectLogger
import dev.krtirtho.spotube.core.playback.CollectionPlaybackHelper
import dev.krtirtho.spotube.core.remote.RemoteCollectionType
import dev.krtirtho.spotube.core.remote.RemotePlaybackController
import dev.krtirtho.spotube.core.share.ShareService
import dev.krtirtho.spotube.core.ui.component.TrackOptionsAction
import dev.krtirtho.spotube.core.ui.component.TrackOptionsContext
import dev.krtirtho.spotube.core.ui.component.TrackOptionsState
import dev.krtirtho.spotube.modules.blacklist.BlacklistRepository
import dev.krtirtho.spotube.modules.downloads.DownloadManager
import dev.krtirtho.spotube.modules.library.LibraryRepository
import dev.krtirtho.spotube.modules.saved_tracks.SavedTracksRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

sealed interface PlaylistScreenState {
    data object Loading : PlaylistScreenState

    sealed interface Data : PlaylistScreenState {
        val playlist: MetadataPlaylist?
        val isSaved: Boolean
        val tracks: List<MetadataTrack>
        val nextPagination: PaginationStrategy?
        val isSaving: Boolean

        data class Loaded(
            override val playlist: MetadataPlaylist? = null,
            override val isSaved: Boolean = false,
            override val tracks: List<MetadataTrack> = emptyList(),
            override val nextPagination: PaginationStrategy? = null,
            override val isSaving: Boolean = false,
        ) : Data {
            fun toLoadingMore(): LoadingMore = LoadingMore(
                playlist = playlist,
                isSaved = isSaved,
                tracks = tracks,
                nextPagination = nextPagination,
                isSaving = isSaving,
            )
        }

        data class LoadingMore(
            override val playlist: MetadataPlaylist? = null,
            override val isSaved: Boolean = false,
            override val tracks: List<MetadataTrack> = emptyList(),
            override val nextPagination: PaginationStrategy? = null,
            override val isSaving: Boolean = false,
        ) : Data
    }

    data class Error(val message: String) : PlaylistScreenState
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModel(
    private val playlistId: String,
    private val repository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
    private val savedTracksRepository: SavedTracksRepository,
    private val playbackHelper: CollectionPlaybackHelper,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val blacklistRepository: BlacklistRepository,
    private val shareService: ShareService,
    private val downloadManager: DownloadManager,
    private val remotePlaybackController: RemotePlaybackController,
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<PlaylistViewModel>()

    private val _state = MutableStateFlow<PlaylistScreenState>(PlaylistScreenState.Loading)
    val uiState: StateFlow<PlaylistScreenState> = _state.asStateFlow()
    val savedPlaylistIds
        get() = libraryRepository.savedPlaylistIdsFlow

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _blacklistedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val blacklistedTrackIds: StateFlow<Set<String>> = _blacklistedTrackIds.asStateFlow()

    private val _blacklistedArtistIds = MutableStateFlow<Set<String>>(emptySet())
    val blacklistedArtistIds: StateFlow<Set<String>> = _blacklistedArtistIds.asStateFlow()

    private val _tracksToAddToPlaylist = MutableStateFlow<List<MetadataTrack>>(emptyList())
    private val _showAddToPlaylistPicker = MutableStateFlow(false)
    val showAddToPlaylistPicker: StateFlow<Boolean> = _showAddToPlaylistPicker.asStateFlow()

    val trackOptionsContext: StateFlow<TrackOptionsContext> = combine(
        audioPlayerQueue.queueFlow,
        audioPlayerQueue.currentQueueEntryFlow,
        savedTracksRepository.savedTracksIdsFlow,
        _blacklistedTrackIds,
        _blacklistedArtistIds,
    ) { queue, currentEntry, savedIds, blacklistedTracks, blacklistedArtists ->
        TrackOptionsContext(
            currentTrackId = (currentEntry as? QueueEntry.StreamingTrack)?.track?.id,
            queueTrackIds = queue.mapNotNull { entry ->
                (entry as? QueueEntry.StreamingTrack)?.track?.id
            }.toSet(),
            savedTrackIds = savedIds,
            blacklistedTrackIds = blacklistedTracks,
            blacklistedArtistIds = blacklistedArtists,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackOptionsContext())

    init {
        viewModelScope.launch {
            repository.pluginManager.selectedMetadataPlugin
                .filterNotNull()
                .flatMapLatest { it.loggedInFlow }
                .distinctUntilChanged()
                .collect {
                    loadInitialData()
                    loadCurrentUser()
                }
        }
        blacklistRepository.blacklistedTracks
            .onEach { tracks -> _blacklistedTrackIds.value = tracks.map { it.id }.toSet() }
            .launchIn(viewModelScope)
        blacklistRepository.blacklistedArtists
            .onEach { artists -> _blacklistedArtistIds.value = artists.map { it.id }.toSet() }
            .launchIn(viewModelScope)
    }

    private suspend fun loadCurrentUser() {
        runCatching {
            _currentUserId.value = libraryRepository.currentUser()?.id
        }.onFailure { e ->
            logger.e(e) { "Failed to load current user" }
        }
    }

    private suspend fun loadInitialData() = runCatching {
        _state.value = PlaylistScreenState.Loading
        val playlistInfo = repository.getPlaylistInfo(playlistId)
        val tracksResult = repository.getPlaylistTracks(playlistId)

        tracksResult?.items?.let { savedTracksRepository.isSavedTracks(it.map { item -> item.id }) }
        _state.value = PlaylistScreenState.Data.Loaded(
            playlist = playlistInfo?.first,
            isSaved = playlistInfo?.second ?: false,
            tracks = tracksResult?.items ?: emptyList(),
            nextPagination = tracksResult?.nextPagination,
        )
    }.onFailure { e ->
        logger.e(e) { "Failed to load playlist" }
        _state.value = PlaylistScreenState.Error(e.message ?: "Unknown error")
    }

    suspend fun loadMoreTracks() = runCatching {
        val currentState = _state.value
        if (currentState is PlaylistScreenState.Data.Loaded && currentState.nextPagination != null) {
            _state.value = currentState.toLoadingMore()
            val result = repository.getPlaylistTracks(playlistId, currentState.nextPagination)

            result?.items?.let { savedTracksRepository.isSavedTracks(it.map { item -> item.id }) }
            _state.value = PlaylistScreenState.Data.Loaded(
                playlist = currentState.playlist,
                isSaved = currentState.isSaved,
                tracks = currentState.tracks + (result?.items ?: emptyList()),
                nextPagination = result?.nextPagination,
                isSaving = currentState.isSaving,
            )
        }
    }.onFailure { e ->
        logger.e(e) { "Failed to load more tracks" }
        _state.value = PlaylistScreenState.Error(e.message ?: "Unknown error")
    }

    fun loadNextTracksPage() {
        viewModelScope.launch {
            loadMoreTracks()
        }
    }

    fun toggleSavedPlaylist() {
        viewModelScope.launch {
            runCatching {
                val isLiked = libraryRepository.isSavedPlaylists(listOf(playlistId)).firstOrNull() ?: false
                if (isLiked) {
                    libraryRepository.removeSavedPlaylists(listOf(playlistId))
                } else {
                    libraryRepository.savePlaylists(listOf(playlistId))
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to toggle saved playlist" }
            }
        }
    }

    fun playPlaylist() {
        val title = (_state.value as? PlaylistScreenState.Data)?.playlist?.title ?: "Playlist"
        remotePlaybackController.requestCollectionPlay(RemoteCollectionType.Playlist, playlistId, title)
    }

    fun addPlaylistToQueue() {
        val title = (_state.value as? PlaylistScreenState.Data)?.playlist?.title ?: "Playlist"
        remotePlaybackController.requestCollectionAddToQueue(RemoteCollectionType.Playlist, playlistId, title)
    }

    fun playPlaylistNext() {
        val title = (_state.value as? PlaylistScreenState.Data)?.playlist?.title ?: "Playlist"
        remotePlaybackController.requestCollectionPlayNext(RemoteCollectionType.Playlist, playlistId, title)
    }

    fun playPlaylistFromTrack(track: MetadataTrack) {
        val title = (_state.value as? PlaylistScreenState.Data)?.playlist?.title ?: "Playlist"
        remotePlaybackController.requestCollectionPlay(
            type = RemoteCollectionType.Playlist,
            id = playlistId,
            title = title,
            startTrack = track,
        )
    }

    fun refresh() {
        viewModelScope.launch {
            repository.invalidateCaches()
            loadInitialData()
        }
    }

    fun updatePlaylist(
        name: String?,
        description: String?,
        isPublic: Boolean?,
        isCollaborating: Boolean?,
        imageBase64: String?,
    ) {
        viewModelScope.launch {
            runCatching {
                libraryRepository.updatePlaylist(
                    id = playlistId,
                    name = name,
                    description = description,
                    isPublic = isPublic,
                    isCollaborating = isCollaborating,
                    imageBase64 = imageBase64,
                    trackIds = null,
                )
            }.onFailure { e ->
                logger.e(e) { "Failed to update playlist" }
            }.onSuccess {
                repository.invalidateCaches()
                loadInitialData()
            }
        }
    }

    fun handleTrackOptionsAction(track: MetadataTrack, action: TrackOptionsAction) {
        viewModelScope.launch {
            when (action) {
                is TrackOptionsAction.StartRadio -> {}
                is TrackOptionsAction.PlayNext -> {
                    val queue = audioPlayerQueue.getQueue()
                    val existing = queue.find { entry ->
                        (entry as? QueueEntry.StreamingTrack)?.track?.matchesTrack(track) == true
                    }
                    if (existing != null) {
                        // Already in the local queue: move it to the next position
                        audioPlayerQueue.removeFromQueue(existing)
                        audioPlayerQueue.addAllAfterCurrent(listOf(QueueEntry.StreamingTrack(track = track, url = "")))
                    } else {
                        remotePlaybackController.requestTrackPlayNext(track)
                    }
                }

                is TrackOptionsAction.AddToQueue -> {
                    remotePlaybackController.requestTrackAddToQueue(track)
                }

                is TrackOptionsAction.AddToJam -> {
                    remotePlaybackController.addTrackToJam(track)
                }

                is TrackOptionsAction.RemoveFromQueue -> {
                    val queue = audioPlayerQueue.getQueue()
                    queue.find { entry ->
                        (entry as? QueueEntry.StreamingTrack)?.track?.matchesTrack(track) == true
                    }?.let { audioPlayerQueue.removeFromQueue(it) }
                }

                is TrackOptionsAction.ToggleFavorite -> {
                    val savedIds = savedTrackIds.value
                    if (savedIds.contains(track.id)) {
                        savedTracksRepository.removeSavedTracks(listOf(track.id))
                    } else {
                        savedTracksRepository.saveTracks(listOf(track.id))
                    }
                }

                is TrackOptionsAction.Download -> downloadManager.enqueue(track)
                is TrackOptionsAction.ToggleBlacklist -> toggleTrackBlacklist(track)
                is TrackOptionsAction.Share -> {
                    val uri = track.externalUri?.takeIf { it.isNotBlank() }
                    if (uri != null) {
                        shareService.share(uri, track.title)
                    }
                }
                is TrackOptionsAction.AddToPlaylist -> {
                    _tracksToAddToPlaylist.value = listOf(track)
                    _showAddToPlaylistPicker.value = true
                }
            }
        }
    }

    fun addTracksToPlaylist(playlistId: String) {
        viewModelScope.launch {
            runCatching {
                libraryRepository.addTracksToPlaylist(
                    playlistId,
                    _tracksToAddToPlaylist.value.map { it.id },
                )
            }.onFailure { e ->
                logger.e(e) { "Failed to add tracks to playlist" }
            }.onSuccess {
                _showAddToPlaylistPicker.value = false
            }
        }
    }

    fun showAddToPlaylistPicker(tracks: List<MetadataTrack>) {
        _tracksToAddToPlaylist.value = tracks
        _showAddToPlaylistPicker.value = true
    }

    fun dismissAddToPlaylistPicker() {
        _showAddToPlaylistPicker.value = false
    }

    fun downloadTracks(tracks: List<MetadataTrack>) {
        tracks.forEach { track -> downloadManager.enqueue(track) }
    }

    fun isTrackBlacklisted(track: MetadataTrack): Boolean {
        val trackIds = _blacklistedTrackIds.value
        val artistIds = _blacklistedArtistIds.value
        return track.id in trackIds || track.artists.any { it.id in artistIds }
    }

    fun toggleTrackBlacklist(track: MetadataTrack) {
        viewModelScope.launch {
            blacklistRepository.toggleTrack(track)
        }
    }

    fun addTracksToJam(tracks: List<MetadataTrack>) {
        remotePlaybackController.addTracksToJam(tracks)
    }

    fun addTracksToQueue(tracks: List<MetadataTrack>) {
        val title = (_state.value as? PlaylistScreenState.Data)?.playlist?.title ?: "Playlist"
        remotePlaybackController.requestTracksAddToQueue(tracks, title)
    }

    fun playTracksNext(tracks: List<MetadataTrack>) {
        val title = (_state.value as? PlaylistScreenState.Data)?.playlist?.title ?: "Playlist"
        remotePlaybackController.requestTracksPlayNext(tracks, title)
    }

    val savedTrackIds
        get() = savedTracksRepository.savedTracksIdsFlow
    private fun MetadataTrack.matchesTrack(other: MetadataTrack): Boolean {
        if (id.isNotBlank() && other.id.isNotBlank()) return id == other.id
        return title == other.title &&
                durationMs == other.durationMs &&
                album?.id == other.album?.id &&
                artists.map { it.id.ifBlank { it.name } } == other.artists.map { it.id.ifBlank { it.name } }
    }
}
