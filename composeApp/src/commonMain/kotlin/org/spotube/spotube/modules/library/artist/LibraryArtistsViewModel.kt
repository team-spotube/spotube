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

package org.spotube.spotube.modules.library.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.library.LibrarySourceFilter
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.LibraryTab
import org.spotube.spotube.modules.library.local.LocalMediaArtist
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

/** One row of the merged Artists tab: a plugin (online) artist or a synthetic local one. */
sealed interface LibraryArtistEntry {
    val id: String
    val title: String

    data class Online(val artist: MetadataArtist.Basic) : LibraryArtistEntry {
        override val id: String get() = artist.id
        override val title: String get() = artist.name
    }

    data class Local(val artist: LocalMediaArtist) : LibraryArtistEntry {
        override val id: String get() = artist.key
        override val title: String get() = artist.name
    }
}

sealed interface LibraryArtistsState {
    data object Loading : LibraryArtistsState

    sealed interface Data : LibraryArtistsState {
        val query: String
        val onlineItems: List<MetadataArtist.Basic>
        val localItems: List<LocalMediaArtist>
        val nextPagination: PaginationStrategy?
        val sourceFilter: LibrarySourceFilter
        val onlineAvailable: Boolean
        val items: List<LibraryArtistEntry>

        data class Loaded(
            override val query: String = "",
            override val onlineItems: List<MetadataArtist.Basic> = emptyList(),
            override val localItems: List<LocalMediaArtist> = emptyList(),
            override val nextPagination: PaginationStrategy? = null,
            override val sourceFilter: LibrarySourceFilter = LibrarySourceFilter.All,
            override val onlineAvailable: Boolean = false
        ) : Data {
            override val items: List<LibraryArtistEntry> =
                mergedArtistEntries(onlineItems, localItems, sourceFilter, query)

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
            override val onlineItems: List<MetadataArtist.Basic> = emptyList(),
            override val localItems: List<LocalMediaArtist> = emptyList(),
            override val nextPagination: PaginationStrategy? = null,
            override val sourceFilter: LibrarySourceFilter = LibrarySourceFilter.All,
            override val onlineAvailable: Boolean = false
        ) : Data {
            override val items: List<LibraryArtistEntry> =
                mergedArtistEntries(onlineItems, localItems, sourceFilter, query)
        }
    }

    data class Error(val message: String) : LibraryArtistsState
}

/**
 * Online artists first, then local ones, narrowed by [sourceFilter] and [query] the way the tab
 * always filtered its online list (by artist name).
 */
private fun mergedArtistEntries(
    onlineItems: List<MetadataArtist.Basic>,
    localItems: List<LocalMediaArtist>,
    sourceFilter: LibrarySourceFilter,
    query: String
): List<LibraryArtistEntry> {
    val online = if (sourceFilter == LibrarySourceFilter.Local) emptyList()
    else onlineItems.filter { artist ->
        query.isBlank() || artist.name.contains(query, ignoreCase = true)
    }
    val local = if (sourceFilter == LibrarySourceFilter.Online) emptyList()
    else localItems.filter { artist ->
        query.isBlank() || artist.name.contains(query, ignoreCase = true)
    }
    return online.map { LibraryArtistEntry.Online(it) } +
            local.map { LibraryArtistEntry.Local(it) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryArtistsViewModel(
    private val repository: LibraryRepository,
    private val libraryState: LibraryState,
    private val collectionsRepository: LocalMediaCollectionsRepository
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<LibraryArtistsViewModel>()

    private val _state = MutableStateFlow<LibraryArtistsState>(LibraryArtistsState.Loading)

    /** False while no metadata plugin is selected or the selected one is logged out. */
    private val onlineAvailable = MutableStateFlow(false)

    val uiState: StateFlow<LibraryArtistsState> = combine(
        _state, libraryState.queryFlow(LibraryTab.Artists),
        libraryState.sourceFilterFlow(LibraryTab.Artists),
        collectionsRepository.artists, onlineAvailable
    ) { state, query, sourceFilter, localItems, onlineAvailable ->
        when (state) {
            is LibraryArtistsState.Data.Loaded -> state.copy(
                query = query,
                sourceFilter = sourceFilter,
                localItems = localItems,
                onlineAvailable = onlineAvailable
            )

            is LibraryArtistsState.Data.LoadingMore -> state.copy(
                query = query,
                sourceFilter = sourceFilter,
                localItems = localItems,
                onlineAvailable = onlineAvailable
            )

            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryArtistsState.Loading)

    init {
        viewModelScope.launch {
            combine(
                repository.pluginManager.selectedMetadataPlugin
                    .flatMapLatest { plugin -> plugin?.loggedInFlow ?: flowOf(false) }
                    .distinctUntilChanged(),
                repository.savedArtistIdsFlow
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
        _state.value = LibraryArtistsState.Loading
        val result = repository.savedArtists()
        _state.value = LibraryArtistsState.Data.Loaded(
            onlineItems = result?.items?.map {
                MetadataArtist.Basic(
                    id = it.id,
                    name = it.name,
                    thumbnails = it.thumbnails,
                    externalUri = it.externalUri
                )
            } ?: emptyList(),
            nextPagination = result?.nextPagination
        )
    }.onFailure { e ->
        logger.e(e) { "Failed to load artists" }
        _state.value = LibraryArtistsState.Error(e.message ?: getString(Res.string.common_unknown_error))
    }

    suspend fun loadMoreData() = runCatching {
        val currentState = _state.value
        if (currentState is LibraryArtistsState.Data.Loaded && currentState.nextPagination != null) {
            _state.value = currentState.toLoadingMore()
            val result = repository.savedArtists(currentState.nextPagination)
            val newItems = result?.items?.map {
                MetadataArtist.Basic(
                    id = it.id,
                    name = it.name,
                    thumbnails = it.thumbnails,
                    externalUri = it.externalUri
                )
            } ?: emptyList()
            _state.value = LibraryArtistsState.Data.Loaded(
                query = currentState.query,
                onlineItems = currentState.onlineItems + newItems,
                localItems = currentState.localItems,
                nextPagination = result?.nextPagination,
                sourceFilter = currentState.sourceFilter,
                onlineAvailable = currentState.onlineAvailable
            )
        }
    }.onFailure { e ->
        logger.e(e) { "Failed to load more artists" }
        _state.value = LibraryArtistsState.Error(e.message ?: getString(Res.string.common_unknown_error))
    }

    fun onQueryChange(query: String) {
        libraryState.setQuery(LibraryTab.Artists, query)
    }

    fun onItemsVisible(lastVisibleIndex: Int?, totalItems: Int) {
        if (lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - 4) {
            // Pagination is an online-only concern; a local-only view never needs another page.
            val data = uiState.value as? LibraryArtistsState.Data
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
