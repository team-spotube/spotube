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

package dev.krtirtho.spotube.modules.saved_tracks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
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
import dev.krtirtho.spotube.modules.blacklist.BlacklistRepository
import dev.krtirtho.spotube.modules.downloads.DownloadManager
import dev.krtirtho.spotube.modules.library.LibraryRepository
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
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent

const val SAVED_TRACKS_COLLECTION_ID = "saved_tracks"

sealed interface SavedTracksScreenState {
    data object Loading : SavedTracksScreenState

    sealed interface Data : SavedTracksScreenState {
        val tracks: List<MetadataTrack>
        val nextPagination: PaginationStrategy?
        val totalCount: Int

        data class Loaded(
            override val tracks: List<MetadataTrack> = emptyList(),
            override val nextPagination: PaginationStrategy? = null,
            override val totalCount: Int = 0,
        ) : Data {
            fun toLoadingMore(): LoadingMore = LoadingMore(
                tracks = tracks,
                nextPagination = nextPagination,
                totalCount = totalCount,
            )
        }

        data class LoadingMore(
            override val tracks: List<MetadataTrack> = emptyList(),
            override val nextPagination: PaginationStrategy? = null,
            override val totalCount: Int = 0,
        ) : Data
    }

    data class Error(val message: String) : SavedTracksScreenState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SavedTracksViewModel(
    private val repository: SavedTracksRepository,
    private val playbackHelper: CollectionPlaybackHelper,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val blacklistRepository: BlacklistRepository,
    private val libraryRepository: LibraryRepository,
    private val shareService: ShareService,
    private val downloadManager: DownloadManager,
    private val remotePlaybackController: RemotePlaybackController,
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<SavedTracksViewModel>()

    private val _state = MutableStateFlow<SavedTracksScreenState>(SavedTracksScreenState.Loading)
    val uiState: StateFlow<SavedTracksScreenState> = _state.asStateFlow()
    val savedTrackIdsFlow: StateFlow<Set<String>>
        get() = repository.savedTracksIdsFlow

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
        _blacklistedTrackIds,
        _blacklistedArtistIds,
    ) { queue, currentEntry, blacklistedTracks, blacklistedArtists ->
        TrackOptionsContext(
            currentTrackId = (currentEntry as? QueueEntry.StreamingTrack)?.track?.id,
            queueTrackIds = queue.mapNotNull { entry ->
                (entry as? QueueEntry.StreamingTrack)?.track?.id
            }.toSet(),
            blacklistedTrackIds = blacklistedTracks,
            blacklistedArtistIds = blacklistedArtists,
            forceFavorite = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackOptionsContext(forceFavorite = true))

    init {
        viewModelScope.launch {
            combine(
                repository.pluginManager.selectedMetadataPlugin
                    .filterNotNull()
                    .flatMapLatest { it.loggedInFlow }
                    .distinctUntilChanged(),
                repository.savedTracksIdsFlow
            ) { _, _ -> }.collect {
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

    private suspend fun loadInitialData() {
        runCatching {
            val tracksResult = repository.getSavedTracks()
            val totalCount = repository.getSavedTracksCount() ?: 0
            _state.value = SavedTracksScreenState.Data.Loaded(
                tracks = tracksResult?.items ?: emptyList(),
                nextPagination = tracksResult?.nextPagination,
                totalCount = totalCount,
            )
        }.onFailure { e ->
            logger.e(e) { "Failed to load saved tracks" }
            _state.value = SavedTracksScreenState.Error(e.message ?: "Unknown error")
        }
    }

    fun loadNextTracksPage() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState is SavedTracksScreenState.Data.Loaded && currentState.nextPagination != null) {
                _state.value = currentState.toLoadingMore()
                runCatching {
                    val result = repository.getSavedTracks(currentState.nextPagination)
                    _state.value = SavedTracksScreenState.Data.Loaded(
                        tracks = currentState.tracks + (result?.items ?: emptyList()),
                        nextPagination = result?.nextPagination,
                        totalCount = currentState.totalCount,
                    )
                }.onFailure { e ->
                    logger.e(e) { "Failed to load more tracks" }
                    _state.value = SavedTracksScreenState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun playSavedTracks() {
        remotePlaybackController.requestCollectionPlay(RemoteCollectionType.SavedTracks, SAVED_TRACKS_COLLECTION_ID, "Saved Tracks")
    }

    fun addSavedTracksToQueue() {
        remotePlaybackController.requestCollectionAddToQueue(RemoteCollectionType.SavedTracks, SAVED_TRACKS_COLLECTION_ID, "Saved Tracks")
    }

    fun playSavedTracksFromTrack(track: MetadataTrack) {
        remotePlaybackController.requestCollectionPlay(
            type = RemoteCollectionType.SavedTracks,
            id = SAVED_TRACKS_COLLECTION_ID,
            title = "Saved Tracks",
            startTrack = track,
        )
    }

    fun refresh() {
        viewModelScope.launch {
            repository.invalidateCaches()
            loadInitialData()
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

                is TrackOptionsAction.ToggleFavorite -> {}
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
        remotePlaybackController.requestTracksAddToQueue(tracks, "Saved Tracks")
    }

    fun playTracksNext(tracks: List<MetadataTrack>) {
        remotePlaybackController.requestTracksPlayNext(tracks, "Saved Tracks")
    }

    suspend fun isSavedTracks(trackIds: List<String>): List<Boolean> {
        return repository.isSavedTracks(trackIds)
    }

    suspend fun saveTracks(trackIds: List<String>) {
        repository.saveTracks(trackIds)
    }

    suspend fun removeSavedTracks(trackIds: List<String>) {
        repository.removeSavedTracks(trackIds)
    }

    private fun MetadataTrack.matchesTrack(other: MetadataTrack): Boolean {
        if (id.isNotBlank() && other.id.isNotBlank()) return id == other.id
        return title == other.title &&
                durationMs == other.durationMs &&
                album?.id == other.album?.id &&
                artists.map { it.id.ifBlank { it.name } } == other.artists.map { it.id.ifBlank { it.name } }
    }
}


sealed interface SavedState<out T> {
    data class Success<T>(val data: T) : SavedState<T>
    data object Loading : SavedState<Nothing>
    data class Error(val message: String) : SavedState<Nothing>
}

@Composable
fun rememberIsSavedTracks(
    trackIds: List<String>,
    repository: SavedTracksRepository = koinInject(),
): SavedState<List<Boolean>> {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<SavedState<List<Boolean>>>(SavedState.Loading) }

    LaunchedEffect(trackIds) {
        scope.launch {
            state = SavedState.Loading
            runCatching {
                val result = repository.isSavedTracks(trackIds)
                state = SavedState.Success(result)
            }.onFailure { e ->
                state = SavedState.Error(e.message ?: "Unknown error")
            }
        }
    }

    return state
}