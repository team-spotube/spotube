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

package org.spotube.spotube.modules.library.local

import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaLibraryCoordinator
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Turns the raw local media index (folders of [LocalMediaTrack]s) into the derived collections
 * the library shows: a flat track list, synthetic albums and synthetic artists. The grouping is
 * redone off the main thread whenever the index changes and never during composition.
 */
class LocalMediaCollectionsRepository(
    coordinator: LocalMediaLibraryCoordinator
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Every indexed track, keyed by path for playlist resolution and pickers. */
    val tracks: StateFlow<List<LocalMediaTrack>> = coordinator.cacheState
        .map { cache -> cache.folders.flatMap { it.tracks } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<LocalMediaAlbum>> = coordinator.cacheState
        .map { cache -> buildLocalAlbums(cache.folders.flatMap { it.tracks }) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<LocalMediaArtist>> = coordinator.cacheState
        .map { cache -> buildLocalArtists(cache.folders.flatMap { it.tracks }) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun findAlbum(key: String): LocalMediaAlbum? =
        albums.value.firstOrNull { it.key == key.trim().lowercase() }

    fun findArtist(key: String): LocalMediaArtist? =
        artists.value.firstOrNull { it.key == key.trim().lowercase() }

    /** Emits the album with [key] whenever the index changes; null while it's not indexed. */
    fun albumFlow(key: String) = albums.map { list ->
        list.firstOrNull { it.key == key.trim().lowercase() }
    }

    /** Emits the artist with [key] whenever the index changes; null while it's not indexed. */
    fun artistFlow(key: String) = artists.map { list ->
        list.firstOrNull { it.key == key.trim().lowercase() }
    }
}
