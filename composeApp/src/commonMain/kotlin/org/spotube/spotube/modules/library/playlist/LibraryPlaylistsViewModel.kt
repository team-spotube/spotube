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
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.LibraryTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.github.vinceglb.filekit.PlatformFile
import org.koin.core.component.KoinComponent
import org.spotube.spotube.core.i18n.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_unknown_error

sealed interface LibraryPlaylistsState {
    data object Loading : LibraryPlaylistsState

    sealed interface Data : LibraryPlaylistsState {
        val query: String
        val allItems: List<MetadataPlaylist>
        val items: List<MetadataPlaylist>
        val nextPagination: PaginationStrategy?

        data class Loaded(
            override val query: String = "",
            override val allItems: List<MetadataPlaylist> = emptyList(),
            override val nextPagination: PaginationStrategy? = null
        ) : Data {
            override val items: List<MetadataPlaylist> = if (query.isNotBlank()) {
                allItems.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            (it.description?.contains(query, ignoreCase = true) == true)
                }
            } else {
                allItems
            }

            fun toLoadingMore(): LoadingMore = LoadingMore(
                query = query,
                allItems = allItems,
                nextPagination = nextPagination
            )
        }

        data class LoadingMore(
            override val query: String = "",
            override val allItems: List<MetadataPlaylist> = emptyList(),
            override val nextPagination: PaginationStrategy? = null
        ) : Data {
            override val items: List<MetadataPlaylist> = if (query.isNotBlank()) {
                allItems.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            (it.description?.contains(query, ignoreCase = true) == true)
                }
            } else {
                allItems
            }
        }
    }

    data class Error(val message: String) : LibraryPlaylistsState
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryPlaylistsViewModel(
    private val repository: LibraryRepository,
    private val libraryState: LibraryState
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<LibraryPlaylistsViewModel>()

    private val _state = MutableStateFlow<LibraryPlaylistsState>(LibraryPlaylistsState.Loading)
    val uiState: StateFlow<LibraryPlaylistsState> = combine(
        _state, libraryState.queryFlow(LibraryTab.Playlists)
    ) { state, query ->
        when (state) {
            is LibraryPlaylistsState.Data.Loaded -> state.copy(query = query)
            is LibraryPlaylistsState.Data.LoadingMore -> state.copy(query = query)
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryPlaylistsState.Loading)

    init {
        viewModelScope.launch {
            combine(
                repository.pluginManager.selectedMetadataPlugin
                    .filterNotNull()
                    .flatMapLatest { it.loggedInFlow }
                    .distinctUntilChanged(),
                repository.savedPlaylistIdsFlow
            ) { _, _ -> }.collect {
                loadInitialData()
            }
        }
    }

    private suspend fun loadInitialData() = runCatching {
        _state.value = LibraryPlaylistsState.Loading
        val result = repository.savedPlaylists()
        _state.value = LibraryPlaylistsState.Data.Loaded(
            allItems = result?.items ?: emptyList(),
            nextPagination = result?.nextPagination
        )
    }.onFailure { e ->
        logger.e(e) { "Failed to load playlists" }
        _state.value = LibraryPlaylistsState.Error(e.message ?: getString(Res.string.common_unknown_error))
    }

    suspend fun loadMoreData() = runCatching {
        val currentState = _state.value
        if (currentState is LibraryPlaylistsState.Data.Loaded && currentState.nextPagination != null) {
            _state.value = currentState.toLoadingMore()
            val result = repository.savedPlaylists(currentState.nextPagination)
            _state.value = LibraryPlaylistsState.Data.Loaded(
                query = currentState.query,
                allItems = currentState.allItems + (result?.items ?: emptyList()),
                nextPagination = result?.nextPagination
            )
        }
    }.onFailure { e ->
        logger.e(e) { "Failed to load more playlists" }
        _state.value = LibraryPlaylistsState.Error(e.message ?: getString(Res.string.common_unknown_error))
    }

    fun onQueryChange(query: String) {
        libraryState.setQuery(LibraryTab.Playlists, query)
    }

    fun onItemsVisible(lastVisibleIndex: Int?, totalItems: Int) {
        if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 4) {
            viewModelScope.launch { loadMoreData() }
        }
    }

    private val playlistForm = PlaylistFormController(viewModelScope, logger)
    val playlistFormState: StateFlow<PlaylistFormUiState> = playlistForm.uiState

    fun onPlaylistFormImagePicked(file: PlatformFile) = playlistForm.onImagePicked(file)

    fun dismissCreatePlaylist() {
        if (playlistFormState.value.isSubmitting) return
        playlistForm.dismiss()
        libraryState.dismissCreatePlaylist()
    }

    fun createPlaylist(data: PlaylistFormData) {
        playlistForm.submit(onSuccess = libraryState::dismissCreatePlaylist) {
            repository.createPlaylist(
                name = data.name,
                description = data.description.ifBlank { null },
                isPublic = data.isPublic,
                isCollaborating = data.isCollaborating,
                imageBase64 = data.imageBase64,
                trackIds = emptyList()
            )
            repository.invalidateCaches()
            loadInitialData()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.invalidateCaches()
            loadInitialData()
        }
    }
}
