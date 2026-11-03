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

package org.spotube.plugin_interfaces.plugin_apis.metadata.playlist

import app.cash.zipline.ZiplineService
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

const val MetadataPlaylistAPI_SERVICE_NAME = "MetadataPlaylistAPI"

interface MetadataPlaylistAPI : ZiplineService {
    suspend fun getPlaylist(id: String): MetadataPlaylist

    suspend fun getPlaylistTracks(
        id: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataTrack>

    suspend fun savedPlaylists(
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataPlaylist>

    suspend fun isSavedPlaylists(ids: List<String>): List<Boolean>
    suspend fun savePlaylists(ids: List<String>)
    suspend fun removeSavedPlaylists(ids: List<String>)

    suspend fun createPlaylist(
        name: String,
        description: String?,
        isPublic: Boolean,
        isCollaborating: Boolean,
        imageBase64: String,
        trackIds: List<String>,
    ): MetadataPlaylist

    suspend fun updatePlaylist(
        id: String,
        name: String?,
        description: String?,
        isPublic: Boolean?,
        isCollaborating: Boolean?,
        imageBase64: String?,
        trackIds: List<String>?
    ): MetadataPlaylist

    suspend fun deletePlaylist(id: String)

    suspend fun addTracksToPlaylist(playlistId: String, trackIds: List<String>)
    suspend fun removeTracksFromPlaylist(playlistId: String, trackIds: List<String>)
}