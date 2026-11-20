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

package org.spotube.spotube.modules.library.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.library.LibrarySourceFilter
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.LibraryTab
import org.spotube.spotube.modules.library.local.LocalMediaAlbum
import org.spotube.spotube.modules.library.local.LocalMediaCollectionsRepository
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
import org.koin.core.component.KoinComponent
import org.spotube.spotube.core.i18n.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_unknown_error

/** One row of the merged Albums tab: a plugin (online) album or a synthetic local one. */
sealed interface LibraryAlbumEntry {
    val id: String
    val title: String

    data class Online(val album: MetadataAlbum.Detailed) : LibraryAlbumEntry {
        override val id: String get() = album.id
        override val title: String get() = album.title
    }

    data class Local(val album: LocalMediaAlbum) : LibraryAlbumEntry {
        override val id: String get() = album.key
        override val title: String get() = album.name
    }
}

sealed interface LibraryAlbumsState {
    data object Loading : LibraryAlbumsState

    sealed interface Data : LibraryAlbumsState {
        val query: String
        val onlineItems: List<MetadataAlbum.Detailed>
        val localItems: List<LocalMediaAlbum>
        val nextPagination: PaginationStrategy?
        val sourceFilter: LibrarySourceFilter
        val onlineAvailable: Boolean
        val items: List<LibraryAlbumEntry>

        data class Loaded(
            override val query: String = "",
            override val onlineItems: List<MetadataAlbum.Detailed> = emptyList(),
            override val localItems: List<LocalMediaAlbum> = emptyList(),
            override val nextPagination: PaginationStrategy? = null,
            override val sourceFilter: LibrarySourceFilter = LibrarySourceFilter.All,
            override val onlineAvailable: Boolean = false
        ) : Data {
            override val items: List<LibraryAlbumEntry> =
                mergedAlbumEntries(onlineItems, localItems, sourceFilter, query)

            fun toLoadingMore(): LoadingMore = LoadingMore(
                query = query,
                onlineItems = onlineItems,
                localItems = localItems,
                nextPagination = nextPagination,
                sourceFilter = sourceFilter,
                onlineAvailable = onlineAvailable
            )
        }

        data class LoadingMore(
            override val query: String = "",
            override val onlineItems: List<MetadataAlbum.Detailed> = emptyList(),
            override val localItems: List<LocalMediaAlbum> = emptyList(),
            override val nextPagination: PaginationStrategy? = null,
            override val sourceFilter: LibrarySourceFilter = LibrarySourceFilter.All,
            override val onlineAvailable: Boolean = false
        ) : Data {
            override val items: List<LibraryAlbumEntry> =
                mergedAlbumEntries(onlineItems, localItems, sourceFilter, query)
        }
    }

    data class Error(val message: String) : LibraryAlbumsState
}

/**
 * Online albums first, then local ones, narrowed by [sourceFilter] and [query] the way the tab
 * always filtered its online list. Local albums also match against their tracks' artists.
 */
private fun mergedAlbumEntries(
    onlineItems: List<MetadataAlbum.Detailed>,
    localItems: List<LocalMediaAlbum>,
    sourceFilter: LibrarySourceFilter,
    query: String
): List<LibraryAlbumEntry> {
    val online = if (sourceFilter == LibrarySourceFilter.Local) emptyList()
    else onlineItems.filter { album ->
        query.isBlank() || album.title.contains(query, ignoreCase = true) ||
                album.artists.any { it.name.contains(query, ignoreCase = true) }
    }
    val local = if (sourceFilter == LibrarySourceFilter.Online) emptyList()
    else localItems.filter { album ->
        query.isBlank() || album.name.contains(query, ignoreCase = true) ||
                album.tracks.any { track ->
                    track.artists.any { it.contains(query, ignoreCase = true) }
                }
    }
    return online.map { LibraryAlbumEntry.Online(it) } +
            local.map { LibraryAlbumEntry.Local(it) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryAlbumsViewModel(
    private val repository: LibraryRepository,
    private val libraryState: LibraryState,
    private val collectionsRepository: LocalMediaCollectionsRepository
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<LibraryAlbumsViewModel>()

    private val _state = MutableStateFlow<LibraryAlbumsState>(LibraryAlbumsState.Loading)

    /** False while no metadata plugin is selected or the selected one is logged out. */
    private val onlineAvailable = MutableStateFlow(false)

    val uiState: StateFlow<LibraryAlbumsState> = combine(
        _state, libraryState.queryFlow(LibraryTab.Albums),
        libraryState.sourceFilterFlow(LibraryTab.Albums),
        collectionsRepository.albums, onlineAvailable
    ) { state, query, sourceFilter, localItems, onlineAvailable ->
        when (state) {
            is LibraryAlbumsState.Data.Loaded -> state.copy(
                query = query,
                sourceFilter = sourceFilter,
                localItems = localItems,
                onlineAvailable = onlineAvailable
            )

            is LibraryAlbumsState.Data.LoadingMore -> state.copy(
                query = query,
                sourceFilter = sourceFilter,
                localItems = localItems,
                onlineAvailable = onlineAvailable
            )

            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryAlbumsState.Loading)

    init {
        viewModelScope.launch {
            combine(
                repository.pluginManager.selectedMetadataPlugin
                    .flatMapLatest { plugin -> plugin?.loggedInFlow ?: flowOf(false) }
                    .distinctUntilChanged(),
                repository.savedAlbumIdsFlow
            ) { _, _ -> }.collect {
                // The plugin flow also emits `false` while logged out or with no plugin selected,
                // so the online list loads (as empty) even without an account; the screen shows the
                // sign-in hint in that case.
                onlineAvailable.value = repository.pluginManager.selectedMetadataPlugin.value
                    ?.loggedInFlow?.value == true
                loadInitialData()
            }
        }
    }

    private suspend fun loadInitialData() = runCatching {
        _state.value = LibraryAlbumsState.Loading
        val result = repository.savedAlbums()
        _state.value = LibraryAlbumsState.Data.Loaded(
            onlineItems = result?.items ?: emptyList(),
            nextPagination = result?.nextPagination
        )
    }.onFailure { e ->
        logger.e(e) { "Failed to load albums" }
        _state.value = LibraryAlbumsState.Error(e.message ?: getString(Res.string.common_unknown_error))
    }

    suspend fun loadMoreData() = runCatching {
        val currentState = _state.value
        if (currentState is LibraryAlbumsState.Data.Loaded && currentState.nextPagination != null) {
            _state.value = currentState.toLoadingMore()
            val result = repository.savedAlbums(currentState.nextPagination)
            _state.value = LibraryAlbumsState.Data.Loaded(
                query = currentState.query,
                onlineItems = currentState.onlineItems + (result?.items ?: emptyList()),
                localItems = currentState.localItems,
                nextPagination = result?.nextPagination,
                sourceFilter = currentState.sourceFilter,
                onlineAvailable = currentState.onlineAvailable
            )
        }
    }.onFailure { e ->
        logger.e(e) { "Failed to load more albums" }
        _state.value = LibraryAlbumsState.Error(e.message ?: getString(Res.string.common_unknown_error))
    }

    fun onQueryChange(query: String) {
        libraryState.setQuery(LibraryTab.Albums, query)
    }

    fun onItemsVisible(lastVisibleIndex: Int?, totalItems: Int) {
        if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 4) {
            // Pagination is an online-only concern; a local-only view never needs another page.
            val data = uiState.value as? LibraryAlbumsState.Data
            if (data?.sourceFilter == LibrarySourceFilter.Local) return
            viewModelScope.launch { loadMoreData() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.invalidateCaches()
            loadInitialData()
        }
    }
}
