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
import org.koin.core.component.KoinComponent
import org.spotube.spotube.core.i18n.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_unknown_error

sealed interface LibraryArtistsState {
    data object Loading : LibraryArtistsState

    sealed interface Data : LibraryArtistsState {
        val query: String
        val allItems: List<MetadataArtist.Basic>
        val items: List<MetadataArtist.Basic>
        val nextPagination: PaginationStrategy?

        data class Loaded(
            override val query: String = "",
            override val allItems: List<MetadataArtist.Basic> = emptyList(),
            override val nextPagination: PaginationStrategy? = null
        ) : Data {
            override val items: List<MetadataArtist.Basic> = if (query.isNotBlank()) {
                allItems.filter { artist ->
                    artist.name.contains(query, ignoreCase = true)
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
            override val allItems: List<MetadataArtist.Basic> = emptyList(),
            override val nextPagination: PaginationStrategy? = null
        ) : Data {
            override val items: List<MetadataArtist.Basic> = if (query.isNotBlank()) {
                allItems.filter { artist ->
                    artist.name.contains(query, ignoreCase = true)
                }
            } else {
                allItems
            }
        }
    }

    data class Error(val message: String) : LibraryArtistsState
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryArtistsViewModel(
    private val repository: LibraryRepository,
    private val libraryState: LibraryState
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<LibraryArtistsViewModel>()

    private val _state = MutableStateFlow<LibraryArtistsState>(LibraryArtistsState.Loading)
    val uiState: StateFlow<LibraryArtistsState> = combine(
        _state, libraryState.queryFlow(LibraryTab.Artists)
    ) { state, query ->
        when (state) {
            is LibraryArtistsState.Data.Loaded -> state.copy(query = query)
            is LibraryArtistsState.Data.LoadingMore -> state.copy(query = query)
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryArtistsState.Loading)

    init {
        viewModelScope.launch {
            combine(
                repository.pluginManager.selectedMetadataPlugin
                    .filterNotNull()
                    .flatMapLatest { it.loggedInFlow }
                    .distinctUntilChanged(),
                repository.savedArtistIdsFlow
            ) { _, _ -> }.collect {
                loadInitialData()
            }
        }
    }

    private suspend fun loadInitialData() = runCatching {
        _state.value = LibraryArtistsState.Loading
        val result = repository.savedArtists()
        _state.value = LibraryArtistsState.Data.Loaded(
            allItems = result?.items?.map {
                MetadataArtist.Basic(
                    id = it.id,
                    name = it.name,
                    thumbnails = it.thumbnails,
                    externalUri = it.externalUri
                )
            } ?: emptyList(),
            nextPagination = result?.nextPagination)
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
                allItems = currentState.allItems + newItems,
                nextPagination = result?.nextPagination
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
