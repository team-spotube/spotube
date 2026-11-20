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

package org.spotube.spotube.modules.library.local_tracks

import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaFolder
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack

data class LocalFolderUi(
    val path: String,
    val name: String,
    val trackCount: Int,
    val artwork: ByteArray?
)

data class LocalTrackUi(
    val path: String,
    val title: String,
    val subtitle: String?,
    val durationLabel: String?,
    val artwork: ByteArray?
)

data class LibraryLocalTracksState(
    val isRefreshing: Boolean = false,
    val folders: List<LocalMediaFolder> = emptyList(),
    val currentFolderPath: String? = null,
    val query: String = "",
    val error: String? = null,
    val canAccessMedia: Boolean = true,
    val visibleFolders: List<LocalFolderUi> = emptyList(),
    val visibleTracks: List<LocalTrackUi> = emptyList(),
    val currentFolder: LocalMediaFolder? = null,
    val totalFolderCount: Int = 0,
    val totalTrackCount: Int = 0,
    val canPlayFolder: Boolean = false
)

internal fun localTracksUiState(
    source: LibraryLocalTracksState,
    query: String
): LibraryLocalTracksState {
    val normalized = query.trim()
    val folder = source.folders.firstOrNull { it.path == source.currentFolderPath }
    val folders = source.folders.filter {
        normalized.isEmpty() || it.name.contains(
            normalized,
            true
        ) || it.tracks.any { track -> localTrackMatches(track, normalized) }
    }
    val tracks = folder?.tracks.orEmpty().filter { localTrackMatches(it, normalized) }
    return source.copy(
        query = query,
        currentFolder = folder,
        visibleFolders = folders.map {
            LocalFolderUi(
                it.path,
                it.name,
                it.trackCount,
                it.tracks.firstOrNull { track -> track.coverBytes?.isNotEmpty() == true }?.coverBytes
            )
        },
        visibleTracks = tracks.map { track ->
            val seconds = track.durationMs / 1000
            LocalTrackUi(
                track.path, track.name,
                listOfNotNull(
                    track.artists.joinToString(", ").takeIf { it.isNotBlank() },
                    track.album?.takeIf { it.isNotBlank() }).joinToString(" • ")
                    .takeIf { it.isNotBlank() },
                if (seconds > 0) "${seconds / 60}:${
                    (seconds % 60).toString().padStart(2, '0')
                }" else null,
                track.coverBytes?.takeIf { it.isNotEmpty() })
        },
        totalFolderCount = source.folders.size,
        totalTrackCount = source.folders.sumOf { it.trackCount },
        canPlayFolder = source.canAccessMedia && tracks.isNotEmpty()
    )
}

private fun localTrackMatches(track: LocalMediaTrack, query: String): Boolean = query.isBlank() ||
        track.name.contains(query, true) || track.album?.contains(
    query,
    true
) == true || track.artists.any { it.contains(query, true) }
