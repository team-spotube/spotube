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

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import org.spotube.spotube.core.db.DatabaseKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Owns the user's local playlists (a JSON document in the local playlists DataStore, following
 * [org.spotube.spotube.modules.library.local_tracks.media.LocalMediaCacheRepository]; wired to
 * [org.spotube.spotube.core.db.Database.localPlaylistsDataStore] in DI).
 *
 * Every mutation reads, transforms and writes the whole document inside a single
 * [androidx.datastore.preferences.core.Preferences.edit] so concurrent writers can't lose data.
 */
class LocalPlaylistRepository(private val store: DataStore<Preferences>) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    val playlistsFlow: Flow<List<LocalPlaylist>> = store.data
        .map { prefs ->
            prefs[DatabaseKeys.LOCAL_PLAYLISTS_STATE_KEY]?.let { raw ->
                runCatching { json.decodeFromString<List<LocalPlaylist>>(raw) }
                    .getOrDefault(emptyList())
            } ?: emptyList()
        }
        // A corrupted store file must degrade to "no playlists" instead of crashing every
        // collector; the next successful edit rewrites the file.
        .catch { emit(emptyList()) }

    suspend fun create(
        name: String,
        description: String? = null,
        coverBase64: String? = null,
        trackPaths: List<String> = emptyList()): LocalPlaylist {
        val playlist = LocalPlaylist(
            id = newLocalPlaylistId(),
            name = name.trim(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            coverBase64 = coverBase64?.takeIf { it.isNotBlank() },
            trackPaths = trackPaths.distinct(),
            createdAtEpochMs = Clock.System.now().toEpochMilliseconds())
        mutate { it + playlist }
        return playlist
    }

    /**
     * Applies [transform] to the playlist with [id]. Returning null from the transform removes
     * the playlist. The store is only written when the playlist was found (and changed).
     */
    suspend fun update(id: String, transform: (LocalPlaylist) -> LocalPlaylist?) {
        mutate { playlists ->
            val index = playlists.indexOfFirst { it.id == id }
            if (index < 0) return@mutate playlists
            val updated = transform(playlists[index]) ?: return@mutate playlists.filterNot { it.id == id }
            playlists.toMutableList().apply { set(index, updated) }
        }
    }

    suspend fun rename(id: String, name: String, description: String?, coverBase64: String?) =
        update(id) { playlist ->
            playlist.copy(
                name = name.trim().ifBlank { playlist.name },
                description = description?.trim()?.takeIf { it.isNotBlank() },
                // A null coverBase64 keeps the current cover; an empty string removes it.
                coverBase64 = when (coverBase64) {
                    null -> playlist.coverBase64
                    "" -> null
                    else -> coverBase64
                })
        }

    suspend fun delete(id: String) {
        mutate { playlists -> playlists.filterNot { it.id == id } }
    }

    /** Appends tracks that aren't in the playlist yet, keeping the existing order. */
    suspend fun addTracks(id: String, trackPaths: List<String>) {
        if (trackPaths.isEmpty()) return
        update(id) { playlist ->
            playlist.copy(trackPaths = playlist.trackPaths + trackPaths.filterNot { it in playlist.trackPaths })
        }
    }

    suspend fun removeTrack(id: String, trackPath: String) {
        update(id) { playlist ->
            playlist.copy(trackPaths = playlist.trackPaths.filterNot { it == trackPath })
        }
    }

    /** Rewrite references after physical file moves, without changing playlist order. */
    suspend fun remapMovedTracks(destinations: Map<String, String>) {
        if (destinations.isEmpty()) return
        mutate { playlists ->
            playlists.map { playlist ->
                playlist.copy(trackPaths = playlist.trackPaths.map { destinations[it] ?: it }.distinct())
            }
        }
    }

    suspend fun moveTrack(id: String, fromIndex: Int, toIndex: Int) {
        update(id) { playlist ->
            val paths = playlist.trackPaths.toMutableList()
            if (fromIndex in paths.indices && toIndex in paths.indices) {
                paths.add(toIndex, paths.removeAt(fromIndex))
                playlist.copy(trackPaths = paths)
            } else {
                playlist
            }
        }
    }

    private suspend fun mutate(transform: (List<LocalPlaylist>) -> List<LocalPlaylist>) {
        store.edit { prefs ->
            val current = prefs[DatabaseKeys.LOCAL_PLAYLISTS_STATE_KEY]?.let { raw ->
                runCatching { json.decodeFromString<List<LocalPlaylist>>(raw) }.getOrNull()
            } ?: emptyList()
            val next = transform(current)
            prefs[DatabaseKeys.LOCAL_PLAYLISTS_STATE_KEY] = json.encodeToString(next)
        }
    }

    private fun newLocalPlaylistId(): String =
        LOCAL_PLAYLIST_ID_PREFIX + Random.Default.nextBytes(16).joinToString("") {
            it.toString(16).padStart(2, '0')
        }
}
