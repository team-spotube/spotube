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

package org.spotube.spotube.modules.library.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.modules.library.local_tracks.LibraryLocalTracksViewModel
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack
import org.spotube.spotube.modules.settings.SettingsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.Res
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.tasks.await
import io.github.vinceglb.filekit.writeText

sealed interface LocalPlaylistScreenState {
    data class Data(
        val playlists: List<LocalPlaylist> = emptyList(),
        val query: String = "",
        val selectedPlaylist: LocalPlaylist? = null,
        val showCreateDialog: Boolean = false,
        val showEditDialog: Boolean = false,
        val showAddTracksDialog: Boolean = false,
        val editingPlaylist: LocalPlaylist? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    ) : LocalPlaylistScreenState

    data object Loading : LocalPlaylistScreenState
    data class Error(val message: String) : LocalPlaylistScreenState
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocalPlaylistViewModel(
    private val repository: LocalPlaylistRepository,
    private val localTracksViewModel: LibraryLocalTracksViewModel,
    private val settingsProvider: SettingsProvider
) : ViewModel() {
    private val logger by injectLogger<LocalPlaylistViewModel>()
    
    private val _uiState = MutableStateFlow<LocalPlaylistScreenState>(LocalPlaylistScreenState.Loading)
    val uiState: StateFlow<LocalPlaylistScreenState> = _uiState.asStateFlow()
    
    private val _query = mutableStateOf("")
    var query: String
        get() = _query.value
        set(value) { _query.value = value }
    
    private val _showCreateDialog = mutableStateOf(false)
    var showCreateDialog: Boolean
        get() = _showCreateDialog.value
        set(value) { _showCreateDialog.value = value }
    
    private val _showEditDialog = mutableStateOf(false)
    var showEditDialog: Boolean
        get() = _showEditDialog.value
        set(value) { _showEditDialog.value = value }
    
    private val _showAddTracksDialog = mutableStateOf(false)
    var showAddTracksDialog: Boolean
        get() = _showAddTracksDialog.value
        set(value) { _showAddTracksDialog.value = value }
    
    private val _editingPlaylist = MutableStateFlow<LocalPlaylist?>(null)
    val editingPlaylist: StateFlow<LocalPlaylist?> = _editingPlaylist.asStateFlow()
    
    private val _showAddTracksForPlaylist = MutableStateFlow<LocalPlaylist?>(null)
    val showAddTracksForPlaylist: StateFlow<LocalPlaylist?> = _showAddTracksForPlaylist.asStateFlow()
    
    init {
        viewModelScope.launch {
            repository.playlists
                .combine(localTracksViewModel.state) { playlists, localTracksState ->
                    LocalPlaylistScreenState.Data(
                        playlists = playlists,
                        query = "",
                        selectedPlaylist = null
                    )
                }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    LocalPlaylistScreenState.Loading
                )
                .collect { state ->
                    _uiState.value = state
                }
        }
    }
    
    fun onQueryChange(query: String) {
        _uiState.update { state ->
            if (state is LocalPlaylistScreenState.Data) {
                state.copy(query = query)
            } else state
        }
    }
    
    fun showCreatePlaylistDialog() {
        _showCreateDialog.value = true
    }
    
    fun dismissCreatePlaylistDialog() {
        _showCreateDialog.value = false
    }
    
    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.createPlaylist(name, description)
                .onSuccess { playlist ->
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            state.copy(playlists = state.playlists + playlist, isLoading = false)
                        } else state
                    }
                    dismissCreatePlaylistDialog()
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            state.copy(isLoading = false, error = e.message)
                        } else LocalPlaylistScreenState.Error(e.message ?: getString(Res.string.common_unknown_error))
                    }
                }
        }
    }
    
    fun showEditPlaylistDialog(playlist: LocalPlaylist) {
        _editingPlaylist.value = playlist
        _showEditDialog.value = true
    }
    
    fun dismissEditPlaylistDialog() {
        _showEditDialog.value = false
        _editingPlaylist.value = null
    }
    
    fun updatePlaylist(playlist: LocalPlaylist) {
        viewModelScope.launch {
            repository.updatePlaylist(playlist)
                .onSuccess {
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            val updatedPlaylists = state.playlists.map { 
                                if (it.id == playlist.id) playlist else it 
                            }
                            state.copy(playlists = updatedPlaylists, editingPlaylist = null)
                        } else state
                    }
                    dismissEditPlaylistDialog()
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            state.copy(error = e.message)
                        } else state
                    }
                }
        }
    }
    
    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
                .onSuccess {
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            state.copy(playlists = state.playlists.filter { it.id != playlistId })
                        } else state
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            state.copy(error = e.message)
                        } else state
                    }
                }
        }
    }
    
    fun showAddTracksDialog(playlist: LocalPlaylist) {
        _showAddTracksForPlaylist.value = playlist
        _showAddTracksDialog.value = true
    }
    
    fun dismissAddTracksDialog() {
        _showAddTracksDialog.value = false
        _showAddTracksForPlaylist.value = null
    }
    
    fun addTracks(playlist: LocalPlaylist, tracks: List<LocalMediaTrack>) {
        viewModelScope.launch {
            repository.addTracks(playlist.id, tracks)
                .onSuccess {
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            val updatedPlaylists = state.playlists.map { 
                                if (it.id == playlist.id) 
                                    it.copy(trackPaths = it.trackPaths + tracks.map { it.path }.filterNot { it in it.trackPaths })
                                else it 
                            }
                            state.copy(playlists = updatedPlaylists)
                        } else state
                    }
                    dismissAddTracksDialog()
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            state.copy(error = e.message)
                        } else state
                    }
                }
        }
    }
    
    fun removeTrack(playlistId: String, trackPath: String) {
        viewModelScope.launch {
            repository.removeTrack(playlistId, trackPath)
                .onSuccess {
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            val updatedPlaylists = state.playlists.map { 
                                if (it.id == playlistId) 
                                    it.copy(trackPaths = it.trackPaths.filter { it != trackPath })
                                else it 
                            }
                            state.copy(playlists = updatedPlaylists)
                        } else state
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            state.copy(error = e.message)
                        } else state
                    }
                }
        }
    }
    
    fun reorderTracks(playlistId: String, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.reorderTracks(playlistId, fromIndex, toIndex)
                .onSuccess {
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            val updatedPlaylists = state.playlists.map { 
                                if (it.id == playlistId) {
                                    val newTrackPaths = it.trackPaths.toMutableList()
                                    val trackPath = newTrackPaths.removeAt(fromIndex)
                                    newTrackPaths.add(toIndex, trackPath)
                                    it.copy(trackPaths = newTrackPaths)
                                } else it 
                            }
                            state.copy(playlists = updatedPlaylists)
                        } else state
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        if (state is LocalPlaylistScreenState.Data) {
                            state.copy(error = e.message)
                        } else state
                    }
                }
        }
    }
}