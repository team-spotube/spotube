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

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUser
import org.spotube.spotube.modules.plugin.PluginManager
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepository(
    val pluginManager: PluginManager
) {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val plugin
        get() = pluginManager.selectedMetadataPlugin.value

    private val playlistCache =
        Cache.Builder<PaginationStrategy, PaginationResult<MetadataPlaylist>>().build()
    private val albumCache =
        Cache.Builder<PaginationStrategy, PaginationResult<MetadataAlbum.Detailed>>().build()
    private val artistCache =
        Cache.Builder<PaginationStrategy, PaginationResult<MetadataArtist.Detailed>>().build()

    private val savedPlaylistIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPlaylistIdsFlow: StateFlow<Set<String>> = savedPlaylistIds.asStateFlow()

    private val savedAlbumIds = MutableStateFlow<Set<String>>(emptySet())
    val savedAlbumIdsFlow: StateFlow<Set<String>> = savedAlbumIds.asStateFlow()

    private val savedArtistIds = MutableStateFlow<Set<String>>(emptySet())
    val savedArtistIdsFlow: StateFlow<Set<String>> = savedArtistIds.asStateFlow()

    init {
        scope.launch {
            pluginManager.selectedMetadataPlugin
                .filterNotNull()
                .flatMapLatest { it.loggedInFlow }
                .distinctUntilChanged()
                .collect { loggedIn ->
                    isLoggedIn = loggedIn
                    invalidateCaches()
                }
        }
    }

    private var isLoggedIn: Boolean = false

    private val authPlugin get() = if (isLoggedIn) plugin else null

    fun invalidateCaches() {
        playlistCache.invalidateAll()
        albumCache.invalidateAll()
        artistCache.invalidateAll()
        savedPlaylistIds.value = emptySet()
        savedAlbumIds.value = emptySet()
        savedArtistIds.value = emptySet()
    }

    suspend fun savedPlaylists(paginationStrategy: PaginationStrategy? = null) =
        authPlugin?.let { plugin ->
            playlistCache.get(
                key = paginationStrategy ?: PaginationStrategy.Offset(0, 20)
            ) {
                pluginManager.withScope {
                    plugin.use {
                        val result = metadataPlaylistAPI.savedPlaylists(paginationStrategy)
                        savedPlaylistIds.value += result.items.map { it.id }.toSet()
                        result
                    }
                }
            }
        }

    suspend fun savedAlbums(paginationStrategy: PaginationStrategy? = null) =
        authPlugin?.let { plugin ->
            albumCache.get(
                key = paginationStrategy ?: PaginationStrategy.Offset(0, 20)
            ) {
                pluginManager.withScope {
                    plugin.use {
                        val result = metadataAlbumAPI.savedAlbums(paginationStrategy)
                        savedAlbumIds.value += result.items.map { it.id }.toSet()
                        result
                    }
                }
            }
        }

    suspend fun savedArtists(paginationStrategy: PaginationStrategy? = null) =
        authPlugin?.let { plugin ->
            artistCache.get(
                key = paginationStrategy ?: PaginationStrategy.Offset(0, 20)
            ) {
                pluginManager.withScope {
                    plugin.use {
                        val result = metadataArtistAPI.savedArtists(paginationStrategy)
                        savedArtistIds.value += result.items.map { it.id }.toSet()
                        result
                    }
                }
            }
        }

    suspend fun isSavedPlaylists(ids: List<String>): List<Boolean> {
        val unknownIds = ids.filterNot { savedPlaylistIds.value.contains(it) }

        if (unknownIds.isEmpty()) {
            return ids.map { true }
        }

        val unknownStates = authPlugin?.let { plugin ->
            val savedStates = pluginManager.withScope {
                plugin.use {
                    metadataPlaylistAPI.isSavedPlaylists(unknownIds)
                }
            }
            val savedIds = unknownIds.filterIndexed { index, id ->
                savedStates.getOrNull(index) == true
            }.toSet()
            savedPlaylistIds.value += savedIds
            savedStates
        } ?: unknownIds.map { false }

        return ids.map { id ->
            savedPlaylistIds.value.contains(id) ||
                    unknownStates.getOrNull(unknownIds.indexOf(id)) ?: false
        }
    }

    suspend fun savePlaylists(ids: List<String>) {
        authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    metadataPlaylistAPI.savePlaylists(ids)
                }
            }
            playlistCache.invalidateAll()
            savedPlaylistIds.value += ids.toSet()
        }
    }

    suspend fun removeSavedPlaylists(ids: List<String>) {
        authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    metadataPlaylistAPI.removeSavedPlaylists(ids)
                }
            }
            playlistCache.invalidateAll()
            savedPlaylistIds.value -= ids.toSet()
        }
    }

    suspend fun isSavedAlbums(ids: List<String>): List<Boolean> {
        val unknownIds = ids.filterNot { savedAlbumIds.value.contains(it) }

        if (unknownIds.isEmpty()) {
            return ids.map { true }
        }

        val unknownStates = authPlugin?.let { plugin ->
            val savedStates = pluginManager.withScope {
                plugin.use {
                    metadataAlbumAPI.isSavedAlbums(unknownIds)
                }
            }
            val savedIds = unknownIds.filterIndexed { index, id ->
                savedStates.getOrNull(index) == true
            }.toSet()
            savedAlbumIds.value += savedIds
            savedStates
        } ?: unknownIds.map { false }

        return ids.map { id ->
            savedAlbumIds.value.contains(id) ||
                    unknownStates.getOrNull(unknownIds.indexOf(id)) ?: false
        }
    }

    suspend fun saveAlbums(ids: List<String>) {
        authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    metadataAlbumAPI.saveAlbums(ids)
                }
            }
            albumCache.invalidateAll()
            savedAlbumIds.value += ids.toSet()
        }
    }

    suspend fun removeSavedAlbums(ids: List<String>) {
        authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    metadataAlbumAPI.removeSavedAlbums(ids)
                }
            }
            albumCache.invalidateAll()
            savedAlbumIds.value -= ids.toSet()
        }
    }

    suspend fun isSavedArtists(ids: List<String>): List<Boolean> {
        val unknownIds = ids.filterNot { savedArtistIds.value.contains(it) }

        if (unknownIds.isEmpty()) {
            return ids.map { true }
        }

        val unknownStates = authPlugin?.let { plugin ->
            val savedStates = pluginManager.withScope {
                plugin.use {
                    metadataArtistAPI.isSavedArtists(unknownIds)
                }
            }
            val savedIds = unknownIds.filterIndexed { index, id ->
                savedStates.getOrNull(index) == true
            }.toSet()
            savedArtistIds.value += savedIds
            savedStates
        } ?: unknownIds.map { false }

        return ids.map { id ->
            savedArtistIds.value.contains(id) ||
                    unknownStates.getOrNull(unknownIds.indexOf(id)) ?: false
        }
    }

    suspend fun saveArtists(ids: List<String>) {
        authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    metadataArtistAPI.saveArtists(ids)
                }
            }
            artistCache.invalidateAll()
            savedArtistIds.value += ids.toSet()
        }
    }

    suspend fun removeSavedArtists(ids: List<String>) {
        authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    metadataArtistAPI.removeSavedArtists(ids)
                }
            }
            artistCache.invalidateAll()
            savedArtistIds.value -= ids.toSet()
        }
    }

    suspend fun currentUser(): MetadataUser? {
        return authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    metadataUserAPI.getUser("")
                }
            }
        }
    }

    suspend fun createPlaylist(
        name: String,
        description: String?,
        isPublic: Boolean,
        isCollaborating: Boolean,
        imageBase64: String,
        trackIds: List<String>): MetadataPlaylist? {
        return authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    val result = metadataPlaylistAPI.createPlaylist(
                        name = name,
                        description = description,
                        isPublic = isPublic,
                        isCollaborating = isCollaborating,
                        imageBase64 = imageBase64,
                        trackIds = trackIds)
                    playlistCache.invalidateAll()
                    savedPlaylistIds.value += result.id
                    result
                }
            }
        }
    }

    suspend fun updatePlaylist(
        id: String,
        name: String?,
        description: String?,
        isPublic: Boolean?,
        isCollaborating: Boolean?,
        imageBase64: String?,
        trackIds: List<String>?): MetadataPlaylist? {
        return authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    val result = metadataPlaylistAPI.updatePlaylist(
                        id = id,
                        name = name,
                        description = description,
                        isPublic = isPublic,
                        isCollaborating = isCollaborating,
                        imageBase64 = imageBase64,
                        trackIds = trackIds)
                    playlistCache.invalidateAll()
                    result
                }
            }
        }
    }

    suspend fun addTracksToPlaylist(playlistId: String, trackIds: List<String>) {
        authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    metadataPlaylistAPI.addTracksToPlaylist(playlistId, trackIds)
                }
            }
            playlistCache.invalidateAll()
        }
    }
}
