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

package org.spotube.spotube.modules.artist

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistOverview
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.modules.plugin.PluginManager
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistRepository(
    private val pluginManager: PluginManager
) {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val plugin
        get() = pluginManager.selectedMetadataPlugin.value

    private val overviewCache = Cache.Builder<String, MetadataArtistOverview>().build()
    private val albumsCache =
        Cache.Builder<Pair<String, PaginationStrategy>, PaginationResult<MetadataAlbum.Detailed>>().build()
    private val relatedArtistsCache =
        Cache.Builder<Pair<String, PaginationStrategy>, PaginationResult<MetadataArtist.Basic>>().build()
    private val featuredPlaylistsCache =
        Cache.Builder<Pair<String, PaginationStrategy>, PaginationResult<MetadataPlaylist>>().build()
    private val topTracksCache = Cache.Builder<String, List<MetadataTrack>>().build()

    init {
        scope.launch {
            pluginManager.selectedMetadataPlugin
                .filterNotNull()
                .flatMapLatest { it.loggedInFlow }
                .distinctUntilChanged()
                .collect {
                    invalidateCaches()
                }
        }
    }

    fun invalidateCaches() {
        overviewCache.invalidateAll()
        albumsCache.invalidateAll()
        relatedArtistsCache.invalidateAll()
        featuredPlaylistsCache.invalidateAll()
        topTracksCache.invalidateAll()
    }

    suspend fun artistOverview(artistId: String) = plugin?.let { plugin ->
        overviewCache.get(artistId) {
            pluginManager.withScope {
                plugin.use {
                    metadataArtistAPI.artistOverview(artistId)
                }
            }
        }
    }

    suspend fun topTracks(artistId: String) = plugin?.let { plugin ->
        topTracksCache.get(artistId) {
            pluginManager.withScope {
                plugin.use {
                    metadataArtistAPI.getArtistTop10Tracks(artistId)
                }
            }
        }
    }

    suspend fun albums(
        artistId: String,
        paginationStrategy: PaginationStrategy? = null
    ) = plugin?.let { plugin ->
        albumsCache.get(
            artistId to (paginationStrategy ?: PaginationStrategy.Offset(0, 20))
        ) {
            pluginManager.withScope {
                plugin.use {
                    metadataArtistAPI.getArtistAlbums(artistId, paginationStrategy)
                }
            }
        }
    }

    suspend fun relatedArtists(
        artistId: String,
        paginationStrategy: PaginationStrategy? = null
    ) = plugin?.let { plugin ->
        relatedArtistsCache.get(
            artistId to (paginationStrategy ?: PaginationStrategy.Offset(0, 20))
        ) {
            pluginManager.withScope {
                plugin.use {
                    metadataArtistAPI.relatedArtists(artistId, paginationStrategy)
                }
            }
        }
    }

    suspend fun featuredPlaylists(
        artistId: String,
        paginationStrategy: PaginationStrategy? = null
    ) = plugin?.let { plugin ->
        featuredPlaylistsCache.get(
            artistId to (paginationStrategy ?: PaginationStrategy.Offset(0, 20))
        ) {
            pluginManager.withScope {
                plugin.use {
                    metadataArtistAPI.featuredPlaylists(artistId, paginationStrategy)
                }
            }
        }
    }
}
