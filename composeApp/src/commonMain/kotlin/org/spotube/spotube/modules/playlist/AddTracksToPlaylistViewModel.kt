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

package org.spotube.spotube.modules.playlist

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.search.SearchRepository
import kotlin.coroutines.cancellation.CancellationException

data class AddTrackResultUi(
    val track: MetadataTrack,
    val isAdding: Boolean,
    val isAdded: Boolean
)

data class AddTracksToPlaylistUiState(
    val queryState: TextFieldState = TextFieldState(),
    val results: List<AddTrackResultUi> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

private data class AddTracksState(
    val results: List<MetadataTrack> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val addingTrackIds: Set<String> = emptySet(),
    val addedTrackIds: Set<String> = emptySet()
)

class AddTracksToPlaylistViewModel(
    private val playlistId: String,
    private val searchRepository: SearchRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {
    private val logger = Logger.withTag("AddTracksToPlaylistViewModel")
    private val queryState = TextFieldState()
    private val state = MutableStateFlow(AddTracksState())
    private var searchJob: Job? = null

    val uiState: StateFlow<AddTracksToPlaylistUiState> = state.map { current ->
        AddTracksToPlaylistUiState(
            queryState = queryState,
            results = current.results.map { track ->
                AddTrackResultUi(
                    track = track,
                    isAdding = track.id in current.addingTrackIds,
                    isAdded = track.id in current.addedTrackIds
                )
            },
            isSearching = current.isSearching,
            hasSearched = current.hasSearched
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AddTracksToPlaylistUiState(queryState = queryState)
    )

    fun search() {
        val query = queryState.text.toString()
        if (query.isBlank() || state.value.isSearching) return
        state.update { it.copy(isSearching = true, hasSearched = true) }
        searchJob = viewModelScope.launch {
            try {
                val results = searchRepository.searchTracks(query).items.map { it.data }
                state.update { it.copy(results = results) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                logger.e(e) { "Failed to search tracks for \"$query\"" }
                state.update { it.copy(results = emptyList()) }
            } finally {
                state.update { it.copy(isSearching = false) }
            }
        }
    }

    fun addTrack(trackId: String) {
        val current = state.value
        if (trackId in current.addingTrackIds || trackId in current.addedTrackIds) return
        state.update { it.copy(addingTrackIds = it.addingTrackIds + trackId) }
        viewModelScope.launch {
            try {
                libraryRepository.addTracksToPlaylist(playlistId, listOf(trackId))
                state.update { it.copy(addedTrackIds = it.addedTrackIds + trackId) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                logger.e(e) { "Failed to add track $trackId to playlist $playlistId" }
            } finally {
                state.update { it.copy(addingTrackIds = it.addingTrackIds - trackId) }
            }
        }
    }

    /** Clears the search so the dialog opens fresh next time; in-flight adds still complete. */
    fun reset() {
        searchJob?.cancel()
        queryState.clearText()
        state.update { AddTracksState() }
    }
}
