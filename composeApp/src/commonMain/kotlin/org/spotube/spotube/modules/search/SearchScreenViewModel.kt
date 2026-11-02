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

package org.spotube.spotube.modules.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSupportedSearchType
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUser
import org.spotube.spotube.core.db.Database
import org.spotube.spotube.core.di.injectLogger
import org.koin.core.component.KoinComponent
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.saved_tracks.SavedTracksRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.spotube.spotube.core.i18n.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.search_error_capabilities
import spotube.composeapp.generated.resources.search_error_search
import spotube.composeapp.generated.resources.search_error_tracks
import spotube.composeapp.generated.resources.search_error_albums
import spotube.composeapp.generated.resources.search_error_artists
import spotube.composeapp.generated.resources.search_error_playlists
import spotube.composeapp.generated.resources.search_error_users

data class SearchPagedState<T>(
    val items: List<T> = emptyList(),
    val nextPagination: PaginationStrategy? = null,
    val hasNextPage: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null)

data class SearchScreenState(
    val query: String = "",
    val supportedSearchTypes: List<MetadataSupportedSearchType> = emptyList(),
    val selectedSearchType: MetadataSupportedSearchType? = null,
    val isLoadingSearchTypes: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val tracks: SearchPagedState<MetadataTrack> = SearchPagedState(),
    val albums: SearchPagedState<MetadataAlbum.Basic> = SearchPagedState(),
    val artists: SearchPagedState<MetadataArtist.Basic> = SearchPagedState(),
    val playlists: SearchPagedState<MetadataPlaylist> = SearchPagedState(),
    val users: SearchPagedState<MetadataUser> = SearchPagedState()) {
    val recentSearchSuggestions: List<String>
        get() = if (query.isBlank()) {
            recentSearches
        } else {
            recentSearches.filter { it.contains(query, ignoreCase = true) }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchScreenViewModel(
    private val repository: SearchRepository,
    private val savedTracksRepository: SavedTracksRepository,
    private val libraryRepository: LibraryRepository,
    private val database: Database) : ViewModel(), KoinComponent {
    companion object {
        private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
        private val SEARCH_TYPES_ORDER = listOf(
            MetadataSupportedSearchType.ALL,
            MetadataSupportedSearchType.TRACK,
            MetadataSupportedSearchType.PLAYLIST,
            MetadataSupportedSearchType.ALBUM,
            MetadataSupportedSearchType.ARTIST,
            MetadataSupportedSearchType.USER)
        private const val QUERY_DEBOUNCE_MS = 350L
        private const val RECENT_SEARCHES_LIMIT = 12
        private const val TRACK_PAGE_SIZE = 20
        private const val ALL_ROW_PAGE_SIZE = 12
    }

    private val logger by injectLogger<SearchScreenViewModel>()

    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(SearchScreenState())
    val state: StateFlow<SearchScreenState> = _state.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private var queryJob: Job? = null

    init {
        viewModelScope.launch {
            database.settingsDataStore.data.collect { preferences ->
                val recent = parseRecentSearches(preferences[RECENT_SEARCHES_KEY])
                _state.value = _state.value.copy(recentSearches = recent)
            }
        }

        loadSupportedSearchTypes()
        viewModelScope.launch { loadCurrentUser() }
    }

    private suspend fun loadCurrentUser() {
        runCatching {
            _currentUserId.value = libraryRepository.currentUser()?.id
        }.onFailure { e ->
            logger.e(e) { "Failed to load current user" }
        }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            delay(QUERY_DEBOUNCE_MS)
            saveRecentSearch(query)
            refreshCurrentSelection(reset = true)
        }
    }

    fun applyRecentSearch(query: String) {
        _state.value = _state.value.copy(query = query)
        queryJob?.cancel()
        viewModelScope.launch {
            saveRecentSearch(query)
            refreshCurrentSelection(reset = true)
        }
    }

    fun clearQuery() {
        queryJob?.cancel()
        _state.value = _state.value.copy(
            query = "",
            tracks = SearchPagedState(),
            albums = SearchPagedState(),
            artists = SearchPagedState(),
            playlists = SearchPagedState(),
            users = SearchPagedState())
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            database.settingsDataStore.edit { preferences ->
                preferences.remove(RECENT_SEARCHES_KEY)
            }
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            val current = _state.value.recentSearches
            val updated = current.filterNot { it.equals(query, ignoreCase = true) }
            persistRecentSearches(updated)
        }
    }

    fun onTabSelected(type: MetadataSupportedSearchType) {
        _state.value = _state.value.copy(
            selectedSearchType = type,
            tracks = SearchPagedState(),
            albums = SearchPagedState(),
            artists = SearchPagedState(),
            playlists = SearchPagedState(),
            users = SearchPagedState())
        if (_state.value.query.isNotBlank()) {
            viewModelScope.launch {
                refreshCurrentSelection(reset = true)
            }
        }
    }

    fun loadNextTracks() {
        val current = _state.value
        if (current.query.isBlank()) return
        if (current.tracks.isLoading || current.tracks.nextPagination == null) return

        viewModelScope.launch {
            loadTracks(reset = false)
        }
    }

    fun loadNextAlbums() {
        val current = _state.value
        if (current.query.isBlank()) return
        if (current.albums.isLoading || current.albums.nextPagination == null) return

        viewModelScope.launch {
            loadAlbums(reset = false)
        }
    }

    fun loadNextArtists() {
        val current = _state.value
        if (current.query.isBlank()) return
        if (current.artists.isLoading || current.artists.nextPagination == null) return

        viewModelScope.launch {
            loadArtists(reset = false)
        }
    }

    fun loadNextPlaylists() {
        val current = _state.value
        if (current.query.isBlank()) return
        if (current.playlists.isLoading || current.playlists.nextPagination == null) return

        viewModelScope.launch {
            loadPlaylists(reset = false)
        }
    }

    fun loadNextUsers() {
        val current = _state.value
        if (current.query.isBlank()) return
        if (current.users.isLoading || current.users.nextPagination == null) return

        viewModelScope.launch {
            loadUsers(reset = false)
        }
    }

    private fun loadSupportedSearchTypes() {
        viewModelScope.launch {
            runCatching {
                repository.loadSupportedSearchTypes()
            }.onSuccess { supportedTypes ->
                val ordered = SEARCH_TYPES_ORDER.filter { it in supportedTypes }
                val selected = _state.value.selectedSearchType
                val selectedType = when {
                    selected != null && selected in ordered -> selected
                    MetadataSupportedSearchType.ALL in ordered -> MetadataSupportedSearchType.ALL
                    else -> ordered.firstOrNull()
                }

                _state.value = _state.value.copy(
                    supportedSearchTypes = ordered,
                    selectedSearchType = selectedType,
                    isLoadingSearchTypes = false)

                if (_state.value.query.isNotBlank()) {
                    refreshCurrentSelection(reset = true)
                }
            }.onFailure {
                _state.value = _state.value.copy(
                    isLoadingSearchTypes = false,
                    supportedSearchTypes = emptyList(),
                    selectedSearchType = null,
                    tracks = SearchPagedState(error = it.message ?: getString(Res.string.search_error_capabilities)))
            }
        }
    }

    private suspend fun refreshCurrentSelection(reset: Boolean) {
        val current = _state.value
        if (current.query.isBlank()) {
            _state.value = current.copy(
                tracks = SearchPagedState(),
                albums = SearchPagedState(),
                artists = SearchPagedState(),
                playlists = SearchPagedState(),
                users = SearchPagedState())
            return
        }

        when (current.selectedSearchType) {
            MetadataSupportedSearchType.ALL -> loadAll(reset = reset)
            MetadataSupportedSearchType.TRACK -> loadTracks(reset = reset)
            MetadataSupportedSearchType.ALBUM -> loadAlbums(reset = reset)
            MetadataSupportedSearchType.ARTIST -> loadArtists(reset = reset)
            MetadataSupportedSearchType.PLAYLIST -> loadPlaylists(reset = reset)
            MetadataSupportedSearchType.USER -> loadUsers(reset = reset)
            null -> Unit
        }
    }

    private suspend fun loadAll(reset: Boolean) = coroutineScope {
        if (!reset) return@coroutineScope

        val query = _state.value.query.trim()
        val supported = _state.value.supportedSearchTypes

        _state.value = _state.value.copy(
            tracks = SearchPagedState(isLoading = true),
            albums = SearchPagedState(isLoading = true),
            artists = SearchPagedState(isLoading = true),
            playlists = SearchPagedState(isLoading = true),
            users = SearchPagedState(isLoading = true))

        if (MetadataSupportedSearchType.ALL in supported) {
            runCatching {
                repository.searchAll(query)
            }.onSuccess { results ->
                val tracks = results.filterIsInstance<MetadataSearchResult.Track>().map { it.data }
                val playlists = results.filterIsInstance<MetadataSearchResult.Playlist>().map { it.data }
                val albums = results.filterIsInstance<MetadataSearchResult.Album>().map { it.data }
                val artists = results.filterIsInstance<MetadataSearchResult.Artist>().map { it.data }
                val users = results.filterIsInstance<MetadataSearchResult.User>().map { it.data }

                val trackSubset = tracks.take(TRACK_PAGE_SIZE)
                savedTracksRepository.isSavedTracks(trackSubset.map { it.id })

                _state.value = _state.value.copy(
                    tracks = SearchPagedState(
                        items = tracks.take(TRACK_PAGE_SIZE),
                        nextPagination = null,
                        hasNextPage = false,
                        isLoading = false),
                    playlists = SearchPagedState(
                        items = playlists.take(ALL_ROW_PAGE_SIZE),
                        nextPagination = null,
                        hasNextPage = false,
                        isLoading = false),
                    albums = SearchPagedState(
                        items = albums.take(ALL_ROW_PAGE_SIZE),
                        nextPagination = null,
                        hasNextPage = false,
                        isLoading = false),
                    artists = SearchPagedState(
                        items = artists.take(ALL_ROW_PAGE_SIZE),
                        nextPagination = null,
                        hasNextPage = false,
                        isLoading = false),
                    users = SearchPagedState(
                        items = users.take(ALL_ROW_PAGE_SIZE),
                        nextPagination = null,
                        hasNextPage = false,
                        isLoading = false))
            }.onFailure {
                val message = it.message ?: getString(Res.string.search_error_search)
                _state.value = _state.value.copy(
                    tracks = SearchPagedState(isLoading = false, error = message),
                    playlists = SearchPagedState(isLoading = false, error = message),
                    albums = SearchPagedState(isLoading = false, error = message),
                    artists = SearchPagedState(isLoading = false, error = message),
                    users = SearchPagedState(isLoading = false, error = message))
            }
            return@coroutineScope
        }

        val jobs = mutableListOf<Job>()
        if (MetadataSupportedSearchType.TRACK in supported) jobs += launch { loadTracks(reset = true) }
        if (MetadataSupportedSearchType.ALBUM in supported) jobs += launch { loadAlbums(reset = true) }
        if (MetadataSupportedSearchType.ARTIST in supported) jobs += launch { loadArtists(reset = true) }
        if (MetadataSupportedSearchType.PLAYLIST in supported) jobs += launch { loadPlaylists(reset = true) }
        if (MetadataSupportedSearchType.USER in supported) jobs += launch { loadUsers(reset = true) }
        jobs.joinAll()

        _state.value = _state.value.copy(
            playlists = _state.value.playlists.copy(items = _state.value.playlists.items.take(ALL_ROW_PAGE_SIZE)),
            albums = _state.value.albums.copy(items = _state.value.albums.items.take(ALL_ROW_PAGE_SIZE)),
            artists = _state.value.artists.copy(items = _state.value.artists.items.take(ALL_ROW_PAGE_SIZE)),
            users = _state.value.users.copy(items = _state.value.users.items.take(ALL_ROW_PAGE_SIZE)))
    }

    private suspend fun loadTracks(reset: Boolean) {
        val currentState = _state.value.tracks
        val query = _state.value.query.trim()
        val pagination = if (reset) null else currentState.nextPagination ?: return

        _state.value = _state.value.copy(
            tracks = if (reset) {
                SearchPagedState(isLoading = true)
            } else {
                currentState.copy(isLoading = true, error = null)
            }
        )

        runCatching {
            repository.searchTracks(query, pagination)
        }.onSuccess { page ->
            val mergedItems = if (reset) {
                page.items.map { it.data }
            } else {
                _state.value.tracks.items + page.items.map { it.data }
            }

            savedTracksRepository.isSavedTracks(mergedItems.map { it.id })

            _state.value = _state.value.copy(
                tracks = SearchPagedState(
                    items = mergedItems,
                    nextPagination = page.nextPagination,
                    hasNextPage = page.nextPagination != null,
                    isLoading = false,
                    error = null)
            )
        }.onFailure { throwable ->
            _state.value = _state.value.copy(
                tracks = _state.value.tracks.copy(
                    isLoading = false,
                    hasNextPage = false,
                    error = throwable.message ?: getString(Res.string.search_error_tracks))
            )
        }
    }

    private suspend fun loadAlbums(reset: Boolean) {
        val currentState = _state.value.albums
        val query = _state.value.query.trim()
        val pagination = if (reset) null else currentState.nextPagination ?: return

        _state.value = _state.value.copy(
            albums = if (reset) {
                SearchPagedState(isLoading = true)
            } else {
                currentState.copy(isLoading = true, error = null)
            }
        )

        runCatching {
            repository.searchAlbums(query, pagination)
        }.onSuccess { page ->
            val mergedItems = if (reset) {
                page.items.map { it.data }
            } else {
                _state.value.albums.items + page.items.map { it.data }
            }

            _state.value = _state.value.copy(
                albums = SearchPagedState(
                    items = mergedItems,
                    nextPagination = page.nextPagination,
                    hasNextPage = page.nextPagination != null,
                    isLoading = false,
                    error = null)
            )
        }.onFailure { throwable ->
            _state.value = _state.value.copy(
                albums = _state.value.albums.copy(
                    isLoading = false,
                    hasNextPage = false,
                    error = throwable.message ?: getString(Res.string.search_error_albums))
            )
        }
    }

    private suspend fun loadArtists(reset: Boolean) {
        val currentState = _state.value.artists
        val query = _state.value.query.trim()
        val pagination = if (reset) null else currentState.nextPagination ?: return

        _state.value = _state.value.copy(
            artists = if (reset) {
                SearchPagedState(isLoading = true)
            } else {
                currentState.copy(isLoading = true, error = null)
            }
        )

        runCatching {
            repository.searchArtists(query, pagination)
        }.onSuccess { page ->
            val mergedItems = if (reset) {
                page.items.map { it.data }
            } else {
                _state.value.artists.items + page.items.map { it.data }
            }

            _state.value = _state.value.copy(
                artists = SearchPagedState(
                    items = mergedItems,
                    nextPagination = page.nextPagination,
                    hasNextPage = page.nextPagination != null,
                    isLoading = false,
                    error = null)
            )
        }.onFailure { throwable ->
            _state.value = _state.value.copy(
                artists = _state.value.artists.copy(
                    isLoading = false,
                    hasNextPage = false,
                    error = throwable.message ?: getString(Res.string.search_error_artists))
            )
        }
    }

    private suspend fun loadPlaylists(reset: Boolean) {
        val currentState = _state.value.playlists
        val query = _state.value.query.trim()
        val pagination = if (reset) null else currentState.nextPagination ?: return

        _state.value = _state.value.copy(
            playlists = if (reset) {
                SearchPagedState(isLoading = true)
            } else {
                currentState.copy(isLoading = true, error = null)
            }
        )

        runCatching {
            repository.searchPlaylists(query, pagination)
        }.onSuccess { page ->
            val mergedItems = if (reset) {
                page.items.map { it.data }
            } else {
                _state.value.playlists.items + page.items.map { it.data }
            }

            _state.value = _state.value.copy(
                playlists = SearchPagedState(
                    items = mergedItems,
                    nextPagination = page.nextPagination,
                    hasNextPage = page.nextPagination != null,
                    isLoading = false,
                    error = null)
            )
        }.onFailure { throwable ->
            _state.value = _state.value.copy(
                playlists = _state.value.playlists.copy(
                    isLoading = false,
                    hasNextPage = false,
                    error = throwable.message ?: getString(Res.string.search_error_playlists))
            )
        }
    }

    private suspend fun loadUsers(reset: Boolean) {
        val currentState = _state.value.users
        val query = _state.value.query.trim()
        val pagination = if (reset) null else currentState.nextPagination ?: return

        _state.value = _state.value.copy(
            users = if (reset) {
                SearchPagedState(isLoading = true)
            } else {
                currentState.copy(isLoading = true, error = null)
            }
        )

        runCatching {
            repository.searchUsers(query, pagination)
        }.onSuccess { page ->
            val mergedItems = if (reset) {
                page.items.map { it.data }
            } else {
                _state.value.users.items + page.items.map { it.data }
            }

            _state.value = _state.value.copy(
                users = SearchPagedState(
                    items = mergedItems,
                    nextPagination = page.nextPagination,
                    hasNextPage = page.nextPagination != null,
                    isLoading = false,
                    error = null)
            )
        }.onFailure { throwable ->
            _state.value = _state.value.copy(
                users = _state.value.users.copy(
                    isLoading = false,
                    hasNextPage = false,
                    error = throwable.message ?: getString(Res.string.search_error_users))
            )
        }
    }

    val savedTrackIds
        get() = savedTracksRepository.savedTracksIdsFlow

    fun toggleTrackIsFavorite(trackId: String) {
        viewModelScope.launch {
            val savedIds = savedTrackIds.value
            if (savedIds.contains(trackId)) {
                savedTracksRepository.removeSavedTracks(listOf(trackId))
            } else {
                savedTracksRepository.saveTracks(listOf(trackId))
            }
        }
    }

    private suspend fun saveRecentSearch(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return

        val current = _state.value.recentSearches
        val updated = (listOf(normalized) + current.filterNot { it.equals(normalized, ignoreCase = true) })
            .take(RECENT_SEARCHES_LIMIT)
        persistRecentSearches(updated)
    }

    private suspend fun persistRecentSearches(searches: List<String>) {
        database.settingsDataStore.edit { preferences ->
            if (searches.isEmpty()) {
                preferences.remove(RECENT_SEARCHES_KEY)
            } else {
                preferences[RECENT_SEARCHES_KEY] = json.encodeToString(searches)
            }
        }
    }

    private fun parseRecentSearches(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<String>>(raw)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(RECENT_SEARCHES_LIMIT)
        }.getOrDefault(emptyList())
    }
}
