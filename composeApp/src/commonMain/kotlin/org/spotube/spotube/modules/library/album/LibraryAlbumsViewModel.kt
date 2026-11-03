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

sealed interface LibraryAlbumsState {
    data object Loading : LibraryAlbumsState

    sealed interface Data : LibraryAlbumsState {
        val query: String
        val allItems: List<MetadataAlbum.Detailed>
        val items: List<MetadataAlbum.Detailed>
        val nextPagination: PaginationStrategy?

        data class Loaded(
            override val query: String = "",
            override val allItems: List<MetadataAlbum.Detailed> = emptyList(),
            override val nextPagination: PaginationStrategy? = null
        ) : Data {
            override val items: List<MetadataAlbum.Detailed> = if (query.isNotBlank()) {
                allItems.filter { album ->
                    album.title.contains(query, ignoreCase = true) ||
                            album.artists.any { artist ->
                                artist.name.contains(query, ignoreCase = true)
                            }
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
            override val allItems: List<MetadataAlbum.Detailed> = emptyList(),
            override val nextPagination: PaginationStrategy? = null
        ) : Data {
            override val items: List<MetadataAlbum.Detailed> = if (query.isNotBlank()) {
                allItems.filter { album ->
                    album.title.contains(query, ignoreCase = true) ||
                            album.artists.any { artist ->
                                artist.name.contains(query, ignoreCase = true)
                            }
                }
            } else {
                allItems
            }
        }
    }

    data class Error(val message: String) : LibraryAlbumsState
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryAlbumsViewModel(
    private val repository: LibraryRepository,
    private val libraryState: LibraryState
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<LibraryAlbumsViewModel>()

    private val _state = MutableStateFlow<LibraryAlbumsState>(LibraryAlbumsState.Loading)
    val uiState: StateFlow<LibraryAlbumsState> = combine(
        _state, libraryState.queryFlow(LibraryTab.Albums)
    ) { state, query ->
        when (state) {
            is LibraryAlbumsState.Data.Loaded -> state.copy(query = query)
            is LibraryAlbumsState.Data.LoadingMore -> state.copy(query = query)
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryAlbumsState.Loading)

    init {
        viewModelScope.launch {
            combine(
                repository.pluginManager.selectedMetadataPlugin
                    .filterNotNull()
                    .flatMapLatest { it.loggedInFlow }
                    .distinctUntilChanged(),
                repository.savedAlbumIdsFlow
            ) { _, _ -> }.collect {
                loadInitialData()
            }
        }
    }

    private suspend fun loadInitialData() = runCatching {
        _state.value = LibraryAlbumsState.Loading
        val result = repository.savedAlbums()
        _state.value = LibraryAlbumsState.Data.Loaded(
            allItems = result?.items ?: emptyList(),
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
                allItems = currentState.allItems + (result?.items ?: emptyList()),
                nextPagination = result?.nextPagination
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
