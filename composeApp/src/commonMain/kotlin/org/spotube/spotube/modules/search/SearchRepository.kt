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

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSupportedSearchType
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUser
import org.spotube.spotube.modules.plugin.PluginManager
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.spotube.spotube.core.i18n.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.search_error_no_metadata_plugin

class SearchRepository(
    private val pluginManager: PluginManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val supportedTypesCache = Cache.Builder<Unit, List<MetadataSupportedSearchType>>().build()
    private val allSearchCache = Cache.Builder<String, List<MetadataSearchResult>>().build()
    private val trackSearchCache = Cache.Builder<Pair<String, PaginationStrategy?>, PaginationResult<MetadataSearchResult.Track>>().build()
    private val albumSearchCache = Cache.Builder<Pair<String, PaginationStrategy?>, PaginationResult<MetadataSearchResult.Album>>().build()
    private val artistSearchCache = Cache.Builder<Pair<String, PaginationStrategy?>, PaginationResult<MetadataSearchResult.Artist>>().build()
    private val playlistSearchCache = Cache.Builder<Pair<String, PaginationStrategy?>, PaginationResult<MetadataSearchResult.Playlist>>().build()
    private val userSearchCache = Cache.Builder<Pair<String, PaginationStrategy?>, PaginationResult<MetadataSearchResult.User>>().build()

    init {
        scope.launch {
            pluginManager.selectedMetadataPlugin
                .filterNotNull()
                .distinctUntilChanged()
                .collect { invalidateCaches() }
        }
    }

    fun invalidateCaches() {
        supportedTypesCache.invalidateAll()
        allSearchCache.invalidateAll()
        trackSearchCache.invalidateAll()
        albumSearchCache.invalidateAll()
        artistSearchCache.invalidateAll()
        playlistSearchCache.invalidateAll()
        userSearchCache.invalidateAll()
    }

    suspend fun loadSupportedSearchTypes(): List<MetadataSupportedSearchType> {
        return supportedTypesCache.get(Unit) {
            val plugin = pluginManager.selectedMetadataPlugin.value
                ?: throw IllegalStateException(getString(Res.string.search_error_no_metadata_plugin))
            pluginManager.withScope {
                plugin.use { metadataSearchAPI.supportedSearchTypes }
            }
        }
    }

    suspend fun searchAll(query: String): List<MetadataSearchResult> {
        return allSearchCache.get(query) {
            val plugin = pluginManager.selectedMetadataPlugin.value
                ?: throw IllegalStateException(getString(Res.string.search_error_no_metadata_plugin))
            pluginManager.withScope {
                plugin.use { metadataSearchAPI.search(query) }
            }
        }
    }

    suspend fun searchTracks(
        query: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataSearchResult.Track> {
        val key = query to pagination
        return trackSearchCache.get(key) {
            val plugin = pluginManager.selectedMetadataPlugin.value
                ?: throw IllegalStateException(getString(Res.string.search_error_no_metadata_plugin))
            pluginManager.withScope {
                plugin.use { metadataSearchAPI.searchTracks(query, pagination) }
            }
        }
    }

    suspend fun searchAlbums(
        query: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataSearchResult.Album> {
        val key = query to pagination
        return albumSearchCache.get(key) {
            val plugin = pluginManager.selectedMetadataPlugin.value
                ?: throw IllegalStateException(getString(Res.string.search_error_no_metadata_plugin))
            pluginManager.withScope {
                plugin.use { metadataSearchAPI.searchAlbums(query, pagination) }
            }
        }
    }

    suspend fun searchArtists(
        query: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataSearchResult.Artist> {
        val key = query to pagination
        return artistSearchCache.get(key) {
            val plugin = pluginManager.selectedMetadataPlugin.value
                ?: throw IllegalStateException(getString(Res.string.search_error_no_metadata_plugin))
            pluginManager.withScope {
                plugin.use { metadataSearchAPI.searchArtists(query, pagination) }
            }
        }
    }

    suspend fun searchPlaylists(
        query: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataSearchResult.Playlist> {
        val key = query to pagination
        return playlistSearchCache.get(key) {
            val plugin = pluginManager.selectedMetadataPlugin.value
                ?: throw IllegalStateException(getString(Res.string.search_error_no_metadata_plugin))
            pluginManager.withScope {
                plugin.use { metadataSearchAPI.searchPlaylists(query, pagination) }
            }
        }
    }

    suspend fun searchUsers(
        query: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataSearchResult.User> {
        val key = query to pagination
        return userSearchCache.get(key) {
            val plugin = pluginManager.selectedMetadataPlugin.value
                ?: throw IllegalStateException(getString(Res.string.search_error_no_metadata_plugin))
            pluginManager.withScope {
                plugin.use { metadataSearchAPI.searchUsers(query, pagination) }
            }
        }
    }
}
