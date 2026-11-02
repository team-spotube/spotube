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

package org.spotube.spotube.modules.saved_tracks

import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
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
class SavedTracksRepository(
    val pluginManager: PluginManager
) {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val plugin
        get() = pluginManager.selectedMetadataPlugin.value

    private val savedTracksCache =
        Cache.Builder<PaginationStrategy, PaginationResult<MetadataTrack>>().build()
    private val totalCountCache = Cache.Builder<String, Int>().build()
    private val savedTrackIds = MutableStateFlow<Set<String>>(emptySet())

    val savedTracksIdsFlow : StateFlow<Set<String>> = savedTrackIds.asStateFlow()

    /**
     * Bumped only when the library itself changes (save, remove, re-login). [savedTracksIdsFlow]
     * also grows while pages and saved-states are fetched, so reloading the list on it restarted
     * paging on every page (#3200).
     */
    private val _libraryChanges = MutableStateFlow(0)
    val libraryChanges: StateFlow<Int> = _libraryChanges.asStateFlow()

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
        savedTracksCache.invalidateAll()
        totalCountCache.invalidateAll()
        savedTrackIds.value = emptySet()
        _libraryChanges.value++
    }

    suspend fun getSavedTracks(paginationStrategy: PaginationStrategy? = null) =
        authPlugin?.let { plugin ->
            val strategy = paginationStrategy ?: PaginationStrategy.Offset(0, 50)
            savedTracksCache.get(strategy) {
                val tracks = pluginManager.withScope {
                    plugin.use {
                        val tracks = metadataTrackAPI.savedTracks(paginationStrategy)
                        tracks
                    }
                }
                savedTrackIds.value += tracks.items.map { it.id }.toSet()
                tracks
            }
        }

    suspend fun getSavedTracksCount(): Int? = plugin?.let { plugin ->
        totalCountCache.get("count") {
            pluginManager.withScope {
                plugin.use {
                    val result = metadataTrackAPI.savedTracks(PaginationStrategy.Offset(0, 1))
                    result.totalCount
                }
            }
        }
    }

    suspend fun saveTracks(ids: List<String>) {
        authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    metadataTrackAPI.saveTracks(ids)
                }
            }
            savedTracksCache.invalidateAll()
            totalCountCache.invalidateAll()
            savedTrackIds.value += ids.toSet()
            _libraryChanges.value++
        }
    }

    suspend fun removeSavedTracks(ids: List<String>) {
        authPlugin?.let { plugin ->
            pluginManager.withScope {
                plugin.use {
                    metadataTrackAPI.removeSavedTracks(ids)
                }
            }
            savedTracksCache.invalidateAll()
            totalCountCache.invalidateAll()
            savedTrackIds.value -= ids.toSet()
            _libraryChanges.value++
        }
    }

    suspend fun isSavedTracks(ids: List<String>): List<Boolean> {
        // Filter out ids that are already known to be saved
        val unknownIds = ids.filterNot { savedTrackIds.value.contains(it) }

        if(unknownIds.isEmpty()) {
            return ids.map { true }
        }

        val unknownStates = plugin?.let { plugin ->
            val savedStates = pluginManager.withScope {
                plugin.use {
                    metadataTrackAPI.isSavedTracks(unknownIds)
                }
            }
            val savedIds =
                unknownIds.filterIndexed { index, string -> savedStates.getOrNull(index) == true }
                    .toSet()
            savedTrackIds.value += savedIds
            savedStates
        } ?: unknownIds.map { false }

        // Combine known saved states with unknown states
        return ids.map { id ->
            savedTrackIds.value.contains(id) || unknownStates.getOrNull(unknownIds.indexOf(id)) ?: false
        }
    }
}
