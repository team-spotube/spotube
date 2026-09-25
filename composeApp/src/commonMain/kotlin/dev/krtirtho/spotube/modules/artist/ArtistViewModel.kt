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

package dev.krtirtho.spotube.modules.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.di.injectLogger
import dev.krtirtho.spotube.core.remote.RemoteCollectionType
import dev.krtirtho.spotube.core.remote.RemotePlaybackController
import dev.krtirtho.spotube.core.share.ShareService
import dev.krtirtho.spotube.core.ui.component.TrackOptionsAction
import dev.krtirtho.spotube.core.ui.component.TrackOptionsContext
import dev.krtirtho.spotube.modules.blacklist.BlacklistRepository
import dev.krtirtho.spotube.modules.downloads.DownloadManager
import dev.krtirtho.spotube.modules.library.LibraryRepository
import dev.krtirtho.spotube.modules.saved_tracks.SavedTracksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

sealed interface ArtistScreenState {
    data object Loading : ArtistScreenState

    data class Loaded(
        val artist: MetadataArtist.Detailed,
        val isArtistSaved: Boolean,
        val topTracks: List<MetadataTrack>,
        val albums: List<MetadataAlbum.Detailed>,
        val albumsNextPagination: PaginationStrategy?,
        val relatedArtists: List<MetadataArtist.Basic>,
        val relatedArtistsNextPagination: PaginationStrategy?,
        val featuredPlaylists: List<MetadataPlaylist>,
        val featuredPlaylistsNextPagination: PaginationStrategy?,
    ) : ArtistScreenState

    data class Error(val message: String) : ArtistScreenState
}

@OptIn(ExperimentalStdlibApi::class)
class ArtistViewModel(
    private val artistId: String,
    private val repository: ArtistRepository,
    private val libraryRepository: LibraryRepository,
    private val savedTracksRepository: SavedTracksRepository,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val blacklistRepository: BlacklistRepository,
    private val shareService: ShareService,
    private val downloadManager: DownloadManager,
    private val remotePlaybackController: RemotePlaybackController,
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<ArtistViewModel>()

    private val _state = MutableStateFlow<ArtistScreenState>(ArtistScreenState.Loading)
    val state: StateFlow<ArtistScreenState> = _state.asStateFlow()

    val savedArtistIds
        get() = libraryRepository.savedArtistIdsFlow

    val savedTrackIds
        get() = savedTracksRepository.savedTracksIdsFlow

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
            libraryRepository.savedArtistIdsFlow.collect {
                val currentState = _state.value
                if (currentState is ArtistScreenState.Loaded) {
                    _state.value = currentState.copy(isArtistSaved = it.contains(artistId))
                }
            }
        }
        blacklistRepository.blacklistedTracks
            .onEach { tracks -> _blacklistedTrackIds.value = tracks.map { it.id }.toSet() }
            .launchIn(viewModelScope)
        blacklistRepository.blacklistedArtists
            .onEach { artists -> _blacklistedArtistIds.value = artists.map { it.id }.toSet() }
            .launchIn(viewModelScope)
        loadCurrentUser()
        loadOverview()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            runCatching {
                _currentUserId.value = libraryRepository.currentUser()?.id
            }.onFailure { e ->
                logger.e(e) { "Failed to load current user" }
            }
        }
    }

    fun refresh() {
        repository.invalidateCaches()
        loadOverview()
    }

    fun toggleSavedArtist() {
        viewModelScope.launch {
            val isLiked = libraryRepository.isSavedArtists(listOf(artistId)).firstOrNull() ?: false
            if (isLiked) {
                libraryRepository.removeSavedArtists(listOf(artistId))
            } else {
                libraryRepository.saveArtists(listOf(artistId))
            }
        }
    }

    fun isArtistBlacklisted(artistId: String): Boolean {
        return _blacklistedArtistIds.value.contains(artistId)
    }

    fun toggleArtistBlacklist(artist: MetadataArtist) {
        viewModelScope.launch {
            blacklistRepository.toggleArtist(artist)
        }
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

    fun loadMoreAlbums() {
        val currentState = _state.value
        if (currentState !is ArtistScreenState.Loaded) return
        val pagination = currentState.albumsNextPagination ?: return

        viewModelScope.launch {
            runCatching {
                repository.albums(artistId, pagination)
            }.onSuccess { result ->
                if (result != null) {
                    _state.value = currentState.copy(
                        albums = currentState.albums + result.items,
                        albumsNextPagination = result.nextPagination,
                    )
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to load more albums" }
            }
        }
    }

    fun loadMoreRelatedArtists() {
        val currentState = _state.value
        if (currentState !is ArtistScreenState.Loaded) return
        val pagination = currentState.relatedArtistsNextPagination ?: return

        viewModelScope.launch {
            runCatching {
                repository.relatedArtists(artistId, pagination)
            }.onSuccess { result ->
                if (result != null) {
                    _state.value = currentState.copy(
                        relatedArtists = currentState.relatedArtists + result.items,
                        relatedArtistsNextPagination = result.nextPagination,
                    )
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to load more related artists" }
            }
        }
    }

    fun loadMoreFeaturedPlaylists() {
        val currentState = _state.value
        if (currentState !is ArtistScreenState.Loaded) return
        val pagination = currentState.featuredPlaylistsNextPagination ?: return

        viewModelScope.launch {
            runCatching {
                repository.featuredPlaylists(artistId, pagination)
            }.onSuccess { result ->
                if (result != null) {
                    _state.value = currentState.copy(
                        featuredPlaylists = currentState.featuredPlaylists + result.items,
                        featuredPlaylistsNextPagination = result.nextPagination,
                    )
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to load more featured playlists" }
            }
        }
    }

    fun addTopTracksToQueue() {
        val artistName = (_state.value as? ArtistScreenState.Loaded)?.artist?.name ?: "Artist"
        remotePlaybackController.requestCollectionAddToQueue(
            RemoteCollectionType.ArtistTopTracks,
            artistId,
            artistName,
        )
    }

    fun playTopTracks() {
        val artistName = (_state.value as? ArtistScreenState.Loaded)?.artist?.name ?: "Artist"
        remotePlaybackController.requestCollectionPlay(
            RemoteCollectionType.ArtistTopTracks,
            artistId,
            artistName,
        )
    }

    fun playTopTracksFromTrack(track: MetadataTrack) {
        val artistName = (_state.value as? ArtistScreenState.Loaded)?.artist?.name ?: "Artist"
        remotePlaybackController.requestCollectionPlay(
            type = RemoteCollectionType.ArtistTopTracks,
            id = artistId,
            title = artistName,
            startTrack = track,
        )
    }
    fun addTracksToJam(tracks: List<MetadataTrack>) {
        remotePlaybackController.addTracksToJam(tracks)
    }

    fun addTracksToQueue(tracks: List<MetadataTrack>) {
        val artistName = (_state.value as? ArtistScreenState.Loaded)?.artist?.name ?: "Artist"
        remotePlaybackController.requestTracksAddToQueue(tracks, artistName)
    }

    fun playTracksNext(tracks: List<MetadataTrack>) {
        val artistName = (_state.value as? ArtistScreenState.Loaded)?.artist?.name ?: "Artist"
        remotePlaybackController.requestTracksPlayNext(tracks, artistName)
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

    private fun loadOverview() {
        _state.value = ArtistScreenState.Loading
        viewModelScope.launch {
            runCatching {
                repository.artistOverview(artistId)
            }.onSuccess { overview ->
                if (overview != null) {
                    savedTracksRepository.isSavedTracks(overview.top10Tracks.map { it.id })
                    _state.value = ArtistScreenState.Loaded(
                        artist = overview.artist,
                        isArtistSaved = libraryRepository.savedArtistIdsFlow.value.contains(artistId),
                        topTracks = overview.top10Tracks,
                        albums = overview.albums.items,
                        albumsNextPagination = overview.albums.nextPagination,
                        relatedArtists = overview.relatedArtists.items,
                        relatedArtistsNextPagination = overview.relatedArtists.nextPagination,
                        featuredPlaylists = overview.featuredPlaylists.items,
                        featuredPlaylistsNextPagination = overview.featuredPlaylists.nextPagination,
                    )
                } else {
                    _state.value = ArtistScreenState.Error("Failed to load artist overview")
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to load artist overview" }
                _state.value = ArtistScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun resolveTopTrackEntries(): List<QueueEntry> {
        val currentState = _state.value
        if (currentState !is ArtistScreenState.Loaded) return emptyList()
        
        val blacklistedTracks = blacklistRepository.getTracksSnapshot()
        val blacklistedArtists = blacklistRepository.getArtistsSnapshot()
        val blacklistedTrackIds = blacklistedTracks.map { it.id }.toSet()
        val blacklistedArtistIds = blacklistedArtists.map { it.id }.toSet()
        
        return currentState.topTracks
            .filter { track ->
                track.id !in blacklistedTrackIds && 
                track.artists.none { it.id in blacklistedArtistIds }
            }
            .map { track ->
                QueueEntry.StreamingTrack(track = track, url = "")
            }
    }

    private fun MetadataTrack.matchesTrack(other: MetadataTrack): Boolean {
        if (id.isNotBlank() && other.id.isNotBlank()) {
            return id == other.id
        }
        return title == other.title &&
                durationMs == other.durationMs &&
                album?.id == other.album?.id &&
                artists.map { it.id.ifBlank { it.name } } == other.artists.map { it.id.ifBlank { it.name } }
    }
}
