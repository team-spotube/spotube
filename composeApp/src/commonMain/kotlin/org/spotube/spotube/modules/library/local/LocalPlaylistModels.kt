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

import kotlinx.serialization.Serializable

/**
 * A user-created playlist of on-device tracks. Tracks are referenced by the path/URI used by
 * [org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack.path], so they stay in
 * sync with the local media index: files that disappear are simply skipped until re-indexed.
 */
@Serializable
data class LocalPlaylist(
    /** Stable unique id, always prefixed with [LOCAL_PLAYLIST_ID_PREFIX] to never collide with online playlist ids. */
    val id: String,
    val name: String,
    val description: String? = null,
    /** Downscaled JPEG cover, base64-encoded; `null` falls back to the first track's embedded art. */
    val coverBase64: String? = null,
    /** Track paths in user-chosen order; duplicates are not allowed. */
    val trackPaths: List<String> = emptyList(),
    val createdAtEpochMs: Long = 0)

const val LOCAL_PLAYLIST_ID_PREFIX = "local:"

fun isLocalPlaylistId(id: String): Boolean = id.startsWith(LOCAL_PLAYLIST_ID_PREFIX)
