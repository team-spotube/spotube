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

package org.spotube.plugin_interfaces.plugin_apis.scrobble

import kotlinx.serialization.Serializable

@Serializable
data class ScrobbleTrack(
    val timestamp: Long,
    val trackId: String,
    val artistId: String,
    val albumId: String?,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val streamingProvider: String, // e.g. "greedy_green_corp", "apple_music" etc
    val durationMs: Long? = null,
)