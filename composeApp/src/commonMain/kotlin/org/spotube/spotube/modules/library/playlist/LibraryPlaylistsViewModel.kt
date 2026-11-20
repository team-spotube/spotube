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
import org.spotube.spotube.modules.library.LibrarySourceFilter
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.LibraryTab
import org.spotube.spotube.modules.library.local.LocalPlaylist
import org.spotube.spotube.modules.library.local.LocalPlaylistRepository
import org.spotube.spotube.modules.library.local.reencodeLocalCoverBase64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.github.vinceglb.filekit.PlatformFile
import org.koin.core.component.KoinComponent
import org.spotube.spotube.core.i18n.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_unknown_error

/** One row of the merged Playlists tab: a plugin (online) playlist or a user-created local one. */
sealed interface LibraryPlaylistEntry {
    val id: String
    val title: String

    data class Online(val playlist: MetadataPlaylist) : LibraryPlaylistEntry {
        override val id: String get() = playlist.id
        override val title: String get() = playlist.title
    }

    data class Local(val playlist: LocalPlaylist) : LibraryPlaylistEntry {
        override val id: String get() = playlist.id
        override val title: String get() = playlist.name
    }
}

sealed interface LibraryPlaylistsState {
    data object Loading : LibraryPlaylistsState

    sealed interface Data : LibraryPlaylistsState {
        val query: String
        val onlineItems: List<MetadataPlaylist>
        val localItems: List<LocalPlaylist>
        val sourceFilter: LibrarySourceFilter
        val onlineAvailable: Boolean
        val nextPagination: PaginationStrategy?
        val items: List<LibraryPlaylistEntry>

        data class Loaded(
            override val query: String = "",
            override val onlineItems: List<MetadataPlaylist> = emptyList(),
            override val localItems: List<LocalPlaylist> = emptyList(),
            override val sourceFilter: LibrarySourceFilter = LibrarySourceFilter.All,
            override val onlineAvailable: Boolean = true,
            override val nextPagination: PaginationStrategy? = null
        ) : Data {
            override val items: List<LibraryPlaylistEntry> = mergedPlaylistItems(
                onlineItems, localItems, sourceFilter, onlineAvailable, query
            )

            fun toLoadingMore(): LoadingMore = LoadingMore(
                query = query,
                onlineItems = onlineItems,
                localItems = localItems,
                sourceFilter = sourceFilter,
                onlineAvailable = onlineAvailable,
                nextPagination = nextPagination
            )
        }

        data class LoadingMore(
            override val query: String = "",
            override val onlineItems: List<MetadataPlaylist> = emptyList(),
            override val localItems: List<LocalPlaylist> = emptyList(),
            override val sourceFilter: LibrarySourceFilter = LibrarySourceFilter.All,
            override val onlineAvailable: Boolean = true,
            override val nextPagination: PaginationStrategy? = null
        ) : Data {
            override val items: List<LibraryPlaylistEntry> = mergedPlaylistItems(
                onlineItems, localItems, sourceFilter, onlineAvailable, query
            )
        }
    }

    data class Error(val message: String) : LibraryPlaylistsState
}

/**
 * The merged Playlists-tab list: online items first, then local ones, honoring the tab's source
 * filter and search query. Online items disappear while the plugin/account is unavailable.
 */
private fun mergedPlaylistItems(
    onlineItems: List<MetadataPlaylist>,
    localItems: List<LocalPlaylist>,
    sourceFilter: LibrarySourceFilter,
    onlineAvailable: Boolean,
    query: String
): List<LibraryPlaylistEntry> {
    val online = if (onlineAvailable && sourceFilter != LibrarySourceFilter.Local) {
        onlineItems
            .filter { playlistMatchesQuery(it.title, it.description, query) }
            .map { LibraryPlaylistEntry.Online(it) }
    } else {
        emptyList()
    }
    val local = if (sourceFilter != LibrarySourceFilter.Online) {
        localItems
            .filter { playlistMatchesQuery(it.name, it.description, query) }
            .map { LibraryPlaylistEntry.Local(it) }
    } else {
        emptyList()
    }
    return online + local
}

private fun playlistMatchesQuery(title: String, description: String?, query: String): Boolean =
    query.isBlank() || title.contains(query, ignoreCase = true) ||
        (description?.contains(query, ignoreCase = true) == true)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryPlaylistsViewModel(
    private val repository: LibraryRepository,
    private val libraryState: LibraryState,
    private val localPlaylistRepository: LocalPlaylistRepository
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<LibraryPlaylistsViewModel>()

    private val _state = MutableStateFlow<LibraryPlaylistsState>(LibraryPlaylistsState.Loading)

    /** False while no metadata plugin is selected or it isn't logged in; drives the sign-in hint. */
    private val onlineAvailable = MutableStateFlow(false)

    val uiState: StateFlow<LibraryPlaylistsState> = combine(
        _state,
        libraryState.queryFlow(LibraryTab.Playlists),
        libraryState.sourceFilterFlow(LibraryTab.Playlists),
        localPlaylistRepository.playlistsFlow,
        onlineAvailable
    ) { state, query, sourceFilter, localPlaylists, online ->
        when (state) {
            is LibraryPlaylistsState.Data.Loaded -> state.copy(
                query = query,
                localItems = localPlaylists,
                sourceFilter = sourceFilter,
                onlineAvailable = online
            )
            is LibraryPlaylistsState.Data.LoadingMore -> state.copy(
                query = query,
                localItems = localPlaylists,
                sourceFilter = sourceFilter,
                onlineAvailable = online
            )
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryPlaylistsState.Loading)

    init {
        viewModelScope.launch {
            combine(
                repository.pluginManager.selectedMetadataPlugin
                    // Emits false with no plugin selected or while logged out instead of never
                    // emitting: the tab is no longer plugin-guarded and must never hang on Loading.
                    .flatMapLatest { plugin -> plugin?.loggedInFlow ?: flowOf(false) }
                    .distinctUntilChanged(),
                repository.savedPlaylistIdsFlow
            ) { _, _ -> }.collect {
                onlineAvailable.value = repository.pluginManager.selectedMetadataPlugin.value
                    ?.loggedInFlow?.value == true
                loadInitialData()
            }
        }
    }

    private suspend fun loadInitialData() = runCatching {
        _state.value = LibraryPlaylistsState.Loading
        // null (no plugin/logged out) still lands in Data with empty online items.
        val result = repository.savedPlaylists()
        _state.value = LibraryPlaylistsState.Data.Loaded(
            onlineItems = result?.items ?: emptyList(),
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
                onlineItems = currentState.onlineItems + (result?.items ?: emptyList()),
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
            // While the tab shows local playlists only, hidden online items must not page in
            // (and flash load-more skeletons under the local list).
            val current = uiState.value as? LibraryPlaylistsState.Data ?: return
            if (current.sourceFilter == LibrarySourceFilter.Local) return
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
        when (data.source) {
            PlaylistFormSource.Local -> playlistForm.submit(onSuccess = libraryState::dismissCreatePlaylist) {
                localPlaylistRepository.create(
                    name = data.name,
                    description = data.description.ifBlank { null },
                    // The form already shrinks picked images; re-encoding still guarantees the cap.
                    coverBase64 = data.imageBase64.takeIf { it.isNotBlank() }?.let {
                        reencodeLocalCoverBase64(it)
                    }
                )
            }

            PlaylistFormSource.Online -> playlistForm.submit(onSuccess = libraryState::dismissCreatePlaylist) {
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
    }

    fun refresh() {
        viewModelScope.launch {
            repository.invalidateCaches()
            loadInitialData()
        }
    }
}
