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

package org.spotube.js_plugin_example.plugin_apis.metadata

import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

class RealMetadataPlaylistAPI : MetadataPlaylistAPI {

    override suspend fun getPlaylist(id: String): MetadataPlaylist {
        return FakeMetadataStore.getPlaylist(id)
    }

    override suspend fun getPlaylistTracks(
        id: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataTrack> {
        return FakeMetadataStore.paginate(FakeMetadataStore.getPlaylistTracks(id), pagination)
    }

    override suspend fun savedPlaylists(
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataPlaylist> {
        return FakeMetadataStore.paginate(FakeMetadataStore.savedPlaylists(), pagination)
    }

    override suspend fun isSavedPlaylists(ids: List<String>): List<Boolean> {
        return FakeMetadataStore.isSavedPlaylists(ids)
    }

    override suspend fun savePlaylists(ids: List<String>) {
        FakeMetadataStore.savePlaylists(ids)
    }

    override suspend fun removeSavedPlaylists(ids: List<String>) {
        FakeMetadataStore.removeSavedPlaylists(ids)
    }

    override suspend fun createPlaylist(
        name: String,
        description: String?,
        isPublic: Boolean,
        isCollaborating: Boolean,
        imageBase64: String,
        trackIds: List<String>
    ): MetadataPlaylist {
        return FakeMetadataStore.createPlaylist(
            name = name,
            description = description,
            isPublic = isPublic,
            isCollaborating = isCollaborating,
            imageBase64 = imageBase64,
            trackIds = trackIds,
        )
    }

    override suspend fun updatePlaylist(
        id: String,
        name: String?,
        description: String?,
        isPublic: Boolean?,
        isCollaborating: Boolean?,
        imageBase64: String?,
        trackIds: List<String>?
    ): MetadataPlaylist {
        return FakeMetadataStore.updatePlaylist(
            id = id,
            name = name,
            description = description,
            isPublic = isPublic,
            isCollaborating = isCollaborating,
            imageBase64 = imageBase64,
            trackIds = trackIds,
        )
    }

    override suspend fun deletePlaylist(id: String) {
        FakeMetadataStore.deletePlaylist(id)
    }

    override suspend fun addTracksToPlaylist(
        playlistId: String,
        trackIds: List<String>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun removeTracksFromPlaylist(
        playlistId: String,
        trackIds: List<String>
    ) {
        TODO("Not yet implemented")
    }
}
