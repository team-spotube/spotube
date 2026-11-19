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

package org.spotube.spotube.modules.library.local.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.core.i18n.getString
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.modules.library.local.LocalMediaCollectionsRepository
import org.spotube.spotube.modules.library.local.LocalPlaylist
import org.spotube.spotube.modules.library.local.LocalPlaylistRepository
import org.spotube.spotube.modules.library.local.reencodeLocalCoverBase64
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack
import org.spotube.spotube.modules.library.playlist.PlaylistFormController
import org.spotube.spotube.modules.library.playlist.PlaylistFormData
import org.spotube.spotube.modules.library.playlist.PlaylistFormUiState
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.media_unknown_artist

/** One resolved track of a local playlist, in playlist order; missing paths are skipped upstream. */
data class LocalPlaylistTrackUi(
    val path: String,
    val title: String,
    val subtitle: String?,
    val durationLabel: String?,
    val artwork: ByteArray?
)

/** One row of the add-tracks picker (the same mapping as [LocalPlaylistTrackUi], minus duration). */
data class LocalTrackPickerTrackUi(
    val path: String,
    val title: String,
    val subtitle: String?,
    val artwork: ByteArray?
)

/** State of the add-tracks sheet; the query and the selection live here, never in the composable. */
data class LocalTrackPickerUiState(
    val visible: Boolean = false,
    val query: String = "",
    val allTracks: List<LocalTrackPickerTrackUi> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val isSubmitting: Boolean = false
)

sealed interface LocalPlaylistUiState {
    data object Loading : LocalPlaylistUiState

    data object NotFound : LocalPlaylistUiState

    data class Loaded(
        val playlist: LocalPlaylist,
        val tracks: List<LocalPlaylistTrackUi>,
        val isPlaying: Boolean,
        val showEditForm: Boolean = false,
        val showAddTracks: Boolean = false,
        val showDeleteConfirm: Boolean = false,
        val editFormState: PlaylistFormUiState = PlaylistFormUiState(),
        val addTracksState: LocalTrackPickerUiState = LocalTrackPickerUiState()
    ) : LocalPlaylistUiState
}

/** Shared row mapping: artists joined " • " with the album appended, falling back to an unknown-artist label. */
private suspend fun localPlaylistTrackUi(track: LocalMediaTrack): LocalPlaylistTrackUi {
    val artistLine = track.artists.filter { it.isNotBlank() }.joinToString(" • ")
        .ifBlank { getString(Res.string.media_unknown_artist) }
    val subtitle = listOfNotNull(
        artistLine.takeIf { it.isNotBlank() },
        track.album?.takeIf { it.isNotBlank() }
    ).joinToString(" • ").takeIf { it.isNotBlank() }
    val seconds = track.durationMs / 1000
    return LocalPlaylistTrackUi(
        path = track.path,
        title = track.name,
        subtitle = subtitle,
        durationLabel = if (seconds > 0) "${seconds / 60}:${
            (seconds % 60).toString().padStart(2, '0')
        }" else null,
        artwork = track.coverBytes?.takeIf { it.isNotEmpty() })
}

private suspend fun pickerTrackUi(track: LocalMediaTrack): LocalTrackPickerTrackUi {
    val info = localPlaylistTrackUi(track)
    return LocalTrackPickerTrackUi(info.path, info.title, info.subtitle, info.artwork)
}

/**
 * Owns one user-created playlist ([org.spotube.spotube.core.navigation.Routes.LocalPlaylist]):
 * the resolved track list, playback, the edit sheet, the add-tracks picker and deletion.
 * Tracks are re-resolved from the local media index on every change, so deleted files simply
 * disappear from the list.
 */
class LocalPlaylistViewModel(
    private val playlistId: String,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val collectionsRepository: LocalMediaCollectionsRepository,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val navigationCommands: NavigationCommands
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<LocalPlaylistViewModel>()

    /** Dialog/sheet flags plus the add-tracks picker state; merged into [uiState]. */
    private data class TransientState(
        val showDeleteConfirm: Boolean = false,
        val addTracks: LocalTrackPickerUiState = LocalTrackPickerUiState()
    )

    private val transient = MutableStateFlow(TransientState())

    private val editForm = PlaylistFormController(viewModelScope, logger)

    val uiState: StateFlow<LocalPlaylistUiState> = combine(
        localPlaylistRepository.playlistsFlow,
        collectionsRepository.tracks,
        audioPlayerQueue.currentCollectionEntryFlow,
        editForm.uiState,
        transient
    ) { playlists, mediaTracks, collectionEntry, formState, t ->
        val playlist = playlists.firstOrNull { it.id == playlistId }
            ?: return@combine LocalPlaylistUiState.NotFound
        val tracksByPath = HashMap<String, LocalMediaTrack>(mediaTracks.size)
        mediaTracks.forEach { tracksByPath[it.path] = it }
        LocalPlaylistUiState.Loaded(
            playlist = playlist,
            tracks = playlist.trackPaths.mapNotNull { tracksByPath[it] }
                .map { localPlaylistTrackUi(it) },
            isPlaying = collectionEntry is QueueCollectionEntry.Playlist && collectionEntry.id == playlistId,
            showEditForm = formState.isVisible,
            showAddTracks = t.addTracks.visible,
            showDeleteConfirm = t.showDeleteConfirm,
            editFormState = formState,
            addTracksState = if (t.addTracks.visible) {
                val inPlaylist = playlist.trackPaths.toHashSet()
                val query = t.addTracks.query.trim()
                // Only tracks that can actually be added; the picker list is rebuilt while open.
                val candidates = mediaTracks
                    .filterNot { it.path in inPlaylist }
                    .map { pickerTrackUi(it) }
                t.addTracks.copy(
                    allTracks = if (query.isBlank()) candidates else candidates.filter {
                        it.title.contains(query, true) ||
                            (it.subtitle?.contains(query, true) == true)
                    })
            } else {
                t.addTracks
            })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalPlaylistUiState.Loading)

    // ---------- Playback ----------

    /** The playlist's tracks in user order, resolved from the current index snapshot. */
    private fun resolveTracks(playlist: LocalPlaylist): List<LocalMediaTrack> {
        val tracksByPath = collectionsRepository.tracks.value.associateBy { it.path }
        return playlist.trackPaths.mapNotNull { tracksByPath[it] }
    }

    private fun LocalMediaTrack.toQueueEntry() = QueueEntry.LocalTrack(
        name = name,
        artists = artists,
        duration = durationMs,
        album = album,
        coverBytes = coverBytes,
        url = path
    )

    private fun playTracks(tracks: List<LocalMediaTrack>, startPosition: Int) {
        if (tracks.isEmpty()) return
        val entries = tracks.map { it.toQueueEntry() }
        viewModelScope.launch {
            audioPlayerQueue.load(
                entries = entries,
                autoPlay = true,
                startPosition = startPosition,
                collectionEntry = QueueCollectionEntry.Playlist(playlistId)
            )
        }
    }

    fun play() {
        if (audioPlayerQueue.isPlaylistPlaying(playlistId)) return
        val state = uiState.value as? LocalPlaylistUiState.Loaded ?: return
        playTracks(resolveTracks(state.playlist), 0)
    }

    fun playFrom(trackPath: String) {
        val state = uiState.value as? LocalPlaylistUiState.Loaded ?: return
        val tracks = resolveTracks(state.playlist)
        val index = tracks.indexOfFirst { it.path == trackPath }
        if (index >= 0) playTracks(tracks, index)
    }

    fun shufflePlay() {
        val state = uiState.value as? LocalPlaylistUiState.Loaded ?: return
        playTracks(resolveTracks(state.playlist).shuffled(), 0)
    }

    fun addToQueue() {
        val state = uiState.value as? LocalPlaylistUiState.Loaded ?: return
        val entries = resolveTracks(state.playlist).map { it.toQueueEntry() }
        if (entries.isEmpty()) return
        viewModelScope.launch { audioPlayerQueue.addAllToQueue(entries) }
    }

    fun removeTrack(trackPath: String) {
        viewModelScope.launch { localPlaylistRepository.removeTrack(playlistId, trackPath) }
    }

    // ---------- Edit form ----------

    fun showEdit() = editForm.show()

    fun dismissEdit() = editForm.dismiss()

    fun onEditImagePicked(file: PlatformFile) = editForm.onImagePicked(file)

    fun updatePlaylist(data: PlaylistFormData) {
        editForm.submit {
            localPlaylistRepository.rename(
                id = playlistId,
                name = data.name,
                description = data.description.ifBlank { null },
                // null keeps the current cover; the edit form's initial data already carries it,
                // so this only re-encodes a newly picked image.
                coverBase64 = if (data.imageBase64.isBlank()) null
                else reencodeLocalCoverBase64(data.imageBase64)
            )
        }
    }

    // ---------- Delete ----------

    /** Opens the "delete playlist?" confirmation dialog. */
    fun confirmDelete() {
        transient.update { it.copy(showDeleteConfirm = true) }
    }

    fun cancelDelete() {
        transient.update { it.copy(showDeleteConfirm = false) }
    }

    /** Actually deletes the playlist and leaves the screen. */
    fun deletePlaylist() {
        transient.update { it.copy(showDeleteConfirm = false) }
        viewModelScope.launch {
            localPlaylistRepository.delete(playlistId)
            navigationCommands.pop()
        }
    }

    // ---------- Add-tracks picker ----------

    fun showAddTracks() {
        transient.update {
            it.copy(addTracks = LocalTrackPickerUiState(visible = true))
        }
    }

    fun dismissAddTracks() {
        if (transient.value.addTracks.isSubmitting) return
        transient.update { it.copy(addTracks = LocalTrackPickerUiState()) }
    }

    fun onAddTracksQueryChange(query: String) {
        transient.update {
            if (!it.addTracks.visible) it
            else it.copy(addTracks = it.addTracks.copy(query = query))
        }
    }

    fun toggleAddTrack(path: String) {
        transient.update {
            if (!it.addTracks.visible) it
            else {
                val selected = it.addTracks.selectedPaths
                it.copy(
                    addTracks = it.addTracks.copy(
                        selectedPaths = if (path in selected) selected - path else selected + path
                    )
                )
            }
        }
    }

    fun addSelectedTracks() {
        val selected = transient.value.addTracks.selectedPaths
        if (selected.isEmpty() || transient.value.addTracks.isSubmitting) return
        transient.update { it.copy(addTracks = it.addTracks.copy(isSubmitting = true)) }
        viewModelScope.launch {
            runCatching { localPlaylistRepository.addTracks(playlistId, selected.toList()) }
                .onSuccess {
                    // addTracks dedupes, so the sheet closes with a clean slate either way.
                    transient.update { it.copy(addTracks = LocalTrackPickerUiState()) }
                }
                .onFailure { e ->
                    logger.e(e) { "Failed to add tracks to local playlist" }
                    transient.update {
                        it.copy(addTracks = it.addTracks.copy(isSubmitting = false))
                    }
                }
        }
    }
}
