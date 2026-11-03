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

package org.spotube.spotube.modules.library.local_tracks.media

import kotlinx.serialization.Serializable

@Serializable
data class LocalMediaTrack(
    val path: String,
    val name: String,
    val artists: List<String>,
    val durationMs: Long,
    val album: String? = null,
    val coverBytes: ByteArray? = null)

@Serializable
data class LocalMediaFolder(
    val path: String,
    val name: String,
    val tracks: List<LocalMediaTrack>) {
    val trackCount: Int
        get() = tracks.size
}

@Serializable
data class LocalMediaCache(
    val folders: List<LocalMediaFolder> = emptyList(),
    val indexedAtEpochMs: Long = 0)
