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

package org.spotube.spotube.modules.library

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.library_filter_source_all
import spotube.composeapp.generated.resources.library_filter_source_local
import spotube.composeapp.generated.resources.library_filter_source_online

enum class LibraryViewMode { Grid, List }

/** Which kind of items the Playlists/Albums/Artists tabs show: everything, plugin (online) items only, or on-device items only. */
enum class LibrarySourceFilter {
    All, Online, Local;

    val labelResource: StringResource
        get() = when (this) {
            All -> Res.string.library_filter_source_all
            Online -> Res.string.library_filter_source_online
            Local -> Res.string.library_filter_source_local
        }
}

data class LibraryToolbarUiState(
    val currentTab: LibraryTab = LibraryTab.Playlists,
    val searchMode: Boolean = false,
    val searchState: TextFieldState = TextFieldState(),
    val canFilter: Boolean = true,
    val canCreatePlaylist: Boolean = true,
    val canRefresh: Boolean = false,
    val isRefreshing: Boolean = false,
    val showCreatePlaylist: Boolean = false,
    val canChangeView: Boolean = true,
    val viewModeOverride: LibraryViewMode? = null,
    val canFilterSource: Boolean = false,
    val sourceFilter: LibrarySourceFilter = LibrarySourceFilter.All
)

/** Shared with the sidebar for the app lifetime; each library tab retains its own filter. */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryState : ViewModel() {
    private val searchMode = MutableStateFlow(false)
    val currentTab = MutableStateFlow(LibraryTab.Playlists)
    private val searchStates = LibraryTab.entries.associateWith { TextFieldState() }
    private val createPlaylistVisible = MutableStateFlow(false)
    private val viewModes = MutableStateFlow<Map<LibraryTab, LibraryViewMode>>(emptyMap())
    private val sourceFilters = MutableStateFlow<Map<LibraryTab, LibrarySourceFilter>>(emptyMap())
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val localRefreshRequests = refreshRequests.asSharedFlow()
    private val localRefreshing = MutableStateFlow(false)

    /** View mode + source filter for the active tab, combined to stay within the typed combine arity. */
    private val tabDisplayState = viewModes.combine(sourceFilters) { modes, filters -> modes to filters }

    val uiState = combine(
        currentTab.flatMapLatest { tab -> queryFlow(tab).map { query -> tab to query } },
        searchMode,
        createPlaylistVisible,
        tabDisplayState,
        localRefreshing
    ) { (tab, query), searching, creating, (modes, filters), refreshing ->
        LibraryToolbarUiState(
            currentTab = tab,
            searchMode = tab != LibraryTab.Downloads && (searching || query.isNotEmpty()),
            searchState = searchStates.getValue(tab),
            canFilter = tab != LibraryTab.Downloads,
            canCreatePlaylist = tab == LibraryTab.Playlists,
            canRefresh = tab == LibraryTab.LocalTracks,
            isRefreshing = tab == LibraryTab.LocalTracks && refreshing,
            showCreatePlaylist = tab == LibraryTab.Playlists && creating,
            canChangeView = tab == LibraryTab.Playlists || tab == LibraryTab.Albums || tab == LibraryTab.Artists,
            viewModeOverride = modes[tab],
            canFilterSource = tab == LibraryTab.Playlists || tab == LibraryTab.Albums || tab == LibraryTab.Artists,
            sourceFilter = filters[tab] ?: LibrarySourceFilter.All
        )
    }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        LibraryToolbarUiState(searchState = searchStates.getValue(LibraryTab.Playlists))
    )

    fun queryFlow(tab: LibraryTab) = snapshotFlow { searchStates.getValue(tab).text.toString() }

    fun setQuery(tab: LibraryTab, query: String) {
        searchStates.getValue(tab).setTextAndPlaceCursorAtEnd(query)
    }

    fun onTabSelected(tab: LibraryTab) {
        currentTab.value = tab
        createPlaylistVisible.value = false
        if (tab == LibraryTab.Downloads) searchMode.value = false
    }

    fun openSearch() {
        if (currentTab.value != LibraryTab.Downloads) searchMode.value = true
    }

    fun closeSearch() {
        setQuery(currentTab.value, "")
        searchMode.value = false
    }

    fun showCreatePlaylist() {
        if (currentTab.value == LibraryTab.Playlists) createPlaylistVisible.value = true
    }

    fun dismissCreatePlaylist() {
        createPlaylistVisible.value = false
    }

    fun refreshLocalTracks() {
        refreshRequests.tryEmit(Unit)
    }

    /** Reported by the local tracks ViewModel so the toolbar's refresh action can show progress. */
    fun setLocalRefreshing(refreshing: Boolean) {
        localRefreshing.value = refreshing
    }

    fun toggleViewMode(isCompact: Boolean) {
        val tab = currentTab.value
        if (tab != LibraryTab.Playlists && tab != LibraryTab.Albums && tab != LibraryTab.Artists) return
        viewModes.update { modes ->
            val current = modes[tab] ?: defaultLibraryViewMode(isCompact)
            modes + (tab to if (current == LibraryViewMode.Grid) LibraryViewMode.List else LibraryViewMode.Grid)
        }
    }

    fun setSourceFilter(tab: LibraryTab, filter: LibrarySourceFilter) {
        if (tab != LibraryTab.Playlists && tab != LibraryTab.Albums && tab != LibraryTab.Artists) return
        sourceFilters.update { filters ->
            if (filter == LibrarySourceFilter.All) filters - tab else filters + (tab to filter)
        }
    }

    fun sourceFilterFlow(tab: LibraryTab) =
        sourceFilters.map { it[tab] ?: LibrarySourceFilter.All }
}

internal fun defaultLibraryViewMode(isCompact: Boolean) =
    if (isCompact) LibraryViewMode.List else LibraryViewMode.Grid
