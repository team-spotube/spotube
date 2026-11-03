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

package org.spotube.plugin_interfaces.plugin_apis.metadata.artist

import app.cash.zipline.ZiplineService
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

const val MetadataArtistAPI_SERVICE_NAME = "MetadataArtistAPI"

interface MetadataArtistAPI : ZiplineService {
    suspend fun getArtist(id: String): MetadataArtist.Detailed
    suspend fun artistOverview(id: String): MetadataArtistOverview
    suspend fun getArtistTop10Tracks(id: String): List<MetadataTrack>

    suspend fun relatedArtists(
        id: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataArtist.Basic>

    suspend fun featuredPlaylists(
        id: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataPlaylist>

    suspend fun getArtistAlbums(
        id: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataAlbum.Detailed>

    suspend fun savedArtists(pagination: PaginationStrategy? = null): PaginationResult<MetadataArtist.Detailed>

    suspend fun isSavedArtists(ids: List<String>): List<Boolean>
    suspend fun saveArtists(ids: List<String>)
    suspend fun removeSavedArtists(ids: List<String>)
}